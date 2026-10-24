package com.erplancers.erp.sales.repository;

import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.sales.domain.Sale;
import com.erplancers.erp.inventory.stock.domain.StockItem;
import com.erplancers.erp.procurement.domain.Supplier;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends ErpRepository<Sale> {
    List<Sale> findByDateBetween(LocalDate dateAfter, LocalDate dateBefore);

    List<Sale> findBySupplier(Supplier supplier);

    List<Sale> findByStockItem(StockItem stockItem);

    List<Sale> findByProduct(Product product);
}
