package com.erplancers.erp.sales.pricing.repository;

import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.sales.pricing.domain.ProductDiscount;
import com.erplancers.erp.inventory.product.domain.ProductOption;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface ProductDiscountRepository extends ErpRepository<ProductDiscount> {
    List<ProductDiscount> findByCustomerGroupsContains(Set<CustomerGroup> customerGroups);

    List<ProductDiscount> findByOption(ProductOption option);

    List<ProductDiscount> findByDiscountBetween(Long discountAfter, Long discountBefore);
}
