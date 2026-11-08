package com.erplancers.erp.procurement.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class PurchaseReceiptLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(PurchaseReceipt receipt) {
        if (receipt.getPurchase() == null || receipt.getIdempotencyKey() == null || receipt.getIdempotencyKey().isBlank()
                || receipt.getPayloadHash() == null || !receipt.getPayloadHash().matches("[0-9a-f]{64}")
                || receipt.getReceivedAt() == null)
            throw new IllegalStateException("Purchase receipt requires a purchase, idempotency key, payload hash and timestamp");
    }
}
