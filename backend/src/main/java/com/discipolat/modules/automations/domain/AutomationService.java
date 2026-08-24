package com.discipolat.modules.automations.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AutomationService {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationExecutionRepository executionRepository;

    public AutomationService(AutomationRuleRepository ruleRepository, AutomationExecutionRepository executionRepository) {
        this.ruleRepository = ruleRepository;
        this.executionRepository = executionRepository;
    }

    // ── CRUD Règles ──────────────────────────────────────────────

    public Page<AutomationRule> listRules(Pageable pageable) {
        return ruleRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    public List<AutomationRule> listActiveRules() {
        return ruleRepository.findByTenantIdAndStatutOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), AutomationRule.Statut.ACTIVE);
    }

    public AutomationRule getRuleById(UUID id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AutomationRule", id));
    }

    public AutomationRule createRule(String titre, String description, String triggerEvent,
                                      String triggerParams, String actionType, String actionParams, UUID userId) {
        AutomationRule rule = new AutomationRule();
        rule.setTenantId(TenantContext.getCurrentTenantId());
        rule.setTitre(titre);
        rule.setDescription(description);
        rule.setTriggerEvent(AutomationRule.TriggerEvent.valueOf(triggerEvent));
        rule.setTriggerParams(triggerParams);
        rule.setActionType(AutomationRule.ActionType.valueOf(actionType));
        rule.setActionParams(actionParams);
        rule.setCrééPar(userId);
        return ruleRepository.save(rule);
    }

    public AutomationRule updateRule(UUID id, Map<String, String> updates) {
        AutomationRule rule = getRuleById(id);
        if (updates.containsKey("titre")) rule.setTitre(updates.get("titre"));
        if (updates.containsKey("description")) rule.setDescription(updates.get("description"));
        if (updates.containsKey("triggerParams")) rule.setTriggerParams(updates.get("triggerParams"));
        if (updates.containsKey("actionParams")) rule.setActionParams(updates.get("actionParams"));
        rule.setUpdatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    public AutomationRule toggleRule(UUID id) {
        AutomationRule rule = getRuleById(id);
        rule.setStatut(rule.getStatut() == AutomationRule.Statut.ACTIVE
                ? AutomationRule.Statut.EN_PAUSE
                : AutomationRule.Statut.ACTIVE);
        rule.setUpdatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    public void deleteRule(UUID id) {
        ruleRepository.delete(getRuleById(id));
    }

    // ── Moteur d'exécution ───────────────────────────────────────

    /**
     * Déclenche l'exécution de toutes les règles actives pour un événement donné.
     * En production, cet appel serait fait par un event listener ou un scheduler.
     */
    public List<AutomationExecution> triggerRules(AutomationRule.TriggerEvent event, String contexte) {
        List<AutomationRule> activeRules = ruleRepository.findByTenantIdAndTriggerEvent(
                TenantContext.getCurrentTenantId(), event);

        return activeRules.stream()
                .filter(rule -> rule.getStatut() == AutomationRule.Statut.ACTIVE)
                .filter(rule -> rule.getMaxExécutions() == 0 || rule.getTotalExécutions() < rule.getMaxExécutions())
                .map(rule -> executeRule(rule, contexte))
                .toList();
    }

    private AutomationExecution executeRule(AutomationRule rule, String contexte) {
        AutomationExecution execution = new AutomationExecution();
        execution.setRule(rule);
        execution.setTenantId(rule.getTenantId());
        execution.setContexte(contexte);

        try {
            // Simulate execution based on action type
            String résultat = switch (rule.getActionType()) {
                case ENVOYER_MESSAGE -> "Message envoyé aux destinataires";
                case ENVOYER_EMAIL -> "Email envoyé";
                case ASSIGNER_FISEUR -> "Faiseur assigné";
                case CRÉER_ALERTE -> "Alerte créée";
                case CRÉER_SUIVI -> "Suivi créé";
                case METTRE_A_JOUR_STATUT -> "Statut mis à jour";
                case GÉNÉRER_RAPPORT -> "Rapport généré";
                case NOTIFIER_ROLE -> "Rôle notifié";
            };

            execution.setStatut(AutomationExecution.Statut.SUCCÈS);
            execution.setRésultat(résultat);

            // Update rule counters
            rule.setTotalExécutions(rule.getTotalExécutions() + 1);
            rule.setDernièreExécution(LocalDateTime.now());
            ruleRepository.save(rule);
        } catch (Exception e) {
            execution.setStatut(AutomationExecution.Statut.ÉCHEC);
            execution.setRésultat("Erreur: " + e.getMessage());
        }

        return executionRepository.save(execution);
    }

    // ── Historique ────────────────────────────────────────────────

    public List<AutomationExecution> listExecutions(UUID ruleId) {
        return executionRepository.findByRuleIdOrderByExécutéLeDesc(ruleId);
    }

    // ── Statistiques ──────────────────────────────────────────────

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRègles", ruleRepository.countByTenantIdAndStatut(tenantId, AutomationRule.Statut.ACTIVE));
        stats.put("règlesEnPause", ruleRepository.countByTenantIdAndStatut(tenantId, AutomationRule.Statut.EN_PAUSE));
        stats.put("triggersDisponibles", AutomationRule.TriggerEvent.values().length);
        stats.put("actionsDisponibles", AutomationRule.ActionType.values().length);
        return stats;
    }
}
