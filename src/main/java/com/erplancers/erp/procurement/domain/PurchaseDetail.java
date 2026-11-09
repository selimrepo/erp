package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.tax.domain.TaxRate;
import com.erplancers.erp.tax.domain.TaxPolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @EntityListeners(PurchaseDetailLifecycleListener.class) @NoArgsConstructor @Getter @Setter
@Table
public class PurchaseDetail extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Purchase purchase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Product product;
    private Long price;
    private Long quantity;
    @Column(nullable = false) private Long receivedQuantity = 0L;
    @ManyToOne(fetch = FetchType.LAZY) private TaxRate taxRate;
    @ManyToOne(fetch = FetchType.LAZY) private TaxPolicy taxPolicy;
    @Column(nullable = false) private Long taxAmount = 0L;
    @Column(nullable=false) private Long withholdingAmount = 0L;
    @Column(nullable=false) private Long reverseChargeTaxAmount = 0L;
    @OneToMany(mappedBy="purchaseDetail",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC")
    private java.util.List<PurchaseDetailTaxComponent> taxComponents = new java.util.ArrayList<>();
    @Column(nullable = false) private Long lineTotal = 0L;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean receiptValidated;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private Long persistedReceivedQuantity;
}
