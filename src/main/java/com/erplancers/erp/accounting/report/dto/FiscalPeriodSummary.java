package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;


public record FiscalPeriodSummary(
        Long fiscalPeriodId,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance
) {
}