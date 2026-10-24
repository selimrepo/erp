package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceReport(
        LocalDate from,
        LocalDate to,
        List<TrialBalanceLine> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit
) {
}