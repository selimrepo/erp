package com.erplancers.erp.inventory.stock.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class StockMovementLifecycleListener {
    @PrePersist
    public void validateInsert(StockMovement movement) {
        if (movement.getProduct() == null || movement.getStore() == null
                || movement.getQuantity() == null || movement.getQuantity() <= 0
                || movement.getMovementType() == null || movement.getMovementType().isBlank()
                || movement.getReferenceType() == null || movement.getReferenceType().isBlank()
                || movement.getReferenceId() == null || movement.getReferenceId().isBlank()
                || movement.getIdempotencyKey() == null || movement.getIdempotencyKey().isBlank()
                || movement.getOccurredAt() == null)
            throw new IllegalStateException("Stock movement requires positive quantity and complete source metadata");
    }

    @PreUpdate
    public void preventUpdate(StockMovement movement) {
        throw new IllegalStateException("Stock movements are immutable ledger records");
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(StockMovement movement) {
        throw new IllegalStateException("Stock movements cannot be deleted; create a compensating movement");
    }
}
