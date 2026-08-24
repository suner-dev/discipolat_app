package com.discipolat.modules.automations.domain;

import com.discipolat.DiscipolatApplication;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du moteur d'automatisations — H2 in-memory.
 *
 * Couvre :
 * - CRUD complet des règles (create, list, get, update, toggle, delete)
 * - Moteur d'exécution : déclenchement multi-règles, filtre par statut, maxExécutions
 * - Historique d'exécution et statistiques
 * - Isolation multi-tenant
 * - Gestion des erreurs et cas limites
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AutomationEngineIntegrationTest {

    @Autowired private AutomationService automationService;
    @Autowired private AutomationRuleRepository ruleRepository;
    @Autowired private AutomationExecutionRepository executionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantA;
    private UUID tenantB;
    private UUID userIdA;
    private UUID userIdB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.fromString("11111111-1111-1111-1111-111111111111");
        tenantB = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Clean automation tables
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : List.of("automation_executions", "automation_rules")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        userIdA = saveUser("admin-a@test.com", tenantA, UserRole.ADMIN);
        userIdB = saveUser("admin-b@test.com", tenantB, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UUID saveUser(String email, UUID tenantId, UserRole role) {
        return userRepository.save(User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash("PLACEHOLDER")
                .firstName("Admin")
                .lastName("Test")
                .role(role)
                .roles(Set.of(role))
                .activeRole(role)
                .statut(UserStatus.ACTIVE)
                .build()).getId();
    }

    private void setTenant(UUID tenantId) {
        TenantContext.setTenantId(tenantId);
    }

    // ====================================================================
    // CRUD RÈGLES
    // ====================================================================

    @Test
    @Order(1)
    @DisplayName("Créer une règle d'automatisation et la persister en base")
    void createRule_shouldPersist() {
        setTenant(tenantA);

        AutomationRule rule = automationService.createRule(
                "Alerte absence",
                "Notifier quand un membre est absent 3 semaines",
                "ABSENCE_SOUTENUE",
                "{\"semaines\":3}",
                "ENVOYER_MESSAGE",
                "{\"message\":\"Membre absent\",\"cibleRole\":\"FAISEUR\"}",
                userIdA
        );

        assertNotNull(rule.getId());
        assertEquals("Alerte absence", rule.getTitre());
        assertEquals(AutomationRule.TriggerEvent.ABSENCE_SOUTENUE, rule.getTriggerEvent());
        assertEquals(AutomationRule.ActionType.ENVOYER_MESSAGE, rule.getActionType());
        assertEquals(AutomationRule.Statut.ACTIVE, rule.getStatut());
        assertEquals(tenantA, rule.getTenantId());
        assertEquals(userIdA, rule.getCrééPar());
        assertEquals(0, rule.getTotalExécutions());
    }

    @Test
    @Order(2)
    @DisplayName("Lister les règles avec pagination")
    void listRules_shouldReturnPaginated() {
        setTenant(tenantA);
        // Create 3 rules
        for (int i = 0; i < 3; i++) {
            automationService.createRule(
                    "Règle " + i, "Description", "QUOTIDIEN", "{}",
                    "ENVOYER_MESSAGE", "{}", userIdA);
        }

        var page = automationService.listRules(org.springframework.data.domain.PageRequest.of(0, 2));
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    @Order(3)
    @DisplayName("Récupérer une règle par ID")
    void getRuleById_shouldReturn() {
        setTenant(tenantA);
        AutomationRule created = automationService.createRule(
                "Test Get", "Desc", "HEBDOMADAIRE", "{}",
                "CRÉER_ALERTE", "{}", userIdA);

        AutomationRule found = automationService.getRuleById(created.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("Test Get", found.getTitre());
    }

    @Test
    @Order(4)
    @DisplayName("Récupérer une règle inexistante → EntityNotFoundException")
    void getRuleById_notFound_shouldThrow() {
        setTenant(tenantA);
        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> automationService.getRuleById(UUID.randomUUID()));
    }

    @Test
    @Order(5)
    @DisplayName("Mettre à jour les champs d'une règle")
    void updateRule_shouldModify() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Original", "Desc", "QUOTIDIEN", "{}",
                "ENVOYER_MESSAGE", "{}", userIdA);

        Map<String, String> updates = Map.of(
                "titre", "Modifié",
                "description", "Nouvelle description",
                "triggerParams", "{\"test\":true}"
        );
        AutomationRule updated = automationService.updateRule(rule.getId(), updates);

        assertEquals("Modifié", updated.getTitre());
        assertEquals("Nouvelle description", updated.getDescription());
        assertEquals("{\"test\":true}", updated.getTriggerParams());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    @Order(6)
    @DisplayName("Basculer le statut ACTIVE ↔ EN_PAUSE")
    void toggleRule_shouldSwitchStatus() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Toggle Test", "Desc", "QUOTIDIEN", "{}",
                "ENVOYER_MESSAGE", "{}", userIdA);
        assertEquals(AutomationRule.Statut.ACTIVE, rule.getStatut());

        AutomationRule toggled = automationService.toggleRule(rule.getId());
        assertEquals(AutomationRule.Statut.EN_PAUSE, toggled.getStatut());

        AutomationRule toggledAgain = automationService.toggleRule(rule.getId());
        assertEquals(AutomationRule.Statut.ACTIVE, toggledAgain.getStatut());
    }

    @Test
    @Order(7)
    @DisplayName("Supprimer une règle")
    void deleteRule_shouldRemove() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "À supprimer", "Desc", "QUOTIDIEN", "{}",
                "ENVOYER_MESSAGE", "{}", userIdA);
        UUID ruleId = rule.getId();

        automationService.deleteRule(ruleId);

        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> automationService.getRuleById(ruleId));
    }

    // ====================================================================
    // LISTE FILTRÉE
    // ====================================================================

    @Test
    @Order(10)
    @DisplayName("Lister uniquement les règles actives")
    void listActiveRules_shouldReturnOnlyActive() {
        setTenant(tenantA);
        automationService.createRule("Active 1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        AutomationRule paused = automationService.createRule("Paused 1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        automationService.toggleRule(paused.getId()); // → EN_PAUSE

        List<AutomationRule> activeRules = automationService.listActiveRules();
        assertEquals(1, activeRules.size());
        assertEquals("Active 1", activeRules.get(0).getTitre());
    }

    // ====================================================================
    // MOTEUR D'EXÉCUTION
    // ====================================================================

    @Test
    @Order(20)
    @DisplayName("Déclencher des règles pour un événement → exécution des règles actives correspondantes")
    void triggerRules_shouldExecuteActiveRules() {
        setTenant(tenantA);
        // Create 2 active rules for QUOTIDIEN and 1 for HEBDOMADAIRE
        automationService.createRule("R1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        automationService.createRule("R2", "D", "QUOTIDIEN", "{}", "CRÉER_ALERTE", "{}", userIdA);
        automationService.createRule("R3", "D", "HEBDOMADAIRE", "{}", "ENVOYER_EMAIL", "{}", userIdA);

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{\"source\":\"test\"}");

        // Only 2 QUOTIDIEN rules should be executed
        assertEquals(2, executions.size());
        assertTrue(executions.stream().allMatch(e -> e.getStatut() == AutomationExecution.Statut.SUCCÈS));
        assertTrue(executions.stream().allMatch(e -> e.getContexte().contains("test")));
    }

    @Test
    @Order(21)
    @DisplayName("Règles EN_PAUSE ne sont pas déclenchées")
    void triggerRules_shouldSkipPausedRules() {
        setTenant(tenantA);
        AutomationRule paused = automationService.createRule(
                "Paused Rule", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        automationService.toggleRule(paused.getId()); // → EN_PAUSE
        automationService.createRule("Active Rule", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{}");

        assertEquals(1, executions.size());
    }

    @Test
    @Order(22)
    @DisplayName("Aucune règle active pour cet événement → liste vide")
    void triggerRules_noMatchingRules_shouldReturnEmpty() {
        setTenant(tenantA);
        automationService.createRule("R1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.HEBDOMADAIRE, "{}");

        assertTrue(executions.isEmpty());
    }

    @Test
    @Order(23)
    @DisplayName("Le compteur totalExécutions est incrémenté après déclenchement")
    void triggerRules_shouldIncrementExecutionCount() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Counter", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        assertEquals(0, rule.getTotalExécutions());

        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");

        AutomationRule updated = automationService.getRuleById(rule.getId());
        assertEquals(1, updated.getTotalExécutions());
        assertNotNull(updated.getDernièreExécution());
    }

    @Test
    @Order(24)
    @DisplayName("Exécuter plusieurs fois → le compteur s'incrémente à chaque fois")
    void triggerRules_multipleExecutions_shouldIncrementEachTime() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Multi", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");

        AutomationRule updated = automationService.getRuleById(rule.getId());
        assertEquals(3, updated.getTotalExécutions());
    }

    // ====================================================================
    // LIMITE MAX D'EXÉCUTIONS
    // ====================================================================

    @Test
    @Order(25)
    @DisplayName("Règle avec maxExécutions=1 → après 1 exécution, plus déclenchée")
    void triggerRules_maxExecutionsReached_shouldStop() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Limited", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        rule.setMaxExécutions(1);
        ruleRepository.save(rule);

        // First trigger → should execute
        List<AutomationExecution> first = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertEquals(1, first.size());

        // Second trigger → should NOT execute (max reached)
        List<AutomationExecution> second = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertTrue(second.isEmpty());

        // Verify counter
        AutomationRule updated = automationService.getRuleById(rule.getId());
        assertEquals(1, updated.getTotalExécutions());
    }

    @Test
    @Order(26)
    @DisplayName("maxExécutions=0 → exécution illimitée")
    void triggerRules_unlimitedExecutions_shouldAlwaysExecute() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Unlimited", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        rule.setMaxExécutions(0);
        ruleRepository.save(rule);

        for (int i = 0; i < 5; i++) {
            List<AutomationExecution> result = automationService.triggerRules(
                    AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
            assertEquals(1, result.size());
        }

        AutomationRule updated = automationService.getRuleById(rule.getId());
        assertEquals(5, updated.getTotalExécutions());
    }

    // ====================================================================
    // HISTORIQUE D'EXÉCUTION
    // ====================================================================

    @Test
    @Order(30)
    @DisplayName("Lister l'historique d'exécution d'une règle")
    void listExecutions_shouldReturnHistory() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "With History", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{\"run\":1}");
        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{\"run\":2}");
        automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{\"run\":3}");

        List<AutomationExecution> history = automationService.listExecutions(rule.getId());
        assertEquals(3, history.size());
        // Most recent first
        assertTrue(history.get(0).getExécutéLe().isAfter(history.get(2).getExécutéLe()) ||
                history.get(0).getExécutéLe().isEqual(history.get(2).getExécutéLe()));
        // All should be successful
        assertTrue(history.stream().allMatch(e -> e.getStatut() == AutomationExecution.Statut.SUCCÈS));
    }

    // ====================================================================
    // STATISTIQUES
    // ====================================================================

    @Test
    @Order(35)
    @DisplayName("Statistiques : comptage par statut")
    void getStats_shouldReturnCorrectCounts() {
        setTenant(tenantA);
        // 2 active rules, 1 paused
        automationService.createRule("A1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        automationService.createRule("A2", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        AutomationRule paused = automationService.createRule("P1", "D", "QUOTIDIEN", "{}", "ENVOYER_EMAIL", "{}", userIdA);
        automationService.toggleRule(paused.getId());

        Map<String, Object> stats = automationService.getStats();
        assertEquals(2L, stats.get("totalRègles"));
        assertEquals(1L, stats.get("règlesEnPause"));
        assertEquals(AutomationRule.TriggerEvent.values().length, stats.get("triggersDisponibles"));
        assertEquals(AutomationRule.ActionType.values().length, stats.get("actionsDisponibles"));
    }

    // ====================================================================
    // ISOLATION MULTI-TENANT
    // ====================================================================

    @Test
    @Order(40)
    @DisplayName("Tenant A ne voit QUE ses règles")
    void tenantIsolation_shouldOnlySeeOwnRules() {
        // Tenant A creates 2 rules
        setTenant(tenantA);
        automationService.createRule("A1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        automationService.createRule("A2", "D", "HEBDOMADAIRE", "{}", "ENVOYER_EMAIL", "{}", userIdA);

        // Tenant B creates 1 rule
        setTenant(tenantB);
        automationService.createRule("B1", "D", "QUOTIDIEN", "{}", "CRÉER_ALERTE", "{}", userIdB);

        // Tenant A sees only its 2 rules
        setTenant(tenantA);
        var pageA = automationService.listRules(org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(2, pageA.getTotalElements());
        assertTrue(pageA.getContent().stream().allMatch(r -> r.getTenantId().equals(tenantA)));

        // Tenant B sees only its 1 rule
        setTenant(tenantB);
        var pageB = automationService.listRules(org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, pageB.getTotalElements());
        assertTrue(pageB.getContent().stream().allMatch(r -> r.getTenantId().equals(tenantB)));
    }

    @Test
    @Order(41)
    @DisplayName("Tenant A ne peut pas récupérer une règle de Tenant B par ID")
    void tenantIsolation_crossTenantAccess_shouldFail() {
        setTenant(tenantA);
        AutomationRule ruleA = automationService.createRule(
                "A-Only", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        setTenant(tenantB);
        // The service uses findById which is tenant-scoped → not found
        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> automationService.getRuleById(ruleA.getId()));
    }

    @Test
    @Order(42)
    @DisplayName("Déclencheur pour Tenant A n'exécute PAS les règles de Tenant B")
    void triggerRules_isolation_shouldNotTriggerOtherTenantRules() {
        setTenant(tenantA);
        automationService.createRule("A-Rule", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        setTenant(tenantB);
        automationService.createRule("B-Rule", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdB);

        // Trigger for Tenant B → only B's rule
        setTenant(tenantB);
        List<AutomationExecution> executionsB = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertEquals(1, executionsB.size());

        // Verify Tenant A's rule was NOT incremented
        setTenant(tenantA);
        var pageA = automationService.listRules(org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(0, pageA.getContent().get(0).getTotalExécutions());
    }

    // ====================================================================
    // TOUS LES TRIGGERS / ACTIONS
    // ====================================================================

    @Test
    @Order(50)
    @DisplayName("Chaque type de trigger peut être déclenché")
    void triggerRules_allTriggerTypes_shouldWork() {
        setTenant(tenantA);

        for (AutomationRule.TriggerEvent event : AutomationRule.TriggerEvent.values()) {
            // Create a rule for each trigger type
            automationService.createRule(
                    "Rule-" + event.name(), "D", event.name(), "{}",
                    "ENVOYER_MESSAGE", "{}", userIdA);

            List<AutomationExecution> executions = automationService.triggerRules(event, "{}");
            assertEquals(1, executions.size(), "Trigger " + event.name() + " should execute 1 rule");
        }

        // Verify all rules have 1 execution each
        var page = automationService.listRules(org.springframework.data.domain.PageRequest.of(0, 100));
        assertEquals(AutomationRule.TriggerEvent.values().length, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(r -> r.getTotalExécutions() == 1));
    }

    @Test
    @Order(51)
    @DisplayName("Chaque type d'action génère le bon résultat")
    void triggerRules_allActionTypes_shouldGenerateCorrectResult() {
        setTenant(tenantA);

        for (AutomationRule.ActionType action : AutomationRule.ActionType.values()) {
            automationService.createRule(
                    "Action-" + action.name(), "D", "QUOTIDIEN", "{}",
                    action.name(), "{}", userIdA);
        }

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertEquals(AutomationRule.ActionType.values().length, executions.size());
        assertTrue(executions.stream().allMatch(e -> e.getRésultat() != null && !e.getRésultat().isEmpty()));
    }

    // ====================================================================
    // CAS LIMITES
    // ====================================================================

    @Test
    @Order(60)
    @DisplayName("Contexte null ne plante pas l'exécution")
    void triggerRules_nullContext_shouldHandleGracefully() {
        setTenant(tenantA);
        automationService.createRule("R1", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, null);
        assertEquals(1, executions.size());
        assertEquals(AutomationExecution.Statut.SUCCÈS, executions.get(0).getStatut());
    }

    @Test
    @Order(61)
    @DisplayName("Créer une règle avec tous les types d'action possibles")
    void createRule_allActionTypes_shouldPersist() {
        setTenant(tenantA);
        int count = 0;
        for (AutomationRule.ActionType action : AutomationRule.ActionType.values()) {
            AutomationRule rule = automationService.createRule(
                    "Action-" + action.name(), "D", "QUOTIDIEN", "{}",
                    action.name(), "{}", userIdA);
            assertEquals(action, rule.getActionType());
            count++;
        }
        var page = automationService.listRules(org.springframework.data.domain.PageRequest.of(0, 100));
        assertEquals(count, page.getTotalElements());
    }

    @Test
    @Order(62)
    @DisplayName("Update sur une règle inexistante → exception")
    void updateRule_notFound_shouldThrow() {
        setTenant(tenantA);
        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> automationService.updateRule(UUID.randomUUID(), Map.of("titre", "X")));
    }

    @Test
    @Order(63)
    @DisplayName("Toggle sur une règle inexistante → exception")
    void toggleRule_notFound_shouldThrow() {
        setTenant(tenantA);
        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> automationService.toggleRule(UUID.randomUUID()));
    }

    @Test
    @Order(64)
    @DisplayName("Delete sur une règle inexistante → exception")
    void deleteRule_notFound_shouldThrow() {
        setTenant(tenantA);
        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> automationService.deleteRule(UUID.randomUUID()));
    }

    @Test
    @Order(65)
    @DisplayName("Liste vide quand aucun trigger ne correspond")
    void triggerRules_noRulesAtAll_shouldReturnEmpty() {
        setTenant(tenantA);
        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.SCORE_SPRITUEL_BAISSE, "{}");
        assertTrue(executions.isEmpty());
    }

    // ====================================================================
    // TRIGGERS MULTIPLES + SCÉNARIOS MÉTIER
    // ====================================================================

    @Test
    @Order(70)
    @DisplayName("Scénario métier : 3 règles, 1 pause → seules les 2 actives s'exécutent")
    void businessScenario_mixedRules() {
        setTenant(tenantA);
        // Rule 1: active, daily
        automationService.createRule("Quotidien email", "Email auto", "QUOTIDIEN", "{}",
                "ENVOYER_EMAIL", "{\"template\":\"daily\"}", userIdA);
        // Rule 2: active, daily
        automationService.createRule("Quotidien alert", "Alerte auto", "QUOTIDIEN", "{}",
                "CRÉER_ALERTE", "{\"niveau\":\"INFO\"}", userIdA);
        // Rule 3: paused, daily
        AutomationRule paused = automationService.createRule("Quotidien pause", "En pause", "QUOTIDIEN", "{}",
                "ENVOYER_MESSAGE", "{}", userIdA);
        automationService.toggleRule(paused.getId());

        // Trigger daily
        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "{\"date\":\"2026-08-24\"}");

        // Only 2 active rules should execute
        assertEquals(2, executions.size());
        assertTrue(executions.stream().allMatch(e ->
                e.getRésultat() != null && !e.getRésultat().isEmpty()));
    }

    @Test
    @Order(71)
    @DisplayName("Scénario métier : règle avec maxExécutions=2, exécutée 3 fois → seulement 2 premières")
    void businessScenario_maxExecutionsCap() {
        setTenant(tenantA);
        AutomationRule rule = automationService.createRule(
                "Limite 2", "D", "QUOTIDIEN", "{}", "ENVOYER_MESSAGE", "{}", userIdA);
        rule.setMaxExécutions(2);
        ruleRepository.save(rule);

        // Execute 3 times
        List<AutomationExecution> e1 = automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertEquals(1, e1.size());

        List<AutomationExecution> e2 = automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertEquals(1, e2.size());

        List<AutomationExecution> e3 = automationService.triggerRules(AutomationRule.TriggerEvent.QUOTIDIEN, "{}");
        assertTrue(e3.isEmpty());

        // History should have exactly 2 executions
        List<AutomationExecution> history = automationService.listExecutions(rule.getId());
        assertEquals(2, history.size());

        // Stats should reflect it
        Map<String, Object> stats = automationService.getStats();
        assertEquals(1L, stats.get("totalRègles"));
    }
}
