package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class PurchaseDetail extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Supplier supplier;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Product product;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<PurchaseDetail> purchaseDetailsRelated;
    Long price;
    Long quantity;
}
