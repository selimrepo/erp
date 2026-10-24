package com.erplancers.erp.crm.opportunity.repository;

import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.common.repository.ErpRepository;
import com.erplancers.erp.projects.domain.Project;

import java.util.List;
import java.util.Set;

public interface OpportunityRepository extends ErpRepository<Opportunity> {
    List<Opportunity> findByCampaignsContaining(Set<Campaign> campaigns);

    List<Opportunity> findByProductsContaining(Set<Product> products);

    List<Opportunity> findByNameContainingIgnoreCase(String name);

    List<Opportunity> findByProjectsContaining(Set<Project> projects);

    List<Opportunity> findByLeadsContaining(Set<Lead> leads);
}
