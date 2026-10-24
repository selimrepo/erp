package com.erplancers.erp.communication.email.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.communication.email.domain.Email;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface EmailRepository extends ErpRepository<Email> {
    List<Email> findByEmailNameContainsIgnoreCase(String emailName);

    List<Email> findBySendingContact(Contact sendingContact);

    List<Email> findByActivated(Boolean activated);
}
