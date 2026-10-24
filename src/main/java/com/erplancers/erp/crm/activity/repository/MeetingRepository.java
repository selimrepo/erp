package com.erplancers.erp.crm.activity.repository;

import com.erplancers.erp.crm.activity.domain.Meeting;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface MeetingRepository extends ErpRepository<Meeting> {
    List<Meeting> findByLocation(Location location);

    List<Meeting> findByContactsContaining(Set<Contact> contacts);

    List<Meeting> findByStoresContaining(Set<Store> stores);

    List<Meeting> findByProductstoDiscussionContaining(Set<Product> productstoDiscussion);

    List<Meeting> findByLeadsContaining(Set<Lead> leads);
}
