package com.discipolat.modules.evaluations.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SubmitEvaluationRequest(
    @NotNull(message = "L'ID de l'évalué est requis")
    UUID evalueId,

    String categorie,

    @NotNull(message = "La note est requise")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    Integer note,

    String commentaire
) {}
