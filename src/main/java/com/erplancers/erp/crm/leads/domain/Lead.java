package com.erplancers.erp.crm.leads.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.campaign.domain.Campaign;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
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

public class Lead extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact contact;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact assistantContact;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Opportunity> opportunities;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Campaign> campaigns;
}
