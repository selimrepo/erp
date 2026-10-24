package com.erplancers.erp.crm.contact.repository;

import com.erplancers.erp.crm.contact.domain.Country;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CountryRepository extends ErpRepository<Country> {
    List<Country> findByCountryNameContainingOrCountryCodeContainingOrCountryTypeContaining(String name,String code,String type);
}
