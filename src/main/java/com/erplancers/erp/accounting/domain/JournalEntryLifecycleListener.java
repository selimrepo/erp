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
        if (entry.getStatus() != JournalEntryStatus.DRAFT)
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
