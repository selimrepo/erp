package com.erplancers.erp.inventory.product.repository;

import com.erplancers.erp.inventory.product.domain.ProductOption;
import com.erplancers.erp.inventory.product.domain.ProductOptionValue;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface ProductOptionRepository extends ErpRepository<ProductOption> {
    List<ProductOption> findByAttributeNameContainingIgnoreCase(String attributeName);

    List<ProductOption> findByOptionValue(ProductOptionValue optionValue);

    List<ProductOption> findByAttributeValueIsContainingIgnoreCase(String attributeValue);
}
