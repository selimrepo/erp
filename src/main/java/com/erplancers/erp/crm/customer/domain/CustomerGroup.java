package com.erplancers.erp.crm.customer.domain;

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

public class CustomerGroup extends EntityBase {
    String customerGroupName;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    CustomerGroup customers;
    
}
