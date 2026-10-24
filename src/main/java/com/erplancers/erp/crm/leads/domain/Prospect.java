package com.erplancers.erp.crm.leads.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.projects.domain.Project;
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

public class Prospect extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact contact;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Project project;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Location location;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Lead lead;
}
