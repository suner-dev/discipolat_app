package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.onboarding.api.OnboardingProgressResponse;
import com.discipolat.modules.onboarding.api.OnboardingStatusResponse;
import com.discipolat.modules.onboarding.api.OnboardingStepResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Constat B2 — wizard d'onboarding d'un tenant, conforme au contrat figé §3.1.
 *
 * <p><b>Ce qui n'allait pas avant ce correctif</b> (constats de l'audit) :
 * <ul>
 *   <li>un {@code orElseThrow} sans argument sur {@code wizardRepo.findById(stepId)}
 *       : aucune vérification du {@code tenantId} → <b>IDOR</b>. Compléter une
 *       étape d'une autre église était possible ;</li>
 *   <li>{@code completeStep} ne faisait que {@code setStatus(COMPLETED)} :
 *       <b>aucun effet métier</b> ;</li>
 *   <li>aucun contrôle d'ordre : les étapes étaient cumulables dans n'importe quel
 *       ordre ;</li>
 *   <li>{@code skipStep} ne demandait aucun motif et ne vérifiait pas la
 *       skippabilité ;</li>
 *   <li>l'<b>entité</b> était renvoyée telle quelle (elle exposait {@code config}
 *       et {@code completedData} sous forme de chaîne JSON brute) ;</li>
 *   <li>{@code complete} exigeait un corps : 400 systématique sans corps ;</li>
 *   <li>aucune isolation par tenant sur les écritures ;</li>
 *   <li>{@code initialize} pouvait créer des doublons en concurrence.</li>
 * </ul>
 *
 * <p>Tous ces points sont traités ici et prouvés par
 * {@code OnboardingWizardServiceTest}, {@code OnboardingWizardControllerTest},
 * {@code OnboardingWizardTenantIsolationTest} et
 * {@code OnboardingWizardInitializeConcurrencyTest}.
 */
