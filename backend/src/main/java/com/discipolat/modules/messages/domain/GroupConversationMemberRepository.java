package com.discipolat.modules.messages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupConversationMemberRepository extends JpaRepository<GroupConversationMember, UUID> {
    List<GroupConversationMember> findByGroupIdOrderByJoinedAtAsc(UUID groupId);
    List<GroupConversationMember> findByUserId(UUID userId);
    Optional<GroupConversationMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    long countByGroupId(UUID groupId);

    @Modifying
    @Query("UPDATE GroupConversationMember m SET m.unreadCount = m.unreadCount + 1 WHERE m.groupId = :groupId AND m.userId != :senderId")
    void incrementUnreadCount(@Param("groupId") UUID groupId, @Param("senderId") UUID senderId);

    @Modifying
    @Query("UPDATE GroupConversationMember m SET m.unreadCount = 0, m.lastReadAt = :now WHERE m.groupId = :groupId AND m.userId = :userId")
    void markAsRead(@Param("groupId") UUID groupId, @Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
