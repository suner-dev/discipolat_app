package com.discipolat.modules.messages.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "group_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class GroupConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "group_type", nullable = false)
    @Builder.Default
    private String groupType = "TEAM"; // TEAM, DEPARTMENT, FAMILY, EVENT, CUSTOM

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_message_sender_id")
    private UUID lastMessageSenderId;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
