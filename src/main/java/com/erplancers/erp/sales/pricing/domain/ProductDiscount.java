package com.erplancers.erp.sales.pricing.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.inventory.product.domain.ProductOption;
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

public class ProductDiscount extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    ProductOption option;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CustomerGroup> customerGroups;
    Long discount;
}
