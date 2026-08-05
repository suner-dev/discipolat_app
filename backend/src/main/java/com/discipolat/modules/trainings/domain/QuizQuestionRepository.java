package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
    List<QuizQuestion> findByModuleIdOrderByOrdreAsc(UUID moduleId);
    long countByModuleId(UUID moduleId);
}
