package com.erplancers.erp.common.reporting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatedMeasureEngineTest {
    @Test
    void evaluatesMarginsRunningTotalsAndPeriodComparisons() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new LinkedHashMap<>(Map.of("sum:revenue", 100, "sum:cost", 60)));
        rows.add(new LinkedHashMap<>(Map.of("sum:revenue", 150, "sum:cost", 80)));
        Map<String, String> formulas = new LinkedHashMap<>();
        formulas.put("marginPct", "({sum:revenue} - {sum:cost}) / {sum:revenue} * 100");
        formulas.put("runningRevenue", "RUNNING_SUM({sum:revenue})");
        formulas.put("periodChange", "PERIOD_CHANGE({sum:revenue})");
        formulas.put("periodChangePct", "PERIOD_CHANGE_PCT({sum:revenue})");

        CalculatedMeasureEngine.apply(rows, formulas);

        assertEquals(0, decimal(rows.get(0).get("marginPct")).compareTo(new BigDecimal("40")));
        assertEquals(0, decimal(rows.get(1).get("marginPct")).compareTo(new BigDecimal("46.66666666666667")));
        assertEquals(0, decimal(rows.get(0).get("runningRevenue")).compareTo(new BigDecimal("100")));
        assertEquals(0, decimal(rows.get(1).get("runningRevenue")).compareTo(new BigDecimal("250")));
        assertEquals(0, decimal(rows.get(1).get("periodChange")).compareTo(new BigDecimal("50")));
        assertEquals(0, decimal(rows.get(1).get("periodChangePct")).compareTo(new BigDecimal("50")));
    }

    @Test
    void rejectsCodeAndDivisionByZero() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new LinkedHashMap<>(Map.of("sum:revenue", 10, "sum:cost", 0)));
        assertThrows(IllegalArgumentException.class, () -> CalculatedMeasureEngine.apply(
                rows, Map.of("unsafe", "java.lang.Runtime.getRuntime()")));
        assertThrows(IllegalArgumentException.class, () -> CalculatedMeasureEngine.apply(
                rows, Map.of("ratio", "{sum:revenue} / {sum:cost}")));
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(value));
    }
}
