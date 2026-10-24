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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestAnalyticsControllerMockMvcTest {

    @Autowired MockMvc mvc;
    @Autowired ErpUserRepository users;
    @Autowired ErpAccessRuleRepository policies;
    @Autowired PasswordEncoder passwordEncoder;

    // The web integration tests must not require a live Kafka producer configuration.
    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        policies.deleteAll();
        users.deleteAll();

        ErpUser admin = new ErpUser();
        admin.setUsername("analytics-admin");
        admin.setPassword(passwordEncoder.encode("analytics-pass"));
        admin.setLevel("ADMIN");
        admin.getRoles().add("ADMIN");
        users.save(admin);
    }

    @Test
    void dashboardEndpointReturnsDashboard() throws Exception {
        mvc.perform(get("/api/analytics/dashboard")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", isA(Number.class)))
                .andExpect(jsonPath("$.resourceCount", isA(Number.class)))
                .andExpect(jsonPath("$.moduleCount", isA(Number.class)))
                .andExpect(jsonPath("$.recordsByModule", notNullValue()))
                .andExpect(jsonPath("$.recordsByResource", notNullValue()));
    }

    @Test
    void catalogEndpointReturnsAllRegisteredResources() throws Exception {
        mvc.perform(get("/api/analytics/catalog")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceCount", greaterThan(0)))
                .andExpect(jsonPath("$.resources", not(empty())))
                .andExpect(jsonPath("$.resources[0].slug", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.resources[0].fields", notNullValue()));
    }

    @Test
    void resourcesEndpointReturnsAuthorizedAnalyticsResources() throws Exception {
        mvc.perform(get("/api/analytics/resources")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].slug", everyItem(not(isEmptyOrNullString()))))
                .andExpect(jsonPath("$[*].aggregatePath", everyItem(containsString("/api/analytics/resources/"))));
    }

    @Test
    void profileEndpointReturnsEntityProfile() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/profile")
                        .param("topValues", "10")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("invoice")))
                .andExpect(jsonPath("$.entity", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.rowCount", isA(Number.class)))
                .andExpect(jsonPath("$.fields", notNullValue()));
    }

    @Test
    void profileEndpointClampsLargeTopValues() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/profile")
                        .param("topValues", "100000")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields", notNullValue()));
    }

    @Test
    void aggregateGetEndpointSupportsCountAndFilters() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/aggregate")
                        .param("metrics", "count")
                        .param("limit", "100")
                        .param("filter.invoiceStatus", "POSTED")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("invoice")))
                .andExpect(jsonPath("$.metrics[0]", is("count")))
                .andExpect(jsonPath("$.filters.invoiceStatus", is("POSTED")))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    void aggregateGetEndpointSupportsGroupByAndNumericMetrics() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/aggregate")
                        .param("groupBy", "invoiceStatus")
                        .param("metrics", "count,sum:amount,avg:amount")
                        .param("orderBy", "count:desc")
                        .param("limit", "25")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy[0]", is("invoiceStatus")))
                .andExpect(jsonPath("$.metrics", hasItems("count", "sum:amount", "avg:amount")))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    void aggregatePostEndpointAcceptsStructuredRequest() throws Exception {
        String body = """
                {
                  "groupBy": [],
                  "metrics": ["count"],
                  "filters": {},
                  "orderBy": "",
                  "limit": 50
                }
                """;

        mvc.perform(post("/api/analytics/resources/invoice/aggregate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("invoice")))
                .andExpect(jsonPath("$.metrics[0]", is("count")))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    void analyticsEndpointsRejectUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/analytics/catalog"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/analytics/resources"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/analytics/resources/invoice/profile"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/analytics/resources/invoice/aggregate"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/analytics/resources/invoice/aggregate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metrics\":[\"count\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void qualityEndpointReturnsCompletenessAndFieldDiagnostics() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/quality")
                        .param("topValues", "5")
                        .with(httpBasic("analytics-admin", "analytics-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("invoice")))
                .andExpect(jsonPath("$.rowCount", isA(Number.class)))
                .andExpect(jsonPath("$.fieldCount", isA(Number.class)))
                .andExpect(jsonPath("$.completenessScore", isA(Number.class)))
                .andExpect(jsonPath("$.status", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.fields", notNullValue()));
    }

    @Test
    void qualityEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/analytics/resources/invoice/quality"))
                .andExpect(status().isUnauthorized());
    }

}
