package com.erplancers.erp.inventory.stock.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.procurement.domain.Supplier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

@Entity @NoArgsConstructor @Getter @Setter @Table
public class ProductStockLocation extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Product product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Store store;
    @ManyToMany(fetch = FetchType.LAZY) private Set<Location> locations;
    private Long quantity;
    @ManyToMany(fetch = FetchType.LAZY) private Set<Supplier> suppliers;
    @ManyToMany(fetch = FetchType.LAZY) private Set<Manufacturer> manufacturers;
}
