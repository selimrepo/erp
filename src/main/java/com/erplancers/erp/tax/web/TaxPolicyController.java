package com.erplancers.erp.tax.web;

import com.erplancers.erp.common.security.PolicyAuthorizationService;
import com.erplancers.erp.tax.service.TaxPolicyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    @PostMapping("/{id}/deactivate")
    public TaxPolicyService.PolicyView deactivate(Authentication authentication, @PathVariable Long id) {
        require(authentication, "WRITE");
        return taxPolicies.deactivatePolicy(id);
    }

    private void require(Authentication authentication, String action) {
        if (!authorization.isAllowed(authentication, "tax-policy", action))
            throw new AccessDeniedException(action + " access denied for tax-policy");
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
            boolean active) {
        TaxPolicyService.PolicyCommand toCommand() {
            return new TaxPolicyService.PolicyCommand(name, companyId, jurisdictionCode, transactionType,
                    taxCategory, rate, validFrom, validUntil, priority, taxInclusive, active);
        }
    }
}
