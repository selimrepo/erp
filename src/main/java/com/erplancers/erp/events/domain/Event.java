package com.erplancers.erp.events.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.leads.domain.Lead;
import com.erplancers.erp.crm.leads.domain.Prospect;
import com.erplancers.erp.finance.domain.Currency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Event extends EntityBase {
    
    String eventName;
    LocalDateTime timeStart;
    LocalDateTime timeEnd;
    Long budget;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    EventLocation location;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> contacts;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Lead> leads;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Prospect> prospects;
}
