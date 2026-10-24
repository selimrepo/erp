package com.erplancers.erp.sales.pricing.repository;

import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.sales.pricing.domain.ProductDiscount;
import com.erplancers.erp.sales.pricing.domain.ProductSpecials;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ProductSpecialsRepository extends ErpRepository<ProductSpecials> {
    List<ProductSpecials> findByTimeStartBetween(LocalDate timeStartAfter, LocalDate timeStartBefore);

    List<ProductSpecials> findByTimeEndBetween(LocalDate timeEndAfter, LocalDate timeEndBefore);

    List<ProductSpecials> findByCustomerGroupsContains(Set<CustomerGroup> customerGroups);

    List<ProductSpecials> findByProductDiscountsContaining(Set<ProductDiscount> productDiscounts);
}
