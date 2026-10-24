package com.erplancers.erp.common.security.web;

import com.erplancers.erp.common.security.domain.ErpPolicy;
import com.erplancers.erp.common.security.domain.ErpUser;
import com.erplancers.erp.common.security.repository.ErpAccessRuleRepository;
import com.erplancers.erp.common.security.repository.ErpUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "erp.registration.admin-token=test-admin-registration-token-1234")
class RegistrationPersistenceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ErpUserRepository users;
    @Autowired private ErpAccessRuleRepository policies;

    @BeforeEach
    void setUp() {
        policies.deleteAll();
        users.deleteAll();
    }

    @Test
    void registerPersistsUserWithEncodedPasswordAndRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"persisted-user","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("persisted-user")));

        ErpUser user = users.findByUsernameIgnoreCase("persisted-user").orElseThrow();
        assertEquals("USER", user.getLevel());
        assertTrue(user.getRoles().contains("USER"));
        assertNotEquals("password123", user.getPassword());

        List<ErpPolicy> defaultPolicies = policies.findByRoleIn(List.of("USER"));
        assertEquals(3, defaultPolicies.size());
        assertTrue(defaultPolicies.stream().allMatch(p ->
                "*".equals(p.getModule())
                        && "*".equals(p.getResource())
                        && p.getEffect() == ErpPolicy.PolicyEffect.ALLOW));
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"duplicate-user","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"duplicate-user","password":"password456"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("ERP username already exists: duplicate-user")));
    }

    @Test
    void registerAdminPersistsAdminUser() throws Exception {
        mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"persisted-admin","password":"password123","adminToken":"test-admin-registration-token-1234"}
                                """))
                .andExpect(status().isCreated());

        ErpUser user = users.findByUsernameIgnoreCase("persisted-admin").orElseThrow();
        assertEquals("ADMIN", user.getLevel());
        assertTrue(user.getRoles().contains("ADMIN"));
        assertTrue(policies.findByRoleIn(List.of("USER")).isEmpty());
    }
}
