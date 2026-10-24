package com.erplancers.erp.common.security.web;

import com.erplancers.erp.common.security.ErpUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegistrationControllerTest {

    private static final String ADMIN_TOKEN = "test-admin-registration-token-1234";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ErpUserService users;

    @Autowired
    private RegistrationController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "adminRegistrationToken", ADMIN_TOKEN);
    }

    @Test
    void registerCreatesStandardUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.message").value("Registration successful"));

        verify(users).create(eq("alice"), eq("password123"), eq("USER"), eq("USER"));
    }

    @Test
    void registerRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"ab","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerAdminCreatesAdminWithValidToken() throws Exception {
        mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"password123","adminToken":"test-admin-registration-token-1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.message").value("Admin registration successful"));

        verify(users).create(eq("admin"), eq("password123"), eq("ADMIN"), eq("ADMIN"));
    }

    @Test
    void registerAdminRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"password123","adminToken":"wrong-admin-token-1234"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerAdminRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register-admin")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"ab","password":"short","adminToken":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
