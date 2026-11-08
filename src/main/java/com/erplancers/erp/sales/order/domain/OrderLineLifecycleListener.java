package com.erplancers.erp.sales.order.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class OrderLineLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(OrderLine line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getUnitPrice() == null || line.getUnitPrice() < 0)
            throw new IllegalStateException("Order line quantity must be positive and unit price non-negative");
        long net = Math.multiplyExact(line.getQuantity(), line.getUnitPrice());
        long tax = line.getTaxAmount() == null ? 0L : line.getTaxAmount();
        if (tax < 0 || Math.addExact(net, tax) != line.getLineTotal())
            throw new IllegalStateException("Order line total must equal net plus tax");
    }
}
