package com.erplancers.erp.inventory.stock.domain;

import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StockAdjustmentLifecycleListenerTest {
    private final StockAdjustmentLifecycleListener listener = new StockAdjustmentLifecycleListener();

    @Test
    void stockAdjustmentRequiresCommandAndImmutablePayload() {
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(new Product());
        adjustment.setStore(new Store());
        adjustment.setDeltaQuantity(5L);
        adjustment.setReason("Cycle count");
        adjustment.setIdempotencyKey("stock-adjustment-1");
        adjustment.setPayloadHash("a".repeat(64));
        adjustment.setAdjustedAt(Instant.now());

        assertThrows(IllegalStateException.class, () -> listener.validateInsert(adjustment));
        adjustment.setCommandValidated(true);
        assertDoesNotThrow(() -> listener.validateInsert(adjustment));
        listener.clearAuthorization(adjustment);
        assertFalse(adjustment.isCommandValidated());
        assertThrows(IllegalStateException.class, () -> listener.preventUpdate(adjustment));
        assertThrows(IllegalStateException.class, () -> listener.preventDelete(adjustment));
    }
}
