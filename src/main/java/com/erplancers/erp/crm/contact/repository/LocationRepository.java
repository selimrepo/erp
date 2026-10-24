package com.erplancers.erp.crm.contact.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface LocationRepository extends ErpRepository<Location> {
    List<Location> findByOwner(Contact owner);
}
