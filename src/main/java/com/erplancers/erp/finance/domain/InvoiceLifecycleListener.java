package com.erplancers.erp.finance.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import java.util.Objects;

public class InvoiceLifecycleListener {
    @PostLoad @PostPersist
    public void capture(Invoice invoice) {
        invoice.setPersistedAmountFingerprint(amountFingerprint(invoice));
    }

    @PrePersist
    public void validateInsert(Invoice invoice) {
        validate(invoice);
        if (isSettledState(invoice.getInvoiceStatus()) || invoice.getInvoiceStatus() == InvoiceStatus.POSTED)
            throw new IllegalStateException("New invoices must be created as DRAFT or CONFIRMED and then processed by workflow commands");
    }

    @PreUpdate
    public void validateUpdate(Invoice invoice) {
        validate(invoice);
        if (invoice.getPersistedAmountFingerprint() != null
                && !Objects.equals(invoice.getPersistedAmountFingerprint(), amountFingerprint(invoice)))
            throw new IllegalStateException("Confirmed invoice amount, tax, currency and type are immutable; use a credit note");
        if (invoice.getPersistedWorkflowState() != null
                && !Objects.equals(invoice.getPersistedWorkflowState(), invoice.getInvoiceStatus() == null ? null : invoice.getInvoiceStatus().name())
                && isSettledState(invoice.getInvoiceStatus()) && !invoice.isSettlementValidated())
            throw new IllegalStateException("Invoice settlement states can only be changed through payment or credit-note workflows");
        if (invoice.getInvoiceStatus() == InvoiceStatus.POSTED && invoice.getLedgerJournalEntry() == null)
            throw new IllegalStateException("Posted invoices must reference their ledger journal");
    }

    @PostUpdate
    public void captureAfterUpdate(Invoice invoice) {
        invoice.setPersistedAmountFingerprint(amountFingerprint(invoice));
        invoice.setSettlementValidated(false);
    }

    @PreRemove
    public void preventDelete(Invoice invoice) {
        if (invoice.getInvoiceStatus() != InvoiceStatus.DRAFT && invoice.getInvoiceStatus() != InvoiceStatus.CANCELLED)
            throw new IllegalStateException("Confirmed or settled invoices cannot be deleted; use a credit note");
    }

    private void validate(Invoice invoice) {
        long amount = value(invoice.getAmount()), net = value(invoice.getNetAmount()), tax = value(invoice.getTaxAmount());
        long paid = value(invoice.getPaidAmount()), credited = value(invoice.getCreditedAmount());
        if (amount < 0 || net < 0 || tax < 0 || paid < 0 || credited < 0)
            throw new IllegalStateException("Invoice monetary values cannot be negative");
        if (amount > 0 && net == 0 && tax == 0) {
            invoice.setNetAmount(amount);
            net = amount;
        }
        if (Math.addExact(net, tax) != amount)
            throw new IllegalStateException("Invoice net amount plus tax must equal gross amount");
        if (Math.addExact(paid, credited) > amount)
            throw new IllegalStateException("Invoice payments and credit notes exceed the gross amount");
        if (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE
                && (invoice.getOriginalInvoice() == null || invoice.getCreditNoteReason() == null || invoice.getCreditNoteReason().isBlank()))
            throw new IllegalStateException("Credit notes require an original invoice and a reason");
        if (invoice.getInvoiceStatus() == InvoiceStatus.PAID && (paid < amount || credited != 0))
            throw new IllegalStateException("PAID invoice state requires full payment and no credit note");
        if (invoice.getInvoiceStatus() == InvoiceStatus.CREDITED && (credited < amount || paid != 0))
            throw new IllegalStateException("CREDITED invoice state requires full credit and no payment");
        if (invoice.getInvoiceStatus() == InvoiceStatus.SETTLED && Math.addExact(paid, credited) != amount)
            throw new IllegalStateException("SETTLED invoice state requires payment and credit totals to equal gross amount");
        if (invoice.getInvoiceStatus() == InvoiceStatus.PARTIALLY_PAID && (paid <= 0 || credited != 0 || paid >= amount))
            throw new IllegalStateException("PARTIALLY_PAID state requires a positive partial payment");
        if (invoice.getInvoiceStatus() == InvoiceStatus.PARTIALLY_CREDITED && (credited <= 0 || paid != 0 || credited >= amount))
            throw new IllegalStateException("PARTIALLY_CREDITED state requires a positive partial credit");
        if (invoice.getInvoiceStatus() == InvoiceStatus.PARTIALLY_SETTLED
                && (paid <= 0 || credited <= 0 || Math.addExact(paid, credited) >= amount))
            throw new IllegalStateException("PARTIALLY_SETTLED state requires partial payments and credits");
        if (invoice.getInvoiceStatus() != null && invoice.getInvoiceStatus() != InvoiceStatus.DRAFT
                && invoice.getInvoiceStatus() != InvoiceStatus.CANCELLED && amount <= 0)
            throw new IllegalStateException("Confirmed invoices must have a positive gross amount");
    }

    private String amountFingerprint(Invoice invoice) {
        return String.join("|", String.valueOf(invoice.getAmount()), String.valueOf(invoice.getNetAmount()),
                String.valueOf(invoice.getTaxAmount()), String.valueOf(invoice.getCurrency() == null ? null : invoice.getCurrency().getId()),
                String.valueOf(invoice.getInvoiceType()), String.valueOf(invoice.getOriginalInvoice() == null ? null : invoice.getOriginalInvoice().getId()));
    }
    private boolean isSettledState(InvoiceStatus status) {
        return status == InvoiceStatus.PARTIALLY_PAID || status == InvoiceStatus.PARTIALLY_CREDITED
                || status == InvoiceStatus.PARTIALLY_SETTLED || status == InvoiceStatus.PAID
                || status == InvoiceStatus.CREDITED || status == InvoiceStatus.SETTLED;
    }
    private long value(Long value) { return value == null ? 0L : value; }
}
