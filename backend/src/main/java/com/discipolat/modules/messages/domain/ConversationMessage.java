package com.discipolat.modules.messages.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "conversation_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    /** For group messages — null if 1:1 conversation */
    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "content")
    private String content;

    /** TEXT, VOICE, IMAGE, FILE, SYSTEM */
    @Column(name = "message_type")
    @Builder.Default
    private String messageType = "TEXT";

    /** URL for voice/image/file attachments */
    @Column(name = "media_url")
    private String mediaUrl;

    /** Duration in seconds for voice messages */
    @Column(name = "media_duration")
    private Integer mediaDuration;

    /** ID of the message this is replying to */
    @Column(name = "reply_to_id")
    private UUID replyToId;

    /** Sender name of the replied message (denormalized for display) */
    @Column(name = "reply_to_sender_name")
    private String replyToSenderName;

    /** Content preview of the replied message */
    @Column(name = "reply_to_content")
    private String replyToContent;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Check if this is a voice message */
    public boolean isVoiceMessage() {
        return "VOICE".equals(messageType);
    }

    /** Check if this is a reply */
    public boolean isReply() {
        return replyToId != null;
    }
}
