package com.erplancers.erp.sales.order.domain;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class OrderLifecycleListener {
    @PrePersist
    public void validateInsert(Order order) {
        if (order.getOrderStatus() != OrderStatus.PROCESSING)
            throw new IllegalStateException("Orders must enter the workflow in PROCESSING state");
    }

    @PreUpdate
    public void validateTransition(Order order) {
        String previous = order.getPersistedWorkflowState();
        String current = order.getOrderStatus() == null ? null : order.getOrderStatus().name();
        if (previous != null && !previous.equals(current) && !order.isFulfillmentValidated())
            throw new IllegalStateException("Order fulfillment state can only change through the shipment/cancellation workflow");
    }

    @PostUpdate
    public void clearAuthorization(Order order) {
        order.setFulfillmentValidated(false);
    }

    @PreRemove
    public void preventDelete(Order order) {
        if (order.getOrderStatus() != OrderStatus.CANCELLED)
            throw new IllegalStateException("Active or shipped orders cannot be deleted");
    }
}
