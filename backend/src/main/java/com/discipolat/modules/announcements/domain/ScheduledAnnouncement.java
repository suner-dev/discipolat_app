package com.discipolat.modules.announcements.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_announcements")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ScheduledAnnouncement {

    public enum Status { DRAFT, SCHEDULED, PUBLISHED, EXPIRED, CANCELLED }
    public enum Target { ALL, DEPARTMENT, FAMILY, ROLE, SPECIFIC_USERS }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target = Target.ALL;

    private UUID targetId; // departmentId, familyId, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private UUID authorId;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean pinToTop = false;

    private Boolean sendNotification = true;

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getPinToTop() { return pinToTop; }
    public void setPinToTop(Boolean pinToTop) { this.pinToTop = pinToTop; }
    public Boolean getSendNotification() { return sendNotification; }
    public void setSendNotification(Boolean sendNotification) { this.sendNotification = sendNotification; }
}
