package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;
import java.util.List;


public record FiscalReport(
        Long fiscalYearId,
        Long fiscalPeriodId,
        List<FiscalReportLine> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal balance
) {
}