package com.erplancers.erp.tax.service;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.tax.domain.*;
import com.erplancers.erp.tax.repository.TaxPolicyRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class TaxPolicyService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private final TaxPolicyRepository policies;
    private final EntityManager entityManager;

    public TaxPolicyService(TaxPolicyRepository policies, EntityManager entityManager) {
        this.policies = policies;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<PolicyView> listPolicies() {
        return policies.findAll(Sort.by(Sort.Direction.DESC, "validFrom")).stream().map(this::view).toList();
    }

    @Transactional
    public PolicyView createPolicy(PolicyCommand command) {
        TaxPolicy policy = new TaxPolicy();
        apply(policy, command);
        return view(policies.saveAndFlush(policy));
    }

    @Transactional
    public PolicyView updatePolicy(Long id, PolicyCommand command) {
        TaxPolicy policy = policies.findById(id).orElseThrow(() -> new IllegalArgumentException("Tax policy not found: " + id));
        apply(policy, command);
        return view(policies.saveAndFlush(policy));
    }

    @Transactional
    public PolicyView deactivatePolicy(Long id) {
        TaxPolicy policy = policies.findById(id).orElseThrow(() -> new IllegalArgumentException("Tax policy not found: " + id));
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
        policy.setTreatment(command.treatment() == null ? TaxTreatment.STANDARD : command.treatment());
        policy.setExemptionCode(command.exemptionCode());
        policy.setRequiredEvidenceType(command.requiredEvidenceType());
        policy.getComponents().clear();
        if (command.components() != null) {
            Set<Integer> sequences = new HashSet<>();
            for (ComponentCommand input : command.components()) {
                if (input.sequence() == null || input.sequence() < 0 || !sequences.add(input.sequence()))
                    throw new IllegalArgumentException("Tax component sequence values must be unique and non-negative");
                TaxPolicyComponent component = new TaxPolicyComponent();
                component.setTaxPolicy(policy);
                component.setName(input.name());
                component.setComponentType(input.componentType());
                component.setRate(input.rate());
                component.setSequence(input.sequence());
                component.setOutputTaxAccount(account(input.outputTaxAccountId()));
                component.setInputTaxAccount(account(input.inputTaxAccountId()));
                policy.getComponents().add(component);
            }
        }
    }

    private Account account(Long id) {
        if (id == null) return null;
        Account account = entityManager.find(Account.class, id);
        if (account == null) throw new IllegalArgumentException("Tax component account not found: " + id);
        if (account.isBlocked() || !account.isPostingAllowed())
            throw new IllegalArgumentException("Tax component account must be active and posting-enabled: " + id);
        return account;
    }

    private PolicyView view(TaxPolicy policy) {
        return new PolicyView(policy.getId(), policy.getName(),
                policy.getCompany() == null ? null : policy.getCompany().getId(),
                policy.getJurisdictionCode(), policy.getTransactionType(), policy.getTaxCategory(),
                policy.getRate(), policy.getValidFrom(), policy.getValidUntil(), policy.getPriority(),
                policy.isTaxInclusive(), policy.isActive(), policy.getTreatment(), policy.getExemptionCode(),
                policy.getRequiredEvidenceType(), policy.getComponents().stream().map(c -> new ComponentView(
                c.getId(), c.getName(), c.getComponentType(), c.getRate(), c.getSequence(),
                c.getOutputTaxAccount() == null ? null : c.getOutputTaxAccount().getId(),
                c.getInputTaxAccount() == null ? null : c.getInputTaxAccount().getId())).toList());
    }

    public record ComponentCommand(String name, TaxComponentType componentType, BigDecimal rate, Integer sequence,
                                   Long outputTaxAccountId, Long inputTaxAccountId) {}
    public record ComponentView(Long id, String name, TaxComponentType componentType, BigDecimal rate, Integer sequence,
                                Long outputTaxAccountId, Long inputTaxAccountId) {}
    public record PolicyCommand(String name, Long companyId, String jurisdictionCode, String transactionType,
                                String taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil,
                                Integer priority, boolean taxInclusive, boolean active, TaxTreatment treatment,
                                String exemptionCode, String requiredEvidenceType, List<ComponentCommand> components) {
        public PolicyCommand(String name, Long companyId, String jurisdictionCode, String transactionType,
                             String taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil,
                             Integer priority, boolean taxInclusive, boolean active) {
            this(name, companyId, jurisdictionCode, transactionType, taxCategory, rate, validFrom, validUntil,
                    priority, taxInclusive, active, TaxTreatment.STANDARD, null, null, List.of());
        }
    }
    public record PolicyView(Long id, String name, Long companyId, String jurisdictionCode, String transactionType,
                             String taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil,
                             Integer priority, boolean taxInclusive, boolean active, TaxTreatment treatment,
                             String exemptionCode, String requiredEvidenceType, List<ComponentView> components) {}
    public record ComponentAmount(String name, TaxComponentType componentType, BigDecimal rate, long amount,
                                  Long outputTaxAccountId, Long inputTaxAccountId) {}
    public record DetailedTaxCalculation(Long policyId, long netAmount, long taxAmount, long withholdingAmount,
                                         long reverseChargeTaxAmount, long grossAmount, BigDecimal ratePercent,
                                         boolean taxInclusive, List<ComponentAmount> components) {}
    public record TaxCalculation(Long policyId, long netAmount, long taxAmount, long grossAmount,
                                 BigDecimal ratePercent, boolean taxInclusive) {}

    @Transactional(readOnly = true)
    public TaxCalculation calculate(Contact company, String jurisdictionCode, String transactionType,
                                    String taxCategory, LocalDate date, long enteredAmount) {
        DetailedTaxCalculation result = calculateDetailed(company, jurisdictionCode, transactionType, taxCategory,
                date, enteredAmount, null, null);
        return new TaxCalculation(result.policyId(), result.netAmount(), result.taxAmount(), result.grossAmount(),
                result.ratePercent(), result.taxInclusive());
    }

    @Transactional(readOnly = true)
    public TaxCalculation calculateForCompanyId(Long companyId, String jurisdictionCode, String transactionType,
                                                 String taxCategory, LocalDate date, long enteredAmount) {
        Contact company = companyId == null ? null : entityManager.find(Contact.class, companyId);
        if (companyId != null && company == null)
            throw new IllegalArgumentException("Tax policy company contact not found: " + companyId);
        return calculate(company, jurisdictionCode, transactionType, taxCategory, date, enteredAmount);
    }

    @Transactional(readOnly = true)
    public DetailedTaxCalculation calculateDetailed(Contact company, String jurisdictionCode, String transactionType,
            String taxCategory, LocalDate date, long enteredAmount, String exemptionCode, String evidenceReference) {
        if (date == null) throw new IllegalArgumentException("Tax calculation date is required");
        if (enteredAmount < 0) throw new IllegalArgumentException("Taxable amount cannot be negative");
        String jurisdiction = normalize(jurisdictionCode), transaction = normalize(transactionType), category = normalize(taxCategory);
        TaxPolicy selected = policies.findByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(date, date)
                .stream().filter(p -> p.getCompany() == null || sameId(p.getCompany(), company))
                .filter(p -> matches(p.getJurisdictionCode(), jurisdiction))
                .filter(p -> matches(p.getTransactionType(), transaction))
                .filter(p -> matches(p.getTaxCategory(), category))
                .max(Comparator.comparingInt((TaxPolicy p) -> specificity(p, company))
                        .thenComparingInt(p -> -p.getPriority()).thenComparing(TaxPolicy::getValidFrom)
                        .thenComparing(p -> p.getId().toString()))
                .orElseThrow(() -> new IllegalStateException("No active tax policy matches the transaction"));

        if (selected.getTreatment() == TaxTreatment.EXEMPT) {
            if (selected.getExemptionCode() == null || !selected.getExemptionCode().equalsIgnoreCase(
                    exemptionCode == null ? "" : exemptionCode.trim()))
                throw new IllegalArgumentException("A valid exemption code is required by the selected tax policy");
            if (selected.getRequiredEvidenceType() != null
                    && (evidenceReference == null || evidenceReference.isBlank()))
                throw new IllegalArgumentException("Exemption evidence reference is required: " + selected.getRequiredEvidenceType());
        }

        List<TaxPolicyComponent> configured = selected.getComponents().stream()
                .sorted(Comparator.comparing(TaxPolicyComponent::getSequence)).toList();
        if (configured.isEmpty()) {
            TaxPolicyComponent fallback = new TaxPolicyComponent();
            fallback.setName(selected.getName());
            fallback.setComponentType(TaxComponentType.STANDARD);
            fallback.setRate(selected.getRate());
            configured = List.of(fallback);
        }
        BigDecimal factor = BigDecimal.ONE;
        if (selected.isTaxInclusive() && selected.getTreatment() != TaxTreatment.EXEMPT) {
            for (TaxPolicyComponent c : configured) {
                if (c.getComponentType() == TaxComponentType.STANDARD || c.getComponentType() == TaxComponentType.COMPOUND)
                    factor = factor.multiply(BigDecimal.ONE.add(c.getRate().movePointLeft(2)));
            }
        }
        BigDecimal entered = BigDecimal.valueOf(enteredAmount);
        long net = selected.isTaxInclusive() ? entered.divide(factor, 0, RoundingMode.HALF_UP).longValueExact() : enteredAmount;
        long outputTax = 0, withholding = 0, reverseCharge = 0;
        long compoundBase = net;
        List<ComponentAmount> amounts = new ArrayList<>();
        for (TaxPolicyComponent component : configured) {
            TaxComponentType type = component.getComponentType();
            BigDecimal rate = component.getRate();
            long componentAmount = 0;
            if (selected.getTreatment() != TaxTreatment.EXEMPT) {
                long base = type == TaxComponentType.COMPOUND ? compoundBase : net;
                componentAmount = BigDecimal.valueOf(base).multiply(rate).divide(HUNDRED, 0, RoundingMode.HALF_UP).longValueExact();
                if (type == TaxComponentType.STANDARD || type == TaxComponentType.COMPOUND) {
                    outputTax = Math.addExact(outputTax, componentAmount);
                    compoundBase = Math.addExact(compoundBase, componentAmount);
                } else if (type == TaxComponentType.WITHHOLDING) withholding = Math.addExact(withholding, componentAmount);
                else if (type == TaxComponentType.REVERSE_CHARGE) reverseCharge = Math.addExact(reverseCharge, componentAmount);
            }
            amounts.add(new ComponentAmount(component.getName(), type, rate, componentAmount,
                    component.getOutputTaxAccount() == null ? null : component.getOutputTaxAccount().getId(),
                    component.getInputTaxAccount() == null ? null : component.getInputTaxAccount().getId()));
        }
        long gross = Math.subtractExact(Math.addExact(net, outputTax), withholding);
        if (gross < 0) throw new IllegalArgumentException("Tax withholding cannot exceed the document gross amount");
        return new DetailedTaxCalculation(selected.getId(), net, outputTax, withholding, reverseCharge, gross,
                selected.getRate(), selected.isTaxInclusive(), List.copyOf(amounts));
    }

    private int specificity(TaxPolicy p, Contact company) {
        return (p.getCompany() != null && sameId(p.getCompany(), company) ? 8 : 0)
                + (p.getJurisdictionCode() != null ? 4 : 0)
                + (p.getTransactionType() != null ? 2 : 0) + (p.getTaxCategory() != null ? 1 : 0);
    }
    private boolean matches(String rule, String value) { return rule == null || Objects.equals(normalize(rule), value); }
    private boolean sameId(Contact left, Contact right) {
        return left != null && right != null && left.getId() != null && left.getId().equals(right.getId());
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
}
