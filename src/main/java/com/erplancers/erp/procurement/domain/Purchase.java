package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.customer.domain.Customer;
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

public class Purchase extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Customer customer;
    LocalDateTime purchaseDate;
    LocalDateTime approvedDate;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<PurchaseDetail> purchaseDetails;
}
