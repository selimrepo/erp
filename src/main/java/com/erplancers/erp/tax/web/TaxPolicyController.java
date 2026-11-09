package com.erplancers.erp.tax.web;

import com.erplancers.erp.common.security.PolicyAuthorizationService;
import com.erplancers.erp.tax.service.TaxPolicyService;
import com.erplancers.erp.tax.domain.TaxComponentType;
import com.erplancers.erp.tax.domain.TaxTreatment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/** Authenticated CRUD boundary for tenant-configurable generic tax policies. */
@RestController
@RequestMapping("/api/tax-policies")
public class TaxPolicyController {
    private final TaxPolicyService taxPolicies;
    private final PolicyAuthorizationService authorization;

    public TaxPolicyController(TaxPolicyService taxPolicies, PolicyAuthorizationService authorization) {
        this.taxPolicies = taxPolicies;
        this.authorization = authorization;
    }

    @GetMapping
    public List<TaxPolicyService.PolicyView> list(Authentication authentication) {
        require(authentication, "READ");
        return taxPolicies.listPolicies();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxPolicyService.PolicyView create(Authentication authentication,
                                               @Valid @RequestBody TaxPolicyRequest request) {
        require(authentication, "WRITE");
        return taxPolicies.createPolicy(request.toCommand());
    }

    @PutMapping("/{id}")
    public TaxPolicyService.PolicyView update(Authentication authentication, @PathVariable Long id,
                                               @Valid @RequestBody TaxPolicyRequest request) {
        require(authentication, "WRITE");
        return taxPolicies.updatePolicy(id, request.toCommand());
    }

    @PostMapping("/calculate")
    public TaxPolicyService.DetailedTaxCalculation calculate(Authentication authentication,
                                                     @Valid @RequestBody TaxCalculationRequest request) {
        require(authentication, "READ");
        return taxPolicies.calculateDetailedForCompanyId(request.companyId(), request.jurisdictionCode(), request.transactionType(),
                request.taxCategory(), request.date(), request.amount(), request.exemptionCode(), request.evidenceType(), request.evidenceReference());
    }

    @PostMapping("/{id}/deactivate")
    public TaxPolicyService.PolicyView deactivate(Authentication authentication, @PathVariable Long id) {
        require(authentication, "WRITE");
        return taxPolicies.deactivatePolicy(id);
    }

    private void require(Authentication authentication, String action) {
        if (!authorization.isAllowed(authentication, "tax-policy", action))
            throw new AccessDeniedException(action + " access denied for tax-policy");
    }

    public record TaxCalculationRequest(Long companyId,
                                        @Size(max = 32) String jurisdictionCode,
                                        @NotBlank @Size(max = 40) String transactionType,
                                        @Size(max = 60) String taxCategory,
                                        @NotNull LocalDate date,
                                        @NotNull @PositiveOrZero Long amount,
                                        String exemptionCode, String evidenceType, String evidenceReference) {
        public TaxCalculationRequest(Long companyId, String jurisdictionCode, String transactionType,
                                     String taxCategory, LocalDate date, Long amount) {
            this(companyId, jurisdictionCode, transactionType, taxCategory, date, amount, null, null, null);
        }
    }

    public record TaxComponentRequest(@NotBlank @Size(max=120) String name, @NotNull TaxComponentType componentType,
                                      @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal rate,
                                      @NotNull @Min(0) Integer sequence, Long outputTaxAccountId, Long inputTaxAccountId) {
        TaxPolicyService.ComponentCommand toCommand() {
            return new TaxPolicyService.ComponentCommand(name, componentType, rate, sequence, outputTaxAccountId, inputTaxAccountId);
        }
    }

    public record TaxPolicyRequest(
            @NotBlank @Size(max = 120) String name,
            Long companyId,
            @Size(max = 32) String jurisdictionCode,
            @Size(max = 40) String transactionType,
            @Size(max = 60) String taxCategory,
            @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal rate,
            @NotNull LocalDate validFrom,
            @NotNull LocalDate validUntil,
            @NotNull @Min(0) Integer priority,
            boolean taxInclusive,
            boolean active,
            TaxTreatment treatment, @Size(max=80) String exemptionCode,
            @Size(max=120) String requiredEvidenceType, List<@Valid TaxComponentRequest> components) {
        public TaxPolicyRequest(String name, Long companyId, String jurisdictionCode, String transactionType,
                String taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil,
                Integer priority, boolean taxInclusive, boolean active) {
            this(name, companyId, jurisdictionCode, transactionType, taxCategory, rate, validFrom, validUntil,
                    priority, taxInclusive, active, TaxTreatment.STANDARD, null, null, List.of());
        }
        TaxPolicyService.PolicyCommand toCommand() {
            return new TaxPolicyService.PolicyCommand(name, companyId, jurisdictionCode, transactionType,
                    taxCategory, rate, validFrom, validUntil, priority, taxInclusive, active, treatment,
                    exemptionCode, requiredEvidenceType,
                    components == null ? List.of() : components.stream().map(TaxComponentRequest::toCommand).toList());
        }
    }
}
