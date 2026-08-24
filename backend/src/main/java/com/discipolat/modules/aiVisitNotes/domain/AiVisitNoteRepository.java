package com.discipolat.modules.aiVisitNotes.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiVisitNoteRepository extends JpaRepository<AiVisitNote, UUID> {
    List<AiVisitNote> findByTenantIdAndMemberIdOrderByCreatedAtDesc(UUID tenantId, UUID memberId);
    List<AiVisitNote> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<AiVisitNote> findByTenantIdAndPastorIdOrderByCreatedAtDesc(UUID tenantId, UUID pastorId);
}
