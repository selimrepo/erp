package com.erplancers.erp.common.reporting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates a deliberately small arithmetic language for BI measures.
 * No SQL, reflection, JavaScript, or arbitrary method invocation is accepted.
 * Base measure references use braces, e.g. {sum:revenue}; formulas are evaluated
 * over the already-authorized, already-aggregated result rows.
 */
public final class CalculatedMeasureEngine {
    private static final Pattern REFERENCE = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_.:-]{0,127})}");
    private static final Pattern TIME_FUNCTION = Pattern.compile(
            "(?i)(RUNNING_SUM|PREVIOUS|PERIOD_CHANGE|PERIOD_CHANGE_PCT)\\s*\\(\\s*\\{([A-Za-z_][A-Za-z0-9_.:-]{0,127})}\\s*\\)");
    private static final MathContext MC = MathContext.DECIMAL64;

    private CalculatedMeasureEngine() {}

    public static void apply(List<Map<String, Object>> rows, Map<String, String> formulas) {
        if (formulas == null || formulas.isEmpty() || rows == null || rows.isEmpty()) return;
        if (formulas.size() > 30) throw new IllegalArgumentException("At most 30 calculated measures are allowed");
        for (Map.Entry<String, String> entry : formulas.entrySet()) {
            String name = entry.getKey();
            String formula = entry.getValue();
            if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]{0,63}") || name.startsWith("_")) {
                throw new IllegalArgumentException("Invalid calculated measure name: " + name);
            }
            if (formula == null || formula.length() > 500) throw new IllegalArgumentException("Formula must be at most 500 characters");
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                String expression = replaceTimeFunctions(formula, rows, i);
                Map<String, BigDecimal> variables = new LinkedHashMap<>();
                Matcher matcher = REFERENCE.matcher(expression);
                StringBuffer replaced = new StringBuffer();
                int variableIndex = 0;
                while (matcher.find()) {
                    String reference = matcher.group(1);
                    if (!row.containsKey(reference)) {
                        throw new IllegalArgumentException("Calculated measure '" + name + "' references unavailable measure '" + reference + "'");
                    }
                    String variable = "v" + variableIndex++;
                    variables.put(variable, decimal(row.get(reference)));
                    matcher.appendReplacement(replaced, variable);
                }
                matcher.appendTail(replaced);
                BigDecimal value = new ArithmeticParser(replaced.toString(), variables).parse();
                row.put(name, value);
            }
        }
    }

    private static String replaceTimeFunctions(String formula, List<Map<String, Object>> rows, int index) {
        Matcher matcher = TIME_FUNCTION.matcher(formula);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String operation = matcher.group(1).toUpperCase(java.util.Locale.ROOT);
            String key = matcher.group(2);
            if (!rows.get(index).containsKey(key)) {
                throw new IllegalArgumentException("Time function references unavailable measure '" + key + "'");
            }
            BigDecimal current = decimal(rows.get(index).get(key));
            BigDecimal value;
            switch (operation) {
                case "RUNNING_SUM" -> {
                    value = BigDecimal.ZERO;
                    for (int j = 0; j <= index; j++) value = value.add(decimal(rows.get(j).get(key)), MC);
                }
                case "PREVIOUS" -> value = index == 0 ? BigDecimal.ZERO : decimal(rows.get(index - 1).get(key));
                case "PERIOD_CHANGE" -> value = index == 0 ? BigDecimal.ZERO : current.subtract(decimal(rows.get(index - 1).get(key)), MC);
                case "PERIOD_CHANGE_PCT" -> {
                    BigDecimal previous = index == 0 ? BigDecimal.ZERO : decimal(rows.get(index - 1).get(key));
                    value = previous.signum() == 0 ? BigDecimal.ZERO
                            : current.subtract(previous, MC).multiply(BigDecimal.valueOf(100), MC).divide(previous, MC);
                }
                default -> throw new IllegalArgumentException("Unsupported time function");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value.toPlainString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Calculated measures require numeric inputs"); }
    }

    private static final class ArithmeticParser {
        private final String input;
        private final Map<String, BigDecimal> variables;
        private int position;

        ArithmeticParser(String input, Map<String, BigDecimal> variables) {
            this.input = input;
            this.variables = variables;
        }

        BigDecimal parse() {
            BigDecimal result = expression();
            whitespace();
            if (position != input.length()) throw invalid();
            return result;
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                whitespace();
                if (take('+')) value = value.add(term(), MC);
                else if (take('-')) value = value.subtract(term(), MC);
                else return value;
            }
        }

        private BigDecimal term() {
            BigDecimal value = unary();
            while (true) {
                whitespace();
                if (take('*')) value = value.multiply(unary(), MC);
                else if (take('/')) {
                    BigDecimal divisor = unary();
                    if (divisor.signum() == 0) throw new IllegalArgumentException("Division by zero in calculated measure");
                    value = value.divide(divisor, MC);
                } else if (take('%')) {
                    BigDecimal divisor = unary();
                    if (divisor.signum() == 0) throw new IllegalArgumentException("Modulo by zero in calculated measure");
                    value = value.remainder(divisor, MC);
                } else return value;
            }
        }

        private BigDecimal unary() {
            whitespace();
            if (take('+')) return unary();
            if (take('-')) return unary().negate(MC);
            if (take('(')) {
                BigDecimal value = expression();
                whitespace();
                if (!take(')')) throw invalid();
                return value;
            }
            if (position < input.length() && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) return number();
            String identifier = identifier();
            BigDecimal value = variables.get(identifier);
            if (value == null) throw new IllegalArgumentException("Unsupported formula token: " + identifier);
            return value;
        }

        private BigDecimal number() {
            int start = position;
            boolean dot = false;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (Character.isDigit(c)) position++;
                else if (c == '.' && !dot) { dot = true; position++; }
                else break;
            }
            try { return new BigDecimal(input.substring(start, position)); }
            catch (NumberFormatException ex) { throw invalid(); }
        }

        private String identifier() {
            whitespace();
            int start = position;
            if (position >= input.length() || !(Character.isLetter(input.charAt(position)) || input.charAt(position) == '_')) throw invalid();
            position++;
            while (position < input.length() && (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) position++;
            return input.substring(start, position);
        }

        private boolean take(char c) {
            if (position < input.length() && input.charAt(position) == c) { position++; return true; }
            return false;
        }
        private void whitespace() { while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++; }
        private IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid calculated-measure formula near position " + position); }
    }
}
