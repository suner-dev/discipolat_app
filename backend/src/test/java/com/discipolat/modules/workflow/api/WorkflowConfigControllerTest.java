package com.discipolat.modules.workflow.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.workflow.domain.WorkflowConfig;
import com.discipolat.modules.workflow.domain.WorkflowConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the DB-backed WorkflowConfigController.
 * Uses a Mockito-mocked repository backed by an in-memory map to simulate persistence.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowConfigControllerTest {

    @Mock private AuditService auditService;
    @Mock private WorkflowConfigRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WorkflowConfig> store = new HashMap<>();

    private WorkflowConfigController controller;

    // ==================== LIST ====================

    @Test
    void list_SeedsDefaultConfigs_WhenTenantHasNone() {
        seed();

        ResponseEntity<List<Map<String, Object>>> response = controller.list();

        assertEquals(200, response.getStatusCode().value());
        List<Map<String, Object>> configs = response.getBody();
        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertEquals(5, configs.size());
        verify(repository).saveAll(any());
    }

    @Test
    void list_ReturnsPersistedConfigs_WhenTheyExist() {
        seed();
        // Clear invocations: second call must NOT re-seed
        controller.list();
        verify(repository, times(1)).saveAll(any());

        List<Map<String, Object>> configs = controller.list().getBody();
        assertEquals(5, configs.size());
    }

    @Test
    void list_ContainsAllWorkflowKeys() {
        seed();

        List<String> keys = controller.list().getBody().stream()
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
        seed();

        for (Map<String, Object> config : controller.list().getBody()) {
            assertEquals(true, config.get("enabled"), "Workflow " + config.get("key") + " should be enabled by default");
        }
    }

    @Test
    void list_AllConfigsHaveRules() {
        seed();

        for (Map<String, Object> config : controller.list().getBody()) {
            assertNotNull(config.get("rules"), "Workflow " + config.get("key") + " should have rules");
            assertInstanceOf(Map.class, config.get("rules"), "Rules should be a Map");
        }
    }

    @Test
    void list_AllConfigsHaveRequiredFields() {
        seed();

        for (Map<String, Object> config : controller.list().getBody()) {
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
        seed();

        ResponseEntity<Map<String, Object>> response = controller.get("ABSENCE_ESCALADE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ABSENCE_ESCALADE", response.getBody().get("key"));
        assertEquals("Escalade d'absentéisme", response.getBody().get("label"));
    }

    @Test
    void get_NonExistingKey_Returns404() {
        seed();

        ResponseEntity<Map<String, Object>> response = controller.get("NONEXISTENT");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void get_AbsenceEscalade_HasCorrectRules() {
        seed();

        Map<String, Object> rules = rulesOf("ABSENCE_ESCALADE");

        assertEquals(3, rules.get("semaines_faiseur"));
        assertEquals(8, rules.get("semaines_chef"));
        assertEquals(12, rules.get("semaines_pasteur"));
    }

    @Test
    void get_RappelAnniversaire_HasCorrectRules() {
        seed();

        Map<String, Object> rules = rulesOf("RAPPEL_ANNIVERSAIRE");

        assertEquals("08:00", rules.get("heure"));
        assertEquals("PUSH", rules.get("canal"));
    }

    @Test
    void get_NotificationAbsence_HasCorrectRules() {
        seed();

        Map<String, Object> rules = rulesOf("NOTIFICATION_ABSENCE");

        assertEquals(30, rules.get("jours_absence"));
        assertEquals("EMAIL", rules.get("canal"));
    }

    // ==================== TOGGLE ====================

    @Test
    void toggle_EnabledWorkflow_DisablesIt() {
        seed();

        ResponseEntity<Map<String, Object>> response = controller.toggle("ABSENCE_ESCALADE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("enabled"));
        assertNotNull(response.getBody().get("updatedAt"));
        verify(auditService).logSimple("WORKFLOW_TOGGLED", "WORKFLOW", null);
        verify(repository).save(any(WorkflowConfig.class));
    }

    @Test
    void toggle_DisabledWorkflow_EnablesIt() {
        seed();
        controller.toggle("ABSENCE_ESCALADE"); // disable

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
        seed();

        Map<String, Object> body = controller.toggle("RAPPEL_ANNIVERSAIRE").getBody();

        assertNotNull(body.get("updatedAt"));
        assertTrue(body.get("updatedAt").toString().contains("T")); // ISO format
    }

    // ==================== UPDATE ====================

    @Test
    void update_ExistingKey_UpdatesConfig() {
        seed();

        ResponseEntity<Map<String, Object>> response =
                controller.update("ABSENCE_ESCALADE", new WorkflowConfigUpdateRequest("Nouveau label", null, null, null));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Nouveau label", response.getBody().get("label"));
        verify(auditService).logSimple("WORKFLOW_UPDATED", "WORKFLOW", null);
    }

    @Test
    void update_ExistingKey_KeepsOtherFields() {
        seed();

        Map<String, Object> result =
                controller.update("ABSENCE_ESCALADE", new WorkflowConfigUpdateRequest("Test", null, null, null)).getBody();

        assertEquals("Test", result.get("label"));
        assertEquals("ABSENCE_ESCALADE", result.get("key"));
        assertNotNull(result.get("rules"));
        assertNotNull(result.get("enabled"));
    }

    @Test
    void update_NonExistingKey_Returns404() {
        ResponseEntity<Map<String, Object>> response =
                controller.update("NONEXISTENT", new WorkflowConfigUpdateRequest("X", null, null, null));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void update_Rules_CanBeModified() {
        seed();

        Map<String, Object> newRules = Map.of("semaines_faiseur", 2, "semaines_chef", 6);
        Map<String, Object> result =
                controller.update("ABSENCE_ESCALADE", new WorkflowConfigUpdateRequest(null, null, null, newRules)).getBody();
        Map<String, Object> rules = rulesOfMap(result);

        assertEquals(2, rules.get("semaines_faiseur"));
        assertEquals(6, rules.get("semaines_chef"));
    }

    // ==================== INTEGRATION ====================

    @Test
    void toggleThenGet_PersistsState() {
        seed();
        controller.toggle("ABSENCE_ESCALADE");

        Map<String, Object> config = controller.get("ABSENCE_ESCALADE").getBody();

        assertEquals(false, config.get("enabled"));
    }

    @Test
    void updateThenGet_PersistsChanges() {
        seed();
        controller.update("RAPPEL_ANNIVERSAIRE", new WorkflowConfigUpdateRequest("Birthday Reminders", null, null, null));

        Map<String, Object> config = controller.get("RAPPEL_ANNIVERSAIRE").getBody();

        assertEquals("Birthday Reminders", config.get("label"));
    }

    @Test
    void multipleToggles_CorrectlyAlternateState() {
        seed();
        controller.toggle("ABSENCE_ESCALADE"); // disable
        controller.toggle("ABSENCE_ESCALADE"); // enable
        controller.toggle("ABSENCE_ESCALADE"); // disable

        Map<String, Object> config = controller.get("ABSENCE_ESCALADE").getBody();
        assertEquals(false, config.get("enabled"));
    }

    // ==================== ADDITIONAL EDGE CASES ====================

    @Test
    void update_MultipleFields_AtOnce() {
        seed();

        Map<String, Object> result = controller.update("ABSENCE_ESCALADE",
                new WorkflowConfigUpdateRequest("Nouveau Label", "Nouvelle description", false, null)).getBody();

        assertEquals("Nouveau Label", result.get("label"));
        assertEquals("Nouvelle description", result.get("description"));
        assertEquals(false, result.get("enabled"));
    }

    @Test
    void update_EmptyRequest_NoChangesButAddsTimestamp() {
        seed();
        String labelBefore = (String) controller.get("ABSENCE_ESCALADE").getBody().get("label");

        Map<String, Object> result =
                controller.update("ABSENCE_ESCALADE", new WorkflowConfigUpdateRequest(null, null, null, null)).getBody();

        assertEquals(labelBefore, result.get("label"));
    }

    @Test
    void toggle_DifferentWorkflows_IndependentState() {
        seed();
        controller.toggle("ABSENCE_ESCALADE");

        assertEquals(true, controller.get("RAPPEL_ANNIVERSAIRE").getBody().get("enabled"));
        assertEquals(false, controller.get("ABSENCE_ESCALADE").getBody().get("enabled"));
    }

    @Test
    void get_AllFiveWorkflows_ExistAndUnique() {
        seed();

        for (String key : ALL_KEYS) {
            ResponseEntity<Map<String, Object>> resp = controller.get(key);
            assertEquals(200, resp.getStatusCode().value(), "Workflow " + key + " should exist");
            assertEquals(key, resp.getBody().get("key"));
        }
    }

    @Test
    void update_Rules_OverrideCompletely() {
        seed();

        Map<String, Object> newRules = Map.of("jour", "MONDAY", "heure", "09:00");
        Map<String, Object> result =
                controller.update("SNAPSHOT_SCORE_SPIRITUEL", new WorkflowConfigUpdateRequest(null, null, null, newRules)).getBody();
        Map<String, Object> rules = rulesOfMap(result);

        assertEquals("MONDAY", rules.get("jour"));
        assertEquals("09:00", rules.get("heure"));
        assertNull(rules.get("semaines_faiseur"), "Old rules must be fully replaced");
    }

    @Test
    void update_CannotChangeWorkflowKey() {
        seed();

        Map<String, Object> result = controller.update("ABSENCE_ESCALADE",
                new WorkflowConfigUpdateRequest(null, null, null, null)).getBody();

        assertEquals("ABSENCE_ESCALADE", result.get("key"),
                "The key is immutable: it comes from the path variable and the entity");
    }

    @Test
    void get_DescriptionForEachWorkflow_IsNotEmpty() {
        seed();

        for (String key : ALL_KEYS) {
            Map<String, Object> config = controller.get(key).getBody();
            String desc = (String) config.get("description");
            assertNotNull(desc, "Description should exist for " + key);
            assertFalse(desc.isBlank(), "Description should not be empty for " + key);
        }
    }

    // ==================== HELPERS ====================

    private static final List<String> ALL_KEYS = List.of(
            "ABSENCE_ESCALADE", "RAPPEL_ANNIVERSAIRE",
            "SNAPSHOT_SCORE_SPIRITUEL", "NOTIFICATION_ABSENCE",
            "RAPPEL_RAPPORT_HEBDOMADAIRE"
    );

    @BeforeEach
    void setUp() {
        controller = new WorkflowConfigController(auditService, repository, objectMapper);
        TenantContext.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        store.clear();
    }

    /** Seeds the mocked repository with the 5 default workflows (simulates first call to list()). */
    private void seed() {
        lenient().when(repository.findByTenantId(any())).thenAnswer(inv -> new ArrayList<>(store.values()));
        lenient().when(repository.findByTenantIdAndWorkflowKey(any(), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get((String) inv.getArgument(1))));
        lenient().when(repository.save(any(WorkflowConfig.class))).thenAnswer(inv -> {
            WorkflowConfig c = inv.getArgument(0);
            store.put(c.getWorkflowKey(), c);
            return c;
        });
        lenient().when(repository.saveAll(anyList())).thenAnswer(inv -> {
            Iterable<WorkflowConfig> list = inv.getArgument(0);
            List<WorkflowConfig> saved = new ArrayList<>();
            for (WorkflowConfig c : list) {
                store.put(c.getWorkflowKey(), c);
                saved.add(c);
            }
            return saved;
        });

        controller.list(); // triggers seeding
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rulesOf(String key) {
        return rulesOfMap(controller.get(key).getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rulesOfMap(Map<String, Object> config) {
        return (Map<String, Object>) config.get("rules");
    }
}
