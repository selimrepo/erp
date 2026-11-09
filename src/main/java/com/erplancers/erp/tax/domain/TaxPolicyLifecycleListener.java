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
        if (policy.getTreatment() == TaxTreatment.EXEMPT
                && (policy.getExemptionCode() == null || policy.getExemptionCode().isBlank()))
            throw new IllegalStateException("Exemption policies must define an exemption code");
        if (policy.getComponents() != null) {
            java.util.Set<Integer> sequences = new java.util.HashSet<>();
            for (TaxPolicyComponent component : policy.getComponents()) {
                if (component.getName() == null || component.getName().isBlank()
                        || component.getComponentType() == null || component.getRate() == null
                        || component.getRate().compareTo(BigDecimal.ZERO) < 0
                        || component.getRate().compareTo(new BigDecimal("100")) > 0
                        || component.getSequence() == null || component.getSequence() < 0
                        || !sequences.add(component.getSequence()))
                    throw new IllegalStateException("Tax components require a name, valid type/rate and unique non-negative sequence");
                if (component.getComponentType() == TaxComponentType.WITHHOLDING
                        && component.getOutputTaxAccount() == null)
                    throw new IllegalStateException("Withholding components require a withholding account mapping");
                if (component.getComponentType() == TaxComponentType.REVERSE_CHARGE
                        && (component.getOutputTaxAccount() == null || component.getInputTaxAccount() == null))
                    throw new IllegalStateException("Reverse-charge components require both input and output tax accounts");
            }
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
