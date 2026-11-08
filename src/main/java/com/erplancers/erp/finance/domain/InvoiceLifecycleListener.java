package com.erplancers.erp.finance.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class InvoiceLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(Invoice invoice) {
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
    }
    private long value(Long value) { return value == null ? 0L : value; }
}
