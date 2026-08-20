package com.discipolat.modules.trainings.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SermonTranscriptionRepository extends JpaRepository<SermonTranscription, UUID> {

    Page<SermonTranscription> findByTenantIdOrderByRecordedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT s FROM SermonTranscription s WHERE s.tenantId = :tenantId AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.fullText) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.speaker) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<SermonTranscription> search(UUID tenantId, String q, Pageable pageable);

    long countByTenantId(UUID tenantId);
}
