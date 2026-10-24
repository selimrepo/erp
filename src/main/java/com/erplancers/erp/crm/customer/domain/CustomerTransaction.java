package com.erplancers.erp.crm.customer.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class CustomerTransaction extends EntityBase {
    String transactionName;
    BigDecimal amount;
    @Enumerated(EnumType.STRING)
    CustomerTransactionDirection customerTransactionDirection;
}
