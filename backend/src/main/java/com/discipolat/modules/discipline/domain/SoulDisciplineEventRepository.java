package com.discipolat.modules.discipline.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SoulDisciplineEventRepository extends JpaRepository<SoulDisciplineEvent, UUID> {

    List<SoulDisciplineEvent> findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(UUID ameId);

    Page<SoulDisciplineEvent> findByAmeIdAndDeletedFalse(UUID ameId, Pageable pageable);

    Page<SoulDisciplineEvent> findByAmeIdAndCategorieAndDeletedFalse(UUID ameId, CategorieDiscipline categorie, Pageable pageable);

    List<SoulDisciplineEvent> findByAmeIdAndResoluFalseAndDeletedFalse(UUID ameId);

    long countByAmeIdAndDeletedFalse(UUID ameId);

    long countByAmeIdAndResoluFalseAndDeletedFalse(UUID ameId);

    long countByAmeIdAndCategorieAndDeletedFalse(UUID ameId, CategorieDiscipline categorie);

    List<SoulDisciplineEvent> findByAmeIdAndDateEvenementBetweenAndDeletedFalse(
            UUID ameId, LocalDate start, LocalDate end);
}
