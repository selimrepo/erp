package com.erplancers.erp.crm.activity.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.warehouse.domain.Store;
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

public class Meeting extends EntityBase {
    @ManyToOne
    Location location;
    @OneToMany
    Set<Call> calls;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> contacts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Store> stores;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Product> productstoDiscussion;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Lead> leads;

}
