package com.discipolat.modules.workflow.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowConfigControllerTest {

    @Mock private AuditService auditService;
    @Mock private SecurityUtils securityUtils;

    private WorkflowConfigController controller;
    private static final String TENANT_KEY = "test-tenant";

    @BeforeEach
    void setUp() {
        controller = new WorkflowConfigController(auditService);
        TenantContext.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==================== LIST ====================

    @Test
    void list_ReturnsDefaultConfigs() {
        ResponseEntity<List<Map<String, Object>>> response = controller.list();

        assertEquals(200, response.getStatusCode().value());
        List<Map<String, Object>> configs = response.getBody();
        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertEquals(5, configs.size());
    }

    @Test
    void list_ContainsAllWorkflowKeys() {
        List<Map<String, Object>> configs = controller.list().getBody();

        List<String> keys = configs.stream()
                .map(c -> (String) c.get("key"))
                .toList();

        assertTrue(keys.contains("ABSENCE_ESCALADE"));
        assertTrue(keys.contains("RAPPEL_ANNIVERSAIRE"));
        assertTrue(keys.contains("SNAPSHOT_SCORE_SPIRITUEL"));
        assertTrue(keys.contains("NOTIFICATION_ABSENCE"));
        assertTrue(keys.contains("RAPPEL_RAPPORT_HEBDOMADAIRE"));
    }

    @Test
    void list_AllConfigsAreEnabledByDefault() {
        List<Map<String, Object>> configs = controller.list().getBody();

        for (Map<String, Object> config : configs) {
            assertEquals(true, config.get("enabled"), "Workflow " + config.get("key") + " should be enabled by default");
        }
    }

    @Test
    void list_AllConfigsHaveRules() {
        List<Map<String, Object>> configs = controller.list().getBody();

        for (Map<String, Object> config : configs) {
            assertNotNull(config.get("rules"), "Workflow " + config.get("key") + " should have rules");
            assertTrue(config.get("rules") instanceof Map, "Rules should be a Map");
        }
    }

    @Test
    void list_AllConfigsHaveRequiredFields() {
        List<Map<String, Object>> configs = controller.list().getBody();

        for (Map<String, Object> config : configs) {
            assertNotNull(config.get("key"));
            assertNotNull(config.get("label"));
            assertNotNull(config.get("description"));
            assertNotNull(config.get("enabled"));
            assertNotNull(config.get("rules"));
            assertNotNull(config.get("createdAt"));
        }
    }

    // ==================== GET ====================

    @Test
    void get_ExistingKey_ReturnsConfig() {
        ResponseEntity<Map<String, Object>> response = controller.get("ABSENCE_ESCALADE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ABSENCE_ESCALADE", response.getBody().get("key"));
        assertEquals("Escalade d'absentéisme", response.getBody().get("label"));
    }

    @Test
    void get_NonExistingKey_Returns404() {
        ResponseEntity<Map<String, Object>> response = controller.get("NONEXISTENT");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void get_AbsenceEscalade_HasCorrectRules() {
        Map<String, Object> config = controller.get("ABSENCE_ESCALADE").getBody();
        Map<String, Object> rules = (Map<String, Object>) config.get("rules");

        assertEquals(3, rules.get("semaines_faiseur"));
        assertEquals(8, rules.get("semaines_chef"));
        assertEquals(12, rules.get("semaines_pasteur"));
    }

    @Test
    void get_RappelAnniversaire_HasCorrectRules() {
        Map<String, Object> config = controller.get("RAPPEL_ANNIVERSAIRE").getBody();
        Map<String, Object> rules = (Map<String, Object>) config.get("rules");

        assertEquals("08:00", rules.get("heure"));
        assertEquals("PUSH", rules.get("canal"));
    }

    @Test
    void get_NotificationAbsence_HasCorrectRules() {
        Map<String, Object> config = controller.get("NOTIFICATION_ABSENCE").getBody();
        Map<String, Object> rules = (Map<String, Object>) config.get("rules");

        assertEquals(30, rules.get("jours_absence"));
        assertEquals("EMAIL", rules.get("canal"));
    }

    // ==================== TOGGLE ====================

    @Test
    void toggle_EnabledWorkflow_DisablesIt() {
        ResponseEntity<Map<String, Object>> response = controller.toggle("ABSENCE_ESCALADE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("enabled"));
        assertNotNull(response.getBody().get("updatedAt"));
        verify(auditService).logSimple("WORKFLOW_TOGGLED", "WORKFLOW", null);
    }

    @Test
    void toggle_DisabledWorkflow_EnablesIt() {
        // First toggle to disable
        controller.toggle("ABSENCE_ESCALADE");
        // Then toggle again to re-enable
        ResponseEntity<Map<String, Object>> response = controller.toggle("ABSENCE_ESCALADE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody().get("enabled"));
    }

    @Test
    void toggle_NonExistingKey_Returns404() {
        ResponseEntity<Map<String, Object>> response = controller.toggle("NONEXISTENT");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void toggle_AddsUpdatedAtTimestamp() {
        ResponseEntity<Map<String, Object>> response = controller.toggle("RAPPEL_ANNIVERSAIRE");

        assertNotNull(response.getBody().get("updatedAt"));
        String timestamp = response.getBody().get("updatedAt").toString();
        assertTrue(timestamp.contains("T")); // ISO format
    }

    // ==================== UPDATE ====================

    @Test
    void update_ExistingKey_UpdatesConfig() {
        Map<String, Object> update = Map.of("label", "Nouveau label");

        ResponseEntity<Map<String, Object>> response = controller.update("ABSENCE_ESCALADE", update);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Nouveau label", response.getBody().get("label"));
        assertNotNull(response.getBody().get("updatedAt"));
        verify(auditService).logSimple("WORKFLOW_UPDATED", "WORKFLOW", null);
    }

    @Test
    void update_ExistingKey_KeepsOtherFields() {
        Map<String, Object> update = Map.of("label", "Test");

        Map<String, Object> result = controller.update("ABSENCE_ESCALADE", update).getBody();

        assertEquals("Test", result.get("label"));
        assertEquals("ABSENCE_ESCALADE", result.get("key"));
        assertNotNull(result.get("rules"));
        assertNotNull(result.get("enabled"));
    }

    @Test
    void update_NonExistingKey_Returns404() {
        ResponseEntity<Map<String, Object>> response = controller.update("NONEXISTENT", Map.of("label", "X"));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void update_Rules_CanBeModified() {
        Map<String, Object> newRules = Map.of("semaines_faiseur", 2, "semaines_chef", 6);
        Map<String, Object> update = Map.of("rules", newRules);

        Map<String, Object> result = controller.update("ABSENCE_ESCALADE", update).getBody();
        Map<String, Object> rules = (Map<String, Object>) result.get("rules");

        assertEquals(2, rules.get("semaines_faiseur"));
        assertEquals(6, rules.get("semaines_chef"));
    }

    // ==================== INTEGRATION ====================

    @Test
    void toggleThenGet_PersistsState() {
        controller.toggle("ABSENCE_ESCALADE");
        Map<String, Object> config = controller.get("ABSENCE_ESCALADE").getBody();

        assertEquals(false, config.get("enabled"));
    }

    @Test
    void updateThenGet_PersistsChanges() {
        controller.update("RAPPEL_ANNIVERSAIRE", Map.of("label", "Birthday Reminders"));
        Map<String, Object> config = controller.get("RAPPEL_ANNIVERSAIRE").getBody();

        assertEquals("Birthday Reminders", config.get("label"));
    }

    @Test
    void multipleToggles_CorrectlyAlternateState() {
        controller.toggle("ABSENCE_ESCALADE"); // disable
        controller.toggle("ABSENCE_ESCALADE"); // enable
        controller.toggle("ABSENCE_ESCALADE"); // disable

        Map<String, Object> config = controller.get("ABSENCE_ESCALADE").getBody();
        assertEquals(false, config.get("enabled"));
    }

    // ==================== ADDITIONAL EDGE CASES ====================

    @Test
    void update_MultipleFields_AtOnce() {
        Map<String, Object> update = Map.of(
                "label", "Nouveau Label",
                "description", "Nouvelle description",
                "enabled", false
        );

        Map<String, Object> result = controller.update("ABSENCE_ESCALADE", update).getBody();

        assertEquals("Nouveau Label", result.get("label"));
        assertEquals("Nouvelle description", result.get("description"));
        assertEquals(false, result.get("enabled"));
        assertNotNull(result.get("updatedAt"));
    }

    @Test
    void update_EmptyMap_NoChangesButAddsTimestamp() {
        Map<String, Object> before = controller.get("ABSENCE_ESCALADE").getBody();

        Map<String, Object> result = controller.update("ABSENCE_ESCALADE", Map.of()).getBody();

        assertEquals(before.get("key"), result.get("key"));
        assertEquals(before.get("label"), result.get("label"));
        assertNotNull(result.get("updatedAt"));
    }

    @Test
    void toggle_DifferentWorkflows_IndependentState() {
        // Disable ABSENCE_ESCALADE
        controller.toggle("ABSENCE_ESCALADE");
        // RAPPEL_ANNIVERSAIRE should still be enabled
        Map<String, Object> other = controller.get("RAPPEL_ANNIVERSAIRE").getBody();
        assertEquals(true, other.get("enabled"));

        // ABSENCE_ESCALADE should be disabled
        Map<String, Object> absent = controller.get("ABSENCE_ESCALADE").getBody();
        assertEquals(false, absent.get("enabled"));
    }

    @Test
    void get_AllFiveWorkflows_ExistAndUnique() {
        List<String> keys = List.of(
                "ABSENCE_ESCALADE", "RAPPEL_ANNIVERSAIRE",
                "SNAPSHOT_SCORE_SPIRITUEL", "NOTIFICATION_ABSENCE",
                "RAPPEL_RAPPORT_HEBDOMADAIRE"
        );

        for (String key : keys) {
            ResponseEntity<Map<String, Object>> resp = controller.get(key);
            assertEquals(200, resp.getStatusCode().value(), "Workflow " + key + " should exist");
            assertEquals(key, resp.getBody().get("key"));
        }
    }

    @Test
    void update_Rules_OverrideCompletely() {
        Map<String, Object> newRules = Map.of("jour", "MONDAY", "heure", "09:00");

        Map<String, Object> result = controller.update("SNAPSHOT_SCORE_SPIRITUEL", Map.of("rules", newRules)).getBody();
        Map<String, Object> rules = (Map<String, Object>) result.get("rules");

        assertEquals("MONDAY", rules.get("jour"));
        assertEquals("09:00", rules.get("heure"));
    }

    @Test
    void list_TenantIsolation_FirstCallCreatesDefaults() {
        // First call for a new tenant should create defaults
        List<Map<String, Object>> configs = controller.list().getBody();
        assertNotNull(configs);
        assertEquals(5, configs.size());
    }

    @Test
    void update_KeyField_CanBeOverwrittenByPutAll() {
        // putAll merges the map, so 'key' field IS overwritten (current behavior)
        Map<String, Object> result = controller.update("ABSENCE_ESCALADE", Map.of("key", "HACKED_KEY")).getBody();

        assertEquals("HACKED_KEY", result.get("key"));
        assertNotNull(result.get("updatedAt"));
    }

    @Test
    void toggle_TimestampFormat_ContainsISO() {
        Map<String, Object> result = controller.toggle("RAPPEL_ANNIVERSAIRE").getBody();
        String timestamp = result.get("updatedAt").toString();

        // Should contain T separator (ISO format)
        assertTrue(timestamp.contains("T"), "Timestamp should be ISO format with T separator, got: " + timestamp);
        assertTrue(timestamp.length() >= 19, "Timestamp should be at least 19 chars, got: " + timestamp);
    }

    @Test
    void get_DescriptionForEachWorkflow_IsNotEmpty() {
        List<String> keys = List.of(
                "ABSENCE_ESCALADE", "RAPPEL_ANNIVERSAIRE",
                "SNAPSHOT_SCORE_SPIRITUEL", "NOTIFICATION_ABSENCE",
                "RAPPEL_RAPPORT_HEBDOMADAIRE"
        );

        for (String key : keys) {
            Map<String, Object> config = controller.get(key).getBody();
            String desc = (String) config.get("description");
            assertNotNull(desc, "Description should exist for " + key);
            assertFalse(desc.isBlank(), "Description should not be empty for " + key);
        }
    }
}
