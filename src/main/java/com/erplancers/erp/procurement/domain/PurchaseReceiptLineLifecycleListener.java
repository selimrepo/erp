package com.erplancers.erp.procurement.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class PurchaseReceiptLineLifecycleListener {
    @PrePersist
    public void validateInsert(PurchaseReceiptLine line) {
        if (!line.isReceiptLineValidated())
            throw new IllegalStateException("Receipt lines can only be created by the goods-receipt workflow");
        if (line.getReceipt() == null || line.getPurchaseDetail() == null)
            throw new IllegalStateException("Receipt line requires a receipt and purchase detail");
        if (line.getQuantity() == null || line.getQuantity() <= 0)
            throw new IllegalStateException("Received quantity must be positive");
        Long receiptPurchase = line.getReceipt().getPurchase() == null ? null : line.getReceipt().getPurchase().getId();
        Long detailPurchase = line.getPurchaseDetail().getPurchase() == null ? null : line.getPurchaseDetail().getPurchase().getId();
        if (receiptPurchase == null || !receiptPurchase.equals(detailPurchase))
            throw new IllegalStateException("Receipt line detail must belong to the receipt purchase");
    }

    @jakarta.persistence.PostPersist
    public void clearAuthorization(PurchaseReceiptLine line) { line.setReceiptLineValidated(false); }

    @jakarta.persistence.PreUpdate
    public void preventUpdate(PurchaseReceiptLine line) {
        throw new IllegalStateException("Purchase receipt lines are immutable; use a return or correction workflow");
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(PurchaseReceiptLine line) {
        throw new IllegalStateException("Purchase receipt lines cannot be deleted");
    }
}
