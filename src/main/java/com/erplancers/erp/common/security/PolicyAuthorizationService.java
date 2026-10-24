package com.erplancers.erp.common.security;

import com.erplancers.erp.common.security.domain.ErpPolicy;
import com.erplancers.erp.common.security.domain.ErpUser;
import com.erplancers.erp.common.security.repository.ErpAccessRuleRepository;
import com.erplancers.erp.common.web.CrudRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("policyAuthorization")
@RequiredArgsConstructor
public class PolicyAuthorizationService {
    private final ErpAccessRuleRepository policies;
    private final CrudRegistry registry;

    public boolean isAllowed(Authentication authentication, String slug, String action) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        CrudRegistry.CrudDescriptor descriptor = null;
        try { descriptor = registry.get(slug); } catch (IllegalArgumentException ignored) {}

        String module = descriptor != null ? descriptor.module() : slug;
        String username = authentication.getName();
        ErpUser user = authentication.getPrincipal() instanceof ErpUser u ? u : null;

        if (hasRole(authentication, "ADMIN")) return true;

        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> normalize(a.getAuthority()))
                .toList();
        List<ErpPolicy> rules = new ArrayList<>(policies.findByUsernameIgnoreCase(username));
        if (!roles.isEmpty()) rules.addAll(policies.findByRoleIn(roles));
        rules = rules.stream()
                .filter(p -> matches(p, module, slug, action))
                .toList();

        if (rules.stream().anyMatch(p -> p.getEffect() == ErpPolicy.PolicyEffect.DENY)) return false;
        if (rules.stream().anyMatch(p -> p.getEffect() == ErpPolicy.PolicyEffect.ALLOW)) return true;
        return user != null && "ADMIN".equalsIgnoreCase(user.getLevel());
    }

    private boolean matches(ErpPolicy p, String module, String resource, String action) {
        return wildcard(p.getAction(), action) && wildcard(p.getModule(), module) && wildcard(p.getResource(), resource);
    }

    private boolean wildcard(String value, String actual) {
        return value == null || "*".equals(value) || value.equalsIgnoreCase(actual);
    }

    private boolean hasRole(Authentication a, String role) {
        return a.getAuthorities().stream().anyMatch(x -> normalize(x.getAuthority()).equalsIgnoreCase(role));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceFirst("^ROLE_", "");
    }
}
