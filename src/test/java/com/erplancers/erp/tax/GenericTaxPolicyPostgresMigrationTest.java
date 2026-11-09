package com.erplancers.erp.tax;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class GenericTaxPolicyPostgresMigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migrationSupportsRegionalJurisdictionsAndEnforcesRelationalConstraints() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE contact (id BIGSERIAL PRIMARY KEY)");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V2026110901__create_generic_tax_policy.sql"));

            try (ResultSet columns = statement.executeQuery("""
                    SELECT character_maximum_length
                    FROM information_schema.columns
                    WHERE table_name = 'tax_policy' AND column_name = 'jurisdiction_code'
                    """)) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt(1)).isEqualTo(32);
            }

            long companyId;
            try (ResultSet company = statement.executeQuery("INSERT INTO contact DEFAULT VALUES RETURNING id")) {
                company.next();
                companyId = company.getLong(1);
            }

            insertPolicy(connection, companyId, "US-CA", 20);
            assertThrows(SQLException.class, () -> insertPolicy(connection, companyId + 1000, "US-NY", 8));
            assertThrows(SQLException.class, () -> insertPolicy(connection, companyId, "US-CA", 125));
        }
    }

    private void insertPolicy(Connection connection, long companyId, String jurisdiction, int rate)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tax_policy
                    (name, company_id, jurisdiction_code, rate, valid_from, valid_until, priority,
                     tax_inclusive, active)
                VALUES (?, ?, ?, ?, DATE '2026-01-01', DATE '2026-12-31', 10, FALSE, TRUE)
                """)) {
            statement.setString(1, "Integration policy");
            statement.setLong(2, companyId);
            statement.setString(3, jurisdiction);
            statement.setInt(4, rate);
            statement.executeUpdate();
        }
    }
}
