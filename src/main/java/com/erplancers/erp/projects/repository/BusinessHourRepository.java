package com.erplancers.erp.projects.repository;

import com.erplancers.erp.projects.domain.BusinessHour;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;


public interface BusinessHourRepository extends ErpRepository<BusinessHour> {
    List<BusinessHour> findByActive(Boolean active);

    List<BusinessHour> findByBusinessHourNameContainsIgnoreCase(String businessHourName);
}
