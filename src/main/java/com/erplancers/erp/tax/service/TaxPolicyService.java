package com.erplancers.erp.tax.service;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.tax.domain.TaxPolicy;
import com.erplancers.erp.tax.repository.TaxPolicyRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Objects;

/** Deterministic tax policy resolution and integer-minor-unit calculation. */
@Service
public class TaxPolicyService {
    private final TaxPolicyRepository policies;
    private final EntityManager entityManager;

    public TaxPolicyService(TaxPolicyRepository policies, EntityManager entityManager) {
        this.policies = policies;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<PolicyView> listPolicies() {
        return policies.findAll(Sort.by(Sort.Direction.DESC, "validFrom")).stream()
                .map(this::view).toList();
    }

    @Transactional
    public PolicyView createPolicy(PolicyCommand command) {
        TaxPolicy policy = new TaxPolicy();
        apply(policy, command);
        return view(policies.saveAndFlush(policy));
    }

    @Transactional
    public PolicyView updatePolicy(Long id, PolicyCommand command) {
        TaxPolicy policy = policies.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tax policy not found: " + id));
        apply(policy, command);
        return view(policies.saveAndFlush(policy));
    }

    @Transactional
    public PolicyView deactivatePolicy(Long id) {
        TaxPolicy policy = policies.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tax policy not found: " + id));
        policy.setActive(false);
        return view(policies.saveAndFlush(policy));
    }

    private void apply(TaxPolicy policy, PolicyCommand command) {
        policy.setName(command.name());
        policy.setCompany(command.companyId() == null ? null : entityManager.find(Contact.class, command.companyId()));
        if (command.companyId() != null && policy.getCompany() == null)
            throw new IllegalArgumentException("Tax policy company contact not found: " + command.companyId());
        policy.setJurisdictionCode(command.jurisdictionCode());
        policy.setTransactionType(command.transactionType());
        policy.setTaxCategory(command.taxCategory());
        policy.setRate(command.rate());
        policy.setValidFrom(command.validFrom());
        policy.setValidUntil(command.validUntil());
        policy.setPriority(command.priority());
        policy.setTaxInclusive(command.taxInclusive());
        policy.setActive(command.active());
    }

    private PolicyView view(TaxPolicy policy) {
        return new PolicyView(policy.getId(), policy.getName(),
                policy.getCompany() == null ? null : policy.getCompany().getId(),
                policy.getJurisdictionCode(), policy.getTransactionType(), policy.getTaxCategory(),
                policy.getRate(), policy.getValidFrom(), policy.getValidUntil(), policy.getPriority(),
                policy.isTaxInclusive(), policy.isActive());
    }

    public record PolicyCommand(String name, Long companyId, String jurisdictionCode, String transactionType,
                                String taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil,
                                Integer priority, boolean taxInclusive, boolean active) {}

    public record PolicyView(Long id, String name, Long companyId, String jurisdictionCode,
                             String transactionType, String taxCategory, BigDecimal rate,
                             LocalDate validFrom, LocalDate validUntil, Integer priority,
                             boolean taxInclusive, boolean active) {}

    @Transactional(readOnly = true)
    public TaxCalculation calculate(Contact company, String jurisdictionCode, String transactionType,
                                    String taxCategory, LocalDate date, long enteredAmount) {
        if (date == null) throw new IllegalArgumentException("Tax calculation date is required");
        if (enteredAmount < 0) throw new IllegalArgumentException("Taxable amount cannot be negative");

        String jurisdiction = normalize(jurisdictionCode);
        String transaction = normalize(transactionType);
        String category = normalize(taxCategory);

        TaxPolicy selected = policies.findByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(date, date)
                .stream()
                .filter(p -> p.getCompany() == null || sameId(p.getCompany(), company))
                .filter(p -> matches(p.getJurisdictionCode(), jurisdiction))
                .filter(p -> matches(p.getTransactionType(), transaction))
                .filter(p -> matches(p.getTaxCategory(), category))
                .max(Comparator
                        .comparingInt((TaxPolicy p) -> specificity(p, company))
                        .thenComparingInt(p -> -p.getPriority())
                        .thenComparing(TaxPolicy::getValidFrom)
                        .thenComparing(p -> p.getId().toString()))
                .orElseThrow(() -> new IllegalStateException("No active tax policy matches the transaction"));

        BigDecimal entered = BigDecimal.valueOf(enteredAmount);
        BigDecimal rate = selected.getRate().movePointLeft(2);
        BigDecimal net;
        BigDecimal tax;
        BigDecimal gross;
        if (selected.isTaxInclusive()) {
            gross = entered;
            net = entered.divide(BigDecimal.ONE.add(rate), 0, RoundingMode.HALF_UP);
            tax = gross.subtract(net);
        } else {
            net = entered;
            tax = entered.multiply(rate).setScale(0, RoundingMode.HALF_UP);
            gross = net.add(tax);
        }
        return new TaxCalculation(selected.getId(), net.longValueExact(), tax.longValueExact(),
                gross.longValueExact(), selected.getRate(), selected.isTaxInclusive());
    }

    private int specificity(TaxPolicy p, Contact company) {
        return (p.getCompany() != null && sameId(p.getCompany(), company) ? 8 : 0)
                + (p.getJurisdictionCode() != null ? 4 : 0)
                + (p.getTransactionType() != null ? 2 : 0)
                + (p.getTaxCategory() != null ? 1 : 0);
    }

    private boolean matches(String rule, String value) {
        return rule == null || Objects.equals(normalize(rule), value);
    }

    private boolean sameId(Contact left, Contact right) {
        return left != null && right != null && left.getId() != null && left.getId().equals(right.getId());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public record TaxCalculation(Long policyId, long netAmount, long taxAmount, long grossAmount,
                                 BigDecimal ratePercent, boolean taxInclusive) {}
}
