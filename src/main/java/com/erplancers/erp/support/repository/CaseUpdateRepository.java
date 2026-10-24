package com.erplancers.erp.support.repository;

import com.erplancers.erp.support.domain.CaseUpdate;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CaseUpdateRepository extends ErpRepository<CaseUpdate> {




    List<CaseUpdate> findByTrackerKey(String trackerKey);


}
