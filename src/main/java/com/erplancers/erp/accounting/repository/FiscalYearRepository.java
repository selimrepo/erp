package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.accounting.domain.FiscalYear;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface FiscalYearRepository extends ErpRepository<FiscalYear> {
    List<FiscalYear> findByCompany(Contact company);
}
