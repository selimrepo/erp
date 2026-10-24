package com.erplancers.erp.inventory.stock.repository;

import com.erplancers.erp.inventory.stock.domain.StockCategory;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface StockCategoryRepository extends ErpRepository<StockCategory> {
    List<StockCategory> findByTitleContainingIgnoreCase(String name);
}
