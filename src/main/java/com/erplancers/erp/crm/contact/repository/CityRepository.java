package com.erplancers.erp.crm.contact.repository;

import com.erplancers.erp.crm.contact.domain.Address;
import com.erplancers.erp.crm.contact.domain.City;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface CityRepository extends ErpRepository<City> {
    List<City> findByCityNameContainsIgnoreCase(String cityName);

    List<City> findByAddressesContaining(Set<Address> addresses);
}
