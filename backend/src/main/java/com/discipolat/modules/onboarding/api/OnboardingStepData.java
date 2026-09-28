package com.discipolat.modules.onboarding.api;

import java.util.Map;

/**
 * DTO d'entrée {@code POST /onboarding-wizard/{id}/complete}.
 *
 * <p>Décision D7 : le corps est <b>facultatif</b> côté contrôleur
 * ({@code @RequestBody(required = false)}). Avant le correctif, la méthode
 * déclarait {@code @RequestBody Map<String, String>} <b>obligatoire</b> : tout
 * appel sans corps — y compris pour une étape qui n'exige aucune donnée — se
 * faisait rejeter en 400 par Spring, ce qui rendait le wizard inutilisable.
 *
 * <p>{@code data} est un objet libre : sa validation dépend du {@code stepType}
 * et est réalisée par {@code OnboardingStepActions} (sinon
 * {@code 400 STEP_DATA_INVALID} avec le détail des champs fautifs).
 */
public record OnboardingStepData(Map<String, Object> data) {

    /** Corps absent, ou corps {@code {}} : équivalents à {@code data == null}. */
    public Map<String, Object> dataOrEmpty() {
        return data == null ? Map.of() : data;
    }
}
