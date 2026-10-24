package com.erplancers.erp.common.reporting;

import com.erplancers.erp.common.web.CrudRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardReportingService {

    private final CrudRegistry registry;
    private final ModuleReportingService modules;

    public DashboardReportingService(CrudRegistry registry, ModuleReportingService modules) {
        this.registry = registry;
        this.modules = modules;
    }

    public Dashboard overview() {
        Map<String, Long> moduleCounts = new LinkedHashMap<>();
        Map<String, Long> resourceCounts = new LinkedHashMap<>();

        registry.all().values().stream()
                .sorted(java.util.Comparator.comparing(CrudRegistry.CrudDescriptor::slug))
                .forEach(d -> {
                    resourceCounts.put(d.slug(), d.repository().count());
                    moduleCounts.merge(d.module(), d.repository().count(), Long::sum);
                });

        long total = resourceCounts.values().stream().mapToLong(Long::longValue).sum();

        return new Dashboard(
                total,
                registry.all().size(),
                moduleCounts.size(),
                moduleCounts,
                resourceCounts,
                modules.all());
    }

    public record Dashboard(
            long totalRecords,
            int resourceCount,
            int moduleCount,
            Map<String, Long> recordsByModule,
            Map<String, Long> recordsByResource,
            Map<String, ModuleReportingService.ModuleReport> modules) {
    }
}
