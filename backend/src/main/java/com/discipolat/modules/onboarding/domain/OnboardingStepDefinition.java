package com.discipolat.modules.onboarding.domain;

import com.discipolat.modules.onboarding.domain.OnboardingWizardStep.StepType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Définition canonique et immuable des 7 étapes du wizard (contrat §3.1).
 *
 * <p>Source de vérité unique pour l'ordre, les titres français et la
 * skippabilité. Décision D5 : les titres sont renvoyés en français par l'API, le
 * web peut les remplacer par ses clés i18n {@code onboarding.step.<STEP_TYPE>.title}
 * — d'où une source unique et non deux listes divergentes.
 *
 * <p><b>L'ordre est figé</b> : le tableau ci-dessous est celui du contrat, et il
 * est aussi l'ordre d'initialisation effectif des étapes. Toute étape ne peut
 * être complétée ou sautée que si <b>toutes</b> les précédentes sont
 * {@code COMPLETED} ou {@code SKIPPED} (sinon {@code 409 STEP_ORDER_VIOLATION}).
 */
public record OnboardingStepDefinition(
        StepType stepType,
        int stepOrder,
        String title,
        String description,
        boolean isSkippable,
        boolean skipRequiresReason) {

    /** Les 7 définitions, dans l'ordre canonique du contrat. */
    public static final List<OnboardingStepDefinition> CANONICAL_ORDER = List.of(
            new OnboardingStepDefinition(StepType.CHURCH_IDENTITY, 0,
                    "Identité de l'église",
                    "Nom, logo, devise et informations de contact.",
                    false, false),
            new OnboardingStepDefinition(StepType.MEMBER_IMPORT, 1,
                    "Import des membres",
                    "Déclarez le nombre de membres que vous allez importer.",
                    true, true),
            new OnboardingStepDefinition(StepType.STRUCTURE, 2,
                    "Familles et départements",
                    "Créez les familles et les départements de votre église.",
                    false, false),
            new OnboardingStepDefinition(StepType.ROLES, 3,
                    "Inviter les responsables",
                    "Invitez les responsables de votre église et leur attribuez un rôle.",
                    true, true),
            new OnboardingStepDefinition(StepType.BRANDING, 4,
                    "Identité visuelle",
                    "Couleurs, logo et thème sombre de votre église.",
                    true, false),
            new OnboardingStepDefinition(StepType.MODULES, 5,
                    "Modules activés",
                    "Choisissez les modules dont votre église a besoin.",
                    true, false),
            new OnboardingStepDefinition(StepType.FIRST_EVENT, 6,
                    "Premier événement",
                    "Planifiez votre première rencontre pour lancer l'activité.",
                    false, false));

    private static final Map<StepType, OnboardingStepDefinition> BY_TYPE = buildIndex();

    private static Map<StepType, OnboardingStepDefinition> buildIndex() {
        Map<StepType, OnboardingStepDefinition> index = new LinkedHashMap<>();
        for (OnboardingStepDefinition definition : CANONICAL_ORDER) {
            index.put(definition.stepType(), definition);
        }
        return Map.copyOf(index);
    }

    /** Nombre total d'étapes du parcours canonique. */
    public static final int TOTAL_STEPS = CANONICAL_ORDER.size();

    public static OnboardingStepDefinition of(StepType stepType) {
        OnboardingStepDefinition definition = BY_TYPE.get(stepType);
        if (definition == null) {
            throw new IllegalArgumentException("Aucune définition pour le type d'étape " + stepType);
        }
        return definition;
    }

    public static Map<StepType, OnboardingStepDefinition> index() {
        return BY_TYPE;
    }
}
