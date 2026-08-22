package com.discipolat.modules.surveys.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {
    List<SurveyResponse> findBySurveyId(UUID surveyId);
    long countBySurveyId(UUID surveyId);
    boolean existsBySurveyIdAndAuteurId(UUID surveyId, UUID auteurId);
}
