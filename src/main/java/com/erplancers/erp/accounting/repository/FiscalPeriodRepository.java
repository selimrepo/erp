package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.accounting.domain.FiscalPeriod;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface FiscalPeriodRepository extends ErpRepository<FiscalPeriod> {
    List<FiscalPeriod> findByNameContainsIgnoreCase(String name);

    List<FiscalPeriod> findByPeriodNumber(Integer periodNumber);

    List<FiscalPeriod> findByStartDateBetween(LocalDate startDateAfter, LocalDate startDateBefore);

    List<FiscalPeriod> findByEndDateBetween(LocalDate endDateAfter, LocalDate endDateBefore);
}
