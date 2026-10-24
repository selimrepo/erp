package com.erplancers.erp.common.reporting;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.common.repository.ErpRepository;
import com.erplancers.erp.common.web.CrudRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModuleReportingServiceTest {

    enum Status {
        OPEN, CLOSED
    }

    static class TestEntity extends EntityBase {
        private final BigDecimal amount;
        private final Status status;

        TestEntity(BigDecimal amount, Status status) {
            this.amount = amount;
            this.status = status;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public Status getStatus() {
            return status;
        }
    }

    @Test
    void reportsModuleTotalsNumericMetricsAndEnumDistribution() {
        ErpRepository<TestEntity> repository = mock(ErpRepository.class);
        TestEntity first = new TestEntity(new BigDecimal("10.50"), Status.OPEN);
        TestEntity second = new TestEntity(new BigDecimal("20.25"), Status.CLOSED);
        when(repository.count()).thenReturn(2L);
        when(repository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        CrudRegistry registry = mock(CrudRegistry.class);
        CrudRegistry.CrudDescriptor descriptor =
                new CrudRegistry.CrudDescriptor("test-entity", TestEntity.class, repository, "accounting");
        when(registry.all()).thenReturn(Map.of("test-entity", descriptor));

        ModuleReportingService service = new ModuleReportingService(registry);

        ModuleReportingService.ModuleReport report = service.module("accounting");

        assertEquals("accounting", report.module());
        assertEquals(2L, report.totalRecords());
        assertEquals(1, report.resourceCount());
        assertEquals(1L, report.populatedResources());

        ModuleReportingService.ResourceReport resource = report.resources().getFirst();
        assertEquals(2L, resource.count());
        assertEquals(new BigDecimal("30.75"), resource.numeric().get("amount").sum());
        assertEquals(new BigDecimal("10.50"), resource.numeric().get("amount").min());
        assertEquals(new BigDecimal("20.25"), resource.numeric().get("amount").max());
        assertEquals(2L, resource.numeric().get("amount").populated());
        assertEquals(Map.of("OPEN", 1L, "CLOSED", 1L), resource.enums().get("status"));
        assertEquals(1, report.numericMetrics().size());
    }

    @Test
    void groupsAllResourcesByModule() {
        ErpRepository<TestEntity> accountingRepository = mock(ErpRepository.class);
        ErpRepository<TestEntity> inventoryRepository = mock(ErpRepository.class);
        when(accountingRepository.count()).thenReturn(1L);
        when(inventoryRepository.count()).thenReturn(3L);
        when(accountingRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new TestEntity(BigDecimal.ONE, Status.OPEN))));
        when(inventoryRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new TestEntity(BigDecimal.TWO, Status.OPEN),
                        new TestEntity(BigDecimal.TEN, Status.CLOSED),
                        new TestEntity(BigDecimal.ONE, Status.OPEN))));

        CrudRegistry registry = mock(CrudRegistry.class);
        when(registry.all()).thenReturn(Map.of(
                "invoice", new CrudRegistry.CrudDescriptor("invoice", TestEntity.class, accountingRepository, "accounting"),
                "stock", new CrudRegistry.CrudDescriptor("stock", TestEntity.class, inventoryRepository, "inventory")));

        Map<String, ModuleReportingService.ModuleReport> reports =
                new ModuleReportingService(registry).all();

        assertEquals(2, reports.size());
        assertEquals(1L, reports.get("accounting").totalRecords());
        assertEquals(3L, reports.get("inventory").totalRecords());
    }

    @Test
    void rejectsUnknownModule() {
        CrudRegistry registry = mock(CrudRegistry.class);
        when(registry.all()).thenReturn(Map.of());

        ModuleReportingService service = new ModuleReportingService(registry);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> service.module("missing"));

        assertEquals("Unknown ERP module: missing", exception.getMessage());
    }
}
