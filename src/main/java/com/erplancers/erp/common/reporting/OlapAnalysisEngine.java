package com.erplancers.erp.common.reporting;

import com.erplancers.erp.common.web.CrudRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic OLAP-style analysis engine for the ERP data model.
 *
 * <p>The ERP is treated as a collection of fact/dimension candidates discovered
 * from the JPA model. A cube query is executed by the database aggregation
 * engine, so the engine does not load the complete dataset into memory.</p>
 *
 * <p>Supports dimensions, measures, slicers, drill-down, roll-up totals and
 * pivoted column output. Relationship paths are accepted by the underlying
 * aggregate engine.</p>
 */
@Service
@Transactional(readOnly = true)
public class OlapAnalysisEngine {

    private final AggregateReportingService aggregates;
    private final CrudRegistry registry;
    private final EntityDataAnalysisService entityAnalysis;

    public OlapAnalysisEngine(
            AggregateReportingService aggregates,
            CrudRegistry registry,
            EntityDataAnalysisService entityAnalysis) {
        this.aggregates = aggregates;
        this.registry = registry;
        this.entityAnalysis = entityAnalysis;
    }

    public CubeResult cube(String resource, CubeRequest request) {
        List<String> dimensions = request.dimensions();
        List<String> measures = request.measures().isEmpty()
                ? List.of("count")
                : request.measures();

        AggregateReportingService.AggregateReport base = aggregates.aggregate(
                resource,
                new AggregateReportingService.AggregateRequest(
                        dimensions,
                        measures,
                        request.filters(),
                        request.orderBy(),
                        request.limit()));

        List<Map<String, Object>> rows = new ArrayList<>(base.data());

        Map<String, Object> grandTotal = Map.of();
        if (request.includeTotals()) {
            AggregateReportingService.AggregateReport total = aggregates.aggregate(
                    resource,
                    new AggregateReportingService.AggregateRequest(
                            List.of(),
                            measures,
                            request.filters(),
                            "",
                            1));
            grandTotal = total.data().isEmpty()
                    ? Map.of()
                    : new LinkedHashMap<>(total.data().get(0));
        }

        List<Map<String, Object>> rollup = request.includeRollup()
                ? rollup(resource, dimensions, measures, request.filters(), request.limit())
                : List.of();

        List<Map<String, Object>> pivot = request.pivotDimension().isBlank()
                ? List.of()
                : pivot(rows, dimensions, measures, request.pivotDimension());

        return new CubeResult(
                resource,
                base.module(),
                dimensions,
                measures,
                request.filters(),
                rows,
                pivot,
                rollup,
                grandTotal,
                rows.size());
    }

    /**
     * Executes a drill-down query by replacing the current dimension set with
     * the supplied finer-grained dimension hierarchy.
     */
    public CubeResult drillDown(String resource, DrillDownRequest request) {
        CubeRequest cube = new CubeRequest(
                request.dimensions(),
                request.measures(),
                request.filters(),
                request.orderBy(),
                request.limit(),
                request.includeTotals(),
                request.includeRollup(),
                request.pivotDimension());
        return cube(resource, cube);
    }

    /**
     * Returns an OLAP catalog for the whole registered ERP model.
     */
    public OlapCatalog catalog() {
        List<OlapResource> resources = registry.all().values().stream()
                .sorted(java.util.Comparator.comparing(CrudRegistry.CrudDescriptor::slug))
                .map(d -> new OlapResource(
                        d.slug(),
                        d.module(),
                        d.entityType().getName()))
                .toList();

        return new OlapCatalog(resources, entityAnalysis.catalog());
    }

    /**
     * Produces a lightweight profile for every registered resource. This is
     * database-backed and reuses the existing entity profiling engine.
     */
    public Map<String, EntityDataAnalysisService.EntityProfile> profileAll(int topValues) {
        Map<String, EntityDataAnalysisService.EntityProfile> profiles =
                new LinkedHashMap<>();

        for (CrudRegistry.CrudDescriptor descriptor : registry.all().values().stream()
                .sorted(java.util.Comparator.comparing(CrudRegistry.CrudDescriptor::slug))
                .toList()) {
            profiles.put(
                    descriptor.slug(),
                    entityAnalysis.profile(descriptor.slug(), topValues));
        }
        return profiles;
    }

