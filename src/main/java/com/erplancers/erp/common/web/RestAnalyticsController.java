package com.erplancers.erp.common.web;

import com.erplancers.erp.common.reporting.AdvancedAnalyticsEngine;
import com.erplancers.erp.common.reporting.AggregateReportingService;
import com.erplancers.erp.common.reporting.DashboardReportingService;
import com.erplancers.erp.common.reporting.DataQualityAnalyticsService;
import com.erplancers.erp.common.reporting.EntityDataAnalysisService;
import com.erplancers.erp.common.security.PolicyAuthorizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class RestAnalyticsController {

    private final AggregateReportingService aggregates;
    private final AdvancedAnalyticsEngine advancedAnalytics;
    private final DashboardReportingService dashboards;
    private final EntityDataAnalysisService analysis;
    private final DataQualityAnalyticsService dataQuality;
    private final PolicyAuthorizationService authorization;
    private final CrudRegistry registry;

    @GetMapping("/dashboard")
    public DashboardReportingService.Dashboard dashboard(Authentication authentication) {
        return dashboards.overview();
    }

    @GetMapping("/catalog")
    public EntityDataAnalysisService.EntityCatalog catalog(Authentication authentication) {
        requireAuthenticated(authentication);
        return analysis.catalog();
    }

    @GetMapping("/resources/{slug}/quality")
    public DataQualityAnalyticsService.DataQualityReport quality(
            Authentication authentication,
            @PathVariable String slug,
            @RequestParam(defaultValue = "10") int topValues) {
        requireRead(authentication, slug);
        return dataQuality.inspect(slug, topValues);
    }

    @GetMapping("/resources/{slug}/profile")
    public EntityDataAnalysisService.EntityProfile profile(
            Authentication authentication,
            @PathVariable String slug,
            @RequestParam(defaultValue = "10") int topValues) {
        requireRead(authentication, slug);
        return analysis.profile(slug, topValues);
    }

    @GetMapping("/resources/{slug}/aggregate")
    public AggregateReportingService.AggregateReport aggregate(
            Authentication authentication,
            @PathVariable String slug,
            @RequestParam(defaultValue = "") String groupBy,
            @RequestParam(defaultValue = "count") String metrics,
            @RequestParam(defaultValue = "") String orderBy,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam Map<String, String> parameters) {

        requireRead(authentication, slug);

        Map<String, String> filters = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("filter.")) {
                filters.put(key.substring("filter.".length()), value);
            }
        });

        List<String> groups = csv(groupBy);
        List<String> metricList = csv(metrics);

        return aggregates.aggregate(slug,
                new AggregateReportingService.AggregateRequest(
                        groups, metricList, filters, orderBy, limit));
    }

    @PostMapping("/resources/{slug}/aggregate")
    public AggregateReportingService.AggregateReport aggregatePost(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody AggregateReportingService.AggregateRequest request) {
        requireRead(authentication, slug);
        return aggregates.aggregate(slug, request);
    }

    @GetMapping("/resources/{slug}/advanced")
    public AdvancedAnalyticsEngine.AdvancedAnalysis advanced(
            Authentication authentication,
            @PathVariable String slug,
            @RequestParam(defaultValue = "") String fields,
            @RequestParam(defaultValue = "10000") int sampleSize,
            @RequestParam(defaultValue = "3.0") double zScoreThreshold) {
        requireRead(authentication, slug);
        return advancedAnalytics.analyze(slug,
                new AdvancedAnalyticsEngine.AnalysisRequest(
                        csv(fields), sampleSize, zScoreThreshold));
    }

    @PostMapping("/resources/{slug}/advanced")
    public AdvancedAnalyticsEngine.AdvancedAnalysis advancedPost(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody AdvancedAnalyticsEngine.AnalysisRequest request) {
        requireRead(authentication, slug);
        return advancedAnalytics.analyze(slug, request);
    }

    @GetMapping("/resources")
    public List<Map<String, Object>> resources(Authentication authentication) {
        return registry.all().values().stream()
                .filter(d -> authorization.isAllowed(authentication, d.slug(), "READ"))
                .map(d -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("module", d.module());
                    item.put("slug", d.slug());
                    item.put("name", d.entityType().getSimpleName());
                    item.put("aggregatePath", "/api/analytics/resources/" + d.slug() + "/aggregate");
                    item.put("advancedPath", "/api/analytics/resources/" + d.slug() + "/advanced");
                    item.put("qualityPath", "/api/analytics/resources/" + d.slug() + "/quality");
                    return item;
                })
                .toList();
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required for ERP analytics");
        }
    }

    private void requireRead(Authentication authentication, String slug) {
        if (!authorization.isAllowed(authentication, slug, "READ")) {
            throw new AccessDeniedException("Read access denied for ERP resource: " + slug);
        }
    }

    private List<String> csv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
