package com.erplancers.erp.content.knowledge.repository;

import com.erplancers.erp.content.knowledge.domain.HelpCategory;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface HelpCategoryRepository extends ErpRepository<HelpCategory> {
    List<HelpCategory> findByCategoryNameContainsIgnoreCase(String categoryName);
}
