package com.erplancers.erp.common.security;

import com.erplancers.erp.common.security.domain.ErpPolicy;
import com.erplancers.erp.common.security.domain.ErpUser;
import com.erplancers.erp.common.security.repository.ErpAccessRuleRepository;
import com.erplancers.erp.common.security.repository.ErpUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ErpUserService {
    private final ErpUserRepository users;
    private final ErpAccessRuleRepository policies;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ErpUser create(String username, String rawPassword, String level, String... roles) {
        if (users.findByUsernameIgnoreCase(username).isPresent()) {
            throw new IllegalArgumentException("ERP username already exists: " + username);
        }
        ErpUser user = new ErpUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setLevel(level == null || level.isBlank() ? "USER" : level);
        user.getRoles().addAll(Arrays.asList(roles));

        ErpUser saved = users.save(user);
        if ("USER".equalsIgnoreCase(saved.getLevel())) {
            grantDefaultUserAccess();
        }
        return saved;
    }

    private void grantDefaultUserAccess() {
        boolean alreadyConfigured = policies.findByRoleIn(java.util.List.of("USER")).stream()
                .anyMatch(p -> "*".equals(p.getModule())
                        && "*".equals(p.getResource())
                        && p.getEffect() == ErpPolicy.PolicyEffect.ALLOW);
        if (alreadyConfigured) return;

        for (String action : new String[]{"READ", "WRITE", "DELETE"}) {
            ErpPolicy policy = new ErpPolicy();
            policy.setRole("USER");
            policy.setModule("*");
            policy.setResource("*");
            policy.setAction(action);
            policy.setEffect(ErpPolicy.PolicyEffect.ALLOW);
            policies.save(policy);
        }
    }
}
