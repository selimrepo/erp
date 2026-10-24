package com.erplancers.erp.inventory.stock.repository;

import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.stock.domain.ProductStockLocation;
import com.erplancers.erp.common.repository.ErpRepository;
import com.erplancers.erp.procurement.domain.Supplier;

import java.util.List;
import java.util.Set;

public interface ProductStockLocationRepository extends ErpRepository<ProductStockLocation> {
    List<ProductStockLocation> findByLocationsContaining(Set<Location> locations);

    List<ProductStockLocation> findByProductAndQuantity(Product product, Long quantity);

    List<ProductStockLocation> findBySuppliersContaining(Set<Supplier> suppliers);

    List<ProductStockLocation> findByManufacturersContaining(Set<Manufacturer> manufacturers);
}
