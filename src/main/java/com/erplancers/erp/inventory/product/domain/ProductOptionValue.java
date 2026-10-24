package com.erplancers.erp.inventory.product.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class ProductOptionValue extends EntityBase {
    
    String attributeName;
    Long priceChange;
    Long quantityChange;
    Long buyPrice;
    Long sellPrice;
    Long width;
    Long height;
    Long length;
}
