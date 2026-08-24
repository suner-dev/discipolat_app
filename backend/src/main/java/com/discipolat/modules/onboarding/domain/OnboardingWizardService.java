package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OnboardingWizardService {

    private final OnboardingWizardRepository wizardRepo;

    private static final List<OnboardingWizardStep.StepType> DEFAULT_STEPS = List.of(
        OnboardingWizardStep.StepType.CHURCH_IDENTITY,
        OnboardingWizardStep.StepType.MEMBER_IMPORT,
        OnboardingWizardStep.StepType.STRUCTURE,
        OnboardingWizardStep.StepType.ROLES,
        OnboardingWizardStep.StepType.BRANDING,
        OnboardingWizardStep.StepType.MODULES,
        OnboardingWizardStep.StepType.FIRST_EVENT
    );

    public OnboardingWizardService(OnboardingWizardRepository wizardRepo) { this.wizardRepo = wizardRepo; }

    // ======================== P3 #116 — TEMPLATES PAR RÔLE ========================

    /** Checklist de première connexion adaptée au rôle. */
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


    public List<OnboardingWizardStep> getSteps() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var steps = wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        if (steps.isEmpty()) {
            return initializeSteps();
        }
        return steps;
    }

    public List<OnboardingWizardStep> initializeSteps() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<OnboardingWizardStep> steps = new ArrayList<>();
        int order = 0;
        for (OnboardingWizardStep.StepType stepType : DEFAULT_STEPS) {
            OnboardingWizardStep step = new OnboardingWizardStep();
            step.setTenantId(tenantId);
            step.setStepType(stepType);
            step.setStepOrder(order++);
            step.setStatus(OnboardingWizardStep.Status.PENDING);
            steps.add(wizardRepo.save(step));
        }
        return steps;
    }

    public OnboardingWizardStep startStep(UUID stepId) {
        OnboardingWizardStep step = wizardRepo.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.IN_PROGRESS);
        step.setStartedAt(LocalDateTime.now());
        return wizardRepo.save(step);
    }

    public OnboardingWizardStep completeStep(UUID stepId, String data) {
        OnboardingWizardStep step = wizardRepo.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.COMPLETED);
        step.setCompletedData(data);
        step.setCompletedAt(LocalDateTime.now());
        return wizardRepo.save(step);
    }

    public OnboardingWizardStep skipStep(UUID stepId) {
        OnboardingWizardStep step = wizardRepo.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.SKIPPED);
        return wizardRepo.save(step);
    }

    public Map<String, Object> getProgress() {
        List<OnboardingWizardStep> steps = getSteps();
        long completed = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.COMPLETED).count();
        long skipped = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.SKIPPED).count();
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalSteps", steps.size());
        progress.put("completedSteps", completed);
        progress.put("skippedSteps", skipped);
        progress.put("percentage", steps.isEmpty() ? 0 : Math.round((completed + skipped) * 100.0 / steps.size()));
        progress.put("isComplete", completed + skipped >= steps.size());
        progress.put("steps", steps);
        return progress;
    }
}
