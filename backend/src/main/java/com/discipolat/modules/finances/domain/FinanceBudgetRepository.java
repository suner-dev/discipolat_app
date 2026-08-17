package com.discipolat.modules.finances.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinanceBudgetRepository extends JpaRepository<FinanceBudget, UUID> {

    List<FinanceBudget> findByDeletedFalseOrderByAnneeDescCategorieAsc();

    List<FinanceBudget> findByDeletedFalseAndAnneeOrderByCategorieAsc(Integer annee);

    Optional<FinanceBudget> findByDeletedFalseAndAnneeAndCategorie(Integer annee, String categorie);
}
