package com.discipolat.modules.prayerJournal.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrayerJournalRepository extends JpaRepository<PrayerJournalEntry, UUID> {
    Page<PrayerJournalEntry> findByMembreIdOrderByCreatedAtDesc(UUID membreId, Pageable pageable);
    List<PrayerJournalEntry> findByMembreIdAndStatut(UUID membreId, PrayerJournalEntry.Statut statut);
    long countByTenantIdAndMembreId(UUID tenantId, UUID membreId);
    long countByTenantIdAndMembreIdAndStatut(UUID tenantId, UUID membreId, PrayerJournalEntry.Statut statut);
}
