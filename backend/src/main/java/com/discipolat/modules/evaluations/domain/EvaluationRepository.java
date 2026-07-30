package com.discipolat.modules.evaluations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Page<Evaluation> findByEvalueIdAndCategorie(UUID evalueId, CategorieEvaluation categorie, Pageable pageable);

    List<Evaluation> findByEvalueIdAndCategorie(UUID evalueId, CategorieEvaluation categorie);

    Page<Evaluation> findByCategorie(CategorieEvaluation categorie, Pageable pageable);

    Page<Evaluation> findByEvalueId(UUID evalueId, Pageable pageable);

    Optional<Evaluation> findByEvaluateurIdAndEvalueIdAndCategorie(UUID evaluateurId, UUID evalueId, CategorieEvaluation categorie);

    boolean existsByEvaluateurIdAndEvalueIdAndCategorie(UUID evaluateurId, UUID evalueId, CategorieEvaluation categorie);

    @Query("SELECT AVG(e.note) FROM Evaluation e WHERE e.evalueId = :evalueId AND e.categorie = :categorie")
    Double averageNoteByEvalueAndCategorie(@Param("evalueId") UUID evalueId, @Param("categorie") CategorieEvaluation categorie);

    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.evalueId = :evalueId AND e.categorie = :categorie")
    long countByEvalueAndCategorie(@Param("evalueId") UUID evalueId, @Param("categorie") CategorieEvaluation categorie);
}
