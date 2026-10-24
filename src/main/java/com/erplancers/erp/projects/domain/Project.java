package com.erplancers.erp.projects.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.sales.pricing.domain.ProductDiscount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
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

public class Project extends EntityBase {
    
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Product> products;
    String name;
    String description;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Location location;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Customer customer;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProductDiscount> productDiscounts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Assignment> assignments;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<ProjectTask> projectTasks;
}
