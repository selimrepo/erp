package com.erplancers.erp.common.security.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "erp_policies",
        indexes = {
                @Index(name = "ix_erp_policy_user", columnList = "username"),
                @Index(name = "ix_erp_policy_module", columnList = "module_name"),
                @Index(name = "ix_erp_policy_resource", columnList = "resource_name")
        })
@Getter
@Setter
@NoArgsConstructor
public class ErpPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120)
    private String username;

    @Column(length = 80)
    private String role;

    @Column(name = "module_name", length = 120)
    private String module;

    @Column(name = "resource_name", length = 160)
    private String resource;

    @Column(nullable = false, length = 30)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PolicyEffect effect = PolicyEffect.ALLOW;

    public enum PolicyEffect {
        ALLOW, DENY
    }
}
