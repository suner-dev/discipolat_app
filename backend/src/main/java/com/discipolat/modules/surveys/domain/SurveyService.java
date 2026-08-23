package com.discipolat.modules.surveys.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;

    public SurveyService(SurveyRepository surveyRepository, SurveyResponseRepository responseRepository) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
    }

    public Page<Survey> list(Pageable pageable, String statut) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (statut != null) {
            return surveyRepository.findByTenantIdAndStatut(tenantId, Survey.Statut.valueOf(statut), pageable);
        }
        return surveyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public Survey getById(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Survey", id));
    }

    public Survey create(String titre, String description, String type, java.util.List<String> options,
                         boolean anonyme, LocalDateTime expiresAt, UUID userId) {
        Survey survey = new Survey();
        survey.setTenantId(TenantContext.getCurrentTenantId());
        survey.setTitre(titre);
        survey.setDescription(description);
        survey.setType(Survey.Type.valueOf(type));
        survey.setOptions(options);
        survey.setAnonyme(anonyme);
        survey.setExpiresAt(expiresAt);
        survey.setCreePar(userId);
        return surveyRepository.save(survey);
    }

    public Survey updateStatut(UUID id, String statut) {
        Survey survey = getById(id);
        survey.setStatut(Survey.Statut.valueOf(statut));
        survey.setUpdatedAt(LocalDateTime.now());
        return surveyRepository.save(survey);
    }

    public SurveyResponse submitResponse(UUID surveyId, java.util.List<String> selections, String reponse, UUID userId) {
        Survey survey = getById(surveyId);
        if (survey.getStatut() != Survey.Statut.ACTIF) {
            throw new IllegalStateException("Le sondage n'est pas actif");
        }
        SurveyResponse response = new SurveyResponse();
        response.setSurvey(survey);
        response.setAuteurId(userId);
        response.setSelections(selections);
        response.setReponse(reponse);
        responseRepository.save(response);
        survey.setTotalReponses(survey.getTotalReponses() + 1);
        surveyRepository.save(survey);
        return response;
    }

    public Map<String, Object> getResults(UUID surveyId) {
        Survey survey = getById(surveyId);
        Map<String, Object> results = new HashMap<>();
        java.util.List<SurveyResponse> responses = responseRepository.findBySurveyId(surveyId);

        if (survey.getType() == Survey.Type.CHOIX_UNIQUE || survey.getType() == Survey.Type.CHOIX_MULTIPLE) {
            Map<String, Long> counts = new java.util.HashMap<>();
            for (String option : survey.getOptions()) {
                counts.put(option, 0L);
            }
            for (SurveyResponse resp : responses) {
                for (String sel : resp.getSelections()) {
                    counts.merge(sel, 1L, Long::sum);
                }
            }
            results.put("results", counts);
        }
        results.put("totalReponses", responses.size());
        results.put("survey", survey);
        return results;
    }
}
