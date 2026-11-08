package com.erplancers.erp.finance.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import java.util.Objects;
import java.util.Set;

public class BankStatementLineLifecycleListener {
    private static final Set<String> UNMATCHED_STATES = Set.of("UNMATCHED", "AMBIGUOUS", "ALREADY_RECONCILED");

    @PostLoad @PostPersist
    public void capture(BankStatementLine line) {
        line.setPersistedMatchedPaymentId(line.getMatchedPayment() == null ? null : line.getMatchedPayment().getId());
        line.setPersistedReconciliationFingerprint(fingerprint(line));
        line.setImportValidated(false);
    }

    @PrePersist
    public void validateInsert(BankStatementLine line) {
        if (!line.isImportValidated())
            throw new IllegalStateException("Bank statement lines can only be created by statement import");
        validate(line);
    }

    @PreUpdate
    public void validateUpdate(BankStatementLine line) {
        validate(line);
        if (!Objects.equals(line.getPersistedReconciliationFingerprint(), fingerprint(line))
                && !line.isReconciliationValidated())
            throw new IllegalStateException("Bank statement reconciliation fields can only be changed by reconciliation workflows");
    }

    @PostUpdate
    public void captureAfterUpdate(BankStatementLine line) {
        capture(line);
        line.setReconciliationValidated(false);
    }

    private String fingerprint(BankStatementLine line) {
        return String.join("|", String.valueOf(line.getMatchedPayment() == null ? null : line.getMatchedPayment().getId()),
                String.valueOf(line.getReconciliationStatus()), String.valueOf(line.getReconciledAt()));
    }

    private void validate(BankStatementLine line) {
        if (line.getStatement() == null || line.getExternalLineId() == null || line.getExternalLineId().isBlank())
            throw new IllegalStateException("Bank statement line requires a statement and external line id");
        if (line.getTransactionDate() == null || line.getTransactionDate().isBefore(line.getStatement().getPeriodStart())
                || line.getTransactionDate().isAfter(line.getStatement().getPeriodEnd()))
            throw new IllegalStateException("Bank statement line date must be within the statement period");
        if (line.getAmount() == null || line.getAmount() == 0)
            throw new IllegalStateException("Bank statement line amount cannot be zero");
        if (line.getMatchedPayment() == null) {
            if (!UNMATCHED_STATES.contains(line.getReconciliationStatus()) || line.getReconciledAt() != null)
                throw new IllegalStateException("Unmatched bank lines cannot carry a matched status or reconciliation timestamp");
        } else if (!"MATCHED".equals(line.getReconciliationStatus()) || line.getReconciledAt() == null) {
            throw new IllegalStateException("Matched bank lines require MATCHED status and a reconciliation timestamp");
        }
    }
}
