package com.discipolat.modules.messages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupConversationRepository extends JpaRepository<GroupConversation, UUID> {
    List<GroupConversation> findByTenantIdAndIsArchivedFalseOrderByLastMessageAtDesc(UUID tenantId);
    List<GroupConversation> findByTenantIdAndGroupTypeAndIsArchivedFalse(UUID tenantId, String groupType);
    Optional<GroupConversation> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT gc FROM GroupConversation gc JOIN GroupConversationMember gcm ON gc.id = gcm.groupId WHERE gcm.userId = :userId AND gc.isArchived = false ORDER BY gc.lastMessageAt DESC")
    List<GroupConversation> findByMemberUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(gc) FROM GroupConversation gc WHERE gc.tenantId = :tenantId AND gc.isArchived = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
