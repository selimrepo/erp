package com.erplancers.erp.platform.notification.repository;


import com.erplancers.erp.platform.notification.domain.Announcement;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface AnnouncementRepository extends ErpRepository<Announcement> {
    List<Announcement> findByAutoPublish(Boolean autoPublish);

    List<Announcement> findByIsActive(Boolean isActive);

    List<Announcement> findByTopicContainingIgnoreCase(Boolean topic);
}
