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
@EntityListeners(StockMovementLifecycleListener.class)
@Table(name = "stock_movement", uniqueConstraints = @UniqueConstraint(name = "uk_stock_movement_idempotency", columnNames = "idempotency_key"))
@Getter @Setter @NoArgsConstructor
public class StockMovement extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Product product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Store store;
    @Column(nullable = false) private Long quantity;
    @Column(nullable = false, length = 24) private String movementType;
    @Column(nullable = false, length = 48) private String referenceType;
    @Column(nullable = false, length = 80) private String referenceId;
    @Column(name = "idempotency_key", nullable = false, length = 180) private String idempotencyKey;
    @Column(nullable = false) private Instant occurredAt;
    @Transient private boolean movementValidated;
}
