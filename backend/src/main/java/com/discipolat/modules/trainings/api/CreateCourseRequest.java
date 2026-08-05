package com.discipolat.modules.trainings.api;

import com.discipolat.modules.trainings.domain.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCourseRequest(
        @NotBlank String titre,
        String description,
        String categorie,
        @NotNull Course.Niveau niveau,
        Integer dureeMinutes,
        UUID formateurId,
        String imageUrl
) {
}
