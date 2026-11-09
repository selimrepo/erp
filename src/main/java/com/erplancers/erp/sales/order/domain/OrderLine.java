package com.erplancers.erp.sales.order.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.tax.domain.TaxRate;
import com.erplancers.erp.tax.domain.TaxPolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(OrderLineLifecycleListener.class)
@Table(name = "erp_order_line")
@Getter @Setter @NoArgsConstructor
public class OrderLine extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;
    @Column(nullable = false)
    private Long quantity;
    @Column(nullable = false)
    private Long unitPrice;
    @Column(nullable = false)
    private Long lineTotal;
    @ManyToOne(fetch = FetchType.LAZY) private TaxRate taxRate;
    @ManyToOne(fetch = FetchType.LAZY) private TaxPolicy taxPolicy;
    @Column(nullable = false) private Long taxAmount = 0L;
}
