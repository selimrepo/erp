package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.finance.domain.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @EntityListeners(JournalEntryLineLifecycleListener.class) @NoArgsConstructor @Getter @Setter
public class JournalEntryLine extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private JournalEntry journalEntry;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Account account;
    private String description;
    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) private Currency currency;
    private BigDecimal foreignDebit;
    private BigDecimal foreignCredit;
    @Enumerated(EnumType.STRING) private JournalEntryStatus journalEntryStatus;
    private LocalDate entryDate;
    @ManyToOne(fetch = FetchType.LAZY) private FiscalYear fiscalYear;
    @ManyToOne(fetch = FetchType.LAZY) private FiscalPeriod fiscalPeriod;
    @ManyToOne(fetch = FetchType.LAZY) private CostCenter costCenter;
    @ManyToOne(fetch = FetchType.LAZY) private ProfitCenter profitCenter;
}
