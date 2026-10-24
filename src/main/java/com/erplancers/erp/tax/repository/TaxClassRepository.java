package com.erplancers.erp.tax.repository;

import com.erplancers.erp.tax.domain.TaxClass;
import com.erplancers.erp.tax.domain.TaxRate;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface TaxClassRepository extends ErpRepository<TaxClass> {
    List<TaxClass> findByTitleContainsIgnoreCase(String title);

    List<TaxClass> findByTaxRatesContaining(Set<TaxRate> taxRates);
}
