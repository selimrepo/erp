package com.erplancers.erp.sales.report;

import com.erplancers.erp.common.reporting.EntityReportService;
import com.erplancers.erp.common.web.CrudRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalesReportService {
    public static final String MODULE = "sales";
    private final CrudRegistry registry;
    private final EntityReportService entityReports;

    public SalesReportService(CrudRegistry registry, EntityReportService entityReports) {
        this.registry = registry;
        this.entityReports = entityReports;
    }

    public ModuleReport report() {
        List<EntityReportService.EntityReport> entities = registry.all().values().stream()
                .filter(d -> MODULE.equals(d.module()))
                .sorted(Comparator.comparing(d -> d.entityType().getSimpleName()))
                .map(d -> entityReports.report(d.slug()))
                .toList();
        return new ModuleReport(MODULE, entities.stream().mapToLong(EntityReportService.EntityReport::count).sum(), entities);
    }

    public EntityReportService.EntityReport entity(String slug) {
        EntityReportService.EntityReport report = entityReports.report(slug);
        if (!MODULE.equals(report.module())) {
            throw new IllegalArgumentException("Resource does not belong to module: " + slug);
        }
        return report;
    }

    public record ModuleReport(String module, long totalRecords, List<EntityReportService.EntityReport> entities) {}
}