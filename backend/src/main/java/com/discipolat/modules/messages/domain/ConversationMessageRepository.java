package com.discipolat.modules.messages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<ConversationMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    List<ConversationMessage> findByGroupIdOrderByCreatedAtAsc(UUID groupId);

    List<ConversationMessage> findByReplyToIdOrderByCreatedAtAsc(UUID replyToId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID senderId);

    @Query(value = "SELECT COUNT(*) FROM conversation_messages " +
            "WHERE tenant_id = :tenantId AND created_at >= :from AND created_at < :to AND is_deleted = FALSE", nativeQuery = true)
    long countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Modifying
    @Query("UPDATE ConversationMessage m SET m.readAt = CURRENT_TIMESTAMP " +
            "WHERE m.conversationId = :conversationId AND m.senderId != :readerId AND m.readAt IS NULL")
    void markAllAsRead(@Param("conversationId") UUID conversationId, @Param("readerId") UUID readerId);
}
