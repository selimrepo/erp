package com.erplancers.erp.accounting.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Set;
import java.util.Objects;

public class AccountDeterminationRuleLifecycleListener {
    private static final Set<String> EVENTS = Set.of(
            "SALES_INVOICE", "PURCHASE_INVOICE", "SALES_CREDIT_NOTE", "PURCHASE_CREDIT_NOTE",
            "CUSTOMER_RECEIPT", "SUPPLIER_PAYMENT");

    @PrePersist @PreUpdate
    public void validate(AccountDeterminationRule rule) {
        if (rule.getEventType() == null || !EVENTS.contains(rule.getEventType()))
            throw new IllegalStateException("Unsupported account determination event type: " + rule.getEventType());
        if (rule.getPriority() == null || rule.getPriority() < 0)
            throw new IllegalStateException("Account determination priority cannot be negative");
        Account debit = rule.getDebitAccount(), credit = rule.getCreditAccount(), tax = rule.getTaxAccount();
        if (debit == null || credit == null || Objects.equals(debit.getId(), credit.getId()))
            throw new IllegalStateException("Account determination requires distinct debit and credit accounts");
        if (tax != null && (Objects.equals(tax.getId(), debit.getId()) || Objects.equals(tax.getId(), credit.getId())))
            throw new IllegalStateException("Tax account must be distinct from debit and credit accounts");
        if (rule.isActive() && (debit.isBlocked() || credit.isBlocked() || !debit.isPostingAllowed() || !credit.isPostingAllowed()
                || tax != null && (tax.isBlocked() || !tax.isPostingAllowed())))
            throw new IllegalStateException("Active account determination rules must use unblocked posting-enabled accounts");
    }
}
