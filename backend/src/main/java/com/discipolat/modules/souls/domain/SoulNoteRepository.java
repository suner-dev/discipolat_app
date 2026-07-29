package com.discipolat.modules.souls.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoulNoteRepository extends JpaRepository<SoulNote, UUID> {
    List<SoulNote> findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(UUID ameId);
    Page<SoulNote> findByAuteurIdAndDeletedFalse(UUID auteurId, Pageable pageable);
    long countByAmeIdAndDeletedFalse(UUID ameId);
}
