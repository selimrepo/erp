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
        invoice.setPersistedSettlementFingerprint(settlementFingerprint(invoice));
        invoice.setPersistedLedgerJournalEntryId(invoice.getLedgerJournalEntry() == null ? null : invoice.getLedgerJournalEntry().getId());
        invoice.setCreationValidated(false);
    }

    @PrePersist
    public void validateInsert(Invoice invoice) {
        validate(invoice);
        if (invoice.getInvoiceStatus() == null || invoice.getInvoiceType() == null)
            throw new IllegalStateException("Invoice type and status are required");
        if (invoice.getInvoiceStatus() == InvoiceStatus.CONFIRMED
                && (!invoice.isCreationValidated() || invoice.getWorkflowKey() == null || invoice.getWorkflowKey().isBlank()))
            throw new IllegalStateException("Confirmed invoices can only be created by sales, procurement or credit-note workflows");
        if (isSettledState(invoice.getInvoiceStatus()) || invoice.getInvoiceStatus() == InvoiceStatus.POSTED
                || value(invoice.getPaidAmount()) != 0 || value(invoice.getCreditedAmount()) != 0)
            throw new IllegalStateException("New invoices must be created unsettled as DRAFT or CONFIRMED and then processed by workflow commands");
    }

    @PreUpdate
    public void validateUpdate(Invoice invoice) {
        validate(invoice);
        if (invoice.getPersistedAmountFingerprint() != null
                && !Objects.equals(invoice.getPersistedAmountFingerprint(), amountFingerprint(invoice))
                && !"DRAFT".equals(invoice.getPersistedWorkflowState()))
            throw new IllegalStateException("Confirmed invoice amount, tax, currency and type are immutable; use a credit note");
        if (invoice.getPersistedSettlementFingerprint() != null
                && !Objects.equals(invoice.getPersistedSettlementFingerprint(), settlementFingerprint(invoice))
                && !invoice.isSettlementValidated())
            throw new IllegalStateException("Paid and credited balances can only be changed by settlement workflows");
        Long currentJournalId = invoice.getLedgerJournalEntry() == null ? null : invoice.getLedgerJournalEntry().getId();
        if (!Objects.equals(invoice.getPersistedLedgerJournalEntryId(), currentJournalId)
                && !invoice.isLedgerPostingValidated())
            throw new IllegalStateException("Invoice ledger linkage can only be changed by the posting workflow");
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
        invoice.setPersistedSettlementFingerprint(settlementFingerprint(invoice));
        invoice.setPersistedLedgerJournalEntryId(invoice.getLedgerJournalEntry() == null ? null : invoice.getLedgerJournalEntry().getId());
        invoice.setSettlementValidated(false);
        invoice.setLedgerPostingValidated(false);
        invoice.setCreationValidated(false);
    }

    @PreRemove
    public void preventDelete(Invoice invoice) {
        if (invoice.getInvoiceStatus() != InvoiceStatus.DRAFT && invoice.getInvoiceStatus() != InvoiceStatus.CANCELLED)
            throw new IllegalStateException("Confirmed or settled invoices cannot be deleted; use a credit note");
    }

    private void validate(Invoice invoice) {
        long amount = value(invoice.getAmount()), net = value(invoice.getNetAmount()), tax = value(invoice.getTaxAmount());
        long withholding = value(invoice.getWithholdingAmount()), reverseCharge = value(invoice.getReverseChargeTaxAmount());
        long paid = value(invoice.getPaidAmount()), credited = value(invoice.getCreditedAmount());
        if (amount < 0 || net < 0 || tax < 0 || withholding < 0 || reverseCharge < 0 || paid < 0 || credited < 0)
            throw new IllegalStateException("Invoice monetary values cannot be negative");
        if (amount > 0 && net == 0 && tax == 0) {
            invoice.setNetAmount(amount);
            net = amount;
        }
        if (Math.subtractExact(Math.addExact(net, tax), withholding) != amount)
            throw new IllegalStateException("Invoice net plus tax less withholding must equal payable gross amount");
        if (Math.addExact(paid, credited) > amount)
            throw new IllegalStateException("Invoice payments and credit notes exceed the gross amount");
        if (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE
                && (invoice.getOriginalInvoice() == null || invoice.getCreditNoteReason() == null || invoice.getCreditNoteReason().isBlank()))
            throw new IllegalStateException("Credit notes require an original invoice and a reason");
        if (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE
                && invoice.getCreditNoteReasonCode() == CreditNoteReasonCode.TAX_CORRECTION
                && (invoice.getTaxEvidenceType() == null || invoice.getTaxEvidenceReference() == null
                || invoice.getTaxEvidenceReference().isBlank()))
            throw new IllegalStateException("Tax correction credit notes require supporting evidence");
        if (invoice.getInvoiceStatus() == InvoiceStatus.PAID && (paid < amount || credited != 0))
            throw new IllegalStateException("PAID invoice state requires full payment and no credit note");
        if (invoice.getInvoiceStatus() == InvoiceStatus.CREDITED && (credited < amount || paid != 0))
            throw new IllegalStateException("CREDITED invoice state requires full credit and no payment");
        if (invoice.getInvoiceStatus() == InvoiceStatus.SETTLED
                && (paid <= 0 || credited <= 0 || Math.addExact(paid, credited) != amount))
            throw new IllegalStateException("SETTLED invoice state requires both payment and credit totals to equal gross amount");
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
        if ((invoice.getInvoiceStatus() == InvoiceStatus.DRAFT || invoice.getInvoiceStatus() == InvoiceStatus.CONFIRMED
                || invoice.getInvoiceStatus() == InvoiceStatus.POSTED || invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED)
                && (paid != 0 || credited != 0))
            throw new IllegalStateException("Unsettled invoice states cannot carry paid or credited balances");
    }

    private String amountFingerprint(Invoice invoice) {
        return String.join("|", String.valueOf(invoice.getAmount()), String.valueOf(invoice.getNetAmount()),
                String.valueOf(invoice.getTaxAmount()), String.valueOf(invoice.getWithholdingAmount()),
                String.valueOf(invoice.getReverseChargeTaxAmount()), String.valueOf(invoice.getCreditNoteReason()),
                String.valueOf(invoice.getCreditNoteReasonCode()), String.valueOf(invoice.getTaxEvidenceType()),
                String.valueOf(invoice.getTaxEvidenceReference()),
                String.valueOf(invoice.getCurrency() == null ? null : invoice.getCurrency().getId()),
                String.valueOf(invoice.getInvoiceType()), String.valueOf(invoice.getOriginalInvoice() == null ? null : invoice.getOriginalInvoice().getId()));
    }
    private String settlementFingerprint(Invoice invoice) {
        return value(invoice.getPaidAmount()) + "|" + value(invoice.getCreditedAmount());
    }
    private boolean isSettledState(InvoiceStatus status) {
        return status == InvoiceStatus.PARTIALLY_PAID || status == InvoiceStatus.PARTIALLY_CREDITED
                || status == InvoiceStatus.PARTIALLY_SETTLED || status == InvoiceStatus.PAID
                || status == InvoiceStatus.CREDITED || status == InvoiceStatus.SETTLED;
    }
    private long value(Long value) { return value == null ? 0L : value; }
}
