package com.erplancers.erp.inventory.stock.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.procurement.domain.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class StockItem extends EntityBase {
    LocalDate dateIn;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Product product;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Supplier supplier;
}
