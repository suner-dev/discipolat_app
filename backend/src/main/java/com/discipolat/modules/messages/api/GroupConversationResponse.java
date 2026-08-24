package com.discipolat.modules.messages.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GroupConversationResponse {
    private UUID id;
    private String name;
    private String description;
    private String groupType;
    private UUID createdBy;
    private String avatarUrl;
    private int memberCount;
    private int unreadCount;
    private boolean isMember;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
