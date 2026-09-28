package com.discipolat.modules.onboarding.api;

import java.util.List;

/**
 * Contrat figé §3.1 — progression globale du wizard d'onboarding.
 *
 * <p>{@code percentage} = arrondi de {@code (completedSteps + skippedSteps) * 100 / totalSteps}.
 * {@code isComplete} = toutes les étapes sont {@code COMPLETED} ou {@code SKIPPED}.
 *
 * @param totalSteps      nombre total d'étapes (7)
 * @param completedSteps  étapes {@code COMPLETED}
 * @param skippedSteps    étapes {@code SKIPPED}
 * @param percentage      progression en pourcentage entier (0-100)
 * @param isComplete      le wizard est-il terminé ?
 * @param steps           détail des étapes
 */
public record OnboardingProgressResponse(
        int totalSteps,
        long completedSteps,
        long skippedSteps,
        int percentage,
        boolean isComplete,
        List<OnboardingStepResponse> steps) {
}
