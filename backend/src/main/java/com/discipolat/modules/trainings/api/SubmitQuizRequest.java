package com.discipolat.modules.trainings.api;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SubmitQuizRequest(
        @NotNull UUID moduleId,
        /** questionId -> index de réponse choisi */
        @NotNull Map<UUID, Integer> reponses
) {
}
