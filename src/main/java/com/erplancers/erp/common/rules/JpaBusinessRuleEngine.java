package com.erplancers.erp.common.rules;

import com.erplancers.erp.common.web.CrudRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metadata-driven business rules evaluated against JPA entities and their
 * relationship graph. Expressions deliberately use a small, deterministic
 * expression language rather than arbitrary Java/script execution.
 */
@Service
public class JpaBusinessRuleEngine {

    private final EntityManager entityManager;
    private final CrudRegistry registry;

    public JpaBusinessRuleEngine(EntityManager entityManager, CrudRegistry registry) {
        this.entityManager = entityManager;
        this.registry = registry;
    }

    @Transactional(readOnly = true)
    public RuleSchema schema(String resource, int relationDepth) {
        Class<?> rootType = registry.get(resource).entityType();
        List<RelationPath> paths = new ArrayList<>();
        discover(rootType, "", 0, normalizeDepth(relationDepth), new HashSet<>(), paths);
        return new RuleSchema(resource, rootType.getName(), paths);
    }

    @Transactional(readOnly = true)
    public EvaluationResult evaluate(String resource, Long id, RuleSet ruleset) {
        Object entity = load(resource, id);
        return evaluateEntity(resource, entity, ruleset, false);
    }

    @Transactional
    public EvaluationResult execute(String resource, Long id, RuleSet ruleset) {
        Object entity = load(resource, id);
        return evaluateEntity(resource, entity, ruleset, true);
    }

    public EvaluationResult evaluateEntity(String resource, Object entity, RuleSet ruleset, boolean persist) {
        Objects.requireNonNull(entity, "entity");
        validateRuleset(resource, ruleset);

        Map<String, Object> values = new LinkedHashMap<>();
        List<String> actions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        List<Rule> ordered = ruleset.rules() == null ? List.of() :
                ruleset.rules().stream()
                        .sorted(Comparator.comparingInt(Rule::priority).reversed())
                        .toList();

        for (Rule rule : ordered) {
            if (!conditionMatches(entity, rule.condition())) continue;

            for (Action action : nullSafe(rule.actions())) {
                String type = action.type().toUpperCase(Locale.ROOT);
                try {
                    switch (type) {
                        case "CALCULATE", "SET" -> {
                            Object value = expression(entity, action.expression());
                            setPath(entity, action.target(), value);
                            values.put(action.target(), value);
                            actions.add(type + ":" + action.target());
                        }
                        case "REJECT", "VALIDATE" -> {
                            String message = action.message().isBlank()
                                    ? "Business rule rejected the operation"
                                    : action.message();
                            errors.add(message);
                            actions.add("REJECT:" + message);
                        }
                        case "LOG" -> actions.add("LOG:" + action.message());
                        default -> throw new IllegalArgumentException(
                                "Unsupported business rule action: " + action.type());
                    }
                } catch (RuntimeException ex) {
                    errors.add(rule.name() + ": " + ex.getMessage());
                }
            }
        }

        if (persist && errors.isEmpty() && !values.isEmpty()) {
            entityManager.merge(entity);
        }

        return new EvaluationResult(
                errors.isEmpty(),
                values,
                List.copyOf(actions),
                List.copyOf(errors));
    }

