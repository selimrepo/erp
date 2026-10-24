package com.erplancers.erp.crm.customer.repository;

import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CustomerGroupRepository extends ErpRepository<CustomerGroup> {
    List<CustomerGroup> findByCustomerGroupNameContainsIgnoreCase(String customerGroupName);
}
