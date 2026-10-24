package com.erplancers.erp.finance.domain;

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

public class PaymentTransaction extends EntityBase {
    
    String status;
    String description;
    String data;
    String referenceId;
    Long amount;
    Long quantity;
    Long payment;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    @Enumerated(EnumType.STRING)
    PaymentTransactionDirection paymentTransactionDirection;
}
