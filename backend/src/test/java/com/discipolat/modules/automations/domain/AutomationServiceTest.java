package com.discipolat.modules.automations.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTest {

    @Mock
    private AutomationRuleRepository ruleRepository;
    @Mock
    private AutomationExecutionRepository executionRepository;

    private AutomationService automationService;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        automationService = new AutomationService(ruleRepository, executionRepository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── CREATE ─────────────────────────────────────────────────

    @Test
    void createRule_shouldSaveAndReturnRule() {
        when(ruleRepository.save(any(AutomationRule.class))).thenAnswer(inv -> {
            AutomationRule rule = inv.getArgument(0);
            rule.setId(UUID.randomUUID());
            return rule;
        });

        AutomationRule created = automationService.createRule(
                "Alerte absences", "Envoie un message après 3 semaines",
                "ABSENCE_SOUTENUE", "{\"semaines\":3}", "ENVOYER_MESSAGE",
                "{\"destinataire\":\"FAISEUR\"}", userId);

        assertNotNull(created);
        assertEquals("Alerte absences", created.getTitre());
        assertEquals(tenantId, created.getTenantId());
        assertEquals(AutomationRule.TriggerEvent.ABSENCE_SOUTENUE, created.getTriggerEvent());
        assertEquals(AutomationRule.ActionType.ENVOYER_MESSAGE, created.getActionType());
        assertEquals(userId, created.getCrééPar());
        assertEquals(AutomationRule.Statut.ACTIVE, created.getStatut());
        verify(ruleRepository).save(any(AutomationRule.class));
    }

    // ── READ ───────────────────────────────────────────────────

    @Test
    void listRules_shouldReturnPage() {
        AutomationRule rule = new AutomationRule();
        rule.setId(UUID.randomUUID());
        rule.setTitre("Test");
        when(ruleRepository.findByTenantIdOrderByCreatedAtDesc(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(rule)));

        Page<AutomationRule> result = automationService.listRules(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Test", result.getContent().get(0).getTitre());
    }

    @Test
    void listActiveRules_shouldFilterByStatus() {
        AutomationRule active = new AutomationRule();
        active.setStatut(AutomationRule.Statut.ACTIVE);
        when(ruleRepository.findByTenantIdAndStatutOrderByCreatedAtDesc(tenantId, AutomationRule.Statut.ACTIVE))
                .thenReturn(List.of(active));

        List<AutomationRule> result = automationService.listActiveRules();

        assertEquals(1, result.size());
        assertEquals(AutomationRule.Statut.ACTIVE, result.get(0).getStatut());
    }

    @Test
    void getRuleById_existingRule_shouldReturn() {
        UUID ruleId = UUID.randomUUID();
        AutomationRule rule = new AutomationRule();
        rule.setId(ruleId);
        rule.setTitre("Test rule");
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        AutomationRule result = automationService.getRuleById(ruleId);

        assertEquals(ruleId, result.getId());
    }

    @Test
    void getRuleById_notFound_shouldThrow() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> automationService.getRuleById(ruleId));
    }

    // ── UPDATE ─────────────────────────────────────────────────

    @Test
    void updateRule_shouldUpdateFields() {
        UUID ruleId = UUID.randomUUID();
        AutomationRule existing = new AutomationRule();
        existing.setId(ruleId);
        existing.setTitre("Old title");
        existing.setUpdatedAt(null);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> updates = Map.of("titre", "New title", "description", "Updated desc");
        AutomationRule updated = automationService.updateRule(ruleId, updates);

        assertEquals("New title", updated.getTitre());
        assertEquals("Updated desc", updated.getDescription());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void updateRule_nonexistent_shouldThrow() {
        when(ruleRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> automationService.updateRule(UUID.randomUUID(), Map.of("titre", "x")));
    }

    // ── TOGGLE ─────────────────────────────────────────────────

    @Test
    void toggleRule_activeToPaused_shouldPause() {
        UUID ruleId = UUID.randomUUID();
        AutomationRule rule = new AutomationRule();
        rule.setId(ruleId);
        rule.setStatut(AutomationRule.Statut.ACTIVE);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AutomationRule toggled = automationService.toggleRule(ruleId);

        assertEquals(AutomationRule.Statut.EN_PAUSE, toggled.getStatut());
    }

    @Test
    void toggleRule_pausedToActive_shouldActivate() {
        UUID ruleId = UUID.randomUUID();
        AutomationRule rule = new AutomationRule();
        rule.setId(ruleId);
        rule.setStatut(AutomationRule.Statut.EN_PAUSE);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AutomationRule toggled = automationService.toggleRule(ruleId);

        assertEquals(AutomationRule.Statut.ACTIVE, toggled.getStatut());
    }

    // ── DELETE ─────────────────────────────────────────────────

    @Test
    void deleteRule_shouldDelete() {
        UUID ruleId = UUID.randomUUID();
        AutomationRule rule = new AutomationRule();
        rule.setId(ruleId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        automationService.deleteRule(ruleId);

        verify(ruleRepository).delete(rule);
    }

    // ── TRIGGER ENGINE ─────────────────────────────────────────

    @Test
    void triggerRules_shouldExecuteActiveRules() {
        AutomationRule rule1 = new AutomationRule();
        rule1.setId(UUID.randomUUID());
        rule1.setTenantId(tenantId);
        rule1.setStatut(AutomationRule.Statut.ACTIVE);
        rule1.setActionType(AutomationRule.ActionType.ENVOYER_MESSAGE);
        rule1.setMaxExécutions(0);
        rule1.setTotalExécutions(0);

        when(ruleRepository.findByTenantIdAndTriggerEvent(tenantId, AutomationRule.TriggerEvent.NOUVEAU_MEMBRE))
                .thenReturn(List.of(rule1));
        when(ruleRepository.save(any(AutomationRule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(executionRepository.save(any(AutomationExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.NOUVEAU_MEMBRE, "Membre: Jean Mbarga");

        assertEquals(1, executions.size());
        assertEquals(AutomationExecution.Statut.SUCCÈS, executions.get(0).getStatut());
        assertEquals("Message envoyé aux destinataires", executions.get(0).getRésultat());
        assertEquals(1, rule1.getTotalExécutions());
        assertNotNull(rule1.getDernièreExécution());
    }

    @Test
    void triggerRules_pausedRule_shouldNotExecute() {
        AutomationRule paused = new AutomationRule();
        paused.setId(UUID.randomUUID());
        paused.setTenantId(tenantId);
        paused.setStatut(AutomationRule.Statut.EN_PAUSE);
        paused.setActionType(AutomationRule.ActionType.ENVOYER_MESSAGE);

        when(ruleRepository.findByTenantIdAndTriggerEvent(tenantId, AutomationRule.TriggerEvent.QUOTIDIEN))
                .thenReturn(List.of(paused));

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.QUOTIDIEN, "Contexte test");

        assertTrue(executions.isEmpty());
        verify(executionRepository, never()).save(any());
    }

    @Test
    void triggerRules_maxExecutionsReached_shouldSkip() {
        AutomationRule capped = new AutomationRule();
        capped.setId(UUID.randomUUID());
        capped.setTenantId(tenantId);
        capped.setStatut(AutomationRule.Statut.ACTIVE);
        capped.setActionType(AutomationRule.ActionType.CRÉER_ALERTE);
        capped.setMaxExécutions(5);
        capped.setTotalExécutions(5);

        when(ruleRepository.findByTenantIdAndTriggerEvent(tenantId, AutomationRule.TriggerEvent.HEBDOMADAIRE))
                .thenReturn(List.of(capped));

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.HEBDOMADAIRE, "Contexte test");

        assertTrue(executions.isEmpty());
    }

    @Test
    void triggerRules_multipleActions_shouldExecuteAll() {
        AutomationRule msgRule = new AutomationRule();
        msgRule.setId(UUID.randomUUID());
        msgRule.setTenantId(tenantId);
        msgRule.setStatut(AutomationRule.Statut.ACTIVE);
        msgRule.setActionType(AutomationRule.ActionType.ENVOYER_MESSAGE);
        msgRule.setMaxExécutions(0);

        AutomationRule alertRule = new AutomationRule();
        alertRule.setId(UUID.randomUUID());
        alertRule.setTenantId(tenantId);
        alertRule.setStatut(AutomationRule.Statut.ACTIVE);
        alertRule.setActionType(AutomationRule.ActionType.CRÉER_ALERTE);
        alertRule.setMaxExécutions(0);

        when(ruleRepository.findByTenantIdAndTriggerEvent(tenantId, AutomationRule.TriggerEvent.RAPPORT_SOUMIS))
                .thenReturn(List.of(msgRule, alertRule));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<AutomationExecution> executions = automationService.triggerRules(
                AutomationRule.TriggerEvent.RAPPORT_SOUMIS, "Rapport de Test");

        assertEquals(2, executions.size());
    }

    // ── EXECUTION HISTORY ──────────────────────────────────────

    @Test
    void listExecutions_shouldReturnHistory() {
        UUID ruleId = UUID.randomUUID();
        AutomationExecution exec = new AutomationExecution();
        exec.setId(UUID.randomUUID());
        when(executionRepository.findByRuleIdOrderByExécutéLeDesc(ruleId))
                .thenReturn(List.of(exec));

        List<AutomationExecution> result = automationService.listExecutions(ruleId);

        assertEquals(1, result.size());
    }

    // ── STATS ──────────────────────────────────────────────────

    @Test
    void getStats_shouldReturnCounts() {
        when(ruleRepository.countByTenantIdAndStatut(tenantId, AutomationRule.Statut.ACTIVE)).thenReturn(5L);
        when(ruleRepository.countByTenantIdAndStatut(tenantId, AutomationRule.Statut.EN_PAUSE)).thenReturn(2L);

        Map<String, Object> stats = automationService.getStats();

        assertEquals(5L, stats.get("totalRègles"));
        assertEquals(2L, stats.get("règlesEnPause"));
        assertEquals(AutomationRule.TriggerEvent.values().length, stats.get("triggersDisponibles"));
        assertEquals(AutomationRule.ActionType.values().length, stats.get("actionsDisponibles"));
    }
}
