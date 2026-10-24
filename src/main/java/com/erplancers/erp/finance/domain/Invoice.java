package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter

public class Invoice extends EntityBase {
    
    String invoiceName;
    String invoiceDescription;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> billingContacts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> shippingContacts;
    @ManyToOne

    Currency currency;
    Long amount;
    @Enumerated(EnumType.STRING)
    InvoiceStatus invoiceStatus;
    @Enumerated(EnumType.STRING)
    InvoiceType invoiceType;

}
