package com.erplancers.erp.procurement.domain;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class PurchaseLifecycleListener {
    @PrePersist
    public void validateInsert(Purchase purchase) {
        if (!"ORDERED".equals(purchase.getStatus()) && !"DRAFT".equals(purchase.getStatus()))
            throw new IllegalStateException("Purchases must start as DRAFT or ORDERED");
    }

    @PreUpdate
    public void validateTransition(Purchase purchase) {
        String previous = purchase.getPersistedWorkflowState();
        String current = purchase.getStatus();
        if (previous != null && !previous.equals(current)
                && ("PARTIALLY_RECEIVED".equals(current) || "RECEIVED".equals(current))
                && !purchase.isReceiptValidated())
            throw new IllegalStateException("Purchase receipt state can only change through the goods-receipt workflow");
    }

    @PostUpdate
    public void clearAuthorization(Purchase purchase) {
        purchase.setReceiptValidated(false);
    }

    @PreRemove
    public void preventDelete(Purchase purchase) {
        if ("PARTIALLY_RECEIVED".equals(purchase.getStatus()) || "RECEIVED".equals(purchase.getStatus()))
            throw new IllegalStateException("Received purchases cannot be deleted");
    }
}
