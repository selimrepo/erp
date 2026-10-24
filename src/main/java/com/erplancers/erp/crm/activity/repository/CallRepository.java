package com.erplancers.erp.crm.activity.repository;

import com.erplancers.erp.crm.activity.domain.Call;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CallRepository extends ErpRepository<Call> {
    List<Call> findByName(String name);

    List<Call> queryByCallerContact_Name(String callerContactName);
    
}
