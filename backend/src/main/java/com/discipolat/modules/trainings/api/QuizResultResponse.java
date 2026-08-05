package com.discipolat.modules.trainings.api;

public record QuizResultResponse(
        int score,          // 0-100
        int bonnesReponses,
        int totalQuestions,
        boolean reussi,
        boolean certificat
) {
}
