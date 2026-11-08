package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @NoArgsConstructor @Getter @Setter
@Table
public class PurchaseDetail extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Purchase purchase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Product product;
    private Long price;
    private Long quantity;
    @Column(nullable = false) private Long receivedQuantity = 0L;
}
