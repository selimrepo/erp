package com.erplancers.erp.inventory.product.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.sales.pricing.domain.ProductDiscount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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

public class ProductsFeatured extends EntityBase {
    
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Product> products;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CustomerGroup> customerGroups;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductDiscount> productDiscounts;

}
