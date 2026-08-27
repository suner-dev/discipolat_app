package com.discipolat.modules.bibleReading.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BibleReadingEntryRepository extends JpaRepository<BibleReadingEntry, UUID> {
    List<BibleReadingEntry> findByPlanIdAndUtilisateurIdOrderByDateLectureDesc(UUID planId, UUID utilisateurId);
    List<BibleReadingEntry> findByUtilisateurIdOrderByDateLectureDesc(UUID utilisateurId);
    List<BibleReadingEntry> findByUtilisateurIdAndLuOrderByDateLectureDesc(UUID utilisateurId, boolean lu);
    List<BibleReadingEntry> findByUtilisateurIdAndDateLectureOrderByDateLectureDesc(UUID utilisateurId, LocalDate date);
    long countByPlanIdAndUtilisateurIdAndLuTrue(UUID planId, UUID utilisateurId);
    List<BibleReadingEntry> findByUtilisateurIdAndDateLectureAfterOrderByDateLectureAsc(UUID utilisateurId, LocalDate date);
}
