package com.discipolat.modules.finances.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.finances.api.FinanceBudgetRequest;
import com.discipolat.modules.finances.api.FinanceTransactionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Outil métier FINANCES : recettes, dépenses, transactions et budget de
 * l'église. Toutes les statistiques sont calculées sur les transactions
 * réelles (aucune donnée fictive). Réservé ADMIN / PASTEUR (contrôlé au
 * niveau contrôleur + garde-fou de module ModuleGateFilter).
 */
@Service
@Transactional
public class FinanceService {

    private static final String[] MOIS_LABELS = {
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    };

    private final FinanceTransactionRepository transactionRepository;
    private final FinanceBudgetRepository budgetRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public FinanceService(FinanceTransactionRepository transactionRepository,
                          FinanceBudgetRepository budgetRepository,
                          SecurityUtils securityUtils,
                          AuditService auditService,
                          EntityPropagationPublisher propagationPublisher) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    /* ----------------------------- Transactions ----------------------------- */

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTransactions(FinanceTransaction.TransactionType type,
                                                      String categorie,
                                                      LocalDate debut, LocalDate fin) {
        LocalDate from = debut != null ? debut : LocalDate.of(2000, 1, 1);
        LocalDate to = fin != null ? fin : LocalDate.now().plusYears(10);
        List<FinanceTransaction> rows;
        if (type != null && categorie != null && !categorie.isBlank()) {
            rows = transactionRepository.findByDeletedFalseAndTypeAndCategorieAndDateTransactionBetween(type, categorie, from, to);
        } else if (type != null) {
            rows = transactionRepository.findByDeletedFalseAndTypeAndDateTransactionBetween(type, from, to);
        } else if (categorie != null && !categorie.isBlank()) {
            rows = transactionRepository.findByDeletedFalseAndCategorieAndDateTransactionBetween(categorie, from, to);
        } else {
            rows = transactionRepository.findByDeletedFalseAndDateTransactionBetween(from, to);
        }
        return rows.stream()
                .sorted(Comparator.comparing(FinanceTransaction::getDateTransaction).reversed())
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> createTransaction(FinanceTransactionRequest request) {
        FinanceTransaction tx = FinanceTransaction.builder()
                .type(request.type())
                .categorie(request.categorie() == null || request.categorie().isBlank()
                        ? "AUTRE" : request.categorie().trim().toUpperCase())
                .montant(request.montant())
                .description(request.description())
                .dateTransaction(request.dateTransaction() != null ? request.dateTransaction() : LocalDate.now())
                // Contexte système (webhook opérateur) : pas d'utilisateur authentifié.
                .createdBy(currentUserIdOrNull())
                .build();
        FinanceTransaction saved = transactionRepository.save(tx);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("FINANCE_TRANSACTION", saved.getId(),
                Map.of("type", saved.getType().name(), "montant", saved.getMontant(),
                        "categorie", saved.getCategorie()),
                "Transaction créée: " + saved.getType() + " " + saved.getMontant());
        return toMap(saved);
    }

    public Map<String, Object> updateTransaction(UUID id, FinanceTransactionRequest request) {
        FinanceTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FinanceTransaction", id));
        String oldType = tx.getType().name();
        tx.setType(request.type());
        tx.setCategorie(request.categorie() == null || request.categorie().isBlank()
                ? "AUTRE" : request.categorie().trim().toUpperCase());
        tx.setMontant(request.montant());
        tx.setDescription(request.description());
        tx.setDateTransaction(request.dateTransaction() != null ? request.dateTransaction() : LocalDate.now());
        FinanceTransaction saved = transactionRepository.save(tx);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishUpdated("FINANCE_TRANSACTION", saved.getId(),
                Map.of("type", oldType, "montant", tx.getMontant()),
                Map.of("type", saved.getType().name(), "montant", saved.getMontant()),
                "Transaction mise à jour");
        return toMap(saved);
    }

    public void deleteTransaction(UUID id) {
        FinanceTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FinanceTransaction", id));
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishSoftDeleted("FINANCE_TRANSACTION", id,
                Map.of("type", tx.getType().name(), "montant", tx.getMontant()),
                "Transaction supprimée");
        tx.setDeleted(true);
        transactionRepository.save(tx);
    }

    /* ------------------------------- Statistiques ---------------------------- */

    /** Statistiques annuelles calculées sur les transactions réelles. */
    @Transactional(readOnly = true)
    public Map<String, Object> stats(int annee) {
        int year = annee > 0 ? annee : Year.now().getValue();
        LocalDate debut = LocalDate.of(year, 1, 1);
        LocalDate fin = LocalDate.of(year, 12, 31);
        List<FinanceTransaction> rows =
                transactionRepository.findByDeletedFalseAndDateTransactionBetween(debut, fin);

        BigDecimal totalRecettes = sum(rows, FinanceTransaction.TransactionType.RECETTE);
        BigDecimal totalDepenses = sum(rows, FinanceTransaction.TransactionType.DEPENSE);

        // Par mois (12 séries, mois sans transactions = 0).
        List<Map<String, Object>> parMois = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int month = m;
            BigDecimal recettes = rows.stream()
                    .filter(t -> t.getType() == FinanceTransaction.TransactionType.RECETTE
                            && t.getDateTransaction().getMonthValue() == month)
                    .map(FinanceTransaction::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal depenses = rows.stream()
                    .filter(t -> t.getType() == FinanceTransaction.TransactionType.DEPENSE
                            && t.getDateTransaction().getMonthValue() == month)
                    .map(FinanceTransaction::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> mois = new LinkedHashMap<>();
            mois.put("mois", MOIS_LABELS[m - 1]);
            mois.put("recettes", recettes);
            mois.put("depenses", depenses);
            parMois.add(mois);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annee", year);
        result.put("totalRecettes", totalRecettes);
        result.put("totalDepenses", totalDepenses);
        result.put("solde", totalRecettes.subtract(totalDepenses));
        result.put("nbTransactions", rows.size());
        result.put("parMois", parMois);
        result.put("recettesParCategorie", byCategory(rows, FinanceTransaction.TransactionType.RECETTE));
        result.put("depensesParCategorie", byCategory(rows, FinanceTransaction.TransactionType.DEPENSE));
        return result;
    }

    /* -------------------------------- Budgets -------------------------------- */

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBudgets(int annee) {
        int year = annee > 0 ? annee : Year.now().getValue();
        List<FinanceBudget> budgets = budgetRepository.findByDeletedFalseAndAnneeOrderByCategorieAsc(year);
        if (budgets.isEmpty()) {
            return List.of();
        }
        LocalDate debut = LocalDate.of(year, 1, 1);
        LocalDate fin = LocalDate.of(year, 12, 31);
        Map<String, BigDecimal> depensesReelles = transactionRepository
                .findByDeletedFalseAndTypeAndDateTransactionBetween(
                        FinanceTransaction.TransactionType.DEPENSE, debut, fin)
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategorie() == null || t.getCategorie().isBlank() ? "AUTRE" : t.getCategorie(),
                        Collectors.reducing(BigDecimal.ZERO, FinanceTransaction::getMontant, BigDecimal::add)));
        return budgets.stream().map(b -> {
            BigDecimal depense = depensesReelles.getOrDefault(b.getCategorie(), BigDecimal.ZERO);
            BigDecimal pct = b.getMontant().signum() == 0
                    ? BigDecimal.ZERO
                    : depense.multiply(BigDecimal.valueOf(100)).divide(b.getMontant(), 1, RoundingMode.HALF_UP);
            String statut = pct.compareTo(BigDecimal.valueOf(100)) >= 0 ? "DEPASSE"
                    : pct.compareTo(BigDecimal.valueOf(75)) >= 0 ? "ALERTE" : "OK";
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("categorie", b.getCategorie());
            map.put("annee", b.getAnnee());
            map.put("montant", b.getMontant());
            map.put("depenseReelle", depense);
            map.put("consommationPct", pct);
            map.put("statut", statut);
            return map;
        }).toList();
    }

    /** Crée ou met à jour le budget d'une catégorie pour une année (upsert). */
    public Map<String, Object> upsertBudget(FinanceBudgetRequest request) {
        Optional<FinanceBudget> existing = budgetRepository
                .findByDeletedFalseAndAnneeAndCategorie(request.annee(), request.categorie());
        FinanceBudget budget;
        String action;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setMontant(request.montant());
            action = "FINANCE_BUDGET_UPDATED";
        } else {
            budget = FinanceBudget.builder()
                    .categorie(request.categorie())
                    .annee(request.annee())
                    .montant(request.montant())
                    .createdBy(securityUtils.getCurrentUserId())
                    .build();
            action = "FINANCE_BUDGET_CREATED";
        }
        FinanceBudget saved = budgetRepository.save(budget);
        // ===== PROPAGATION CENTRALISÉE =====
        if ("FINANCE_BUDGET_UPDATED".equals(action)) {
            propagationPublisher.publishUpdated("FINANCE_BUDGET", saved.getId(),
                    Map.of(),
                    Map.of("categorie", saved.getCategorie(), "montant", saved.getMontant()),
                    "Budget mis à jour: " + saved.getCategorie());
        } else {
            propagationPublisher.publishCreated("FINANCE_BUDGET", saved.getId(),
                    Map.of("categorie", saved.getCategorie(), "montant", saved.getMontant(),
                            "annee", saved.getAnnee()),
                    "Budget créé: " + saved.getCategorie());
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", saved.getId());
        map.put("categorie", saved.getCategorie());
        map.put("annee", saved.getAnnee());
        map.put("montant", saved.getMontant());
        return map;
    }

    public void deleteBudget(UUID id) {
        FinanceBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FinanceBudget", id));
        budget.setDeleted(true);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishDeleted("FINANCE_BUDGET", id,
                Map.of("categorie", budget.getCategorie(), "montant", budget.getMontant()),
                "Budget supprimé: " + budget.getCategorie());
        budgetRepository.save(budget);
    }

    /* -------------------------------- Helpers -------------------------------- */

    /**
     * Identifiant de l'utilisateur courant, ou null en contexte système
     * (webhook opérateur non authentifié) — évite une exception qui marquerait
     * la transaction « rollback-only » et ferait échouer la confirmation.
     */
    private UUID currentUserIdOrNull() {
        try {
            return securityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toMap(FinanceTransaction t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("type", t.getType() != null ? t.getType().name() : "");
        map.put("categorie", t.getCategorie());
        map.put("montant", t.getMontant());
        map.put("description", t.getDescription() == null ? "" : t.getDescription());
        map.put("dateTransaction", t.getDateTransaction() != null ? t.getDateTransaction().toString() : "");
        map.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        return map;
    }

    private BigDecimal sum(List<FinanceTransaction> rows, FinanceTransaction.TransactionType type) {
        return rows.stream()
                .filter(t -> t.getType() == type)
                .map(FinanceTransaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> byCategory(List<FinanceTransaction> rows,
                                                 FinanceTransaction.TransactionType type) {
        return rows.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(
                        t -> t.getCategorie() == null || t.getCategorie().isBlank() ? "AUTRE" : t.getCategorie(),
                        Collectors.reducing(BigDecimal.ZERO, FinanceTransaction::getMontant, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("categorie", e.getKey());
                    map.put("total", e.getValue());
                    return map;
                })
                .toList();
    }
}
