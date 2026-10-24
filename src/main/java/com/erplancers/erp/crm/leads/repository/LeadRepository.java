package com.erplancers.erp.crm.leads.repository;

import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface LeadRepository extends ErpRepository<Lead> {
    List<Lead> findByContact(Contact contact);

    List<Lead> findByOpportunitiesContaining(Set<Opportunity> opportunities);

    List<Lead> findByCampaignsContaining(Set<Campaign> campaigns);
}
