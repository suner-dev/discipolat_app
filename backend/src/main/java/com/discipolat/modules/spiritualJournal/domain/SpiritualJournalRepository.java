package com.discipolat.modules.spiritualJournal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SpiritualJournalRepository extends JpaRepository<SpiritualJournal, UUID> {
    List<SpiritualJournal> findByAuteurIdOrderByDateEntreeDesc(UUID auteurId);
    List<SpiritualJournal> findByAuteurIdAndTypeEntreeOrderByDateEntreeDesc(UUID auteurId, SpiritualJournal.TypeEntree type);
    List<SpiritualJournal> findByAuteurIdAndFavoriTrueOrderByDateEntreeDesc(UUID auteurId);
    List<SpiritualJournal> findByTenantIdAndPubliqueTrueOrderByDateEntreeDesc(UUID tenantId);
    long countByAuteurId(UUID auteurId);
}
