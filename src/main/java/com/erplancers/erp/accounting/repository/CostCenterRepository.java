package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.accounting.domain.CostCenter;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface CostCenterRepository extends ErpRepository<CostCenter> {
    List<CostCenter> findByCompany_NameContainsOrCompany_LastNameContains(String name,String lastname);

    List<CostCenter> findByDescriptionContainsIgnoreCase(String description);

    List<CostCenter> findByActive(Boolean active);
    List<CostCenter> findByCompany_NameContainsIgnoreCase(String company);

    List<CostCenter> findByValidFromBetweenOrValidToBetween(LocalDate validFromAfter, LocalDate validFromBefore, LocalDate validToAfter, LocalDate validToBefore);
}
