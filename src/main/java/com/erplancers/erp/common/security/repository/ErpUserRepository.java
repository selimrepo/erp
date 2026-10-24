package com.erplancers.erp.common.security.repository;

import com.erplancers.erp.common.security.domain.ErpUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErpUserRepository extends JpaRepository<ErpUser, Long> {
    Optional<ErpUser> findByUsernameIgnoreCase(String username);
}
