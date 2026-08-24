package com.discipolat.modules.surveys.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "survey_responses")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    private UUID auteurId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "survey_response_selections", joinColumns = @JoinColumn(name = "response_id"))
    @Column(name = "selection")
    private List<String> selections = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String reponse;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Survey getSurvey() { return survey; }
    public void setSurvey(Survey survey) { this.survey = survey; }
    public UUID getAuteurId() { return auteurId; }
    public void setAuteurId(UUID auteurId) { this.auteurId = auteurId; }
    public List<String> getSelections() { return selections; }
    public void setSelections(List<String> selections) { this.selections = selections; }
    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
