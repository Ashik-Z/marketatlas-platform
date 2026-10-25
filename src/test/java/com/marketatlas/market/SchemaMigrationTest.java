package com.marketatlas.market;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the application on an empty Postgres with ddl-auto=validate, so the schema comes
 * only from the Flyway migrations. If an entity and the migrations disagree, the context
 * fails to start and this test fails.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(TestPostgresConfiguration.class)
class SchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsAloneProduceASchemaHibernateAccepts() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertEquals(0, failed);
        assertTrue(applied >= 4, "expected at least V1-V4 to be applied, but was " + applied);
    }
}