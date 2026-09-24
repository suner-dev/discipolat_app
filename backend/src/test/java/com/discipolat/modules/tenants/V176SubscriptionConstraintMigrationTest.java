package com.discipolat.modules.tenants;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V176SubscriptionConstraintMigrationTest {

    @Test
    void allowsPendingChangeAndCanBeAppliedAgain() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:quota_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tenant_subscriptions (id INT PRIMARY KEY, status VARCHAR(20) NOT NULL)");
            statement.execute("ALTER TABLE tenant_subscriptions DROP CONSTRAINT IF EXISTS tenant_subscriptions_status_check");
            statement.execute("ALTER TABLE tenant_subscriptions ADD CONSTRAINT tenant_subscriptions_status_check CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'PAUSED', 'EXPIRED', 'PENDING_CHANGE'))");
            assertDoesNotThrow(() -> statement.execute("ALTER TABLE tenant_subscriptions DROP CONSTRAINT IF EXISTS tenant_subscriptions_status_check"));
            assertDoesNotThrow(() -> statement.execute("ALTER TABLE tenant_subscriptions ADD CONSTRAINT tenant_subscriptions_status_check CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'PAUSED', 'EXPIRED', 'PENDING_CHANGE'))"));
            statement.execute("INSERT INTO tenant_subscriptions VALUES (1, 'ACTIVE')");
            statement.execute("INSERT INTO tenant_subscriptions VALUES (2, 'PENDING_CHANGE')");
            assertDoesNotThrow(() -> statement.execute("INSERT INTO tenant_subscriptions VALUES (3, 'PENDING_CHANGE')"));
            assertThrows(Exception.class, () -> statement.execute("INSERT INTO tenant_subscriptions VALUES (4, 'UNKNOWN')"));
        }
    }
}
