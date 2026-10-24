package com.erplancers.erp.common.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic ERP BI facade over the database aggregation and advanced analytics
 * engines.
 *
 * <p>BI consumers describe dimensions, measures and filters instead of writing
 * report-specific repository queries. The database performs the aggregation;
 * this service adds BI operations such as ranking, percentage-of-total and
 * grand totals.</p>
 */
@Service
@Transactional(readOnly = true)
public class ErpBiEngine {

    private final AggregateReportingService aggregates;
    private final AdvancedAnalyticsEngine advancedAnalytics;

    public ErpBiEngine(
            AggregateReportingService aggregates,
            AdvancedAnalyticsEngine advancedAnalytics) {
        this.aggregates = aggregates;
        this.advancedAnalytics = advancedAnalytics;
    }

    public BiReport query(String resource, BiQuery request) {
        AggregateReportingService.AggregateRequest aggregateRequest =
                new AggregateReportingService.AggregateRequest(
                        request.dimensions(),
                        request.measures(),
                        request.filters(),
                        request.orderBy(),
                        request.limit());

        AggregateReportingService.AggregateReport report =
                aggregates.aggregate(resource, aggregateRequest);

        String rankingMeasure = rankingMeasure(report.metrics());
        Double grandTotalValue = null;
        Map<String, Object> grandTotal = new LinkedHashMap<>();

        if (request.includeGrandTotal() || request.includeShareOfTotal()) {
            AggregateReportingService.AggregateReport totalReport =
                    aggregates.aggregate(
                            resource,
                            new AggregateReportingService.AggregateRequest(
                                    List.of(),
                                    List.of(rankingMeasure),
                                    request.filters(),
                                    "",
                                    1));

            if (!totalReport.data().isEmpty()) {
                Object value = totalReport.data().get(0).get(rankingMeasure);
                if (value instanceof Number number) {
                    grandTotalValue = number.doubleValue();
                }
            }

            if (request.includeGrandTotal() && !totalReport.data().isEmpty()) {
                grandTotal.putAll(totalReport.data().get(0));
            }
        }

        List<Map<String, Object>> rows = enrichRows(
                report.data(),
                rankingMeasure,
                grandTotalValue,
                request.includeRank(),
                request.includeShareOfTotal());
        CalculatedMeasureEngine.apply(rows, request.calculatedMeasures());
        if (!grandTotal.isEmpty() && request.calculatedMeasures() != null && !request.calculatedMeasures().isEmpty()) {
            List<Map<String, Object>> totalRows = new ArrayList<>();
            totalRows.add(grandTotal);
            CalculatedMeasureEngine.apply(totalRows, request.calculatedMeasures());
        }

        return new BiReport(
                report.resource(),
                report.module(),
                report.groupBy(),
                report.metrics(),
                report.filters(),
                rows,
                grandTotal,
                rows.size());
    }

    public AdvancedAnalyticsEngine.AdvancedAnalysis analyze(
            String resource,
            BiAnalysisRequest request) {

        return advancedAnalytics.analyze(
                resource,
                new AdvancedAnalyticsEngine.AnalysisRequest(
                        request.fields(),
                        request.sampleSize(),
                        request.zScoreThreshold(),
                        request.aggregate()));
    }

    private List<Map<String, Object>> enrichRows(
            List<Map<String, Object>> source,
            String measure,
            Double grandTotal,
            boolean includeRank,
            boolean includeShareOfTotal) {

        List<Map<String, Object>> rows = source.stream()
                .map(LinkedHashMap::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (includeRank) {
            List<Map<String, Object>> ranked = rows.stream()
                    .sorted(Comparator.<Map<String, Object>>comparingDouble(
                            row -> numeric(row.get(measure))).reversed())
                    .toList();

            int rank = 1;
            for (Map<String, Object> row : ranked) {
                row.put("_rank", rank++);
            }
        }

        if (includeShareOfTotal && grandTotal != null && grandTotal != 0) {
            for (Map<String, Object> row : rows) {
                row.put("_shareOfTotal",
                        numeric(row.get(measure)) / grandTotal);
            }
        }

        return rows;
    }

    private String rankingMeasure(List<String> measures) {
        for (String measure : measures) {
            if (!"count".equalsIgnoreCase(measure)
                    && !"count(*)".equalsIgnoreCase(measure)) {
                return measure;
            }
        }
        return "count";
    }

    private double numeric(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    public record BiQuery(
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            String orderBy,
            Integer limit,
            Boolean includeRank,
            Boolean includeShareOfTotal,
            Boolean includeGrandTotal,
            Map<String, String> calculatedMeasures) {

        public BiQuery {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            measures = measures == null ? List.of("count") : List.copyOf(measures);
            filters = filters == null ? Map.of() : Map.copyOf(filters);
            orderBy = orderBy == null ? "" : orderBy;
            limit = limit == null || limit <= 0 ? 100 : limit;
            includeRank = Boolean.TRUE.equals(includeRank);
            includeShareOfTotal = Boolean.TRUE.equals(includeShareOfTotal);
            includeGrandTotal = Boolean.TRUE.equals(includeGrandTotal);
            calculatedMeasures = calculatedMeasures == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(calculatedMeasures));
        }

        public BiQuery(List<String> dimensions, List<String> measures, Map<String, String> filters,
                       String orderBy, Integer limit, Boolean includeRank,
                       Boolean includeShareOfTotal, Boolean includeGrandTotal) {
            this(dimensions, measures, filters, orderBy, limit, includeRank,
                    includeShareOfTotal, includeGrandTotal, Map.of());
        }
    }

    public record BiReport(
            String resource,
            String module,
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            List<Map<String, Object>> rows,
            Map<String, Object> grandTotal,
            int rowCount) {
    }

    public record BiAnalysisRequest(
            List<String> fields,
            Integer sampleSize,
            Double zScoreThreshold,
            AggregateReportingService.AggregateRequest aggregate) {

        public BiAnalysisRequest {
            fields = fields == null ? List.of() : List.copyOf(fields);
            sampleSize = sampleSize == null || sampleSize <= 0 ? 100 : sampleSize;
            zScoreThreshold = zScoreThreshold == null || zScoreThreshold <= 0 ? 3.0 : zScoreThreshold;
        }
    }
}
