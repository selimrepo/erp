package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

@Entity @EntityListeners(InvoiceLifecycleListener.class) @NoArgsConstructor @Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_invoice_workflow_key", columnNames = "workflow_key"))
public class Invoice extends EntityBase {
    private String invoiceName;
    private String invoiceDescription;
    @ManyToMany(fetch = FetchType.LAZY) private Set<Contact> billingContacts;
    @ManyToMany(fetch = FetchType.LAZY) private Set<Contact> shippingContacts;
    @ManyToOne(fetch = FetchType.LAZY) private Currency currency;
    private Long amount;
    @Column(nullable = false) private Long netAmount = 0L;
    @Column(nullable = false) private Long taxAmount = 0L;
    private Long paidAmount = 0L;
    @Column(nullable = false) private Long creditedAmount = 0L;
    @ManyToOne(fetch = FetchType.LAZY) private Invoice originalInvoice;
    @ManyToOne(fetch = FetchType.LAZY) private com.erplancers.erp.accounting.domain.JournalEntry ledgerJournalEntry;
    private String creditNoteReason;
    @Column(name = "workflow_key", length = 180) private String workflowKey;
    @Enumerated(EnumType.STRING) private InvoiceStatus invoiceStatus;
    @Enumerated(EnumType.STRING) private InvoiceType invoiceType;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private String persistedAmountFingerprint;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private String persistedSettlementFingerprint;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private Long persistedLedgerJournalEntryId;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean ledgerPostingValidated;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean settlementValidated;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean creationValidated;
}
