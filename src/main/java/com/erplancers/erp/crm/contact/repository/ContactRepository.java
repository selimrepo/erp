package com.erplancers.erp.crm.contact.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface ContactRepository extends ErpRepository<Contact> {
    List<Contact> findByNameOrLastNameContainingIgnoreCase(String name, String lastName);

    List<Contact> findByDonotCall(Boolean donotCall);

    List<Contact> findByIsCompany(Boolean isCompany);
}
