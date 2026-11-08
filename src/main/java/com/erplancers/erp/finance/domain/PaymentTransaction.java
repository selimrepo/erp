package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity @NoArgsConstructor @Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency", columnNames = "idempotency_key"))
public class PaymentTransaction extends EntityBase {
    private String status;
    private LocalDate paymentDate;
    private String description;
    private String data;
    private String referenceId;
    private Long amount;
    private Long quantity;
    private Long payment;
    @Column(name = "idempotency_key", length = 180) private String idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY) private Currency currency;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Invoice invoice;
    @ManyToOne(fetch = FetchType.LAZY) private Contact contact;
    @ManyToOne(fetch = FetchType.LAZY) private com.erplancers.erp.accounting.domain.JournalEntry ledgerJournalEntry;
    @Enumerated(EnumType.STRING) private PaymentTransactionDirection paymentTransactionDirection;
}
