package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.accounting.domain.ProfitCenter;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ProfitCenterRepository extends ErpRepository<ProfitCenter> {
    List<ProfitCenter> findByCompany(Contact company);
    List<ProfitCenter> findByCodeContainingIgnoreCase(String code);

    List<ProfitCenter> findByName(String name);

    List<ProfitCenter> findByParent(ProfitCenter parent);

    List<ProfitCenter> findByChildrenContaining(Set<ProfitCenter> children);

    List<ProfitCenter> findByActive(boolean active);

    List<ProfitCenter> findByValidFromBetween(LocalDate validFromAfter, LocalDate validFromBefore);

    List<ProfitCenter> findByValidToBetween(LocalDate validToAfter, LocalDate validToBefore);
}
