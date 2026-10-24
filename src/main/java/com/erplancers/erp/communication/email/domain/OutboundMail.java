package com.erplancers.erp.communication.email.domain;

import com.erplancers.erp.common.domain.EntityBase;

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

public class OutboundMail extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Email email;
}
