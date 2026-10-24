package com.erplancers.erp.tax.repository;

import com.erplancers.erp.crm.contact.domain.City;
import com.erplancers.erp.tax.domain.Zone;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface ZoneRepository extends ErpRepository<Zone> {
    List<Zone> findByName(String name);

    List<Zone> findByCitiesContaining(Set<City> cities);
}
