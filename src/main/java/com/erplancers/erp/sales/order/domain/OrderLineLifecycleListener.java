package com.erplancers.erp.sales.order.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class OrderLineLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(OrderLine line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getUnitPrice() == null || line.getUnitPrice() < 0)
            throw new IllegalStateException("Order line quantity must be positive and unit price non-negative");
        long entered = Math.multiplyExact(line.getQuantity(), line.getUnitPrice());
        long tax = line.getTaxAmount() == null ? 0L : line.getTaxAmount();
        boolean inclusive = line.getTaxPolicy() != null && line.getTaxPolicy().isTaxInclusive();
        long expected = inclusive ? entered : Math.addExact(entered, tax);
        if (tax < 0 || (inclusive && tax > entered) || expected != line.getLineTotal())
            throw new IllegalStateException("Order line total is inconsistent with its tax-inclusive/exclusive policy");
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(OrderLine line) {
        throw new IllegalStateException("Reserved sales order lines cannot be deleted; cancel the order instead");
    }
}
