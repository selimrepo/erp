package com.erplancers.erp.common.workflow;

import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.stock.domain.ProductStockLocation;
import com.erplancers.erp.inventory.stock.domain.ProductStockLocationLifecycleListener;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.procurement.domain.PurchaseDetail;
import com.erplancers.erp.procurement.domain.PurchaseDetailLifecycleListener;
import com.erplancers.erp.sales.order.domain.Order;
import com.erplancers.erp.sales.order.domain.OrderLifecycleListener;
import com.erplancers.erp.sales.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainLifecycleGuardTest {
    @Test
    void orderCreationAndFulfillmentMustUseCommands() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setIdempotencyKey("order-test");
        order.setIdempotencyPayloadHash("a".repeat(64));
        OrderLifecycleListener listener = new OrderLifecycleListener();

        assertThrows(IllegalStateException.class, () -> listener.validateInsert(order));
        order.setOrderCreationValidated(true);
        assertDoesNotThrow(() -> listener.validateInsert(order));
        listener.clearCreationAuthorization(order);

        order.setPersistedWorkflowState("PROCESSING");
        order.setOrderStatus(OrderStatus.SHIPPED);
        assertThrows(IllegalStateException.class, () -> listener.validateTransition(order));
        order.setFulfillmentValidated(true);
        assertDoesNotThrow(() -> listener.validateTransition(order));
        listener.clearAuthorization(order);
        assertFalse(order.isFulfillmentValidated());
    }

    @Test
    void stockBalanceCannotChangeWithoutAStockMovementCommand() {
        ProductStockLocation stock = new ProductStockLocation();
        stock.setProduct(new Product());
        stock.setStore(new Store());
        stock.setQuantity(10L);
        stock.setPersistedQuantity(10L);
        stock.setQuantity(7L);
        ProductStockLocationLifecycleListener listener = new ProductStockLocationLifecycleListener();

        assertThrows(IllegalStateException.class, () -> listener.validate(stock));
        stock.setStockAdjustmentValidated(true);
        assertDoesNotThrow(() -> listener.validate(stock));
        listener.captureAfterUpdate(stock);
        assertEquals(7L, stock.getPersistedQuantity());
        assertFalse(stock.isStockAdjustmentValidated());
    }

    @Test
    void receivedQuantityCannotBeEditedOutsideGoodsReceipt() {
        PurchaseDetail line = new PurchaseDetail();
        line.setQuantity(10L);
        line.setPrice(25L);
        line.setTaxAmount(0L);
        line.setLineTotal(250L);
        line.setReceivedQuantity(0L);
        line.setPersistedReceivedQuantity(0L);
        line.setReceivedQuantity(4L);
        PurchaseDetailLifecycleListener listener = new PurchaseDetailLifecycleListener();

        assertThrows(IllegalStateException.class, () -> listener.validate(line));
        line.setReceiptValidated(true);
        assertDoesNotThrow(() -> listener.validate(line));
        listener.captureAfterUpdate(line);
        assertEquals(4L, line.getPersistedReceivedQuantity());
        assertFalse(line.isReceiptValidated());
    }
}
