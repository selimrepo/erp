package com.erplancers.erp.inventory.product.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class ProductOption extends EntityBase {
    
    String attributeName;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    ProductOptionValue optionValue;
    String attributeValue;
}
