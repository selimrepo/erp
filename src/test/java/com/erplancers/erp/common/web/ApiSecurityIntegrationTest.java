package com.erplancers.erp.common.web;

import com.erplancers.erp.common.reporting.ErpBiEngine;
import com.erplancers.erp.common.security.domain.ErpPolicy;
import com.erplancers.erp.common.security.domain.ErpUser;
import com.erplancers.erp.common.security.repository.ErpAccessRuleRepository;
import com.erplancers.erp.common.security.repository.ErpUserRepository;
import com.erplancers.erp.finance.domain.PaymentTransaction;
import com.erplancers.erp.finance.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ErpUserRepository users;
    @Autowired ErpAccessRuleRepository policies;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ErpBiEngine bi;
    @Autowired PaymentTransactionRepository paymentTransactions;

    @BeforeEach
    void setUp() {
        paymentTransactions.deleteAll();
        paymentTransactions.save(paymentTransaction("SETTLED", 100L));
        paymentTransactions.save(paymentTransaction("SETTLED", 250L));
        paymentTransactions.save(paymentTransaction("PENDING", 50L));
        policies.deleteAll();
        users.deleteAll();
        users.save(user("admin", "admin-pass", "ADMIN", "ADMIN"));
        users.save(user("accounting", "accounting-pass", "USER", "ACCOUNTING"));
        users.save(user("readonly", "readonly-pass", "USER", "REPORTS"));
        allowRole("ACCOUNTING", "accounting", "*", "READ");
        allowRole("ACCOUNTING", "accounting", "invoice", "WRITE");
        allowRole("REPORTS", "*", "*", "READ");
        denyUser("readonly", "finance", "invoice", "READ");
    }

    @Test void unauthenticatedApiIsRejected() throws Exception {
        mvc.perform(get("/api/resources")).andExpect(status().isUnauthorized());
    }

    @Test void swaggerIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test void adminCanSeeResources() throws Exception {
        mvc.perform(get("/api/resources").with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isOk()).andExpect(jsonPath("$", is(not(empty()))));
    }

    @Test void accountingUserGetsOnlyAuthorizedResources() throws Exception {
        mvc.perform(get("/api/resources").with(httpBasic("accounting", "accounting-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].module", everyItem(is("accounting"))));
    }

    @Test void invalidPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/resources").with(httpBasic("accounting", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test void accountingCanReadAuthorizedResource() throws Exception {
        mvc.perform(get("/api/resources/account").with(httpBasic("accounting", "accounting-pass")))
                .andExpect(status().isOk());
    }

    @Test void paymentTransactionBiEngineWorksDirectly() {
        var report = bi.query(
                "payment-transaction",
                new ErpBiEngine.BiQuery(
                        java.util.List.of("status"),
                        java.util.List.of("count"),
                        java.util.Map.of(),
                        "count:desc",
                        100,
                        false,
                        false,
                        false));

        assertThat(report.resource()).isEqualTo("payment-transaction");
        assertThat(report.rows()).isNotNull();
    }

    @Test void adminCanGroupPaymentTransactionsByStatus() throws Exception {
        mvc.perform(post("/api/bi/resources/payment-transaction/query")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status"],
                                  "measures": ["count"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.measures", hasItem("count")))
                .andExpect(jsonPath("$.rows", notNullValue()));
    }

    @Test void adminCanSumPaymentTransactionAmount() throws Exception {
        mvc.perform(post("/api/bi/resources/payment-transaction/query")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": [],
                                  "measures": ["count", "sum:amount"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.measures", hasItems("count", "sum:amount")))
                .andExpect(jsonPath("$.rows", notNullValue()));
    }

    @Test void adminCanRunPaymentTransactionBiQueryWithDimensionAndSum() throws Exception {
        mvc.perform(post("/api/bi/resources/payment-transaction/query")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status"],
                                  "measures": ["count", "sum:amount"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.module", is("finance")))
                .andExpect(jsonPath("$.measures", hasItems("count", "sum:amount")))
                .andExpect(jsonPath("$.rowCount", is(2)))
                .andExpect(jsonPath("$.rows[0].status", is("SETTLED")))
                .andExpect(jsonPath("$.rows[0].count", is(2)))
                .andExpect(jsonPath("$['rows'][0]['sum:amount']", is(350)));
    }

    @Test void adminCanRunBiAnalyze() throws Exception {
        mvc.perform(post("/api/bi/resources/payment-transaction/analyze")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "fields": ["amount", "quantity"],
                                  "sampleSize": 100,
                                  "zScoreThreshold": 3.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregated", is(false)));
    }

    @Test void adminCanRunOlapCubeAndDrillDown() throws Exception {
        mvc.perform(post("/api/bi/olap/resources/payment-transaction/cube")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status"],
                                  "measures": ["count", "sum:amount"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 100,
                                  "includeTotals": true,
                                  "includeRollup": true,
                                  "pivotDimension": "status"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.cells", notNullValue()))
                .andExpect(jsonPath("$.grandTotal", notNullValue()))
                .andExpect(jsonPath("$.pivot", notNullValue()))
                .andExpect(jsonPath("$.rollup", notNullValue()));

        mvc.perform(post("/api/bi/olap/resources/payment-transaction/drilldown")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status"],
                                  "measures": ["count"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 100,
                                  "includeTotals": true,
                                  "includeRollup": false,
                                  "pivotDimension": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")));
    }

    @Test void adminCanAutoDiscoverJpaOlapRelationsWithConfiguredDepth() throws Exception {
        mvc.perform(get("/api/bi/olap/cubes")
                        .with(httpBasic("admin", "admin-pass"))
                        .param("relationDepth", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cubeCount", greaterThan(0)));

        mvc.perform(get("/api/bi/olap/cubes/payment-transaction/relations")
                        .with(httpBasic("admin", "admin-pass"))
                        .param("relation", "currency")
                        .param("relationDepth", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.paths", not(empty())));

        mvc.perform(get("/api/bi/olap/cubes/payment-transaction")
                        .with(httpBasic("admin", "admin-pass"))
                        .param("relationDepth", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions", hasItem("currency.title")));
    }

    @Test void adminCanQueryAutomaticallyGeneratedCubeThroughJpaRelation() throws Exception {
        mvc.perform(post("/api/bi/olap/cubes/payment-transaction/query")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": [],
                                  "measures": ["count"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 25,
                                  "includeTotals": true,
                                  "includeRollup": false,
                                  "pivotDimension": "",
                                  "relation": "currency",
                                  "relationDepth": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.dimensions", hasItem("currency.title")))
                .andExpect(jsonPath("$.cells", notNullValue()));
    }

    @Test void adminCanReadOlapCatalogAndProfiles() throws Exception {
        mvc.perform(get("/api/bi/olap/catalog").with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", not(empty())));

        mvc.perform(get("/api/bi/olap/profiles?topValues=5")
                        .with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isOk());
    }

    @Test void adminCanExercisePaymentTransactionCrudApi() throws Exception {
        var created = mvc.perform(post("/api/resources/payment-transaction")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "values": {
                                    "status": "POSTED",
                                    "description": "CI test",
                                    "amount": "100",
                                    "quantity": "2",
                                    "payment": "100"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).get("id").asLong();

        mvc.perform(get("/api/resources/payment-transaction/" + id)
                        .with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.status", is("POSTED")));

        mvc.perform(put("/api/resources/payment-transaction/" + id)
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {"values":{"status":"SETTLED","amount":"150"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.status", is("SETTLED")));

        mvc.perform(patch("/api/resources/payment-transaction/" + id)
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {"values":{"description":"patched"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.description", is("patched")));

        mvc.perform(delete("/api/resources/payment-transaction/" + id)
                        .with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isNoContent());
    }

    @Test void adminCanListPaymentTransactionsAndReadMetadata() throws Exception {
        mvc.perform(get("/api/resources/payment-transaction")
                        .with(httpBasic("admin", "admin-pass"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)));

        mvc.perform(get("/api/resources/payment-transaction/metadata")
                        .with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test void adminCanFilterPaymentTransactionsInBiQuery() throws Exception {
        mvc.perform(post("/api/bi/resources/payment-transaction/query")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status"],
                                  "measures": ["count", "sum:amount"],
                                  "filters": {"status": "POSTED"},
                                  "orderBy": "sum:amount:desc",
                                  "limit": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.dimensions", hasItem("status")))
                .andExpect(jsonPath("$.rows", notNullValue()));
    }

    @Test void adminCanRunOlapCubeWithoutPivot() throws Exception {
        mvc.perform(post("/api/bi/olap/resources/payment-transaction/cube")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status"],
                                  "measures": ["count", "sum:amount"],
                                  "filters": {},
                                  "orderBy": "sum:amount:desc",
                                  "limit": 25,
                                  "includeTotals": true,
                                  "includeRollup": false,
                                  "pivotDimension": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.cells", notNullValue()))
                .andExpect(jsonPath("$.grandTotal", notNullValue()));
    }

    @Test void adminCanDrillDownPaymentTransactionsWithFinerDimension() throws Exception {
        mvc.perform(post("/api/bi/olap/resources/payment-transaction/drilldown")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "dimensions": ["status", "paymentTransactionDirection"],
                                  "measures": ["count"],
                                  "filters": {},
                                  "orderBy": "count:desc",
                                  "limit": 25,
                                  "includeTotals": false,
                                  "includeRollup": false,
                                  "pivotDimension": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource", is("payment-transaction")))
                .andExpect(jsonPath("$.cells", notNullValue()));
    }

    @Test void crudRejectsUnknownResource() throws Exception {
        mvc.perform(get("/api/resources/does-not-exist")
                        .with(httpBasic("admin", "admin-pass")))
                .andExpect(status().isConflict());
    }

    @Test void crudMetadataRequiresReadAuthorization() throws Exception {
        mvc.perform(get("/api/resources/customer/metadata")
                        .with(httpBasic("accounting", "accounting-pass")))
                .andExpect(status().isForbidden());
    }

    @Test void unauthorizedBiOlapAndCrudWritesAreRejected() throws Exception {
        mvc.perform(get("/api/bi/olap/catalog"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/bi/resources/payment-transaction/query")
                        .with(httpBasic("admin", "admin-pass"))
                        .contentType("application/json")
                        .content("{\"dimensions\":[\"status\"],\"measures\":[\"count\"]}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/resources/payment-transaction")
                        .with(httpBasic("accounting", "accounting-pass"))
                        .contentType("application/json")
                        .content("{\"values\":{\"status\":\"DENIED\"}}"))
                .andExpect(status().isForbidden());
    }

    @Test void accountingCannotReadUnprivilegedResource() throws Exception {
        mvc.perform(get("/api/resources/customer").with(httpBasic("accounting", "accounting-pass")))
                .andExpect(status().isForbidden());
    }

    @Test void userSpecificDenyOverridesRoleAllow() throws Exception {
        mvc.perform(get("/api/resources/invoice").with(httpBasic("readonly", "readonly-pass")))
                .andExpect(status().isForbidden());
    }

    @Test void reportsFollowPolicyAuthorization() throws Exception {
        mvc.perform(get("/api/reports/resources/invoice").with(httpBasic("readonly", "readonly-pass")))
                .andExpect(status().isForbidden());
    }

    private PaymentTransaction paymentTransaction(String status, Long amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setQuantity(1L);
        return transaction;
    }

    private ErpUser user(String username, String password, String level, String role) {
        ErpUser u = new ErpUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setLevel(level);
        u.getRoles().add(role);
        return u;
    }

    private void allowRole(String role, String module, String resource, String action) {
        policy(null, role, module, resource, action, ErpPolicy.PolicyEffect.ALLOW);
    }

    private void denyUser(String username, String module, String resource, String action) {
        policy(username, null, module, resource, action, ErpPolicy.PolicyEffect.DENY);
    }

    private void policy(String username, String role, String module, String resource, String action,
                        ErpPolicy.PolicyEffect effect) {
        ErpPolicy p = new ErpPolicy();
        p.setUsername(username);
        p.setRole(role);
        p.setModule(module);
        p.setResource(resource);
        p.setAction(action);
        p.setEffect(effect);
        policies.save(p);
    }
}
