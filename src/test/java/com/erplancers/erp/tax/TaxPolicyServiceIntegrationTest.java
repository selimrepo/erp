package com.erplancers.erp.tax;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.tax.domain.TaxPolicy;
import com.erplancers.erp.tax.repository.TaxPolicyRepository;
import com.erplancers.erp.tax.service.TaxPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tax policy rate must be between 0 and 100 percent");
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
