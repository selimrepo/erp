package com.erplancers.erp.tax.service;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.tax.domain.TaxPolicy;
import com.erplancers.erp.tax.repository.TaxPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/** Deterministic tax policy resolution and integer-minor-unit calculation. */
@Service
public class TaxPolicyService {
    private final TaxPolicyRepository policies;

    public TaxPolicyService(TaxPolicyRepository policies) {
        this.policies = policies;
    }

    @Transactional(readOnly = true)
    public TaxCalculation calculate(Contact company, String jurisdictionCode, String transactionType,
                                    String taxCategory, LocalDate date, long enteredAmount,
                                    int currencyScale) {
        if (date == null) throw new IllegalArgumentException("Tax calculation date is required");
        if (enteredAmount < 0) throw new IllegalArgumentException("Taxable amount cannot be negative");
        if (currencyScale < 0 || currencyScale > 4) throw new IllegalArgumentException("Unsupported currency scale");

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
