package com.erplancers.erp.tax;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.tax.domain.TaxComponentType;
import com.erplancers.erp.tax.domain.TaxPolicyComponent;
import com.erplancers.erp.tax.domain.TaxTreatment;
import com.erplancers.erp.tax.domain.TaxPolicy;
import com.erplancers.erp.tax.repository.TaxPolicyRepository;
import com.erplancers.erp.tax.service.TaxPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TaxPolicyService.class)
class TaxPolicyServiceIntegrationTest {
    @Autowired TaxPolicyRepository policies;
    @Autowired TaxPolicyService service;
    @Autowired EntityManager entityManager;

    @Test
    void resolvesSpecificPolicyAndCalculatesExclusiveTaxInMinorUnits() {
        TaxPolicy global = policy("Global default", "TR", null, null, "18", 100, false);
        TaxPolicy specific = policy("Domestic sales VAT", "TR", "SALE", "STANDARD", "20", 10, false);
        policies.save(global);
        policies.saveAndFlush(specific);

        var result = service.calculate(null, "tr", "sale", "standard",
                LocalDate.of(2026, 11, 9), 10_000L);

        assertThat(result.policyId()).isEqualTo(specific.getId());
        assertThat(result.netAmount()).isEqualTo(10_000L);
        assertThat(result.taxAmount()).isEqualTo(2_000L);
        assertThat(result.grossAmount()).isEqualTo(12_000L);
    }

    @Test
    void resolvesSalesAndPurchasePoliciesIndependentlyForDocumentWorkflows() {
        TaxPolicy sales = policy("Sales VAT", "TR", "SALE", "STANDARD", "20", 1, false);
        TaxPolicy purchase = policy("Purchase VAT", "TR", "PURCHASE", "STANDARD", "10", 1, false);
        policies.save(sales);
        policies.saveAndFlush(purchase);

        var salesResult = service.calculate(null, "TR", "SALE", "STANDARD",
                LocalDate.of(2026, 11, 9), 5_000L);
        var purchaseResult = service.calculate(null, "TR", "PURCHASE", "STANDARD",
                LocalDate.of(2026, 11, 9), 5_000L);

        assertThat(salesResult.policyId()).isEqualTo(sales.getId());
        assertThat(salesResult.taxAmount()).isEqualTo(1_000L);
        assertThat(purchaseResult.policyId()).isEqualTo(purchase.getId());
        assertThat(purchaseResult.taxAmount()).isEqualTo(500L);
    }

    @Test
    void calculatesTaxInclusiveAmountsAndHonorsValidityDates() {
        policies.saveAndFlush(policy("Inclusive rate", "DE", "SALE", "REDUCED", "19",
                1, true));

        var result = service.calculate(null, "DE", "SALE", "REDUCED",
                LocalDate.of(2026, 11, 9), 11_900L);

        assertThat(result.netAmount()).isEqualTo(10_000L);
        assertThat(result.taxAmount()).isEqualTo(1_900L);
        assertThat(result.grossAmount()).isEqualTo(11_900L);
    }

    @Test
    void companyPolicyOverridesGlobalPolicyWithoutLeakingAcrossTenants() {
        Contact tenantA = new Contact();
        tenantA.setName("Tenant A");
        entityManager.persist(tenantA);
        Contact tenantB = new Contact();
        tenantB.setName("Tenant B");
        entityManager.persist(tenantB);

        TaxPolicy global = policy("Global default", "TR", "SALE", "STANDARD", "18", 1, false);
        TaxPolicy tenantPolicy = policy("Tenant A override", "TR", "SALE", "STANDARD", "20", 100, false);
        tenantPolicy.setCompany(tenantA);
        policies.save(global);
        policies.saveAndFlush(tenantPolicy);

        var tenantAResult = service.calculate(tenantA, "TR", "SALE", "STANDARD",
                LocalDate.of(2026, 11, 9), 10_000L);
        var tenantBResult = service.calculate(tenantB, "TR", "SALE", "STANDARD",
                LocalDate.of(2026, 11, 9), 10_000L);

        assertThat(tenantAResult.ratePercent()).isEqualByComparingTo("20");
        assertThat(tenantBResult.ratePercent()).isEqualByComparingTo("18");
        assertThat(tenantAResult.policyId()).isEqualTo(tenantPolicy.getId());
        assertThat(tenantBResult.policyId()).isEqualTo(global.getId());
    }

    @Test
    void createsListsAndDeactivatesPoliciesWithoutDeletingAuditHistory() {
        var command = new TaxPolicyService.PolicyCommand("Managed policy", null, "US-CA", "SALE",
                "STANDARD", new BigDecimal("8.25"), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), 5, false, true);

        var created = service.createPolicy(command);
        assertThat(service.listPolicies()).extracting(TaxPolicyService.PolicyView::id).contains(created.id());

