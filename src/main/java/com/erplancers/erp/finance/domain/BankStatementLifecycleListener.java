package com.erplancers.erp.finance.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Set;

public class BankStatementLifecycleListener {
    private static final Set<String> STATES = Set.of("IMPORTED", "PARTIALLY_RECONCILED", "RECONCILED");

    @PrePersist
    public void validateInsert(BankStatement statement) { validate(statement); }

    @PreUpdate
    public void validateUpdate(BankStatement statement) {
        validate(statement);
        String previous = statement.getPersistedWorkflowState();
        String current = statement.getStatus();
        if (previous != null && !previous.equals(current) && !statement.isReconciliationValidated())
            throw new IllegalStateException("Bank statement reconciliation state can only be changed by reconciliation workflows");
    }

    @jakarta.persistence.PostUpdate
    public void clearAuthorization(BankStatement statement) { statement.setReconciliationValidated(false); }

    private void validate(BankStatement statement) {
        if (statement.getIdempotencyKey() == null || statement.getIdempotencyKey().isBlank()
                || statement.getPayloadHash() == null || !statement.getPayloadHash().matches("[0-9a-f]{64}"))
            throw new IllegalStateException("Bank statement requires an idempotency key and SHA-256 payload hash");
        if (statement.getStatementReference() == null || statement.getStatementReference().isBlank()
                || statement.getCurrency() == null || statement.getPeriodStart() == null || statement.getPeriodEnd() == null
                || statement.getPeriodEnd().isBefore(statement.getPeriodStart()))
            throw new IllegalStateException("Bank statement header is incomplete or has an invalid date range");
        if (!STATES.contains(statement.getStatus()))
            throw new IllegalStateException("Invalid bank statement state: " + statement.getStatus());
    }
}
