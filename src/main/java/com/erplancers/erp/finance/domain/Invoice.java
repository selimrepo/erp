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
    @Column(nullable=false) private Long withholdingAmount = 0L;
    @Column(nullable=false) private Long reverseChargeTaxAmount = 0L;
    @OneToMany(mappedBy="invoice",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC")
    private Set<InvoiceTaxComponent> taxComponents = new java.util.LinkedHashSet<>();
    private Long paidAmount = 0L;
    @Column(nullable = false) private Long creditedAmount = 0L;
    @ManyToOne(fetch = FetchType.LAZY) private Invoice originalInvoice;
    @ManyToOne(fetch = FetchType.LAZY) private com.erplancers.erp.accounting.domain.JournalEntry ledgerJournalEntry;
    private String creditNoteReason;
    @Enumerated(EnumType.STRING) @Column(length=32) private CreditNoteReasonCode creditNoteReasonCode;
    @Column(length=120) private String taxEvidenceType;
    @Column(length=240) private String taxEvidenceReference;
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
