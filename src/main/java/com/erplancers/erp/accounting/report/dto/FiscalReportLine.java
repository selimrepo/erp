package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;


public record FiscalReportLine(
        Long journalEntryId,
        String entryId,
        LocalDate entryDate,
        Long accountId,
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit,
        String description
) {
}