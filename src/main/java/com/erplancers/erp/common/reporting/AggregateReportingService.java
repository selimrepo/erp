package com.erplancers.erp.common.reporting;

import com.erplancers.erp.common.web.CrudRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.IdentifiableType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic database-backed ERP BI aggregation engine.
 *
 * <p>Paths may traverse JPA relationships, for example:
 * {@code customer.name}, {@code product.productName} or
 * {@code invoice.invoiceStatus}. Associations are joined automatically,
 * including collection-valued associations.</p>
 *
 * <p>This keeps BI generic: dashboards do not need a report class for every
 * customer/product/sales combination.</p>
 */
@Service
@Transactional(readOnly = true)
public class AggregateReportingService {

    private static final int MAX_LIMIT = 1000;

    private final EntityManager entityManager;
    private final CrudRegistry registry;

    public AggregateReportingService(EntityManager entityManager, CrudRegistry registry) {
        this.entityManager = entityManager;
        this.registry = registry;
    }

    public AggregateReport aggregate(String slug, AggregateRequest request) {
        CrudRegistry.CrudDescriptor descriptor = descriptor(slug);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<?> root = query.from(descriptor.entityType());

        Map<String, From<?, ?>> joins = new LinkedHashMap<>();
        List<Expression<?>> selections = new ArrayList<>();
        List<Expression<?>> groups = new ArrayList<>();

        for (String field : request.groupBy()) {
            Expression<?> expression = resolvePath(root, field, joins);
            selections.add(expression);
            groups.add(expression);
        }

        List<String> metrics = request.metrics().isEmpty()
                ? List.of("count")
                : request.metrics();

        Map<String, Expression<?>> metricExpressions = new LinkedHashMap<>();
        for (String metric : metrics) {
            Expression<?> expression = metricExpression(cb, root, metric, joins);
            selections.add(expression);
            metricExpressions.put(metric, expression);
        }

        query.multiselect(selections.toArray(Expression[]::new));

        Predicate predicate = predicates(cb, root, request.filters(), joins);
        if (predicate != null) {
            query.where(predicate);
        }

        if (!groups.isEmpty()) {
            query.groupBy(groups);
        }

        applyOrder(query, cb, root, request.orderBy(), metricExpressions, joins);

        int limit = Math.max(1, Math.min(request.limit(), MAX_LIMIT));
        List<Object[]> rows = entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();

        List<Map<String, Object>> data = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> values = new LinkedHashMap<>();
            int index = 0;

            for (String field : request.groupBy()) {
                values.put(field, row[index++]);
            }
            for (String metric : metrics) {
                values.put(metric, row[index++]);
            }

            data.add(values);
        }

