package com.erplancers.erp.crm.campaign.repository;

import com.erplancers.erp.crm.campaign.domain.CampaignLog;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CampaignLogRepository extends ErpRepository<CampaignLog> {

    List<CampaignLog> findByHitsGreaterThan(Long hitsIsGreaterThan);

    List<CampaignLog> findByTrackerKey(String trackerKey);

    List<CampaignLog> findByTrackerStatus(Boolean trackerStatus);
}
