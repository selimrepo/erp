package com.erplancers.erp.sales.quote.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class QuoteLineLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(QuoteLine line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getUnitPrice() == null || line.getUnitPrice() < 0)
            throw new IllegalStateException("Quote line quantity must be positive and unit price non-negative");
        long net = Math.multiplyExact(line.getQuantity(), line.getUnitPrice());
        long tax = line.getTaxAmount() == null ? 0L : line.getTaxAmount();
        if (tax < 0 || Math.addExact(net, tax) != line.getLineTotal())
            throw new IllegalStateException("Quote line total must equal net plus tax");
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(QuoteLine line) {
        if (line.getQuote() != null && !"DRAFT".equals(line.getQuote().getStatus()))
            throw new IllegalStateException("Sent, accepted or converted quote lines cannot be deleted");
    }
}