    private Object load(String resource, Long id) {
        if (id == null) throw new IllegalArgumentException("Entity id is required");
        Class<?> type = registry.get(resource).entityType();
        Object entity = entityManager.find(type, id);
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Entity not found: " + resource + "/" + id);
        }
        return entity;
    }

    private void validateRuleset(String resource, RuleSet ruleset) {
        if (ruleset == null) throw new IllegalArgumentException("Ruleset is required");
        if (ruleset.entity() != null && !ruleset.entity().isBlank()
                && !ruleset.entity().equals(resource)) {
            throw new IllegalArgumentException(
                    "Ruleset entity '" + ruleset.entity() + "' does not match '" + resource + "'");
        }
        for (Rule rule : nullSafe(ruleset.rules())) {
            if (rule.condition() != null) validatePath(resource, rule.condition().path());
            for (Action action : nullSafe(rule.actions())) {
                if (!action.target().isBlank()
                        && ("SET".equalsIgnoreCase(action.type())
                        || "CALCULATE".equalsIgnoreCase(action.type()))) {
                    validateWritablePath(resource, action.target());
                }
                if (action.expression() != null && !action.expression().isBlank()) {
                    validateExpressionPaths(resource, action.expression());
                }
            }
        }
    }

    private boolean conditionMatches(Object entity, Condition condition) {
        if (condition == null || condition.path().isBlank()) return true;
        Object actual = value(entity, condition.path());
        String op = condition.operator().toUpperCase(Locale.ROOT);
        Object expected = condition.value();

        return switch (op) {
            case "EXISTS", "NOT_NULL" -> actual != null;
            case "NULL", "IS_NULL" -> actual == null;
            case "EQUALS", "==" -> compare(actual, expected) == 0;
            case "NOT_EQUALS", "!=" -> compare(actual, expected) != 0;
            case "GT", ">" -> compare(actual, expected) > 0;
            case "GTE", ">=" -> compare(actual, expected) >= 0;
            case "LT", "<" -> compare(actual, expected) < 0;
            case "LTE", "<=" -> compare(actual, expected) <= 0;
            case "CONTAINS" -> actual != null && String.valueOf(actual).contains(String.valueOf(expected));
            case "IN" -> expected instanceof Collection<?> c && c.stream()
                    .anyMatch(item -> compare(actual, item) == 0);
            default -> throw new IllegalArgumentException("Unsupported condition operator: " + condition.operator());
        };
    }

    private int compare(Object left, Object right) {
        if (left == null || right == null) return Objects.equals(left, right) ? 0 : left == null ? -1 : 1;
        if (left instanceof Number || right instanceof Number) {
            return decimal(left).compareTo(decimal(right));
        }
        if (left instanceof Enum<?> e) left = e.name();
        if (right instanceof Enum<?> e) right = e.name();
        if (left instanceof Comparable<?> comparable
                && left.getClass().isAssignableFrom(right.getClass())) {
            @SuppressWarnings("unchecked")
            int result = ((Comparable<Object>) comparable).compareTo(right);
            return result;
        }
        return String.valueOf(left).compareToIgnoreCase(String.valueOf(right));
    }

    private Object expression(Object entity, String expression) {
        String e = expression == null ? "" : expression.trim();
        if (e.isBlank()) return null;
        if (e.startsWith("SUM(") && e.endsWith(")")) return aggregate(entity, e.substring(4, e.length() - 1), "SUM");
        if (e.startsWith("AVG(") && e.endsWith(")")) return aggregate(entity, e.substring(4, e.length() - 1), "AVG");
        if (e.startsWith("MIN(") && e.endsWith(")")) return aggregate(entity, e.substring(4, e.length() - 1), "MIN");
        if (e.startsWith("MAX(") && e.endsWith(")")) return aggregate(entity, e.substring(4, e.length() - 1), "MAX");
        if (e.startsWith("COUNT(") && e.endsWith(")")) {
            Object value = value(entity, e.substring(6, e.length() - 1).trim());
            return value instanceof Collection<?> c ? c.size() : value == null ? 0 : 1;
        }
        return new ArithmeticParser(entity, e).parse();
    }

    private Object aggregate(Object entity, String path, String operation) {
        List<Object> values = flattenedValues(entity, path.trim());
        List<BigDecimal> numbers = values.stream().filter(Objects::nonNull)
                .map(this::decimal).toList();
        if (operation.equals("COUNT")) return values.size();
        if (numbers.isEmpty()) return BigDecimal.ZERO;
        return switch (operation) {
            case "SUM" -> numbers.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "AVG" -> numbers.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(numbers.size()), 8, RoundingMode.HALF_UP);
            case "MIN" -> numbers.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case "MAX" -> numbers.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            default -> throw new IllegalArgumentException("Unsupported aggregate: " + operation);
        };
    }

    private List<Object> flattenedValues(Object root, String path) {
        List<Object> current = List.of(root);
        for (String part : path.split("\\.")) {
            List<Object> next = new ArrayList<>();
            for (Object item : current) {
                if (item == null) continue;
                Object v = property(item, part);
                if (v instanceof Collection<?> c) next.addAll(c);
                else next.add(v);
            }
            current = next;
        }
        return current;
    }

    private Object value(Object entity, String path) {
        if (path == null || path.isBlank()) return entity;
        List<Object> values = flattenedValues(entity, path);
        if (values.isEmpty()) return null;
        return values.size() == 1 ? values.getFirst() : values;
    }

    private void setPath(Object root, String path, Object value) {
        String[] parts = path.split("\\.");
        if (parts.length != 1) {
            throw new IllegalArgumentException("Actions may only set root properties: " + path);
        }
        Field field = field(root.getClass(), parts[0]);
        try {
            field.setAccessible(true);
            field.set(root, coerce(value, field.getType()));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Cannot set property: " + path, ex);
        }
    }

    private void validateWritablePath(String resource, String path) {
        if (path == null || path.isBlank() || path.contains(".")) {
            throw new IllegalArgumentException("Rule action target must be a writable root property: " + path);
        }
        CrudRegistry.CrudDescriptor descriptor = registry.get(resource);
        Field field = field(descriptor.entityType(), path);
        if (java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
            throw new IllegalArgumentException("Rule action target is final: " + path);
        }
    }

    private void validateExpressionPaths(String resource, String expression) {
        String cleaned = expression.replaceAll("[+\\-*/(),]", " ");
        Set<String> functions = Set.of("SUM", "AVG", "MIN", "MAX", "COUNT");
        for (String token : cleaned.split("\\s+")) {
            if (token.isBlank() || token.matches("[0-9.]+") || token.equalsIgnoreCase("null")) continue;
            if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) continue;
            if (functions.contains(token.toUpperCase(Locale.ROOT))) continue;
            if (token.matches("[A-Za-z_][A-Za-z0-9_.]*")) validatePath(resource, token);
        }
    }

    private void validatePath(String resource, String path) {
        if (path == null || path.isBlank()) return;
        Class<?> current = registry.get(resource).entityType();
        String[] parts = path.split("\\.");
        for (String part : parts) {
            Attribute<?, ?> attribute = attribute(current, part);
            if (attribute == null) throw new IllegalArgumentException(
                    "Unknown JPA property path '" + path + "' on " + current.getSimpleName());
            current = targetType(attribute);
        }
    }

    private void discover(Class<?> type, String prefix, int depth, int max,
                          Set<Class<?>> visited, List<RelationPath> result) {
        if (depth > max || !visited.add(type)) return;
        ManagedType<?> managed = entityManager.getMetamodel().managedType(type);
        managed.getAttributes().stream().sorted(Comparator.comparing(Attribute::getName)).forEach(a -> {
            String path = prefix.isBlank() ? a.getName() : prefix + "." + a.getName();
            Class<?> target = targetType(a);
            boolean relation = a.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC;
            result.add(new RelationPath(path, a.getJavaType().getName(),
                    a.getPersistentAttributeType().name(), relation));
            if (relation && target != null && entityManager.getMetamodel().getManagedTypes().stream()
                    .anyMatch(m -> m.getJavaType().equals(target))) {
                discover(target, path, depth + 1, max, new HashSet<>(visited), result);
            }
        });
    }

    private Attribute<?, ?> attribute(Class<?> type, String name) {
        try {
            return entityManager.getMetamodel().managedType(type).getAttribute(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Class<?> targetType(Attribute<?, ?> a) {
        if (a instanceof PluralAttribute<?, ?, ?> p) return p.getElementType().getJavaType();
        return a.getJavaType();
    }

    private Object property(Object bean, String name) {
        try {
            Field field = field(bean.getClass(), name);
            field.setAccessible(true);
            return field.get(bean);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Cannot read property '" + name + "'", ex);
        }
    }

    private Field field(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new IllegalArgumentException("Unknown property '" + name + "' on " + type.getSimpleName());
    }

    private Object coerce(Object value, Class<?> target) {
        if (value == null) return null;
        if (target.isInstance(value)) return value;
        if (Number.class.isAssignableFrom(target) || target.isPrimitive()) {
            BigDecimal n = decimal(value);
            if (target == BigDecimal.class) return n;
            if (target == Integer.class || target == int.class) return n.intValue();
            if (target == Long.class || target == long.class) return n.longValue();
            if (target == Double.class || target == double.class) return n.doubleValue();
            if (target == Float.class || target == float.class) return n.floatValue();
            if (target == Short.class || target == short.class) return n.shortValue();
            if (target == Byte.class || target == byte.class) return n.byteValue();
        }
        if (target == String.class) return String.valueOf(value);
        if (target == Boolean.class || target == boolean.class) return Boolean.valueOf(String.valueOf(value));
        if (target.isEnum()) return Enum.valueOf(target.asSubclass(Enum.class), String.valueOf(value));
        return value;
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(String.valueOf(value));
    }

    private int normalizeDepth(int depth) {
        return Math.max(0, Math.min(depth, 6));
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private final class ArithmeticParser {
        private final Object entity;
        private final String input;
        private int pos;

        ArithmeticParser(Object entity, String input) { this.entity = entity; this.input = input; }

        Object parse() {
            Object result = expression();
            skip();
            if (pos != input.length()) throw new IllegalArgumentException("Invalid expression: " + input);
            return result;
        }

        private Object expression() {
            Object result = term();
            while (true) {
                skip();
                if (match('+')) result = decimal(result).add(decimal(term()));
                else if (match('-')) result = decimal(result).subtract(decimal(term()));
                else return result;
            }
        }

        private Object term() {
            Object result = factor();
            while (true) {
                skip();
                if (match('*')) result = decimal(result).multiply(decimal(factor()));
                else if (match('/')) result = decimal(result).divide(decimal(factor()), 8, RoundingMode.HALF_UP);
                else return result;
            }
        }

        private Object factor() {
            skip();
            if (match('(')) { Object v = expression(); require(')'); return v; }
            if (pos < input.length() && (input.charAt(pos) == '-' || Character.isDigit(input.charAt(pos)))) {
                int start = pos++;
                while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) pos++;
                return new BigDecimal(input.substring(start, pos));
            }
            int start = pos;
            while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos))
                    || input.charAt(pos) == '_' || input.charAt(pos) == '.')) pos++;
            if (start == pos) throw new IllegalArgumentException("Expected value in expression: " + input);
            return expressionValue(input.substring(start, pos));
        }

        private Object expressionValue(String token) {
            if (token.equalsIgnoreCase("true")) return true;
            if (token.equalsIgnoreCase("false")) return false;
            if (token.equalsIgnoreCase("null")) return null;
            return value(entity, token);
        }

        private boolean match(char c) {
            if (pos < input.length() && input.charAt(pos) == c) { pos++; return true; }
            return false;
        }

        private void require(char c) {
            if (!match(c)) throw new IllegalArgumentException("Expected '" + c + "' in expression: " + input);
        }

        private void skip() { while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++; }
    }

    public record RuleSet(String name, String entity, String trigger, List<Rule> rules) {}
    public record Rule(String name, int priority, Condition condition, List<Action> actions) {}
    public record Condition(String path, String operator, Object value) {}
    public record Action(String type, String target, String expression, String message) {
        public Action {
            type = type == null ? "" : type;
            target = target == null ? "" : target;
            expression = expression == null ? "" : expression;
            message = message == null ? "" : message;
        }
    }
    public record EvaluationResult(boolean valid, Map<String, Object> values,
                                    List<String> actions, List<String> errors) {}
    public record RuleSchema(String resource, String entity, List<RelationPath> paths) {}
    public record RelationPath(String path, String javaType, String persistentType, boolean relation) {}
}
