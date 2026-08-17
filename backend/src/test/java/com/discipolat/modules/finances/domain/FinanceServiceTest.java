package com.discipolat.modules.finances.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.finances.api.FinanceBudgetRequest;
import com.discipolat.modules.finances.api.FinanceTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock private FinanceTransactionRepository transactionRepository;
    @Mock private FinanceBudgetRepository budgetRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private AuditService auditService;

    private FinanceService service;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        service = new FinanceService(transactionRepository, budgetRepository, securityUtils, auditService);
    }

    private FinanceTransaction tx(UUID id, FinanceTransaction.TransactionType type, String categorie,
                                  String montant, LocalDate date) {
        return FinanceTransaction.builder().id(id).type(type).categorie(categorie)
                .montant(new BigDecimal(montant)).dateTransaction(date).build();
    }

    @Test
    void createTransaction_persistsAndAudits() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.save(any(FinanceTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FinanceTransactionRequest request = new FinanceTransactionRequest(
                FinanceTransaction.TransactionType.RECETTE, "dime",
                new BigDecimal("1500.00"), "Dîmes du dimanche", LocalDate.of(2026, 8, 17));

        Map<String, Object> result = service.createTransaction(request);

        assertThat(result).containsEntry("type", "RECETTE")
                .containsEntry("categorie", "DIME")
                .containsEntry("montant", new BigDecimal("1500.00"));
        verify(transactionRepository).save(any(FinanceTransaction.class));
        verify(auditService).logSimple("FINANCE_TRANSACTION_CREATED", "FINANCE_TRANSACTION", (UUID) result.get("id"));
    }

    @Test
    void listTransactions_filtersByTypeAndPeriod() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(transactionRepository.findByDeletedFalseAndTypeAndDateTransactionBetween(
                FinanceTransaction.TransactionType.DEPENSE, debut, fin))
                .thenReturn(List.of(
                        tx(UUID.randomUUID(), FinanceTransaction.TransactionType.DEPENSE, "LOYER",
                                "800.00", LocalDate.of(2026, 3, 5)),
                        tx(UUID.randomUUID(), FinanceTransaction.TransactionType.DEPENSE, "MATERIEL",
                                "120.50", LocalDate.of(2026, 4, 10))));

        List<Map<String, Object>> rows = service.listTransactions(
                FinanceTransaction.TransactionType.DEPENSE, null, debut, fin);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("type", "DEPENSE");
        assertThat(rows).extracting(m -> m.get("categorie")).contains("LOYER", "MATERIEL");
    }

    @Test
    void stats_computesMonthlySeriesAndSolde() {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(transactionRepository.findByDeletedFalseAndDateTransactionBetween(debut, fin))
                .thenReturn(List.of(
                        tx(UUID.randomUUID(), FinanceTransaction.TransactionType.RECETTE, "DIME",
                                "1000.00", LocalDate.of(2026, 2, 10)),
                        tx(UUID.randomUUID(), FinanceTransaction.TransactionType.RECETTE, "DIME",
                                "500.00", LocalDate.of(2026, 2, 20)),
                        tx(UUID.randomUUID(), FinanceTransaction.TransactionType.DEPENSE, "LOYER",
                                "600.00", LocalDate.of(2026, 2, 5))));

        Map<String, Object> stats = service.stats(2026);

        assertThat(stats).containsEntry("annee", 2026);
        assertThat(stats).containsEntry("totalRecettes", new BigDecimal("1500.00"))
                .containsEntry("totalDepenses", new BigDecimal("600.00"))
                .containsEntry("solde", new BigDecimal("900.00"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parMois = (List<Map<String, Object>>) stats.get("parMois");
        assertThat(parMois).hasSize(12);
        // Février (index 1) agrège les 3 transactions ; janvier reste à 0.
        assertThat(parMois.get(1)).containsEntry("recettes", new BigDecimal("1500.00"))
                .containsEntry("depenses", new BigDecimal("600.00"));
        assertThat(parMois.get(0)).containsEntry("recettes", BigDecimal.ZERO)
                .containsEntry("depenses", BigDecimal.ZERO);
    }

    @Test
    void upsertBudget_createsThenUpdatesSameCategory() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(budgetRepository.findByDeletedFalseAndAnneeAndCategorie(2026, "LOGIQUE")).thenReturn(Optional.empty());
        when(budgetRepository.save(any(FinanceBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> created = service.upsertBudget(
                new FinanceBudgetRequest("LOGIQUE", 2026, new BigDecimal("10000.00")));
        assertThat(created).containsEntry("categorie", "LOGIQUE").containsEntry("montant", new BigDecimal("10000.00"));
        verify(auditService).logSimple("FINANCE_BUDGET_CREATED", "FINANCE_BUDGET", (UUID) created.get("id"));

        // Mise à jour : la catégorie existe déjà → pas de doublon, audit UPDATED.
        FinanceBudget existing = FinanceBudget.builder().id(UUID.randomUUID())
                .categorie("LOGIQUE").annee(2026).montant(new BigDecimal("10000.00")).build();
        when(budgetRepository.findByDeletedFalseAndAnneeAndCategorie(2026, "LOGIQUE"))
                .thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);

        Map<String, Object> updated = service.upsertBudget(
                new FinanceBudgetRequest("LOGIQUE", 2026, new BigDecimal("12000.00")));
        assertThat(updated).containsEntry("montant", new BigDecimal("12000.00"));
        verify(auditService).logSimple("FINANCE_BUDGET_UPDATED", "FINANCE_BUDGET", existing.getId());
    }

    @Test
    void listBudgets_computesConsumptionAndStatus() {
        FinanceBudget budget = FinanceBudget.builder().id(UUID.randomUUID())
                .categorie("LOYER").annee(2026).montant(new BigDecimal("10000.00")).build();
        when(budgetRepository.findByDeletedFalseAndAnneeOrderByCategorieAsc(2026))
                .thenReturn(List.of(budget));
        when(transactionRepository.findByDeletedFalseAndTypeAndDateTransactionBetween(
                FinanceTransaction.TransactionType.DEPENSE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(
                        tx(UUID.randomUUID(), FinanceTransaction.TransactionType.DEPENSE, "LOYER",
                                "8000.00", LocalDate.of(2026, 5, 1))));

        List<Map<String, Object>> budgets = service.listBudgets(2026);

        assertThat(budgets).hasSize(1);
        assertThat(budgets.get(0)).containsEntry("depenseReelle", new BigDecimal("8000.00"))
                .containsEntry("statut", "ALERTE"); // 80 % ≥ 75 %
    }
}
