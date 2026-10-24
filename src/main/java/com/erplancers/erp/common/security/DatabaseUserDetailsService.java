package com.erplancers.erp.common.security;

import com.erplancers.erp.common.security.repository.ErpUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final ErpUserRepository users;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("ERP user not found: " + username));
    }
}
