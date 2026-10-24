package com.erplancers.erp.common.web;

import com.erplancers.erp.common.security.domain.ErpUser;
import com.erplancers.erp.common.security.repository.ErpAccessRuleRepository;
import com.erplancers.erp.common.security.repository.ErpUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdvancedAnalyticsMockMvcTest {

    @Autowired MockMvc mvc;
    @Autowired ErpUserRepository users;
    @Autowired ErpAccessRuleRepository policies;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        policies.deleteAll();
        users.deleteAll();

        ErpUser admin = new ErpUser();
        admin.setUsername("advanced-admin");
        admin.setPassword(passwordEncoder.encode("advanced-pass"));
        admin.setLevel("ADMIN");
        admin.getRoles().add("ADMIN");
        users.save(admin);
    }

    @Test
    void advancedGetReturnsStatisticsAndCorrelationMatrix() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/advanced")
                        .param("fields", "amount")
                        .param("sampleSize", "100")
                        .param("zScoreThreshold", "2.5")
                        .with(httpBasic("advanced-admin", "advanced-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("invoice")))
                .andExpect(jsonPath("$.fields[0]", is("amount")))
                .andExpect(jsonPath("$.sampleSize", isA(Number.class)))
                .andExpect(jsonPath("$.statistics.amount", notNullValue()))
                .andExpect(jsonPath("$.statistics.amount.count", isA(Number.class)))
                .andExpect(jsonPath("$.correlations.amount.amount", isA(Number.class)));
    }

    @Test
    void advancedPostAcceptsStructuredRequest() throws Exception {
        String body = """
                {
                  "fields": ["amount"],
                  "sampleSize": 50,
                  "zScoreThreshold": 3.0
                }
                """;

        mvc.perform(post("/api/analytics/resources/invoice/advanced")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(httpBasic("advanced-admin", "advanced-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("invoice")))
                .andExpect(jsonPath("$.fields[0]", is("amount")))
                .andExpect(jsonPath("$.statistics.amount", notNullValue()));
    }

    @Test
    void advancedPostCanAnalyzeAggregateMetrics() throws Exception {
        String body = """
                {
                  "fields": ["sum:amount"],
                  "sampleSize": 50,
                  "zScoreThreshold": 3.0,
                  "aggregate": {
                    "groupBy": ["invoiceType"],
                    "metrics": ["count", "sum:amount"],
                    "filters": {},
                    "orderBy": "count:desc",
                    "limit": 50
                  }
                }
                """;

        mvc.perform(post("/api/analytics/resources/invoice/advanced")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(httpBasic("advanced-admin", "advanced-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregated", is(true)))
                .andExpect(jsonPath("$.groupBy[0]", is("invoiceType")))
                .andExpect(jsonPath("$.metrics[1]", is("sum:amount")))
                .andExpect(jsonPath("$.fields[0]", is("sum:amount")))
                .andExpect(jsonPath("$.statistics['sum:amount']", notNullValue()))
                .andExpect(jsonPath("$.correlations['sum:amount']['sum:amount']", isA(Number.class)));
    }

    @Test
    void advancedEndpointRejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/advanced"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/analytics/resources/invoice/advanced")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":[\"amount\"]}"))
                .andExpect(status().isUnauthorized());
    }
}
