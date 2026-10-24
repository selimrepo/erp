package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;


public record PeriodComparison(
        Long accountId,
        String accountCode,
        String accountName,

        BigDecimal firstPeriodDebit,
        BigDecimal firstPeriodCredit,
        BigDecimal firstPeriodBalance,

        BigDecimal secondPeriodDebit,
        BigDecimal secondPeriodCredit,
        BigDecimal secondPeriodBalance,

        BigDecimal debitDifference,
        BigDecimal creditDifference,
        BigDecimal balanceDifference
) {
}