package com.erplancers.erp.inventory.product.domain;
import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.inventory.stock.domain.StockStatus;
import com.erplancers.erp.procurement.domain.Supplier;
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

public class Product extends EntityBase {
    
    String productName;
    String productDescription;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Location location;
    @Enumerated(EnumType.STRING)
    StockStatus stockStatus;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Manufacturer> manufacturers;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Supplier> suppliers;
    Long buyPrice;
    Long sellPrice;
    Long length;
    Long width;
    Long height;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductOption> options;

}
