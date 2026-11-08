package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    Contact contact;

    @Enumerated(EnumType.STRING)
    PaymentTransactionDirection paymentTransactionDirection;
}
