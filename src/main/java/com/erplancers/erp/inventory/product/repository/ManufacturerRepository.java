package com.erplancers.erp.inventory.product.repository;

import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface ManufacturerRepository extends ErpRepository<Manufacturer> {
    List<Manufacturer> findByProductsContaining(Set<Product> products);

    List<Manufacturer> findByCampaignsContaining(Set<Campaign> campaigns);

    List<Manufacturer> findByOpportunitiesContaining(Set<Opportunity> opportunities);

    List<Manufacturer> findByStoresContaining(Set<Store> stores);
}
