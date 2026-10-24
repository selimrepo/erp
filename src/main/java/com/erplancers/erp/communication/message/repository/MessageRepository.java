package com.erplancers.erp.communication.message.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.communication.message.domain.Message;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface MessageRepository extends ErpRepository<Message> {
    List<Message> findByMessageContainingIgnoreCase(String message);

    List<Message> findByReceiversContaining(Set<Contact> receivers);

    List<Message> findByTopicContainingIgnoreCase(String topic);
}
