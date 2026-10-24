package com.erplancers.erp.common.security.web;

import com.erplancers.erp.common.security.domain.ErpUser;
import com.erplancers.erp.common.security.repository.ErpUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTest {

    private static final String ADMIN_TOKEN = "test-admin-registration-token-1234";

    @Autowired MockMvc mvc;
    @Autowired ErpUserRepository users;

    @DynamicPropertySource
    static void registrationProperties(DynamicPropertyRegistry registry) {
        registry.add("erp.registration.admin-token", () -> ADMIN_TOKEN);
    }

    @BeforeEach
    void setUp() {
        users.deleteAll();
    }

    @Test
    void registerPersistsUserWithEncodedPasswordAndRole() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("alice")));

        ErpUser user = users.findByUsernameIgnoreCase("alice").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getPassword()).isNotEqualTo("password123");
        org.assertj.core.api.Assertions.assertThat(user.getRoles()).containsExactly("USER");
        org.assertj.core.api.Assertions.assertThat(user.getLevel()).isEqualTo("USER");
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","password":"password456"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERP username already exists: alice"));
    }

    @Test
    void adminRegistrationPersistsAdminUser() throws Exception {
        mvc.perform(post("/api/auth/register-admin")
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","password":"password123","adminToken":"test-admin-registration-token-1234"}
                                """))
                .andExpect(status().isCreated());

        ErpUser user = users.findByUsernameIgnoreCase("admin").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getRoles()).containsExactly("ADMIN");
        org.assertj.core.api.Assertions.assertThat(user.getLevel()).isEqualTo("ADMIN");
    }
}
