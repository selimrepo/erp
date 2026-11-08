package com.erplancers.erp.procurement.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Objects;

public class PurchaseDetailLifecycleListener {
    @PostLoad @PostPersist
    public void capture(PurchaseDetail line) {
        line.setPersistedReceivedQuantity(line.getReceivedQuantity());
    }

    @PrePersist @PreUpdate
    public void validate(PurchaseDetail line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getPrice() == null || line.getPrice() < 0)
            throw new IllegalStateException("Purchase line quantity must be positive and price non-negative");
        long received = line.getReceivedQuantity() == null ? 0L : line.getReceivedQuantity();
        if (received < 0 || received > line.getQuantity())
            throw new IllegalStateException("Received quantity must be between zero and ordered quantity");
        if (line.getPersistedReceivedQuantity() != null && !Objects.equals(line.getPersistedReceivedQuantity(), line.getReceivedQuantity())
                && !line.isReceiptValidated())
            throw new IllegalStateException("Received quantity can only be changed by the goods-receipt workflow");
        long net = Math.multiplyExact(line.getQuantity(), line.getPrice());
        long tax = line.getTaxAmount() == null ? 0L : line.getTaxAmount();
        if (tax < 0 || Math.addExact(net, tax) != line.getLineTotal())
            throw new IllegalStateException("Purchase line total must equal net plus tax");
    }

    @PostUpdate
    public void captureAfterUpdate(PurchaseDetail line) {
        line.setPersistedReceivedQuantity(line.getReceivedQuantity());
        line.setReceiptValidated(false);
    }
}
