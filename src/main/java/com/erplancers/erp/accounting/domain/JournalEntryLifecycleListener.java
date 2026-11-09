package com.erplancers.erp.accounting.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostUpdate;
import java.util.Objects;

/** Keeps posted journal headers immutable while allowing DRAFT -> POSTED. */
public class JournalEntryLifecycleListener {
    @jakarta.persistence.PrePersist
    public void validateInsert(JournalEntry entry) {
        if (entry.getStatus() != JournalEntryStatus.DRAFT || entry.getIdempotencyKey() == null
                || entry.getIdempotencyPayloadHash() == null || !entry.getIdempotencyPayloadHash().matches("[0-9a-f]{64}"))
            throw new IllegalStateException("Journal entries must be created as DRAFT and posted through the accounting workflow");
    }

    @PostLoad
    @PostPersist
    public void capture(JournalEntry entry) {
        entry.setPersistedPostingFingerprint(fingerprint(entry));
    }

    @PreUpdate
    public void validateUpdate(JournalEntry entry) {
        if (JournalEntryStatus.DRAFT.name().equals(entry.getPersistedWorkflowState())
                && entry.getStatus() == JournalEntryStatus.POSTED && !entry.isPostingValidated())
            throw new IllegalStateException("Journal posting must go through the balanced journal command");
        if (entry.getStatus() == JournalEntryStatus.POSTED) {
            validatePosting(entry);
        }
        if (JournalEntryStatus.POSTED.name().equals(entry.getPersistedWorkflowState())
                && !Objects.equals(entry.getPersistedPostingFingerprint(), fingerprint(entry))) {
            throw new IllegalStateException("Posted journal headers are immutable; create a reversal instead");
        }
    }

    @PostUpdate
    public void captureAfterUpdate(JournalEntry entry) {
        entry.setPersistedPostingFingerprint(fingerprint(entry));
        entry.setPostingValidated(false);
    }

    @PreRemove
    public void preventDelete(JournalEntry entry) {
        if (entry.getStatus() == JournalEntryStatus.POSTED)
            throw new IllegalStateException("Posted journal entries cannot be deleted; create a reversal instead");
    }

    private void validatePosting(JournalEntry entry) {
        if (entry.getEntryDate() == null || entry.getFiscalYear() == null || entry.getFiscalPeriod() == null)
            throw new IllegalStateException("A posted journal requires an entry date, fiscal year and fiscal period");

        FiscalYear year = entry.getFiscalYear();
        FiscalPeriod period = entry.getFiscalPeriod();
        if (year.isClosed() || period.isClosed())
            throw new IllegalStateException("A journal cannot be posted into a closed fiscal year or period");
        if (period.getFiscalYear() == null || !Objects.equals(period.getFiscalYear().getId(), year.getId()))
            throw new IllegalStateException("Journal fiscal period does not belong to its fiscal year");
        if (year.getStartDate() == null || year.getEndDate() == null
                || entry.getEntryDate().isBefore(year.getStartDate()) || entry.getEntryDate().isAfter(year.getEndDate())
                || period.getStartDate() == null || period.getEndDate() == null
                || entry.getEntryDate().isBefore(period.getStartDate()) || entry.getEntryDate().isAfter(period.getEndDate()))
            throw new IllegalStateException("Journal entry date is outside its fiscal year or period");
        if (entry.getLines() == null || entry.getLines().size() < 2)
            throw new IllegalStateException("A posted journal must contain at least two lines");

        java.math.BigDecimal debit = java.math.BigDecimal.ZERO;
        java.math.BigDecimal credit = java.math.BigDecimal.ZERO;
        for (JournalEntryLine line : entry.getLines()) {
            if (line.getAccount() == null || line.getAccount().isBlocked() || !line.getAccount().isPostingAllowed())
                throw new IllegalStateException("Posted journal lines require unblocked posting-enabled accounts");
            java.math.BigDecimal lineDebit = line.getDebit() == null ? java.math.BigDecimal.ZERO : line.getDebit();
            java.math.BigDecimal lineCredit = line.getCredit() == null ? java.math.BigDecimal.ZERO : line.getCredit();
            if (lineDebit.signum() < 0 || lineCredit.signum() < 0
                    || (lineDebit.signum() > 0) == (lineCredit.signum() > 0))
                throw new IllegalStateException("Each journal line must have exactly one positive debit or credit");
            debit = debit.add(lineDebit);
            credit = credit.add(lineCredit);
        }
        if (debit.signum() <= 0 || debit.compareTo(credit) != 0)
            throw new IllegalStateException("Journal debits and credits must be equal and greater than zero");
    }

    private String fingerprint(JournalEntry entry) {
        return String.join("|",
                String.valueOf(entry.getEntryId()),
                String.valueOf(entry.getEntryDate()),
                String.valueOf(entry.getDescription()),
                String.valueOf(entry.getCompany() == null ? null : entry.getCompany().getId()),
                String.valueOf(entry.getFiscalYear() == null ? null : entry.getFiscalYear().getId()),
                String.valueOf(entry.getFiscalPeriod() == null ? null : entry.getFiscalPeriod().getId()),
                String.valueOf(entry.getEntryType()),
                String.valueOf(entry.getIdempotencyKey()), String.valueOf(entry.getIdempotencyPayloadHash()));
    }
}
