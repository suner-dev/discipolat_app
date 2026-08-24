package com.discipolat.modules.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, UUID> {
    List<AiChatConversation> findByUserIdAndSessionIdOrderByCreatedAtAsc(UUID userId, UUID sessionId);
    List<AiChatConversation> findByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteByUserIdAndSessionId(UUID userId, UUID sessionId);
}
