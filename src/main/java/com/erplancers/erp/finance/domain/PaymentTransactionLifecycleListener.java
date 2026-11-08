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
        payment.setPersistedPaymentStatus(payment.getStatus());
        payment.setPersistedLedgerJournalEntryId(payment.getLedgerJournalEntry() == null ? null : payment.getLedgerJournalEntry().getId());
        payment.setSettlementValidated(false);
    }

    @PrePersist @PreUpdate
    public void validate(PaymentTransaction payment) {
        if ("COMPLETED".equalsIgnoreCase(payment.getStatus())) {
            if (payment.getPersistedPaymentStatus() == null && !payment.isSettlementValidated())
                throw new IllegalStateException("Completed payments can only be created by the invoice settlement workflow");
            if (payment.getIdempotencyKey() == null || payment.getIdempotencyPayloadHash() == null
                    || !payment.getIdempotencyPayloadHash().matches("[0-9a-f]{64}")
                    || payment.getInvoice() == null || payment.getCurrency() == null
                    || payment.getPaymentTransactionDirection() == null || payment.getPaymentDate() == null
                    || payment.getAmount() == null || payment.getAmount() <= 0)
                throw new IllegalStateException("Completed payment requires invoice, currency, direction, date and positive amount");
            if (payment.getPayment() != null && !Objects.equals(payment.getPayment(), payment.getAmount()))
                throw new IllegalStateException("Payment amount snapshots must agree");
        }
        if (payment.getLedgerJournalEntry() != null && !"COMPLETED".equalsIgnoreCase(payment.getStatus()))
            throw new IllegalStateException("Only completed payments can be linked to a ledger journal");
        if (payment.getPersistedPaymentStatus() != null
                && !payment.getPersistedPaymentStatus().equalsIgnoreCase(payment.getStatus())
                && !payment.isSettlementValidated())
            throw new IllegalStateException("Payment status can only change through a settlement workflow");
        if ("COMPLETED".equalsIgnoreCase(payment.getPersistedPaymentStatus())
                && !"COMPLETED".equalsIgnoreCase(payment.getStatus()))
            throw new IllegalStateException("Completed payments cannot be reopened through status updates");
        if (payment.getPersistedFinancialFingerprint() != null
                && "COMPLETED".equalsIgnoreCase(payment.getStatus())
                && !Objects.equals(payment.getPersistedFinancialFingerprint(), fingerprint(payment)))
            throw new IllegalStateException("Completed payment financial fields are immutable");
        Long currentJournalId = payment.getLedgerJournalEntry() == null ? null : payment.getLedgerJournalEntry().getId();
        if (!Objects.equals(payment.getPersistedLedgerJournalEntryId(), currentJournalId)
                && !payment.isLedgerPostingValidated())
            throw new IllegalStateException("Payment ledger linkage can only be changed by the posting workflow");
    }

    @PostUpdate
    public void captureAfterUpdate(PaymentTransaction payment) {
        payment.setPersistedFinancialFingerprint(fingerprint(payment));
        payment.setPersistedPaymentStatus(payment.getStatus());
        payment.setPersistedLedgerJournalEntryId(payment.getLedgerJournalEntry() == null ? null : payment.getLedgerJournalEntry().getId());
        payment.setLedgerPostingValidated(false);
        payment.setSettlementValidated(false);
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
