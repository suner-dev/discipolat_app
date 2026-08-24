package com.discipolat.modules.groupMessages.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {
    List<GroupMessage> findByTenantIdAndGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID tenantId, UUID groupId);
    long countByTenantIdAndGroupIdAndIsDeletedFalse(UUID tenantId, UUID groupId);
}
