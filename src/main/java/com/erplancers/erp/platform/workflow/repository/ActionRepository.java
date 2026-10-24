package com.erplancers.erp.platform.workflow.repository;

import com.erplancers.erp.platform.workflow.domain.Action;
import com.erplancers.erp.common.repository.ErpRepository;
import java.util.List;


public interface ActionRepository extends ErpRepository<Action> {
    List<Action> findByActionDescription(String description);
    List<Action> findByActionNameContainingIgnoreCase(String actionName);

    

    List<Action> findByActionTypeContainingIgnoreCase(String actionType);
}
