package com.erplancers.erp.accounting.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;


public record GeneralLedgerLine(
        Long journalEntryId,
        String entryId,
        LocalDate date,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance
) {
}