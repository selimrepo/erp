package com.erplancers.erp.tax.repository;

import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.tax.domain.TaxRate;
import com.erplancers.erp.tax.domain.Zone;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface TaxRateRepository extends ErpRepository<TaxRate> {
    List<TaxRate> findByNameContainingIgnoreCase(String name);

    List<TaxRate> findByZone(Zone zone);

    List<TaxRate> findByLocation(Location location);

    List<TaxRate> findByCustomerGroupsContains(Set<CustomerGroup> customerGroups);
}
