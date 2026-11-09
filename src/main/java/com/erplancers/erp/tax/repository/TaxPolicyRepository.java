package com.erplancers.erp.tax.repository;

import com.erplancers.erp.tax.domain.TaxPolicy;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaxPolicyRepository extends ErpRepository<TaxPolicy> {
    List<TaxPolicy> findByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
            LocalDate dateFrom, LocalDate dateUntil);
}
