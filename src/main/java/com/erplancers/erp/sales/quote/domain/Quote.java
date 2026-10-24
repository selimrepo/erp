package com.erplancers.erp.sales.quote.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.sales.domain.Sale;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
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

public class Quote extends EntityBase {
    String name;
    LocalDate date;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact contactBilling;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact contactShipping;
    Long numberQuote;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Opportunity> opportunities;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Sale> sales;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Invoice> invoices;

}
