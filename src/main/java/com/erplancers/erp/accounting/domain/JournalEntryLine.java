package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.finance.domain.Currency;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class JournalEntryLine extends EntityBase {
    @ManyToOne
    JournalEntry journalEntry;
    @ManyToOne
    Account account;
    String description;
    BigDecimal debit = BigDecimal.ZERO;
    BigDecimal credit = BigDecimal.ZERO;
    @ManyToOne
    Currency currency;
    BigDecimal foreignDebit;
    BigDecimal foreignCredit;
    @Enumerated(EnumType.STRING)
    JournalEntryStatus journalEntryStatus;
    LocalDate entryDate;
    @ManyToOne
    FiscalYear fiscalYear;
    @ManyToOne
    FiscalPeriod fiscalPeriod;
    @ManyToOne
    CostCenter costCenter;
    @ManyToOne
    ProfitCenter profitCenter;
}
