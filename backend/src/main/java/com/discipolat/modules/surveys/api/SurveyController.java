package com.discipolat.modules.surveys.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.surveys.domain.Survey;
import com.discipolat.modules.surveys.domain.SurveyResponse;
import com.discipolat.modules.surveys.domain.SurveyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<Survey>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut) {
        Page<Survey> surveys = surveyService.list(PageRequest.of(page, size), statut);
        return ResponseEntity.ok(PageResponse.of(surveys.getContent(), page, size,
                surveys.getTotalElements(), surveys.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Survey> get(@PathVariable UUID id) {
        return ResponseEntity.ok(surveyService.getById(id));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<Map<String, Object>> results(@PathVariable UUID id) {
        return ResponseEntity.ok(surveyService.getResults(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Survey> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        java.util.List<String> options = (java.util.List<String>) body.getOrDefault("options", java.util.List.of());
        String expiresAtStr = (String) body.getOrDefault("expiresAt", null);
        LocalDateTime expiresAt = expiresAtStr != null && !expiresAtStr.isEmpty()
                ? LocalDateTime.parse(expiresAtStr) : null;

        Survey survey = surveyService.create(
                (String) body.get("titre"),
                (String) body.getOrDefault("description", ""),
                (String) body.getOrDefault("type", "CHOIX_UNIQUE"),
                options,
                (Boolean) body.getOrDefault("anonyme", true),
                expiresAt,
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(survey);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Survey> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(surveyService.updateStatut(id, body.get("statut")));
    }

    @PostMapping("/{id}/responses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurveyResponse> submitResponse(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        java.util.List<String> selections = (java.util.List<String>) body.getOrDefault("selections", java.util.List.of());
        String reponse = (String) body.get("reponse");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(surveyService.submitResponse(id, selections, reponse, userId));
    }
}