@Service
@Transactional
public class OnboardingWizardService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingWizardService.class);

    private final OnboardingWizardRepository wizardRepo;
    private final OnboardingStepActions stepActions;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<TenantOnboardingStatusPort> tenantStatusPort;
    private final ObjectProvider<TenantService> tenantServiceProvider;
    private final SecurityUtils securityUtils;

    public OnboardingWizardService(OnboardingWizardRepository wizardRepo,
                                   OnboardingStepActions stepActions,
                                   ObjectMapper objectMapper,
                                   ObjectProvider<TenantOnboardingStatusPort> tenantStatusPort,
                                   ObjectProvider<TenantService> tenantServiceProvider,
                                   SecurityUtils securityUtils) {
        this.wizardRepo = wizardRepo;
        this.stepActions = stepActions;
        this.objectMapper = objectMapper;
        this.tenantStatusPort = tenantStatusPort;
        this.tenantServiceProvider = tenantServiceProvider;
        this.securityUtils = securityUtils;
    }

    // ==================================================================
    // Lecture
    // ==================================================================

    /** `GET /` — 7 étapes, initialise si la table est vide. Idempotent. */
    @Transactional(readOnly = true)
    public List<OnboardingStepResponse> getSteps() {
        return toResponses(loadOrInitialize());
    }

    /** `GET /progress` */
    @Transactional(readOnly = true)
    public OnboardingProgressResponse getProgress() {
        List<OnboardingWizardStep> steps = loadOrInitialize();
        long completed = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.COMPLETED).count();
        long skipped = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.SKIPPED).count();
        int total = steps.size();
        return new OnboardingProgressResponse(
                total,
                completed,
                skipped,
                percentage(completed + skipped, total),
                total > 0 && completed + skipped >= total,
                toResponses(steps));
    }

    /**
     * `GET /status` — état d'achèvement au niveau du tenant.
     *
     * <p>Alimenté par les colonnes additives {@code tenants.onboarding_completed_at}
     * et {@code tenants.onboarding_completed_by} (V183, décision D2). La lecture
     * du tenant passe par {@link TenantStatusReadPort} pour ne pas créer de
     * dépendance circulaire entre le module onboarding et le module tenants.
     */
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus() {
        UUID tenantId = TenantContext.requireTenantId();
        List<OnboardingWizardStep> steps = loadOrInitialize();
        long completed = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.COMPLETED).count();
        long skipped = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.SKIPPED).count();
        int total = steps.size();

        TenantOnboardingStatusPort port = tenantStatusPort.getIfAvailable();
        // Port absent = pas encore de colonne d'achèvement (avant la migration V183) :
        // l'onboarding n'est alors pas déclaré terminé, ce qui est le fail-closed
        // correct (le portail web affichera « En configuration »).
        java.time.Instant completedAt = port == null ? null : port.completedAtOf(tenantId);
        UUID completedBy = port == null ? null : port.completedByOf(tenantId);

        return new OnboardingStatusResponse(
                completedAt != null,
                completedAt,
                completedBy == null ? null : completedBy.toString(),
                total,
                completed,
                skipped,
                percentage(completed + skipped, total));
    }

    // ======================== TEMPLATES PAR ROLE (contrat §3.1 : inchangé) ========================

    /**
     * Checklist de première connexion adaptée au rôle, servie par
     * {@code GET /onboarding-wizard/templates/{role}}. Le contrat §3.1 impose que
     * cet endpoint reste inchangé : il est donc conservé à l'identique.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> roleTemplate(String role) {
        List<Map<String, Object>> steps = switch (role) {
            case "PASTEUR", "ADMIN" -> List.of(
                    step("identity", "Configurer l'identité de l'église", "Nom, logo, devise et informations de contact.", "/admin/settings", "Ces informations apparaîtront sur tous les rapports et le portail public."),
                    step("structure", "Créer les familles et départements", "Structurez votre église en familles (cellules) et départements.", "/families", "Commencez par 3 à 5 familles pilotes avant d'étendre."),
                    step("team", "Inviter les responsables", "Ajoutez chefs de famille, responsables et faiseurs.", "/users", "Chaque responsable recevra un email d'invitation avec son rôle."),
                    step("souls", "Importer les âmes", "Importez vos membres depuis Excel/CSV avec l'assistant de migration.", "/data-migration", "Le mapping des colonnes est proposé automatiquement par l'IA."),
                    step("modules", "Activer les modules utiles", "Choisissez les modules adaptés à votre église.", "/platform/modules", "Vous pourrez désactiver un module plus tard sans perte de données."));
            case "CHEF_DE_FAMILLE" -> List.of(
                    step("family", "Découvrir ma famille", "Visualisez les membres de votre famille spirituelle.", "/my-team", "Envoyez des encouragements pour renforcer les liens."),
                    step("followup", "Suivre mes disciples", "Consultez les âmes qui vous sont assignées.", "/souls", "Les alertes intelligentes signalent les décrochements."),
                    step("events", "Planifier une rencontre", "Organisez la prochaine rencontre de famille.", "/events", "Les membres inscrits recevront une notification automatique."),
                    step("reports", "Comprendre mon tableau de bord", "Indicateurs clés de votre famille.", "/dashboard", "Le score de cohésion se met à jour chaque semaine."));
            case "FAISEUR" -> List.of(
                    step("souls", "Mes disciples", "Voir les âmes que j'accompagne.", "/souls", "Notez chaque contact dans le journal pour un suivi de qualité."),
                    step("interactions", "Enregistrer un contact", "Appel, visite, prière : tracez vos interactions.", "/visits", "Une interaction enregistrée = 5 points d'engagement."),
                    step("challenges", "Rejoindre un défi hebdo", "Défis hebdomadaires et récompenses.", "/weekly-challenges", "Terminez 3 défis pour débloquer un certificat."),
                    step("prayers", "Journal de prière", "Notez vos sujets de prière pour vos disciples.", "/prayer-journal", "Vos disciples ne voient jamais le contenu de vos prières."));
            default -> List.of(
                    step("profile", "Compléter mon profil", "Photo, contacts et préférences.", "/profile", "Un profil complet facilite le contact avec votre famille."),
                    step("family", "Ma famille spirituelle", "Rencontrez les membres de votre famille.", "/my-team", "Envoyez un encouragement à un membre cette semaine !"),
                    step("events", "Mes événements", "Calendrier personnel et RSVP.", "/upcoming-events", "Répondez 'J'y vais' pour aider les organisateurs."),
                    step("surveys", "Participer aux sondages", "Donnez votre avis sur la vie de l'église.", "/surveys", "Les résultats globaux sont visibles après votre vote."),
                    step("followup", "Demander un accompagnement", "Besoin d'un faiseur ou d'un conseil ?", "/follow-up-requests", "Votre demande reste confidentielle entre vous et l'équipe pastorale."));
        };

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role);
        result.put("totalSteps", steps.size());
        result.put("steps", steps);
        return result;
    }

    private static Map<String, Object> step(String key, String title, String description,
                                            String path, String tooltip) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("title", title);
        m.put("description", description);
        m.put("path", path);
        m.put("tooltip", tooltip);
        return m;
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    /** `POST /initialize` — idempotent. */
    public List<OnboardingStepResponse> initializeSteps() {
        return toResponses(createStepsIfAbsent());
    }

    /**
     * Crée les 7 étapes si aucune n'existe pour le tenant courant.
     *
     * <p>La course entre deux initialisations concurrentes est arbitrée par
     * l'index unique {@code uk_onboarding_step_tenant_type} (V183) : en cas de
     * violation, on relit les étapes existantes au lieu de renvoyer un 500.
     */
    private List<OnboardingWizardStep> createStepsIfAbsent() {
        UUID tenantId = TenantContext.requireTenantId();
        List<OnboardingWizardStep> existing = wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        if (!existing.isEmpty()) {
            return existing;
        }
        try {
            List<OnboardingWizardStep> created = new java.util.ArrayList<>();
            int order = 0;
            for (OnboardingStepDefinition definition : OnboardingStepDefinition.CANONICAL_ORDER) {
                OnboardingWizardStep step = new OnboardingWizardStep();
                step.setTenantId(tenantId);
                step.setStepType(definition.stepType());
                step.setStepOrder(order++);
                step.setStatus(OnboardingWizardStep.Status.PENDING);
                created.add(wizardRepo.save(step));
            }
            return created;
        } catch (DataIntegrityViolationException concurrentInitialization) {
            log.info("Initialisation concurrente du wizard détectée pour le tenant {} : relecture", tenantId);
            return wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        }
    }

    private List<OnboardingWizardStep> loadOrInitialize() {
        UUID tenantId = TenantContext.requireTenantId();
        List<OnboardingWizardStep> steps = wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        return steps.isEmpty() ? createStepsIfAbsent() : steps;
    }

    // ==================================================================
    // Cycle de vie d'une étape
    // ==================================================================

    /**
     * `POST /{id}/start` — {@code PENDING} → {@code IN_PROGRESS}.
     *
     * @throws DomainException 404 {@code STEP_NOT_FOUND} (id inconnu OU autre tenant),
     *                         409 {@code STEP_ORDER_VIOLATION},
     *                         409 {@code STEP_ALREADY_COMPLETED}
     */
    public OnboardingStepResponse startStep(UUID stepId) {
        OnboardingWizardStep step = requireStepOfCurrentTenant(stepId);
        assertPreviousStepsSettled(step);

        if (step.getStatus() == OnboardingWizardStep.Status.COMPLETED
                || step.getStatus() == OnboardingWizardStep.Status.SKIPPED) {
            throw new DomainException(
                    "Cette étape est déjà terminée",
                    HttpStatus.CONFLICT, "STEP_ALREADY_COMPLETED",
                    Map.of("status", step.getStatus().name()));
        }

        step.setStatus(OnboardingWizardStep.Status.IN_PROGRESS);
        if (step.getStartedAt() == null) {
            step.setStartedAt(LocalDateTime.now());
        }
        return toResponse(wizardRepo.save(step));
    }

    /**
     * `POST /{id}/complete` — {@code data} facultative (décision D7).
     *
     * <p>Exécute l'action métier réelle de l'étape (voir
     * {@link OnboardingStepActions}) puis stocke le {@code completedData}
     * normalisé. En cas de donnée invalide, <b>aucune écriture n'a lieu</b>.
     */
    public OnboardingStepResponse completeStep(UUID stepId, Map<String, Object> data) {
        OnboardingWizardStep step = requireStepOfCurrentTenant(stepId);
        assertPreviousStepsSettled(step);

        if (step.getStatus() == OnboardingWizardStep.Status.COMPLETED) {
            throw new DomainException(
                    "Cette étape est déjà complétée",
                    HttpStatus.CONFLICT, "STEP_ALREADY_COMPLETED",
                    Map.of("status", step.getStatus().name()));
        }
        if (step.getStatus() == OnboardingWizardStep.Status.SKIPPED) {
            throw new DomainException(
                    "Cette étape a été sautée",
                    HttpStatus.CONFLICT, "STEP_ALREADY_COMPLETED",
                    Map.of("status", OnboardingWizardStep.Status.SKIPPED.name()));
        }

        UUID tenantId = TenantContext.requireTenantId();
        Map<String, Object> outcome = stepActions.execute(step.getStepType(), tenantId, data);

        step.setStatus(OnboardingWizardStep.Status.COMPLETED);
        step.setCompletedData(writeJson(outcome));
        if (step.getStartedAt() == null) {
            step.setStartedAt(LocalDateTime.now());
        }
        step.setCompletedAt(LocalDateTime.now());
        OnboardingWizardStep saved = wizardRepo.save(step);

        markOnboardingCompletedIfAllStepsSettled(tenantId);
        return toResponse(saved);
    }

    /**
     * `POST /{id}/skip` — {@code PENDING}/{@code IN_PROGRESS} → {@code SKIPPED}.
     *
     * @throws DomainException 400 {@code STEP_SKIP_REASON_REQUIRED},
     *                         404 {@code STEP_NOT_FOUND},
     *                         409 {@code STEP_NOT_SKIPPABLE},
     *                         409 {@code STEP_ALREADY_COMPLETED},
     *                         409 {@code STEP_ORDER_VIOLATION}
     */
    public OnboardingStepResponse skipStep(UUID stepId, String reason) {
        OnboardingWizardStep step = requireStepOfCurrentTenant(stepId);
        assertPreviousStepsSettled(step);

        OnboardingStepDefinition definition = OnboardingStepDefinition.of(step.getStepType());
        if (!definition.isSkippable()) {
            throw new DomainException(
                    "Cette étape ne peut pas être sautée",
                    HttpStatus.CONFLICT, "STEP_NOT_SKIPPABLE",
                    Map.of("stepType", step.getStepType().name()));
        }
        if (step.getStatus() == OnboardingWizardStep.Status.COMPLETED
                || step.getStatus() == OnboardingWizardStep.Status.SKIPPED) {
            throw new DomainException(
                    "Cette étape est déjà terminée",
                    HttpStatus.CONFLICT, "STEP_ALREADY_COMPLETED",
                    Map.of("status", step.getStatus().name()));
        }

        String normalizedReason = reason == null ? null : reason.trim();
        if (definition.skipRequiresReason() && (normalizedReason == null || normalizedReason.isEmpty())) {
            throw new DomainException(
                    "Un motif est obligatoire pour sauter cette étape",
                    HttpStatus.BAD_REQUEST, "STEP_SKIP_REASON_REQUIRED",
                    Map.of("stepType", step.getStepType().name()));
        }

        step.setStatus(OnboardingWizardStep.Status.SKIPPED);
        step.setSkipReason(normalizedReason);
        if (step.getStartedAt() == null) {
            step.setStartedAt(LocalDateTime.now());
        }
        step.setCompletedAt(LocalDateTime.now());
        return toResponse(wizardRepo.save(step));
    }

    // ==================================================================
    // Achèvement global de l'onboarding (décision D2)
    // ==================================================================

    /**
     * Quand les 7 étapes sont {@code COMPLETED} ou {@code SKIPPED}, l'achèvement
     * est materialisé sur le tenant (colonnes additives V183) et audité.
     *
     * <p>Le drapeau n'est posé <b>qu'à la toute fin</b> : un wizard à 6/7 ne
     * marque jamais le tenant. L'appel est idempotent, donc une étape rejouée ne
     * déplace pas la date de fin.
     */
    private void markOnboardingCompletedIfAllStepsSettled(UUID tenantId) {
        List<OnboardingWizardStep> steps = wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        if (steps.isEmpty() || steps.stream().anyMatch(step -> !isCompleted(step))) {
            return;
        }
        TenantService tenantService = tenantServiceProvider.getIfAvailable();
        if (tenantService == null) {
            return;
        }
        tenantService.markOnboardingCompleted(securityUtils.getCurrentUserId());
    }

    // ==================================================================
    // Garde-fous
    // ==================================================================

    /**
     * Récupère une étape en garantissant qu'elle appartient au tenant courant.
     *
     * <p>Protection IDOR explicite : un id d'étape d'un autre tenant renvoie
     * exactement le même {@code 404 STEP_NOT_FOUND} qu'un id inexistant, pour ne
     * pas révéler l'existence de l'étape d'autrui.
     */
    private OnboardingWizardStep requireStepOfCurrentTenant(UUID stepId) {
        UUID tenantId = TenantContext.requireTenantId();
        OnboardingWizardStep step = wizardRepo.findById(stepId)
                .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                .orElseThrow(() -> new DomainException(
                        "Étape d'onboarding introuvable",
                        HttpStatus.NOT_FOUND, "STEP_NOT_FOUND",
                        Map.of("stepId", String.valueOf(stepId))));
        return step;
    }

    /**
     * Une étape ne peut être complétée, démarrée ou sautée que si TOUTES les étapes
     * précédentes sont {@code COMPLETED} ou {@code SKIPPED}.
     */
    private void assertPreviousStepsSettled(OnboardingWizardStep step) {
        UUID tenantId = TenantContext.requireTenantId();
        List<OnboardingWizardStep> siblings = wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        for (OnboardingWizardStep sibling : siblings) {
            if (sibling.getStepOrder() >= step.getStepOrder()) {
                continue;
            }
            if (!isCompleted(sibling)) {
                throw new DomainException(
                        "Complétez d'abord les étapes précédentes",
                        HttpStatus.CONFLICT, "STEP_ORDER_VIOLATION",
                        Map.of(
                                "blockingStepType", sibling.getStepType().name(),
                                "blockingStepOrder", String.valueOf(sibling.getStepOrder()),
                                "currentStepType", step.getStepType().name()));
            }
        }
    }

    private static boolean isCompleted(OnboardingWizardStep step) {
        return step.getStatus() == OnboardingWizardStep.Status.COMPLETED
                || step.getStatus() == OnboardingWizardStep.Status.SKIPPED;
    }

    // ==================================================================
    // Conversion
    // ==================================================================

    private List<OnboardingStepResponse> toResponses(List<OnboardingWizardStep> steps) {
        Map<OnboardingWizardStep.StepType, OnboardingStepDefinition> definitions =
                OnboardingStepDefinition.index();
        return steps.stream()
                .map(step -> toResponse(step, definitions.get(step.getStepType())))
                .toList();
    }

    private OnboardingStepResponse toResponse(OnboardingWizardStep step) {
        return toResponse(step, OnboardingStepDefinition.of(step.getStepType()));
    }

    private OnboardingStepResponse toResponse(OnboardingWizardStep step, OnboardingStepDefinition definition) {
        return OnboardingStepResponse.from(step, definition, objectMapper);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (JsonProcessingException notSerialisable) {
            throw new DomainException(
                    "Le résultat de l'étape n'a pas pu être enregistré",
                    HttpStatus.INTERNAL_SERVER_ERROR, "STEP_DATA_SERIALIZATION_FAILED");
        }
    }

    private static int percentage(long done, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(done * 100.0 / total);
    }

}
