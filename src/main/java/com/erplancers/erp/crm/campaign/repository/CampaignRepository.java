package com.erplancers.erp.crm.campaign.repository;

import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CampaignRepository extends ErpRepository<Campaign> {
    List<Campaign> findByCampaignNameContainingIgnoreCase(String name);

    List<Campaign> findByTrackerKey(String trackerKey);

    List<Campaign> findByTrackerCountBetween(Integer trackerCountAfter, Integer trackerCountBefore);


    List<Campaign> findByImpressionsBetween(Integer impressionsAfter, Integer impressionsBefore);

    List<Campaign> findByActualRevenueIsBetween(Long actualRevenueAfter, Long actualRevenueBefore);
}
