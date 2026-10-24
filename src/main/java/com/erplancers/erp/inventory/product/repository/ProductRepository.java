package com.erplancers.erp.inventory.product.repository;

import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.product.domain.ProductOption;
import com.erplancers.erp.inventory.stock.domain.StockStatus;
import com.erplancers.erp.common.repository.ErpRepository;
import com.erplancers.erp.procurement.domain.Supplier;

import java.util.List;
import java.util.Set;

public interface ProductRepository extends ErpRepository<Product> {
    List<Product> findByProductNameContainingIgnoreCase(String productName);

    List<Product> findByProductDescriptionContainingIgnoreCase(String productDescription);

    List<Product> findByStockStatus(StockStatus stockStatus);

    List<Product> findByManufacturersContaining(Set<Manufacturer> manufacturers);

    List<Product> findBySuppliersContaining(Set<Supplier> suppliers);

    List<Product> findByBuyPriceBetween(Long buyPriceAfter, Long buyPriceBefore);

    List<Product> findBySellPriceBetween(Long sellPriceAfter, Long sellPriceBefore);

    List<Product> findByLengthBetween(Long lengthAfter, Long lengthBefore);

    List<Product> findByWidthBetween(Long widthAfter, Long widthBefore);

    List<Product> findByHeightBetween(Long heightAfter, Long heightBefore);

    List<Product> findByOptionsContaining(Set<ProductOption> options);
}
