package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record CostCenterReport(
        Long costCenterId,
        String costCenterCode,
        String costCenterName,
        LocalDate from,
        LocalDate to,
        List<CostCenterLine> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal balance
) {
}