package com.erplancers.erp.inventory.product.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Manufacturer extends EntityBase {


    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Product> products;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Campaign> campaigns;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Opportunity> opportunities;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Store> stores;

}
