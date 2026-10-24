package com.erplancers.erp.common.security.repository;

import com.erplancers.erp.common.security.domain.ErpPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErpAccessRuleRepository extends JpaRepository<ErpPolicy, Long> {
    List<ErpPolicy> findByUsernameIgnoreCase(String username);
    List<ErpPolicy> findByRoleIn(List<String> roles);
}
