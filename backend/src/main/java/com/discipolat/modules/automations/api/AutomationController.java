package com.discipolat.modules.automations.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.automations.domain.AutomationExecution;
import com.discipolat.modules.automations.domain.AutomationRule;
import com.discipolat.modules.automations.domain.AutomationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/automations")
public class AutomationController {

    private final AutomationService service;

    public AutomationController(AutomationService service) {
        this.service = service;
    }

    // ── Règles ────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<PageResponse<AutomationRule>> listRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AutomationRule> rules = service.listRules(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(rules.getContent(), page, size,
                rules.getTotalElements(), rules.getTotalPages()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AutomationRule>> listActive() {
        return ResponseEntity.ok(service.listActiveRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutomationRule> getRule(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getRuleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<AutomationRule> createRule(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AutomationRule rule = service.createRule(
                body.get("titre"), body.get("description"),
                body.get("triggerEvent"), body.get("triggerParams"),
                body.get("actionType"), body.get("actionParams"), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<AutomationRule> updateRule(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateRule(id, body));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<AutomationRule> toggleRule(@PathVariable UUID id) {
        return ResponseEntity.ok(service.toggleRule(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        service.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // ── Trigger manuel ────────────────────────────────────────────

    @PostMapping("/trigger/{event}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<AutomationExecution>> trigger(
            @PathVariable String event,
            @RequestBody(required = false) Map<String, String> body) {
        String contexte = body != null ? body.getOrDefault("contexte", "{}") : "{}";
        List<AutomationExecution> executions = service.triggerRules(
                AutomationRule.TriggerEvent.valueOf(event), contexte);
        return ResponseEntity.ok(executions);
    }

    // ── Historique ────────────────────────────────────────────────

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<AutomationExecution>> listExecutions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listExecutions(id));
    }

    // ── Stats ─────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats());
    }

    // ── Enums dispo ───────────────────────────────────────────────

    @GetMapping("/triggers")
    public ResponseEntity<Map<String, String>> listTriggers() {
        Map<String, String> triggers = new java.util.LinkedHashMap<>();
        for (AutomationRule.TriggerEvent e : AutomationRule.TriggerEvent.values()) {
            triggers.put(e.name(), switch (e) {
                case ABSENCE_SOUTENUE -> "Membre absent X semaines consécutives";
                case NOUVEAU_MEMBRE -> "Nouveau membre rejoint l'église";
                case RAPPORT_SOUMIS -> "Rapport faiseur soumis";
                case RAPPORT_EN_RETARD -> "Rapport non soumis avant la date limite";
                case PRIÈRE_CRÉÉE -> "Nouvelle prière créée";
                case ÉVÉNEMENT_CRÉÉ -> "Nouvel événement créé";
                case DEMANDE_TRANSFERT -> "Demande de transfert soumise";
                case ALERT_CRÉÉE -> "Nouvelle alerte créée";
                case SCORE_SPRITUEL_BAISSE -> "Score spirituel en baisse significative";
                case QUOTIDIEN -> "Déclenché chaque jour";
                case HEBDOMADAIRE -> "Déclenché chaque semaine";
                case PERSONNALISÉ -> "Déclenché manuellement";
            });
        }
        return ResponseEntity.ok(triggers);
    }

    @GetMapping("/actions")
    public ResponseEntity<Map<String, String>> listActions() {
        Map<String, String> actions = new java.util.LinkedHashMap<>();
        for (AutomationRule.ActionType a : AutomationRule.ActionType.values()) {
            actions.put(a.name(), switch (a) {
                case ENVOYER_MESSAGE -> "Envoyer une notification in-app";
                case ENVOYER_EMAIL -> "Envoyer un email";
                case ASSIGNER_FISEUR -> "Assigner automatiquement un faiseur";
                case CRÉER_ALERTE -> "Créer une alerte";
                case CRÉER_SUIVI -> "Créer un suivi";
                case METTRE_A_JOUR_STATUT -> "Mettre à jour un statut";
                case GÉNÉRER_RAPPORT -> "Générer un rapport";
                case NOTIFIER_ROLE -> "Notifier un rôle spécifique";
            });
        }
        return ResponseEntity.ok(actions);
    }
}
