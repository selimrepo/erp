package com.erplancers.erp.inventory.stock.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.inventory.product.domain.Product;
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

public class ProductStockLocation extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Product product;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Location> locations;
    Long quantity;
    @OneToMany
    Set<Supplier> suppliers;
    @OneToMany
    Set<Manufacturer> manufacturers;
}
