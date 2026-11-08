package com.erplancers.erp.inventory.stock.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Objects;

public class ProductStockLocationLifecycleListener {
    @PostLoad
    public void captureLoaded(ProductStockLocation stock) {
        capture(stock);
    }

    @PostPersist
    public void captureAfterInsert(ProductStockLocation stock) {
        capture(stock);
        stock.setStockAdjustmentValidated(false);
    }

    @PrePersist
    @PreUpdate
    public void validate(ProductStockLocation stock) {
        if (stock.getProduct() == null || stock.getStore() == null || stock.getQuantity() == null || stock.getQuantity() < 0)
            throw new IllegalStateException("Stock balance requires product, warehouse and a non-negative quantity");
        if (stock.getPersistedStockKey() != null && !Objects.equals(stock.getPersistedStockKey(), stockKey(stock)))
            throw new IllegalStateException("A stock balance cannot be moved to another product or warehouse");
        if (stock.getPersistedQuantity() != null && !Objects.equals(stock.getPersistedQuantity(), stock.getQuantity())
                && !stock.isStockAdjustmentValidated())
            throw new IllegalStateException("Stock quantities can only be changed through a stock movement workflow");
    }

    @PostUpdate
    public void captureAfterUpdate(ProductStockLocation stock) {
        capture(stock);
        stock.setStockAdjustmentValidated(false);
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(ProductStockLocation stock) {
        if (stock.getQuantity() != null && stock.getQuantity() != 0)
            throw new IllegalStateException("Non-zero stock balances cannot be deleted");
    }

    private void capture(ProductStockLocation stock) {
        stock.setPersistedQuantity(stock.getQuantity());
        stock.setPersistedStockKey(stockKey(stock));
    }

    private String stockKey(ProductStockLocation stock) {
        return String.valueOf(stock.getProduct() == null ? null : stock.getProduct().getId())
                + "|" + String.valueOf(stock.getStore() == null ? null : stock.getStore().getId());
    }
}
