package com.erplancers.erp.sales.quote.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDate;
import java.util.Set;

public class QuoteLifecycleListener {
    private static final Set<String> STATES = Set.of("DRAFT", "SENT", "ACCEPTED", "REJECTED", "EXPIRED", "CONVERTED");

    @PrePersist
    public void validateInsert(Quote quote) {
        validate(quote);
        if ("CONVERTED".equals(quote.getStatus()))
            throw new IllegalStateException("Quotes must be converted through the order conversion workflow");
    }

    @PreUpdate
    public void validateUpdate(Quote quote) {
        validate(quote);
        if ("CONVERTED".equals(quote.getStatus())
                && !"CONVERTED".equals(quote.getPersistedWorkflowState()) && !quote.isConversionValidated())
            throw new IllegalStateException("Quote conversion must run through the transactional order workflow");
    }

    @jakarta.persistence.PostUpdate
    public void clearAuthorization(Quote quote) { quote.setConversionValidated(false); }

    private void validate(Quote quote) {
        if (quote.getStatus() == null || !STATES.contains(quote.getStatus()))
            throw new IllegalStateException("Invalid quote status: " + quote.getStatus());
        if (Set.of("SENT", "ACCEPTED").contains(quote.getStatus())) {
            if (quote.getContactBilling() == null || quote.getContactShipping() == null || quote.getCurrency() == null)
                throw new IllegalStateException("Sent or accepted quotes require billing contact, shipping contact and currency");
            if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now()))
                throw new IllegalStateException("Expired quotes cannot be sent or accepted");
        }
        if ("CONVERTED".equals(quote.getStatus())
                && (quote.getConvertedOrder() == null || quote.getConversionIdempotencyKey() == null
                    || quote.getConversionPayloadHash() == null))
            throw new IllegalStateException("Converted quote must reference its order and conversion fingerprint");
    }
}
