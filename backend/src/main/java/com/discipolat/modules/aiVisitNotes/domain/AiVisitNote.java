package com.discipolat.modules.aiVisitNotes.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_visit_notes")
public class AiVisitNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    private UUID visitId;

    private UUID memberId;

    private UUID pastorId;

    @Column(columnDefinition = "TEXT")
    private String rawTranscription;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(columnDefinition = "TEXT")
    private String aiActionItems; // JSON array of action items

    @Column(columnDefinition = "TEXT")
    private String aiSentiment; // POSITIVE, NEUTRAL, CONCERNING, CRITICAL

    private Boolean isVerified = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getVisitId() { return visitId; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
    public UUID getPastorId() { return pastorId; }
    public void setPastorId(UUID pastorId) { this.pastorId = pastorId; }
    public String getRawTranscription() { return rawTranscription; }
    public void setRawTranscription(String rawTranscription) { this.rawTranscription = rawTranscription; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getAiActionItems() { return aiActionItems; }
    public void setAiActionItems(String aiActionItems) { this.aiActionItems = aiActionItems; }
    public String getAiSentiment() { return aiSentiment; }
    public void setAiSentiment(String aiSentiment) { this.aiSentiment = aiSentiment; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
