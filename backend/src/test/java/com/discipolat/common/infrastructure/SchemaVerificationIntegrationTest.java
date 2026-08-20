package com.discipolat.common.infrastructure;

import com.discipolat.DiscipolatApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SCHEMA VERIFICATION — verifies all expected database tables exist.
 *
 * Uses H2 with ddl-auto: create-drop. Hibernate creates the schema from
 * JPA entities. This test verifies that all expected tables are created
 * with a minimum number of columns (resilient to H2 vs PostgreSQL differences).
 *
 * When a table is missing, it means either:
 * 1. The entity class is missing from the scan
 * 2. The @Table annotation has the wrong name
 * 3. The entity is not annotated with @Entity
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@DisplayName("Schema Verification — all tables created by Hibernate")
class SchemaVerificationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== CORE TABLES ====================

    @Test
    @DisplayName("Core tables exist: users, souls, families, departments, events")
    void coreTablesExist() {
        assertTableExists("users");
        assertTableExists("souls");
        assertTableExists("families");
        assertTableExists("departments");
        assertTableExists("events");
    }

    @Test
    @DisplayName("Users table has >= 8 columns")
    void usersTableHasEnoughColumns() {
        assertTableHasMinColumns("users", 8);
    }

    @Test
    @DisplayName("Souls table has >= 10 columns")
    void soulsTableHasEnoughColumns() {
        assertTableHasMinColumns("souls", 10);
    }

    @Test
    @DisplayName("Departments table has >= 5 columns")
    void departmentsTableHasEnoughColumns() {
        assertTableHasMinColumns("departments", 5);
    }

    @Test
    @DisplayName("Events table has >= 5 columns")
    void eventsTableHasEnoughColumns() {
        assertTableHasMinColumns("events", 5);
    }

    // ==================== AUTH & MULTI-TENANCY ====================

    @Test
    @DisplayName("User roles table exists (multi-role support)")
    void userRolesExists() {
        assertTableExists("user_roles");
    }

    @Test
    @DisplayName("Soul history table exists (audit trail)")
    void soulHistoryExists() {
        assertTableExists("soul_history");
    }

    // ==================== PLATFORM ====================

    @Test
    @DisplayName("Church settings table exists")
    void churchSettingsExists() {
        assertTableExists("church_settings");
    }

    @Test
    @DisplayName("Config revisions table exists (versioning)")
    void configRevisionsExists() {
        assertTableExists("config_revisions");
        assertTableHasMinColumns("config_revisions", 5);
    }

    // ==================== BUSINESS MODULES ====================

    @Test
    @DisplayName("Reports tables exist: maker_reports, family_reports")
    void reportsExists() {
        assertTableExists("maker_reports");
        assertTableHasMinColumns("maker_reports", 5);
        assertTableExists("family_reports");
    }

    @Test
    @DisplayName("Prayers table exists")
    void prayersExists() {
        assertTableExists("prayers");
    }

    @Test
    @DisplayName("Prayers table exists (includes actions de grâce)")
    void prayerActionsDeGraceExists() {
        assertTableExists("prayers");
        assertTableHasMinColumns("prayers", 5);
    }

    @Test
    @DisplayName("Evaluations table exists")
    void evaluationsExists() {
        assertTableExists("evaluations");
    }

    @Test
    @DisplayName("Soul discipline events table exists")
    void disciplineEventsExists() {
        assertTableExists("soul_discipline_events");
        assertTableHasMinColumns("soul_discipline_events", 8);
    }

    @Test
    @DisplayName("Transfer requests table exists")
    void transferRequestsExists() {
        assertTableExists("transfer_requests");
    }

    @Test
    @DisplayName("Parallel followups table exists")
    void parallelFollowupsExists() {
        assertTableExists("parallel_followups");
    }

    // ==================== DEPARTMENT TOOLS ====================

    @Test
    @DisplayName("Department tasks table exists")
    void departmentTasksExists() {
        assertTableExists("department_tasks");
    }

    @Test
    @DisplayName("Member departments table exists (dept assignments)")
    void departmentMembersExists() {
        assertTableExists("member_departments");
    }

    @Test
    @DisplayName("Department documents table exists")
    void departmentDocumentsExists() {
        assertTableExists("department_documents");
    }

    // ==================== FINANCES ====================

    @Test
    @DisplayName("Finance transactions table exists")
    void financeTransactionsExists() {
        assertTableExists("finance_transactions");
        assertTableHasMinColumns("finance_transactions", 5);
    }

    // ==================== COMMUNICATIONS ====================

    @Test
    @DisplayName("Communications table exists")
    void communicationsExists() {
        assertTableExists("communications");
    }

    // ==================== TRAININGS ====================

    @Test
    @DisplayName("Courses table exists")
    void coursesExists() {
        assertTableExists("courses");
    }

    @Test
    @DisplayName("Sermon transcriptions table exists")
    void sermonTranscriptionsExists() {
        assertTableExists("sermon_transcriptions");
        assertTableHasMinColumns("sermon_transcriptions", 8);
    }

    // ==================== MESSAGING ====================

    @Test
    @DisplayName("Conversations table exists")
    void conversationsExists() {
        assertTableExists("conversations");
    }

    @Test
    @DisplayName("Conversation messages table exists")
    void conversationMessagesExists() {
        assertTableExists("conversation_messages");
    }

    // ==================== NOTIFICATIONS & ALERTS ====================

    @Test
    @DisplayName("Notifications table exists")
    void notificationsExists() {
        assertTableExists("notifications");
        assertTableHasMinColumns("notifications", 5);
    }

    @Test
    @DisplayName("Notification templates table exists")
    void notificationTemplatesExists() {
        assertTableExists("notification_templates");
    }

    @Test
    @DisplayName("Alerts table exists")
    void alertsExists() {
        assertTableExists("alerts");
        assertTableHasMinColumns("alerts", 8);
    }

    // ==================== FILES ====================

    @Test
    @DisplayName("Files table exists")
    void filesExists() {
        assertTableExists("files");
    }

    @Test
    @DisplayName("Entity attachments table exists")
    void entityAttachmentsExists() {
        assertTableExists("entity_attachments");
    }

    // ==================== MEMBERS ====================

    @Test
    @DisplayName("Member presences table exists")
    void memberPresencesExists() {
        assertTableExists("member_presences");
    }

    @Test
    @DisplayName("Member requests table exists")
    void memberRequestsExists() {
        assertTableExists("member_requests");
    }

    // ==================== OBJECTIVES ====================

    @Test
    @DisplayName("Objectives table exists")
    void objectivesExists() {
        assertTableExists("objectives");
    }

    // ==================== VISITS ====================

    @Test
    @DisplayName("Visits table exists")
    void visitsExists() {
        assertTableExists("visits");
    }

    // ==================== COMPLETE TABLE LIST ====================

    @Test
    @DisplayName("Minimum 40 tables exist in the schema")
    void minimumTableCount() {
        List<String> tables = getAllTableNames();

        assertTrue(tables.size() >= 40,
                "Expected at least 40 tables, found " + tables.size());
    }

    @Test
    @DisplayName("All critical module tables are present")
    void criticalTablesPresent() {
        Set<String> tables = getAllTableNamesLowercase();

        Set<String> critical = Set.of(
            // Core
            "users", "souls", "families", "departments", "events",
            // Platform
            "config_revisions", "church_settings",
            // Business
            "maker_reports", "family_reports", "prayers", "evaluations", "soul_discipline_events",
            "transfer_requests", "parallel_followups",
            // Department tools
            "department_tasks",
            // Finances
            "finance_transactions",
            // Communications
            "communications",
            // Training
            "courses", "sermon_transcriptions",
            // Messaging
            "conversations", "conversation_messages",
            // Notifications
            "notifications", "alerts",
            // Files
            "files",
            // Members
            "member_presences", "member_requests",
            // Objectives
            "objectives",
            // Visits
            "visits",
            // Audit
            "soul_history",
            // Inventory (new module)
            "inventory_items"
        );

        Set<String> missing = critical.stream()
                .filter(t -> !tables.contains(t))
                .collect(Collectors.toSet());

        assertTrue(missing.isEmpty(),
                "Missing critical tables: " + missing);
    }

    @Test
    @DisplayName("Inventory module table has >= 15 columns")
    void inventoryHasEnoughColumns() {
        assertTableHasMinColumns("inventory_items", 15);
    }

    @Test
    @DisplayName("Custom fields table exists for extensibility")
    void customFieldsExist() {
        // custom_fields or custom_field tables may exist
        Set<String> tables = getAllTableNamesLowercase();
        boolean hasCustomFields = tables.contains("custom_fields") ||
                                  tables.stream().anyMatch(t -> t.startsWith("custom_field"));
        assertTrue(hasCustomFields, "Should have at least one custom field table");
    }

    // ==================== HELPERS ====================

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?",
                Integer.class, tableName.toUpperCase()
        );
        assertEquals(1, count, "Table '" + tableName + "' should exist");
    }

    private void assertTableHasMinColumns(String tableName, int minColumns) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?",
                Integer.class, tableName.toUpperCase()
        );
        assertTrue(count >= minColumns,
                "Table '" + tableName + "' should have at least " + minColumns +
                " columns, found " + count);
    }

    private List<String> getAllTableNames() {
        return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'"
        ).stream()
                .map(row -> (String) row.get("TABLE_NAME"))
                .sorted()
                .collect(Collectors.toList());
    }

    private Set<String> getAllTableNamesLowercase() {
        return getAllTableNames().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
