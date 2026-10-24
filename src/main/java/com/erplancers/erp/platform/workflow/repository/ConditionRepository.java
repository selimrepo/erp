package com.erplancers.erp.platform.workflow.repository;

import com.erplancers.erp.platform.workflow.domain.Condition;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface ConditionRepository extends ErpRepository<Condition> {
    List<Condition> findByConditionNameContainingIgnoreCase(String conditionName);
}
