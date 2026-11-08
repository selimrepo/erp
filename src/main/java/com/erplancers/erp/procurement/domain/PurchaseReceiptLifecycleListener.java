package com.erplancers.erp.procurement.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class PurchaseReceiptLifecycleListener {
    @PrePersist
    public void validateInsert(PurchaseReceipt receipt) {
        if (!receipt.isReceiptValidated())
            throw new IllegalStateException("Purchase receipts can only be created by the goods-receipt workflow");
        validate(receipt);
    }

    @PreUpdate
    public void validateUpdate(PurchaseReceipt receipt) { validate(receipt); }

    @jakarta.persistence.PostPersist
    public void clearAuthorization(PurchaseReceipt receipt) { receipt.setReceiptValidated(false); }

    @jakarta.persistence.PreRemove
    public void preventDelete(PurchaseReceipt receipt) {
        throw new IllegalStateException("Purchase receipts are immutable audit records");
    }

    private void validate(PurchaseReceipt receipt) {
        if (receipt.getPurchase() == null || receipt.getIdempotencyKey() == null || receipt.getIdempotencyKey().isBlank()
                || receipt.getPayloadHash() == null || !receipt.getPayloadHash().matches("[0-9a-f]{64}")
                || receipt.getReceivedAt() == null)
            throw new IllegalStateException("Purchase receipt requires a purchase, idempotency key, payload hash and timestamp");
    }
}
