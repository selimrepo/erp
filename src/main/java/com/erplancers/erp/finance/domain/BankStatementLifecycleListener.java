package com.erplancers.erp.finance.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Set;

public class BankStatementLifecycleListener {
    private static final Set<String> STATES = Set.of("IMPORTED", "PARTIALLY_RECONCILED", "RECONCILED");

    @PrePersist @PreUpdate
    public void validate(BankStatement statement) {
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
