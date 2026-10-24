package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;


public record AccountBalanceReport(
        Long accountId,
        String accountCode,
        String accountName,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal balance
) {
}