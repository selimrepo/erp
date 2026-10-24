package com.erplancers.erp.support.repository;

import com.erplancers.erp.support.domain.CaseEvent;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface CaseEventRepository extends ErpRepository<CaseEvent> {

    List<CaseEvent> findByCaseEventDateBetween(LocalDate caseEventDateAfter, LocalDate caseEventDateBefore);
}
