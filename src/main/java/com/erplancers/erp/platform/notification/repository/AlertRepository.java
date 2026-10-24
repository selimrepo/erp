package com.erplancers.erp.platform.notification.repository;

import com.erplancers.erp.platform.notification.domain.Alert;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
public interface AlertRepository extends ErpRepository<Alert> {
    List<Alert> findByAlertName(String alertName);

    List<Alert> findByAlertDescriptionContainingIgnoreCase(String alertDescription);

    List findByAlertEnabled(Boolean alertEnabled);

    List findByAlertConfirmed(Boolean alertConfirmed);

    List findByIsRead(Boolean isRead);
}
