package com.erplancers.erp.inventory.stock.repository;

import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.stock.domain.StockItem;
import com.erplancers.erp.procurement.domain.Supplier;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockItemRepository extends ErpRepository<StockItem> {
    List<StockItem> findByDateInBetween(LocalDate dateInAfter, LocalDate dateInBefore);

    List<StockItem> findByProduct(Product product);

    List<StockItem> findBySupplier(Supplier supplier);
}
