package com.discipolat.modules.finances.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {

    List<FinanceTransaction> findByDeletedFalseOrderByDateTransactionDesc();

    List<FinanceTransaction> findByDeletedFalseAndDateTransactionBetween(LocalDate debut, LocalDate fin);

    List<FinanceTransaction> findByDeletedFalseAndTypeAndDateTransactionBetween(
            FinanceTransaction.TransactionType type, LocalDate debut, LocalDate fin);

    List<FinanceTransaction> findByDeletedFalseAndCategorieAndDateTransactionBetween(
            String categorie, LocalDate debut, LocalDate fin);

    List<FinanceTransaction> findByDeletedFalseAndTypeAndCategorieAndDateTransactionBetween(
            FinanceTransaction.TransactionType type, String categorie, LocalDate debut, LocalDate fin);

    List<FinanceTransaction> findByDeletedFalseAndTypeInAndDateTransactionBetween(
            Collection<FinanceTransaction.TransactionType> types, LocalDate debut, LocalDate fin);

    Page<FinanceTransaction> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<FinanceTransaction> findByTenantIdAndId(UUID tenantId, UUID id);

    List<FinanceTransaction> findByTenantIdAndReconciledFalse(UUID tenantId);
}
