package com.discipolat.modules.messages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID senderId);

    @Modifying
    @Query("UPDATE ConversationMessage m SET m.readAt = CURRENT_TIMESTAMP " +
            "WHERE m.conversationId = :conversationId AND m.senderId != :readerId AND m.readAt IS NULL")
    void markAllAsRead(@Param("conversationId") UUID conversationId, @Param("readerId") UUID readerId);
}
