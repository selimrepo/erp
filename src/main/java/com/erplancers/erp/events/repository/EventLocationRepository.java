package com.erplancers.erp.events.repository;

import com.erplancers.erp.crm.contact.domain.Address;
import com.erplancers.erp.events.domain.EventLocation;
import com.erplancers.erp.common.repository.ErpRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventLocationRepository extends ErpRepository<EventLocation> {
    List<EventLocation> findByLocationNameContainsIgnoreCase(String locationName, Pageable pageable);

    List<EventLocation> findByAddress_PostcodeContainsIgnoreCase(String addressName);

    List<EventLocation> findByAddress(Address address);
}
