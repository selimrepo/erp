package com.erplancers.erp.crm.opportunity.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.projects.domain.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Opportunity extends EntityBase {
    String name;
    LocalDate date;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Campaign> campaigns;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Lead> leads;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Product> products;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Project> projects;
}
