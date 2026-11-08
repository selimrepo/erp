package com.erplancers.erp.finance.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostUpdate;
import java.util.Objects;

/** Completed payments are immutable financial facts; only the ledger link may be added later. */
public class PaymentTransactionLifecycleListener {
    @PostLoad @PostPersist
    public void capture(PaymentTransaction payment) {
        payment.setPersistedFinancialFingerprint(fingerprint(payment));
    }

    @PrePersist @PreUpdate
    public void validate(PaymentTransaction payment) {
        if ("COMPLETED".equalsIgnoreCase(payment.getStatus())) {
            if (payment.getInvoice() == null || payment.getCurrency() == null
                    || payment.getPaymentTransactionDirection() == null || payment.getPaymentDate() == null
                    || payment.getAmount() == null || payment.getAmount() <= 0)
                throw new IllegalStateException("Completed payment requires invoice, currency, direction, date and positive amount");
            if (payment.getPayment() != null && !Objects.equals(payment.getPayment(), payment.getAmount()))
                throw new IllegalStateException("Payment amount snapshots must agree");
        }
        if (payment.getLedgerJournalEntry() != null && !"COMPLETED".equalsIgnoreCase(payment.getStatus()))
            throw new IllegalStateException("Only completed payments can be linked to a ledger journal");
        if (payment.getPersistedFinancialFingerprint() != null
                && "COMPLETED".equalsIgnoreCase(payment.getStatus())
                && !Objects.equals(payment.getPersistedFinancialFingerprint(), fingerprint(payment)))
            throw new IllegalStateException("Completed payment financial fields are immutable");
    }

    @PostUpdate
    public void captureAfterUpdate(PaymentTransaction payment) {
        payment.setPersistedFinancialFingerprint(fingerprint(payment));
    }

    @PreRemove
    public void preventDelete(PaymentTransaction payment) {
        if ("COMPLETED".equalsIgnoreCase(payment.getStatus()))
            throw new IllegalStateException("Completed payments cannot be deleted; use a reversal or refund workflow");
    }

    private String fingerprint(PaymentTransaction payment) {
        return String.join("|", String.valueOf(payment.getInvoice() == null ? null : payment.getInvoice().getId()),
                String.valueOf(payment.getCurrency() == null ? null : payment.getCurrency().getId()),
                String.valueOf(payment.getAmount()), String.valueOf(payment.getPaymentTransactionDirection()),
                String.valueOf(payment.getPaymentDate()), String.valueOf(payment.getReferenceId()),
                String.valueOf(payment.getContact() == null ? null : payment.getContact().getId()));
    }
}
