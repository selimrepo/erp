package com.erplancers.erp.crm.customer.repository;

import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CustomerRepository extends ErpRepository<Customer> {
    List<Customer> findByContact_NameContainsIgnoreCaseOrContact_NameContainsIgnoreCase(String name,String lastname);

}
