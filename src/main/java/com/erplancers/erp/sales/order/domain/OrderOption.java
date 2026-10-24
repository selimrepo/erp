package com.erplancers.erp.sales.order.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
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

public class OrderOption extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Product product;
    String option;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductOption> productOptions;
    String optionTag;
    Long price;
    Long quantity;
}
