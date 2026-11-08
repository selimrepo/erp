package com.erplancers.erp.accounting.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;

/** Journal lines can only be created while the parent is a draft and never edited after posting. */
public class JournalEntryLineLifecycleListener {
    @PrePersist
    public void validateInsert(JournalEntryLine line) {
        if (line.getJournalEntry() == null || line.getJournalEntry().getStatus() != JournalEntryStatus.DRAFT)
            throw new IllegalStateException("Journal lines can only be added to a draft journal");
        validateAmounts(line);
    }

    @PreUpdate
    public void preventUpdate(JournalEntryLine line) {
        if (line.getJournalEntry() == null || line.getJournalEntry().getStatus() != JournalEntryStatus.DRAFT)
            throw new IllegalStateException("Posted journal lines are immutable; create a reversal instead");
        validateAmounts(line);
    }

    @PreRemove
    public void preventDelete(JournalEntryLine line) {
        if (line.getJournalEntry() == null || line.getJournalEntry().getStatus() != JournalEntryStatus.DRAFT)
            throw new IllegalStateException("Posted journal lines cannot be deleted; create a reversal instead");
    }

    private void validateAmounts(JournalEntryLine line) {
        BigDecimal debit = line.getDebit() == null ? BigDecimal.ZERO : line.getDebit();
        BigDecimal credit = line.getCredit() == null ? BigDecimal.ZERO : line.getCredit();
        if (debit.signum() < 0 || credit.signum() < 0 || debit.signum() > 0 && credit.signum() > 0
                || debit.signum() == 0 && credit.signum() == 0)
            throw new IllegalStateException("A journal line must contain one positive debit or credit amount");
        if (line.getAccount() == null) throw new IllegalStateException("Journal line requires an account");
    }
}
