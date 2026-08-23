package com.discipolat.modules.broadcast.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_messages")
public class BroadcastMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "channel")
    @Enumerated(EnumType.STRING)
    private BroadcastChannel channel = BroadcastChannel.ALL;

    @Column(name = "target_roles")
    private String targetRoles;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_count")
    private Integer readCount = 0;

    @Column(name = "total_recipients")
    private Integer totalRecipients = 0;

    public enum BroadcastChannel {
        ALL, PUSH, EMAIL, SMS, IN_APP
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public BroadcastChannel getChannel() { return channel; }
    public void setChannel(BroadcastChannel channel) { this.channel = channel; }
    public String getTargetRoles() { return targetRoles; }
    public void setTargetRoles(String targetRoles) { this.targetRoles = targetRoles; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public Integer getReadCount() { return readCount; }
    public void setReadCount(Integer readCount) { this.readCount = readCount; }
    public Integer getTotalRecipients() { return totalRecipients; }
    public void setTotalRecipients(Integer totalRecipients) { this.totalRecipients = totalRecipients; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
