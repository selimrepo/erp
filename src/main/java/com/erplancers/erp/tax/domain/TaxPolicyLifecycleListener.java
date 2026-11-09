package com.erplancers.erp.tax.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.math.BigDecimal;

public class TaxPolicyLifecycleListener {
    @PrePersist
    @PreUpdate
    public void validate(TaxPolicy policy) {
        if (policy.getName() == null || policy.getName().isBlank())
            throw new IllegalStateException("Tax policy name is required");
        BigDecimal rate = policy.getRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalStateException("Tax policy rate must be between 0 and 100 percent");
        if (policy.getValidFrom() == null || policy.getValidUntil() == null
                || policy.getValidUntil().isBefore(policy.getValidFrom()))
            throw new IllegalStateException("Tax policy validity dates are invalid");
        if (policy.getPriority() == null || policy.getPriority() < 0)
            throw new IllegalStateException("Tax policy priority cannot be negative");
        policy.setJurisdictionCode(normalize(policy.getJurisdictionCode()));
        policy.setTransactionType(normalize(policy.getTransactionType()));
        policy.setTaxCategory(normalize(policy.getTaxCategory()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
