package com.discipolat.modules.groupMessages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {
    List<GroupMessage> findByTenantIdAndGroupIdAndIsDeletedIsFalseOrderByCreatedAtAsc(UUID tenantId, UUID groupId);
    long countByTenantIdAndGroupIdAndIsDeletedIsFalse(UUID tenantId, UUID groupId);

    java.util.List<GroupMessage> findByTenantIdAndGroupIdAndContentContainingIgnoreCaseAndIsDeletedIsFalseOrderByCreatedAtDesc(
            UUID tenantId, UUID groupId, String content);
}