        var deactivated = service.deactivatePolicy(created.id());
        assertThat(deactivated.active()).isFalse();
        assertThat(policies.findById(created.id())).isPresent();
    }

    @Test
    void rejectsRatesAboveOneHundredPercent() {
        TaxPolicy invalid = policy("Invalid rate", "TR", "SALE", "STANDARD", "100.01", 10, false);

        assertThatThrownBy(() -> policies.saveAndFlush(invalid))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Tax policy rate must be between 0 and 100 percent");
    }

    @Test
    void resolvesTaxInclusiveCompoundComponentsWithConsistentGrossRounding() {
        TaxPolicy policy = policy("Inclusive compound tax", "TR", "SALE", "STANDARD", "0", 1, true);
        policy.getComponents().add(component(policy, "VAT", TaxComponentType.STANDARD, "10", 1, null, null));
        policy.getComponents().add(component(policy, "Compound levy", TaxComponentType.COMPOUND, "5", 2, null, null));
        policies.saveAndFlush(policy);

        var result = service.calculateDetailed(null, "TR", "SALE", "STANDARD",
                LocalDate.of(2026, 11, 9), 11_550L, null, null, null);

        assertThat(result.netAmount()).isEqualTo(10_000L);
        assertThat(result.taxAmount()).isEqualTo(1_550L);
        assertThat(result.grossAmount()).isEqualTo(11_550L);
    }

    @Test
    void validatesExemptionCodeEvidenceTypeAndReferenceBeforeZeroTaxTreatment() {
        TaxPolicy exempt = policy("Export exemption", "TR", "SALE", "EXPORT", "0", 1, false);
        exempt.setTreatment(TaxTreatment.EXEMPT);
        exempt.setExemptionCode("EXPORT-11");
        exempt.setRequiredEvidenceType("EXPORT_CERTIFICATE");
        policies.saveAndFlush(exempt);

        assertThatThrownBy(() -> service.calculateDetailed(null, "TR", "SALE", "EXPORT",
                LocalDate.of(2026, 11, 9), 10_000L, "EXPORT-11", "OTHER_DOCUMENT", "DOC-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("evidence type");
        assertThatThrownBy(() -> service.calculateDetailed(null, "TR", "SALE", "EXPORT",
                LocalDate.of(2026, 11, 9), 10_000L, "EXPORT-11", "EXPORT_CERTIFICATE", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("evidence reference");

        var result = service.calculateDetailed(null, "TR", "SALE", "EXPORT",
                LocalDate.of(2026, 11, 9), 10_000L, "EXPORT-11", "EXPORT_CERTIFICATE", "GUMRUK-2026-1");
        assertThat(result.taxAmount()).isZero();
        assertThat(result.grossAmount()).isEqualTo(10_000L);
    }

    @Test
    void calculatesCompoundTaxWithholdingAndReverseChargeComponents() {
        Account inputTax = new Account(); inputTax.setName("Input VAT"); inputTax.setCode("191"); inputTax.setPostingAllowed(true);
        Account outputTax = new Account(); outputTax.setName("Output VAT"); outputTax.setCode("391"); outputTax.setPostingAllowed(true);
        entityManager.persist(inputTax); entityManager.persist(outputTax);

        TaxPolicy policy = policy("Multi-component tax", "TR", "PURCHASE", "STANDARD", "0", 1, false);
        policy.getComponents().add(component(policy, "VAT", TaxComponentType.STANDARD, "10", 1, null, null));
        policy.getComponents().add(component(policy, "Compound levy", TaxComponentType.COMPOUND, "5", 2, null, null));
        policy.getComponents().add(component(policy, "Withholding", TaxComponentType.WITHHOLDING, "2", 3, outputTax, null));
        policy.getComponents().add(component(policy, "Reverse charge", TaxComponentType.REVERSE_CHARGE, "10", 4, outputTax, inputTax));
        policies.saveAndFlush(policy);

        var result = service.calculateDetailed(null, "TR", "PURCHASE", "STANDARD",
                LocalDate.of(2026, 11, 9), 10_000L, null, null, null);

        assertThat(result.netAmount()).isEqualTo(10_000L);
        assertThat(result.taxAmount()).isEqualTo(1_550L);
        assertThat(result.withholdingAmount()).isEqualTo(200L);
        assertThat(result.reverseChargeTaxAmount()).isEqualTo(1_000L);
        assertThat(result.grossAmount()).isEqualTo(11_350L);
        assertThat(result.components()).hasSize(4);
    }

    private TaxPolicyComponent component(TaxPolicy policy, String name, TaxComponentType type,
                                         String rate, int sequence, Account output, Account input) {
        TaxPolicyComponent component = new TaxPolicyComponent();
        component.setTaxPolicy(policy); component.setName(name); component.setComponentType(type);
        component.setRate(new BigDecimal(rate)); component.setSequence(sequence);
        component.setOutputTaxAccount(output); component.setInputTaxAccount(input);
        return component;
    }

    @Test
    void failsClosedWhenNoPolicyMatches() {
        assertThatThrownBy(() -> service.calculate(null, "US", "SALE", "STANDARD",
                LocalDate.of(2026, 11, 9), 1_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active tax policy");
    }

    private TaxPolicy policy(String name, String jurisdiction, String transaction,
                             String category, String rate, int priority, boolean inclusive) {
        TaxPolicy policy = new TaxPolicy();
        policy.setName(name);
        policy.setJurisdictionCode(jurisdiction);
        policy.setTransactionType(transaction);
        policy.setTaxCategory(category);
        policy.setRate(new BigDecimal(rate));
        policy.setValidFrom(LocalDate.of(2026, 1, 1));
        policy.setValidUntil(LocalDate.of(2026, 12, 31));
        policy.setPriority(priority);
        policy.setTaxInclusive(inclusive);
        policy.setActive(true);
        return policy;
    }
}
