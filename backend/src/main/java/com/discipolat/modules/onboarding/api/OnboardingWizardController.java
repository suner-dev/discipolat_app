package com.discipolat.modules.onboarding.api;

import com.discipolat.modules.onboarding.domain.OnboardingWizardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Constat B2 — API du wizard d'onboarding, conforme au contrat figé §3.1.
 *
 * <p><b>RBAC</b> : les lectures exigent {@code isAuthenticated()}, <b>toutes</b>
 * les mutations exigent {@code @authz.isTenantAdmin()}. Avant ce correctif, la
 * classe portait seulement {@code @PreAuthorize("isAuthenticated()")} : n'importe
 * quel membre authentifié — y compris un {@code MEMBRE} — pouvait modifier les
 * étapes du wizard, donc renommer l'église, inviter des utilisateurs, activer
 * des modules et créer un événement.
 *
 * <p><b>Corps facultatif (décision D7)</b> : {@code /complete} accepte un corps
 * absent (équivalent à {@code {}}). Avant, la signature
 * {@code @RequestBody Map<String, String>} rendait l'annotation obligatoire et
 * toute completion sans corps recevait un 400.
 */
@RestController
@RequestMapping("/api/v1/onboarding-wizard")
@PreAuthorize("isAuthenticated()")
public class OnboardingWizardController {

    private final OnboardingWizardService service;

    public OnboardingWizardController(OnboardingWizardService service) {
        this.service = service;
    }

    /** `GET /` — 7 étapes, initialise si vide. */
    @GetMapping
    public ResponseEntity<List<OnboardingStepResponse>> getSteps() {
        return ResponseEntity.ok(service.getSteps());
    }

    /** `GET /progress` */
    @GetMapping("/progress")
    public ResponseEntity<OnboardingProgressResponse> progress() {
        return ResponseEntity.ok(service.getProgress());
    }

    /** `GET /status` — état d'achèvement au niveau du tenant. */
    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusResponse> status() {
        return ResponseEntity.ok(service.getStatus());
    }

    /** `POST /initialize` — idempotent. */
    @PostMapping("/initialize")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<List<OnboardingStepResponse>> initialize() {
        return ResponseEntity.ok(service.initializeSteps());
    }

    /** `POST /{id}/start` */
    @PostMapping("/{id}/start")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<OnboardingStepResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(service.startStep(id));
    }

    /**
     * `POST /{id}/complete` — {@code data} facultative.
     *
     * <p>{@code @RequestBody(required = false)} : un appel sans corps est
     * valide, ce qui rend l'étape «_BRANDING_ sans donnée » et toute étape dont
     * les données ne sont pas requises réellement complétable.
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<OnboardingStepResponse> complete(
            @PathVariable UUID id,
            @RequestBody(required = false) OnboardingStepData body) {
        return ResponseEntity.ok(service.completeStep(id, body == null ? Map.of() : body.dataOrEmpty()));
    }

    /**
     * `POST /{id}/skip` — motif obligatoire si l'étape l'exige.
     *
     * <p>Le corps reste facultatif : l'étape peut être sautable sans motif
     * ({@code skipRequiresReason = false}), auquel cas l'absence de corps est
     * acceptée et le service décide.
     */
    @PostMapping("/{id}/skip")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<OnboardingStepResponse> skip(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(service.skipStep(id, reason));
    }

    // ======================== P3 #116 — ONBOARDING INTERACTIF PAR RÔLE ========================

    /**
     * Checklist de première connexion + tutoriel guidé, adapté au rôle.
     * Contrat §3.1 : endpoint <b>inchangé</b>.
     */
    @GetMapping("/templates/{role}")
    public ResponseEntity<Map<String, Object>> roleTemplate(@PathVariable String role) {
        return ResponseEntity.ok(service.roleTemplate(role.toUpperCase()));
    }
}
