package com.discipolat.modules.finances.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.currency.domain.CurrencyService;
import com.discipolat.modules.currency.domain.TenantCurrencyResolver;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
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

    /**
     * Défaut historique, désormais défini au SEUL endroit :
     * {@link com.discipolat.modules.currency.domain.TenantCurrencyResolver#FALLBACK}.
     *
     * <p>Avant, cette constante valait « XAF » ici et les entités
     * {@code FinanceAccount}/{@code FinanceDonation} portaient « XOF » : deux
     * valeurs par défaut pour le même concept, selon le chemin d'écriture.</p>
     */
    static final String DEVISE_DEFAUT =
            com.discipolat.modules.currency.domain.TenantCurrencyResolver.FALLBACK;

    private final FinanceTransactionRepository transactionRepository;
    private final FinanceBudgetRepository budgetRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final CurrencyService currencyService;
    private final com.discipolat.modules.currency.domain.TenantCurrencyResolver currencyResolver;
    private final Iso4217CurrencyValidator currencyValidator;
    private final FinanceAccountRepository accountRepository;
    private final FinanceDonationRepository donationRepository;
    private final FinanceTontineRepository tontineRepository;
    private final FinanceTontineMemberRepository tontineMemberRepository;
    private final FinanceTontinePayoutRepository tontinePayoutRepository;

    public FinanceService(FinanceTransactionRepository transactionRepository,
                          FinanceBudgetRepository budgetRepository,
                          SecurityUtils securityUtils,
                          AuditService auditService,
                          EntityPropagationPublisher propagationPublisher,
                          CurrencyService currencyService,
                          Iso4217CurrencyValidator currencyValidator,
                          TenantCurrencyResolver currencyResolver,
                          FinanceAccountRepository accountRepository,
                          FinanceDonationRepository donationRepository,
                          FinanceTontineRepository tontineRepository,
                          FinanceTontineMemberRepository tontineMemberRepository,
                          FinanceTontinePayoutRepository tontinePayoutRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.currencyService = currencyService;
        this.currencyValidator = currencyValidator;
        this.currencyResolver = currencyResolver;
        this.accountRepository = accountRepository;
        this.donationRepository = donationRepository;
        this.tontineRepository = tontineRepository;
        this.tontineMemberRepository = tontineMemberRepository;
        this.tontinePayoutRepository = tontinePayoutRepository;
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
        // A3 (M9) — la transaction porte sa devise ISO-4217 exacte et son montant
        // en unités mineures ; le montant saisi est validé CONTRE la devise du
        // tenant (rejet comptable des décimales sur XAF/JPY…, pas une préférence).
        String devise = resolveDeviseTenant();
        currencyValidator.validateAmount(devise, request.montant());
        FinanceTransaction tx = FinanceTransaction.builder()
                .type(request.type())
                .categorie(request.categorie() == null || request.categorie().isBlank()
                        ? "AUTRE" : request.categorie().trim().toUpperCase())
                .montant(request.montant())
                .devise(devise)
                .montantMinor(currencyValidator.toMinorUnits(devise, request.montant()))
                .tauxVersBase(BigDecimal.ONE)
                .montantBase(request.montant())
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
        String devise = tx.getDevise() != null ? tx.getDevise() : resolveDeviseTenant();
        currencyValidator.validateAmount(devise, request.montant());
        tx.setDevise(devise);
        tx.setMontantMinor(currencyValidator.toMinorUnits(devise, request.montant()));
        if (tx.getTauxVersBase() == null) tx.setTauxVersBase(BigDecimal.ONE);
        // Taux 1 ⇒ la saisie est dans la devise de base : contre-valeur identique.
        if (tx.getTauxVersBase().compareTo(BigDecimal.ONE) == 0) tx.setMontantBase(request.montant());
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

    /** Détail d'un budget par identifiant, consommation calculée sur les dépenses réelles de l'année. */
    @Transactional(readOnly = true)
    public Map<String, Object> getBudget(UUID tenantId, UUID id) {
        // findById ne passe pas par @Filter (chargement par identité) : la
        // clé est revue avec le tenant, sinon un budget d'un autre tenant
        // serait lu (IDOR).
        FinanceBudget b = budgetRepository.findByTenantIdAndId(tenantId, id)
                .filter(x -> !x.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("FinanceBudget", id));
        LocalDate debut = LocalDate.of(b.getAnnee(), 1, 1);
        LocalDate fin = LocalDate.of(b.getAnnee(), 12, 31);
        BigDecimal depense = transactionRepository
                .findByDeletedFalseAndTypeAndDateTransactionBetween(
                        FinanceTransaction.TransactionType.DEPENSE, debut, fin)
                .stream()
                .filter(t -> b.getCategorie().equals(t.getCategorie()))
                .map(FinanceTransaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    public void deleteBudget(UUID tenantId, UUID id) {
        // Même règle que getBudget : findById ne passe pas par @Filter ;
        // sans re-vérification du tenant, un soft delete était possible
        // sur le budget d'un autre tenant (IDOR en écriture).
        FinanceBudget budget = budgetRepository.findByTenantIdAndId(tenantId, id)
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

    /**
     * A3 (M9) — Devise de saisie = devise primaire du tenant (currency_configs).
     * Le repli XAF ne couvre que les cas sans contexte tenant (webhook opérateur)
     * ou tenant sans configuration : c'est un défaut, pas une contrainte — toute
     * devise ISO-4217 est configurable par le tenant.
     */
    private String resolveDeviseTenant() {
        try {
            var primary = currencyService.getPrimaryCurrency();
            if (primary != null && primary.getCurrencyCode() != null && !primary.getCurrencyCode().isBlank()) {
                return primary.getCurrencyCode().toUpperCase(Locale.ROOT);
            }
        } catch (RuntimeException noTenantContext) {
            // webhook / tâche système : pas de TenantContext, défaut documenté.
        }
        return DEVISE_DEFAUT;
    }

    private Map<String, Object> toMap(FinanceTransaction t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("type", t.getType() != null ? t.getType().name() : "");
        map.put("categorie", t.getCategorie());
        map.put("montant", t.getMontant());
        // A3 (M9) — audit multi-devises : la devise portée par la ligne prime sur
        // la devise primaire du tenant (qui peut avoir changé depuis la saisie).
        map.put("devise", t.getDevise());
        map.put("montantMinor", t.getMontantMinor());
        map.put("tauxVersBase", t.getTauxVersBase());
        map.put("montantBase", t.getMontantBase());
        map.put("description", t.getDescription() == null ? "" : t.getDescription());
        map.put("dateTransaction", t.getDateTransaction() != null ? t.getDateTransaction().toString() : "");
        map.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        // P0 #6 — Multi-devise : symbole et fuseau du tenant pour l'affichage
        try {
            var primaryCurrency = currencyService.getPrimaryCurrency();
            if (t.getDevise() == null) map.put("devise", primaryCurrency.getCurrencyCode());
            map.put("deviseSymbole", primaryCurrency.getCurrencySymbol());
            map.put("fuseauHoraire", primaryCurrency.getTimezone());
        } catch (Exception e) {
            if (t.getDevise() == null) map.put("devise", DEVISE_DEFAUT);
            map.put("deviseSymbole", "FCFA");
        }
        return map;
    }

    /**
     * P0 #6 — Multi-devise : statistiques avec conversion dans la devise cible.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> statsWithCurrency(int annee, String targetCurrency) {
        Map<String, Object> baseStats = stats(annee);
        if (targetCurrency == null || targetCurrency.isBlank()) return baseStats;

        try {
            var primaryCurrency = currencyService.getPrimaryCurrency();
            String fromCurrency = primaryCurrency.getCurrencyCode();
            if (fromCurrency.equalsIgnoreCase(targetCurrency)) return baseStats;

            // Conversion des totaux
            BigDecimal totalRecettes = (BigDecimal) baseStats.get("totalRecettes");
            BigDecimal totalDepenses = (BigDecimal) baseStats.get("totalDepenses");
            Double convRecettes = currencyService.convertAmount(
                    totalRecettes.doubleValue(), fromCurrency, targetCurrency);
            Double convDepenses = currencyService.convertAmount(
                    totalDepenses.doubleValue(), fromCurrency, targetCurrency);

            baseStats.put("deviseAffichage", targetCurrency);
            baseStats.put("totalRecettesConverti", Math.round(convRecettes * 100.0) / 100.0);
            baseStats.put("totalDepensesConverti", Math.round(convDepenses * 100.0) / 100.0);
            baseStats.put("soldeConverti", Math.round((convRecettes - convDepenses) * 100.0) / 100.0);
            baseStats.put("tauxConversion", currencyService.convertAmount(1.0, fromCurrency, targetCurrency));
        } catch (Exception e) {
            // Devise non trouvée, retourner les stats de base
        }
        return baseStats;
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

    // ========== ACCOUNTS (V236) ==========

    public List<Map<String, Object>> listAccounts(UUID tenantId) {
        return accountRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getName());
            m.put("accountNumber", a.getAccountNumber());
            m.put("bankName", a.getBankName());
            m.put("balance", a.getBalance());
            m.put("devise", a.getDevise());
            m.put("isActive", a.isActive());
            m.put("createdAt", a.getCreatedAt().toString());
            m.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
            return m;
        }).toList();
    }

    public Map<String, Object> getAccount(UUID tenantId, UUID id) {
        FinanceAccount a = accountRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("FinanceAccount", "id", id.toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("accountNumber", a.getAccountNumber());
        m.put("bankName", a.getBankName());
        m.put("balance", a.getBalance());
        m.put("devise", a.getDevise());
        m.put("isActive", a.isActive());
        m.put("createdAt", a.getCreatedAt().toString());
        m.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
        return m;
    }

    public Map<String, Object> createAccount(UUID tenantId, UUID actorId, Map<String, Object> body) {
        FinanceAccount a = FinanceAccount.builder()
                .tenantId(tenantId)
                .name((String) body.get("name"))
                .accountNumber((String) body.get("accountNumber"))
                .bankName((String) body.get("bankName"))
                .balance(decimalOrDefault(body.get("balance"), java.math.BigDecimal.ZERO))
                .devise(deviseOf(body.get("devise")))
                .isActive(true)
                .build();
        FinanceAccount saved = accountRepository.save(a);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("name", saved.getName());
        m.put("accountNumber", saved.getAccountNumber());
        m.put("bankName", saved.getBankName());
        m.put("balance", saved.getBalance());
        m.put("devise", saved.getDevise());
        m.put("isActive", saved.isActive());
        m.put("createdAt", saved.getCreatedAt().toString());
        return m;
    }

    // ========== Helpers de saisie (V236) ==========

    /**
     * Montant obligatoire, converti proprement.
     *
     * <p>{@code new BigDecimal(String.valueOf(body.get("amount")))} levait une
     * {@code NumberFormatException} sur la chaîne {@code "null"} quand le champ
     * manquait — donc une 500 « erreur interne » pour une simple erreur de
     * saisie, au lieu d'un 400 explicite.
     */
    private static BigDecimal requireDecimal(Object raw, String field) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException(field + " est requis");
        }
        try {
            return new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " n'est pas un montant valide : " + raw);
        }
    }

    private static BigDecimal decimalOrDefault(Object raw, BigDecimal def) {
        if (raw == null || String.valueOf(raw).isBlank()) return def;
        try {
            return new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Montant invalide : " + raw);
        }
    }

    /**
     * Fréquence de tontine, en signalant les valeurs acceptées.
     * {@code Frequency.valueOf} levait une exception opaque sur une valeur
     * absente (« null ») ou mal orthographiée.
     */
    private static FinanceTontine.Frequency requireFrequency(Object raw) {        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException("frequency est requis");
        }
        String v = String.valueOf(raw).trim().toUpperCase();
        for (FinanceTontine.Frequency f : FinanceTontine.Frequency.values()) {
            if (f.name().equals(v)) return f;
        }
        throw new IllegalArgumentException("Fréquence invalide : \"" + raw + "\". Attendu : "
                + java.util.Arrays.stream(FinanceTontine.Frequency.values())
                    .map(Enum::name).collect(java.util.stream.Collectors.joining(", ")));
    }

    /**
     * Devise d'un compte ou d'un don.
     *
     * <p>Reprend la devise PRIMARYAIRE du tenant ({@code resolveDeviseTenant}),
     * déjà utilisée par {@link #createTransaction}. Les ajouts V236 codaient
     * « XOF » en dur : un tenant configuré en EUR se retrouvait avec des
     * comptes et des dons en XOF alors que ses transactions étaient en EUR.
     */
    /**
     * Devise d'une écriture : valeur explicite si fournie, sinon celle du tenant.
     *
     * <p>Délégué à {@link TenantCurrencyResolver} pour que Finance et Payments
     * partagent exactement la même règle (M9).</p>
     */
    private String deviseOf(Object explicit) {
        return currencyResolver.resolve(explicit);
    }

    // ========== DONATIONS (V236) ==========

    public List<Map<String, Object>> listDonations(UUID tenantId) {
        return donationRepository.findByTenantIdOrderByDonationDateDesc(tenantId).stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("donorName", d.getDonorName());
            m.put("amount", d.getAmount());
            m.put("devise", d.getDevise());
            m.put("donationDate", d.getDonationDate().toString());
            m.put("purpose", d.getPurpose());
            m.put("isAnonymous", d.isAnonymous());
            m.put("createdAt", d.getCreatedAt().toString());
            return m;
        }).toList();
    }

    public Map<String, Object> createDonation(UUID tenantId, UUID actorId, Map<String, Object> body) {
        FinanceDonation d = FinanceDonation.builder()
                .tenantId(tenantId)
                .donorName((String) body.get("donorName"))
                .amount(requireDecimal(body.get("amount"), "amount"))
                .devise(deviseOf(body.get("devise")))
                .donationDate(body.get("donationDate") != null ? java.time.Instant.parse(String.valueOf(body.get("donationDate"))) : java.time.Instant.now())
                .purpose((String) body.get("purpose"))
                .isAnonymous(Boolean.parseBoolean(String.valueOf(body.get("isAnonymous"))))
                .build();
        FinanceDonation saved = donationRepository.save(d);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("donorName", saved.getDonorName());
        m.put("amount", saved.getAmount());
        m.put("devise", saved.getDevise());
        m.put("donationDate", saved.getDonationDate().toString());
        m.put("purpose", saved.getPurpose());
        m.put("isAnonymous", saved.isAnonymous());
        m.put("createdAt", saved.getCreatedAt().toString());
        return m;
    }

    // ========== TONTINES (V236) ==========

    public List<Map<String, Object>> listTontines(UUID tenantId) {
        return tontineRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("amountPerTurn", t.getAmountPerTurn());
            m.put("frequency", t.getFrequency().name());
            m.put("startDate", t.getStartDate().toString());
            m.put("endDate", t.getEndDate() != null ? t.getEndDate().toString() : null);
            m.put("isActive", t.isActive());
            m.put("createdAt", t.getCreatedAt().toString());
            m.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
            return m;
        }).toList();
    }

    public Map<String, Object> getTontine(UUID tenantId, UUID id) {
        FinanceTontine t = tontineRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("FinanceTontine", "id", id.toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("description", t.getDescription());
        m.put("amountPerTurn", t.getAmountPerTurn());
        m.put("frequency", t.getFrequency().name());
        m.put("startDate", t.getStartDate().toString());
        m.put("endDate", t.getEndDate() != null ? t.getEndDate().toString() : null);
        m.put("isActive", t.isActive());
        m.put("createdAt", t.getCreatedAt().toString());
        m.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
        return m;
    }

    public Map<String, Object> createTontine(UUID tenantId, UUID actorId, Map<String, Object> body) {
        FinanceTontine t = FinanceTontine.builder()
                .tenantId(tenantId)
                .name((String) body.get("name"))
                .description((String) body.get("description"))
                .amountPerTurn(requireDecimal(body.get("amountPerTurn"), "amountPerTurn"))
                .frequency(requireFrequency(body.get("frequency")))
                .startDate(body.get("startDate") != null ? java.time.Instant.parse(String.valueOf(body.get("startDate"))) : java.time.Instant.now())
                .endDate(body.get("endDate") != null ? java.time.Instant.parse(String.valueOf(body.get("endDate"))) : null)
                .isActive(true)
                .build();
        FinanceTontine saved = tontineRepository.save(t);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("name", saved.getName());
        m.put("description", saved.getDescription());
        m.put("amountPerTurn", saved.getAmountPerTurn());
        m.put("frequency", saved.getFrequency().name());
        m.put("startDate", saved.getStartDate().toString());
        m.put("endDate", saved.getEndDate() != null ? saved.getEndDate().toString() : null);
        m.put("isActive", saved.isActive());
        m.put("createdAt", saved.getCreatedAt().toString());
        return m;
    }

    public List<Map<String, Object>> listTontineMembers(UUID tenantId, UUID tontineId) {
        return tontineMemberRepository.findByTenantIdAndTontineIdOrderByTurnOrderAsc(tenantId, tontineId).stream().map(m -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", m.getId());
            v.put("tontineId", m.getTontineId());
            v.put("userId", m.getUserId());
            v.put("joinedAt", m.getJoinedAt().toString());
            v.put("turnOrder", m.getTurnOrder());
            v.put("isActive", m.isActive());
            return v;
        }).toList();
    }

    /** Ajoute un membre à une tontine (tenant + tontine revérifiés, anti-IDOR). */
    public Map<String, Object> createTontineMember(UUID tenantId, UUID tontineId, Map<String, Object> body) {
        tontineRepository.findByTenantIdAndId(tenantId, tontineId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("FinanceTontine", "id", tontineId.toString()));
        UUID userId = UUID.fromString(String.valueOf(body.get("userId")));
        int turnOrder = body.get("turnOrder") != null ? Integer.parseInt(String.valueOf(body.get("turnOrder"))) : 0;
        FinanceTontineMember member = FinanceTontineMember.builder()
                .tenantId(tenantId)
                .tontineId(tontineId)
                .userId(userId)
                .turnOrder(turnOrder)
                .isActive(true)
                .build();
        FinanceTontineMember saved = tontineMemberRepository.save(member);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", saved.getId());
        v.put("tontineId", saved.getTontineId());
        v.put("userId", saved.getUserId());
        v.put("joinedAt", saved.getJoinedAt().toString());
        v.put("turnOrder", saved.getTurnOrder());
        v.put("isActive", saved.isActive());
        return v;
    }

    public List<Map<String, Object>> listTontinePayouts(UUID tenantId, UUID tontineId) {
        return tontinePayoutRepository.findByTenantIdAndTontineIdOrderByPayoutDateDesc(tenantId, tontineId).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("tontineId", p.getTontineId());
            m.put("memberId", p.getMemberId());
            m.put("amount", p.getAmount());
            m.put("payoutDate", p.getPayoutDate().toString());
            m.put("turnNumber", p.getTurnNumber());
            m.put("createdAt", p.getCreatedAt().toString());
            return m;
        }).toList();
    }

    // ========== REPORTS (V236) ==========

    public Map<String, Object> reportSummary(UUID tenantId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accounts", accountRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).size());
        m.put("donations", donationRepository.findByTenantIdOrderByDonationDateDesc(tenantId).size());
        m.put("tontines", tontineRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId).size());
        return m;
    }

    public List<Map<String, Object>> reportByCategory(UUID tenantId) {
        return transactionRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(FinanceTransaction::getCategorie))
                .entrySet().stream().map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("categorie", e.getKey());
                    m.put("count", e.getValue().size());
                    return m;
                }).toList();
    }

    public List<Map<String, Object>> reportCashFlow(UUID tenantId) {
        return transactionRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getDateTransaction().getMonth()))
                .entrySet().stream().map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("month", e.getKey().name());
                    m.put("count", e.getValue().size());
                    return m;
                }).toList();
    }

    // ========== TRANSACTION DETAIL / RECONCILE (V236) ==========

    public Map<String, Object> getTransaction(UUID tenantId, UUID id) {
        FinanceTransaction t = transactionRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("FinanceTransaction", "id", id.toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("type", t.getType().name());
        m.put("categorie", t.getCategorie());
        m.put("montant", t.getMontant());
        m.put("devise", t.getDevise());
        m.put("description", t.getDescription());
        m.put("dateTransaction", t.getDateTransaction().toString());
        m.put("createdAt", t.getCreatedAt().toString());
        return m;
    }

    public List<Map<String, Object>> listUnreconciledTransactions(UUID tenantId) {
        return transactionRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(t -> !t.isReconciled())
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("type", t.getType().name());
                    m.put("categorie", t.getCategorie());
                    m.put("montant", t.getMontant());
                    m.put("devise", t.getDevise());
                    m.put("dateTransaction", t.getDateTransaction().toString());
                    return m;
                }).toList();
    }

    public Map<String, Object> reconcileTransaction(UUID tenantId, UUID id) {
        FinanceTransaction t = transactionRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("FinanceTransaction", "id", id.toString()));
        t.setReconciled(true);
        transactionRepository.save(t);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("reconciled", t.isReconciled());
        return m;
    }
}