    private List<Map<String, Object>> rollup(
            String resource,
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            int limit) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (int level = dimensions.size() - 1; level >= 0; level--) {
            List<String> grouping = dimensions.subList(0, level);

            AggregateReportingService.AggregateReport report =
                    aggregates.aggregate(
                            resource,
                            new AggregateReportingService.AggregateRequest(
                                    grouping,
                                    measures,
                                    filters,
                                    "",
                                    Math.min(limit, 1000)));

            for (Map<String, Object> row : report.data()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("_level", level);
                item.putAll(row);
                result.add(item);
            }
        }

        return result;
    }

    private List<Map<String, Object>> pivot(
            List<Map<String, Object>> rows,
            List<String> dimensions,
            List<String> measures,
            String pivotDimension) {

        if (!dimensions.contains(pivotDimension)) {
            throw new IllegalArgumentException(
                    "Pivot dimension must be included in dimensions: " + pivotDimension);
        }

        List<String> remaining = dimensions.stream()
                .filter(d -> !d.equals(pivotDimension))
                .toList();

        Map<String, Map<String, Object>> matrix = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String rowKey = remaining.stream()
                    .map(d -> String.valueOf(row.get(d)))
                    .reduce((a, b) -> a + " | " + b)
                    .orElse("_all");

            Map<String, Object> output = matrix.computeIfAbsent(
                    rowKey,
                    ignored -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        for (String d : remaining) {
                            m.put(d, row.get(d));
                        }
                        return m;
                    });

            String pivotValue = String.valueOf(row.get(pivotDimension));
            for (String measure : measures) {
                output.put(pivotValue + "." + measure, row.get(measure));
            }
        }

        return new ArrayList<>(matrix.values());
    }

    public record CubeRequest(
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            String orderBy,
            Integer limit,
            Boolean includeTotals,
            Boolean includeRollup,
            String pivotDimension) {

        public CubeRequest {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            measures = measures == null ? List.of("count") : List.copyOf(measures);
            filters = filters == null ? Map.of() : Map.copyOf(filters);
            orderBy = orderBy == null ? "" : orderBy;
            pivotDimension = pivotDimension == null ? "" : pivotDimension;
            limit = limit == null || limit <= 0 ? 100 : Math.min(limit, 1000);
            includeTotals = Boolean.TRUE.equals(includeTotals);
            includeRollup = Boolean.TRUE.equals(includeRollup);
        }
    }

    public record DrillDownRequest(
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            String orderBy,
            Integer limit,
            Boolean includeTotals,
            Boolean includeRollup,
            String pivotDimension) {
        public DrillDownRequest {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
            measures = measures == null ? List.of("count") : List.copyOf(measures);
            filters = filters == null ? Map.of() : Map.copyOf(filters);
            orderBy = orderBy == null ? "" : orderBy;
            pivotDimension = pivotDimension == null ? "" : pivotDimension;
            limit = limit == null || limit <= 0 ? 100 : Math.min(limit, 1000);
            includeTotals = Boolean.TRUE.equals(includeTotals);
            includeRollup = Boolean.TRUE.equals(includeRollup);
        }
    }

    public record CubeResult(
            String resource,
            String module,
            List<String> dimensions,
            List<String> measures,
            Map<String, String> filters,
            List<Map<String, Object>> cells,
            List<Map<String, Object>> pivot,
            List<Map<String, Object>> rollup,
            Map<String, Object> grandTotal,
            int cellCount) {
    }

    public record OlapCatalog(
            List<OlapResource> resources,
            EntityDataAnalysisService.EntityCatalog entityCatalog) {
    }

    public record OlapResource(
            String resource,
            String module,
            String entity) {
    }
}
