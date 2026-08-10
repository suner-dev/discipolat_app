package com.discipolat.modules.files.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntityAttachmentRepository extends JpaRepository<EntityAttachment, UUID> {

    List<EntityAttachment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityAttachment.EntityType entityType, UUID entityId);

    void deleteByEntityTypeAndEntityId(EntityAttachment.EntityType entityType, UUID entityId);
}
