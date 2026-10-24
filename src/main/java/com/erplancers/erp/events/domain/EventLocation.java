package com.erplancers.erp.events.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Address;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class EventLocation extends EntityBase {
    
    String locationName;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Address address;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact contactOwner;
    Integer capacity;

}
