package com.erplancers.erp.inventory.stock.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class StockMovementLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(StockMovement movement) {
        if (movement.getProduct() == null || movement.getStore() == null
                || movement.getQuantity() == null || movement.getQuantity() <= 0
                || movement.getMovementType() == null || movement.getMovementType().isBlank()
                || movement.getReferenceType() == null || movement.getReferenceType().isBlank()
                || movement.getReferenceId() == null || movement.getReferenceId().isBlank()
                || movement.getIdempotencyKey() == null || movement.getIdempotencyKey().isBlank()
                || movement.getOccurredAt() == null)
            throw new IllegalStateException("Stock movement requires positive quantity and complete source metadata");
    }
}
