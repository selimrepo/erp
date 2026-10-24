package com.erplancers.erp.platform.classification.repository;

import com.erplancers.erp.platform.classification.domain.Category;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CategoryRepository extends ErpRepository<Category> {
    List<Category> findByCategoryNameContainsIgnoreCase(String categoryName);
}
