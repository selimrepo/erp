package com.erplancers.erp.inventory.stock.domain;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class StockAdjustmentLifecycleListener {
    @PrePersist
    public void validateInsert(StockAdjustment adjustment) {
        if (!adjustment.isCommandValidated() || adjustment.getProduct() == null || adjustment.getStore() == null
                || adjustment.getDeltaQuantity() == null || adjustment.getDeltaQuantity() == 0
                || adjustment.getDeltaQuantity() == Long.MIN_VALUE
                || adjustment.getReason() == null || adjustment.getReason().isBlank()
                || adjustment.getIdempotencyKey() == null || adjustment.getIdempotencyKey().isBlank()
                || adjustment.getPayloadHash() == null || !adjustment.getPayloadHash().matches("[0-9a-f]{64}")
                || adjustment.getAdjustedAt() == null)
            throw new IllegalStateException("Stock adjustments require a valid command, source, non-zero delta and idempotency fingerprint");
    }

    @PostPersist
    public void clearAuthorization(StockAdjustment adjustment) { adjustment.setCommandValidated(false); }

    @PreUpdate
    public void preventUpdate(StockAdjustment adjustment) {
        throw new IllegalStateException("Stock adjustments are immutable audit records");
    }

    @PreRemove
    public void preventDelete(StockAdjustment adjustment) {
        throw new IllegalStateException("Stock adjustments cannot be deleted; create a compensating adjustment");
    }
}
