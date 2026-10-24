package com.erplancers.erp.accounting.report.dto;

import java.time.LocalDate;
import java.util.List;


public record GeneralLedgerReport(
        Long accountId,
        String accountCode,
        String accountName,
        LocalDate from,
        LocalDate to,
        List<GeneralLedgerLine> lines
) {
}