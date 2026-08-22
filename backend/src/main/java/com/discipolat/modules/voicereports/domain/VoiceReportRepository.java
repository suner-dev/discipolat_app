package com.discipolat.modules.voicereports.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VoiceReportRepository extends JpaRepository<VoiceReport, UUID> {
    List<VoiceReport> findTop50ByOrderByCreatedAtDesc();
    List<VoiceReport> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
    List<VoiceReport> findByRelatedSoulIdOrderByCreatedAtDesc(UUID soulId);

    @Query("SELECT COUNT(v) FROM VoiceReport v WHERE v.authorId = :authorId AND v.syncedOffline = true")
    long countOfflineSyncedByAuthor(@Param("authorId") UUID authorId);
}