        return new AggregateReport(
                slug,
                descriptor.module(),
                request.groupBy(),
                metrics,
                request.filters(),
                data);
    }

    private void applyOrder(
            CriteriaQuery<Object[]> query,
            CriteriaBuilder cb,
            Root<?> root,
            String orderBy,
            Map<String, Expression<?>> metricExpressions,
            Map<String, From<?, ?>> joins) {

        if (orderBy == null || orderBy.isBlank()) {
            return;
        }

        String target = orderBy;
        boolean descending = false;

        int lastColon = orderBy.lastIndexOf(':');
        if (lastColon > 0) {
            String direction = orderBy.substring(lastColon + 1);
            if ("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction)) {
                target = orderBy.substring(0, lastColon);
                descending = "desc".equalsIgnoreCase(direction);
            }
        }

        Expression<?> expression = metricExpressions.get(target);
        if (expression == null) {
            if ("count".equalsIgnoreCase(target)) {
                expression = metricExpression(cb, root, "count", joins);
            } else {
                expression = resolvePath(root, target, joins);
            }
        }

        query.orderBy(descending ? cb.desc(expression) : cb.asc(expression));
    }

    private Expression<?> metricExpression(
            CriteriaBuilder cb,
            Root<?> root,
            String metric,
            Map<String, From<?, ?>> joins) {

        String normalized = metric.trim().toLowerCase();

        if ("count".equals(normalized) || "count(*)".equals(normalized)) {
            return cb.count(root);
        }

        int colon = normalized.indexOf(':');
        if (colon <= 0 || colon == normalized.length() - 1) {
            throw new IllegalArgumentException(
                    "Metric must be count or function:field " +
                    "(sum, avg, min, max, countDistinct)");
        }

        String function = normalized.substring(0, colon);
        String field = metric.substring(colon + 1);
        Expression<?> expression = resolvePath(root, field, joins);

        if (!isNumber(expression.getJavaType())
                && !"countdistinct".equals(function)
                && !"count_distinct".equals(function)) {
            throw new IllegalArgumentException(
                    "Metric " + metric + " requires a numeric field");
        }

        return switch (function) {
            case "sum" -> sum(cb, expression);
            case "avg" -> cb.avg(asNumber(expression));
            case "min" -> cb.min(asNumber(expression));
            case "max" -> cb.max(asNumber(expression));
            case "countdistinct", "count_distinct" -> cb.countDistinct(expression);
            default -> throw new IllegalArgumentException(
                    "Unsupported metric: " + function);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Number> asNumber(Expression<?> expression) {
        return (Expression) expression;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Number> sum(
            CriteriaBuilder cb,
            Expression<?> expression) {
        return cb.sum((Expression) expression);
    }

    private Predicate predicates(
            CriteriaBuilder cb,
            Root<?> root,
            Map<String, String> filters,
            Map<String, From<?, ?>> joins) {

        if (filters == null || filters.isEmpty()) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String operator = "eq";
            String field = key;

            int colon = key.lastIndexOf(':');
            if (colon > 0) {
                field = key.substring(0, colon);
                operator = key.substring(colon + 1).toLowerCase();
            }

            Expression<?> expression = resolvePath(root, field, joins);
            Class<?> type = expression.getJavaType();
            Object typed = convert(value, type);

            predicates.add(switch (operator) {
                case "eq" -> cb.equal(expression, typed);
                case "ne" -> cb.notEqual(expression, typed);
                case "like" -> cb.like(
                        cb.lower(expression.as(String.class)),
                        value.toLowerCase());
                case "gt" -> cb.greaterThan(
                        asComparable(expression), (Comparable) typed);
                case "gte" -> cb.greaterThanOrEqualTo(
                        asComparable(expression), (Comparable) typed);
                case "lt" -> cb.lessThan(
                        asComparable(expression), (Comparable) typed);
                case "lte" -> cb.lessThanOrEqualTo(
                        asComparable(expression), (Comparable) typed);
                case "isnull" -> cb.isNull(expression);
                case "notnull" -> cb.isNotNull(expression);
                default -> throw new IllegalArgumentException(
                        "Unsupported filter operator: " + operator);
            });
        }

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    /**
     * Resolves a scalar JPA path and automatically creates LEFT joins for
     * relationship segments. Collection relationships are therefore usable
     * from generic BI requests without hand-written JPQL.
     */
    private Expression<?> resolvePath(
            Root<?> root,
            String path,
            Map<String, From<?, ?>> joins) {

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("BI field path cannot be blank");
        }

        String[] parts = path.split("\\.");
        From<?, ?> current = root;
        StringBuilder joinPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                throw new IllegalArgumentException("Invalid BI field path: " + path);
            }

            ManagedType<?> managed = entityManager.getMetamodel()
                    .managedType(current.getJavaType());
            Attribute<?, ?> attribute;

            try {
                attribute = managed.getAttribute(part);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Unknown BI field '" + path + "' on "
                                + managed.getJavaType().getSimpleName(), ex);
            }

            boolean association = attribute.getPersistentAttributeType()
                    != Attribute.PersistentAttributeType.BASIC
                    && attribute.getPersistentAttributeType()
                    != Attribute.PersistentAttributeType.EMBEDDED;

            if (!association) {
                if (i != parts.length - 1) {
                    throw new IllegalArgumentException(
                            "BI path '" + path + "' traverses through scalar field '" + part + "'");
                }
                return current.get(part);
            }

            if (joinPath.length() > 0) {
                joinPath.append('.');
            }
            joinPath.append(part);

            String key = joinPath.toString();
            From<?, ?> join = joins.get(key);
            if (join == null) {
                join = current.join(part, JoinType.LEFT);
                joins.put(key, join);
            }
            current = join;

            // A terminal relationship is a valid OLAP dimension, but JPA
            // cannot portably GROUP BY the entity-valued expression itself.
            // Resolve it to the target entity identifier so singular and
            // collection-valued aggregate properties can be selected as
            // dimensions (for example, customer or orderLines).
            if (i == parts.length - 1) {
                ManagedType<?> targetType = entityManager.getMetamodel()
                        .managedType(current.getJavaType());
                try {
                    if (!(targetType instanceof IdentifiableType<?> identifiableType)) {
                        throw new IllegalArgumentException("Target is not an identifiable entity");
                    }
                    return current.get(
                            identifiableType.getId(identifiableType.getIdType().getJavaType()).getName());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            "BI relationship dimension '" + path
                                    + "' must target an entity with a single identifier", ex);
                }
            }
        }

        return current;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Comparable> asComparable(Expression<?> expression) {
        return (Expression) expression;
    }

    private Object convert(String value, Class<?> type) {
        if (value == null || "null".equalsIgnoreCase(value)) {
            return null;
        }
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.valueOf(value);
        if (type == Long.class || type == long.class) return Long.valueOf(value);
        if (type == Double.class || type == double.class) return Double.valueOf(value);
        if (type == Float.class || type == float.class) return Float.valueOf(value);
        if (type == Short.class || type == short.class) return Short.valueOf(value);
        if (type == BigDecimal.class) return new BigDecimal(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.valueOf(value);
        if (type == LocalDate.class) return LocalDate.parse(value);
        if (type == LocalDateTime.class) return LocalDateTime.parse(value);
        if (type == Instant.class) return Instant.parse(value);
        if (type == OffsetDateTime.class) return OffsetDateTime.parse(value);
        if (type == ZonedDateTime.class) return ZonedDateTime.parse(value);
        if (type == OffsetTime.class) return OffsetTime.parse(value);

        if (type.isEnum()) {
            return Arrays.stream(type.getEnumConstants())
                    .filter(v -> v.toString().equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid " + type.getSimpleName() + ": " + value));
        }

        throw new IllegalArgumentException(
                "Unsupported BI filter type: " + type.getName());
    }

    private CrudRegistry.CrudDescriptor descriptor(String slug) {
        CrudRegistry.CrudDescriptor descriptor = registry.all().get(slug);
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "Unknown ERP resource: " + slug);
        }
        return descriptor;
    }

    private boolean isNumber(Class<?> type) {
        return type != null
                && (Number.class.isAssignableFrom(type)
                || type == byte.class
                || type == short.class
                || type == int.class
                || type == long.class
                || type == float.class
                || type == double.class);
    }

    public record AggregateRequest(
            List<String> groupBy,
            List<String> metrics,
            Map<String, String> filters,
            String orderBy,
            int limit) {

        public AggregateRequest {
            groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
            metrics = metrics == null ? List.of("count") : List.copyOf(metrics);
            filters = filters == null ? Map.of() : Map.copyOf(filters);
            orderBy = orderBy == null ? "" : orderBy;
            limit = limit <= 0 ? 100 : limit;
        }
    }

    public record AggregateReport(
            String resource,
            String module,
            List<String> groupBy,
            List<String> metrics,
            Map<String, String> filters,
            List<Map<String, Object>> data) {
    }
}
