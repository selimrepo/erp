package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;


public record AccountFiscalSummary(
        Long accountId,
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance
) {
}