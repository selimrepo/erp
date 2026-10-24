package com.erplancers.erp.crm.customer.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.procurement.domain.Purchase;
import com.erplancers.erp.sales.domain.Sale;
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

public class Customer extends EntityBase {
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact contact;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    CustomerGroup customerGroup;
    @OneToMany
    Set<Purchase> purchases;
    @OneToMany
    Set<Sale> sales;
    @OneToMany
    Set<CustomerTransaction> customerTransactions;
    
}
