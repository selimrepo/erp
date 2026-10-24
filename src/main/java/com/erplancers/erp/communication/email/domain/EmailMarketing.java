package com.erplancers.erp.communication.email.domain;

import com.erplancers.erp.common.domain.EntityBase;

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

public class EmailMarketing extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    EmailAddress fromEmailAddress;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EmailAddress> toEmailAddresses;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Email inbound;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<EmailAddress> outbounds;
}
