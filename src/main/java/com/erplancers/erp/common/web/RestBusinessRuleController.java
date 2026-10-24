package com.erplancers.erp.common.web;

import com.erplancers.erp.common.rules.JpaBusinessRuleEngine;
import com.erplancers.erp.common.security.PolicyAuthorizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/business-rules")
@SecurityRequirement(name = "basicAuth")
public class RestBusinessRuleController {

    private final JpaBusinessRuleEngine engine;
    private final PolicyAuthorizationService authorization;

    @GetMapping("/schema/{resource}")
    public JpaBusinessRuleEngine.RuleSchema schema(
            Authentication authentication,
            @PathVariable String resource,
            @RequestParam(defaultValue = "2") int relationDepth) {
        requireRead(authentication, resource);
        return engine.schema(resource, relationDepth);
    }

    @PostMapping("/resources/{resource}/{id}/evaluate")
    public JpaBusinessRuleEngine.EvaluationResult evaluate(
            Authentication authentication,
            @PathVariable String resource,
            @PathVariable Long id,
            @RequestBody JpaBusinessRuleEngine.RuleSet ruleset) {
        requireRead(authentication, resource);
        return engine.evaluate(resource, id, ruleset);
    }

    @PostMapping("/resources/{resource}/{id}/execute")
    public JpaBusinessRuleEngine.EvaluationResult execute(
            Authentication authentication,
            @PathVariable String resource,
            @PathVariable Long id,
            @RequestBody JpaBusinessRuleEngine.RuleSet ruleset) {
        requireWrite(authentication, resource);
        return engine.execute(resource, id, ruleset);
    }

    private void requireRead(Authentication authentication, String resource) {
        if (!authorization.isAllowed(authentication, resource, "READ")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Business-rule READ access denied for resource: " + resource);
        }
    }

    private void requireWrite(Authentication authentication, String resource) {
        if (!authorization.isAllowed(authentication, resource, "WRITE")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Business-rule WRITE access denied for resource: " + resource);
        }
    }
}
