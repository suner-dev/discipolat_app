package com.discipolat.modules.trainings.api;

import com.discipolat.modules.trainings.domain.QuizQuestion;

import java.util.UUID;

/**
 * Question de quiz exposée à l'apprenant : la bonne réponse (reponseIndex)
 * n'est jamais envoyée au client — elle reste connue du serveur uniquement.
 */
public record QuizQuestionResponse(
        UUID id,
        UUID moduleId,
        String question,
        String propositions,
        int ordre
) {
    public static QuizQuestionResponse from(QuizQuestion q) {
        return new QuizQuestionResponse(q.getId(), q.getModuleId(), q.getQuestion(),
                q.getPropositions(), q.getOrdre());
    }
}
