package com.erplancers.erp.finance.repository;

import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CurrencyRepository extends ErpRepository<Currency> {
    List<Currency> findByTitleContainsIgnoreCase(String title);

    List<Currency> findByRateBetween(Float rateAfter, Float rateBefore);
}
