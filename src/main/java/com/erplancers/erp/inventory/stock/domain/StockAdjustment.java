package com.erplancers.erp.inventory.stock.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@EntityListeners(StockAdjustmentLifecycleListener.class)
@Table(name="stock_adjustment", uniqueConstraints=@UniqueConstraint(name="uk_stock_adjustment_idempotency", columnNames="idempotency_key"))
@Getter @Setter @NoArgsConstructor
public class StockAdjustment extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Product product;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Store store;
    @Column(name="delta_quantity", nullable=false) private Long deltaQuantity;
    @Column(nullable=false, length=500) private String reason;
    @Column(name="idempotency_key", nullable=false, length=180) private String idempotencyKey;
    @Column(name="payload_hash", nullable=false, length=64) private String payloadHash;
    @Column(name="adjusted_at", nullable=false) private Instant adjustedAt;
    @Transient private boolean commandValidated;
}
