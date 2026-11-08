package com.erplancers.erp.inventory.stock.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Objects;

public class ProductStockLocationLifecycleListener {
    @PostLoad @PostPersist
    public void capture(ProductStockLocation stock) {
        stock.setPersistedQuantity(stock.getQuantity());
    }

    @PrePersist @PreUpdate
    public void validate(ProductStockLocation stock) {
        if (stock.getProduct() == null || stock.getStore() == null || stock.getQuantity() == null || stock.getQuantity() < 0)
            throw new IllegalStateException("Stock balance requires product, warehouse and a non-negative quantity");
        if (stock.getPersistedQuantity() != null && !Objects.equals(stock.getPersistedQuantity(), stock.getQuantity())
                && !stock.isStockAdjustmentValidated())
            throw new IllegalStateException("Stock quantities can only be changed through a stock movement workflow");
    }

    @jakarta.persistence.PostPersist
    public void clearAfterInsert(ProductStockLocation stock) {
        stock.setPersistedQuantity(stock.getQuantity());
        stock.setStockAdjustmentValidated(false);
    }

    @PostUpdate
    public void captureAfterUpdate(ProductStockLocation stock) {
        stock.setPersistedQuantity(stock.getQuantity());
        stock.setStockAdjustmentValidated(false);
    }
}
