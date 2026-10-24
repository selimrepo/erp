package com.erplancers.erp.common.web;

import com.erplancers.erp.common.reporting.AdvancedAnalyticsEngine;
import com.erplancers.erp.common.reporting.ErpBiEngine;
import com.erplancers.erp.common.reporting.OlapAnalysisEngine;
import com.erplancers.erp.common.reporting.JpaOlapCubeService;
import com.erplancers.erp.common.security.PolicyAuthorizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bi")
@SecurityRequirement(name = "basicAuth")
public class RestBiController {

    private final ErpBiEngine bi;
    private final OlapAnalysisEngine olap;
    private final JpaOlapCubeService jpaOlap;
    private final PolicyAuthorizationService authorization;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, CachedBiReport> queryCache = new ConcurrentHashMap<>();

    @PostMapping("/resources/{slug}/query")
    public ErpBiEngine.BiReport query(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody ErpBiEngine.BiQuery request) {
        requireRead(authentication, slug);
        long started = System.nanoTime();
        String roles = authentication.getAuthorities().stream().map(a -> a.getAuthority()).sorted()
                .collect(java.util.stream.Collectors.joining(","));
        String queryJson;
        try { queryJson = mapper.writeValueAsString(request); }
        catch (Exception ex) { queryJson = request.toString(); }
        String cacheKey = authentication.getName() + "|" + roles + "|" + slug + "|" + queryJson;
        Instant now = Instant.now();
        CachedBiReport cached = queryCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            audit(authentication.getName(), slug, started, true, true);
            return cached.report();
        }
        try {
            ErpBiEngine.BiReport report = bi.query(slug, request);
            if (queryCache.size() > 2000) queryCache.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
            if (queryCache.size() > 2500) queryCache.clear();
            queryCache.put(cacheKey, new CachedBiReport(report, now.plusSeconds(30)));
            audit(authentication.getName(), slug, started, false, true);
            return report;
        } catch (RuntimeException ex) {
            audit(authentication.getName(), slug, started, false, false);
            throw ex;
        }
    }

    private void audit(String username, String slug, long startedNanos, boolean cacheHit, boolean succeeded) {
        long elapsedMillis = Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
        try {
            jdbc.update("INSERT INTO bi_query_audit (username, resource_slug, elapsed_ms, cache_hit, succeeded, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    username, slug, elapsedMillis, cacheHit, succeeded, Timestamp.from(Instant.now()));
        } catch (RuntimeException ignored) {
            // BI queries must remain available if optional telemetry storage is unavailable.
        }
    }

    private record CachedBiReport(ErpBiEngine.BiReport report, Instant expiresAt) {}


    @PostMapping("/resources/{slug}/analyze")
    public AdvancedAnalyticsEngine.AdvancedAnalysis analyze(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody ErpBiEngine.BiAnalysisRequest request) {
        requireRead(authentication, slug);
        return bi.analyze(slug, request);
    }

    @GetMapping("/olap/cubes")
    public JpaOlapCubeService.AutoCubeCatalog autoCubes(
            Authentication authentication,
            @RequestParam(defaultValue = "2") int relationDepth) {
        requireAuthenticated(authentication);
        return jpaOlap.cubes(relationDepth);
    }

    @GetMapping("/olap/cubes/{cube}/relations")
    public JpaOlapCubeService.RelationDiscovery relations(
            Authentication authentication,
            @PathVariable String cube,
            @RequestParam(required = false, defaultValue = "") String relation,
            @RequestParam(defaultValue = "2") int relationDepth) {
        requireRead(authentication, cube);
        return jpaOlap.discoverRelations(cube, relation, relationDepth);
    }

    @GetMapping("/olap/cubes/{cube}")
    public JpaOlapCubeService.AutoCube autoCube(
            Authentication authentication,
            @PathVariable String cube,
            @RequestParam(defaultValue = "2") int relationDepth) {
        requireRead(authentication, cube);
        return jpaOlap.cube(cube, relationDepth);
    }

    @PostMapping("/olap/cubes/{cube}/query")
    public OlapAnalysisEngine.CubeResult queryAutoCube(
            Authentication authentication,
            @PathVariable String cube,
            @RequestBody JpaOlapCubeService.AutoCubeQueryRequest request) {
        requireRead(authentication, cube);
        return jpaOlap.query(cube, request);
    }

    @GetMapping("/olap/catalog")
    public OlapAnalysisEngine.OlapCatalog catalog(Authentication authentication) {
        requireAuthenticated(authentication);
        return olap.catalog();
    }

    @GetMapping("/olap/profiles")
    public Object profiles(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int topValues) {
        requireAuthenticated(authentication);
        return olap.profileAll(topValues);
    }

    @PostMapping("/olap/resources/{slug}/cube")
    public OlapAnalysisEngine.CubeResult cube(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody OlapAnalysisEngine.CubeRequest request) {
        requireRead(authentication, slug);
        return olap.cube(slug, request);
    }

    @PostMapping("/olap/resources/{slug}/drilldown")
    public OlapAnalysisEngine.CubeResult drillDown(
            Authentication authentication,
            @PathVariable String slug,
            @RequestBody OlapAnalysisEngine.DrillDownRequest request) {
        requireRead(authentication, slug);
        return olap.drillDown(slug, request);
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    private void requireRead(Authentication authentication, String slug) {
        if (!authorization.isAllowed(authentication, slug, "READ")) {
            throw new AccessDeniedException(
                    "BI READ access denied for resource: " + slug);
        }
    }
}
