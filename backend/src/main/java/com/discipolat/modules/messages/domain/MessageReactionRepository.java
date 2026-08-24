package com.discipolat.modules.messages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    List<MessageReaction> findByMessageIdOrderByCreatedAtAsc(UUID messageId);
    Optional<MessageReaction> findByMessageIdAndUserId(UUID messageId, UUID userId);
    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);
    void deleteByMessageIdAndUserId(UUID messageId, UUID userId);
    void deleteByMessageId(UUID messageId);

    @Query("SELECT r.emoji, COUNT(r) as cnt FROM MessageReaction r WHERE r.messageId = :messageId GROUP BY r.emoji")
    List<Object[]> countByEmoji(@Param("messageId") UUID messageId);
}
