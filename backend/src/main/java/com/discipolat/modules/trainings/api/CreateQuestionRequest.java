package com.discipolat.modules.trainings.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionRequest(
        @NotBlank String question,
        @NotBlank String propositions,
        @NotNull Integer reponseIndex,
        int ordre
) {
}
