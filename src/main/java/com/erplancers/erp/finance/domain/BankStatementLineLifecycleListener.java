package com.erplancers.erp.finance.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Set;

public class BankStatementLineLifecycleListener {
    private static final Set<String> UNMATCHED_STATES = Set.of("UNMATCHED", "AMBIGUOUS", "ALREADY_RECONCILED");

    @PrePersist @PreUpdate
    public void validate(BankStatementLine line) {
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
