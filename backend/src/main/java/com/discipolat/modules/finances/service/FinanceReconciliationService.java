package com.discipolat.modules.finances.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.finances.domain.FinanceBankStatementLine;
import com.discipolat.modules.finances.domain.FinanceBankStatementLineRepository;
import com.discipolat.modules.finances.domain.FinanceTransaction;
import com.discipolat.modules.finances.domain.FinanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * §G6.4 — « finance : rapprochement » (chemin critique n°8).
 * Import de relevé bancaire (idempotent par externalKey), rapprochement
 * AUTOMATIQUE (montant exact + date ± tolérance, ou référence commune),
 * rapprochement MANUEL des résidus, et contrôle d'intégrité du grand livre.
 * Rien n'est jamais supprimé : les lignes changent seulement de statut.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FinanceReconciliationService {

    /** Tolérance de dates (jours) pour le rapprochement automatique par montant. */
    static final int DATE_TOLERANCE_DAYS = 3;

    private final FinanceBankStatementLineRepository lineRepository;
    private final FinanceTransactionRepository transactionRepository;

    public record StatementLineInput(LocalDate dateTransaction, BigDecimal montant,
                                     String description, String reference, String externalKey) {}

    /**
     * Importe un relevé : les lignes déjà connues (même externalKey) sont
     * IGNOREÉES — un re-import du même fichier n'a jamais doublonné.
     */
    public Map<String, Object> importStatement(UUID tenantId, List<StatementLineInput> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Au moins une ligne de relevé est requise");
        }
        Set<String> keys = lines.stream()
                .map(StatementLineInput::externalKey)
                .filter(k -> k != null && !k.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> alreadyImported = keys.isEmpty() ? Set.of()
                : lineRepository.findByTenantIdAndExternalKeyIn(tenantId, keys).stream()
                        .map(FinanceBankStatementLine::getExternalKey)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());

        int imported = 0;
        int skipped = 0;
        for (StatementLineInput in : lines) {
            if (in.dateTransaction() == null || in.montant() == null) {
                throw new IllegalArgumentException("dateTransaction et montant sont requis");
            }
            if (in.externalKey() != null && alreadyImported.contains(in.externalKey())) {
                skipped++;
                continue;
            }
            lineRepository.save(FinanceBankStatementLine.builder()
                    .tenantId(tenantId)
                    .dateTransaction(in.dateTransaction())
                    .montant(in.montant())
                    .description(in.description())
                    .reference(in.reference())
                    .externalKey(in.externalKey())
                    .status(FinanceBankStatementLine.Status.UNMATCHED)
                    .build());
            imported++;
        }
        Map<String, Object> auto = autoMatch(tenantId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skippedDuplicates", skipped);
        result.putAll(auto);
        return result;
    }

    /**
     * Rapprochement automatique : montant STRICTEMENT égal ET (date ± 3 jours
     * OU référence présente dans la description de la transaction).
     * Une transaction ne peut matcher qu'UNE seule ligne.
     */
    public Map<String, Object> autoMatch(UUID tenantId) {
        List<FinanceBankStatementLine> unmatched =
                lineRepository.findByTenantIdAndStatus(tenantId, FinanceBankStatementLine.Status.UNMATCHED);
        Set<UUID> alreadyMatchedTx = matchedTransactionIds(tenantId);
        List<FinanceTransaction> candidates = new ArrayList<>(transactionRepository.findAll().stream()
                .filter(t -> tenantId.equals(t.getTenantId()))
                .filter(t -> !t.isDeleted())
                .filter(t -> !alreadyMatchedTx.contains(t.getId()))
                .toList());

        int matched = 0;
        for (FinanceBankStatementLine line : unmatched) {
            FinanceTransaction hit = null;
            for (FinanceTransaction t : candidates) {
                if (t.getMontant() == null || line.getMontant() == null
                        || t.getMontant().compareTo(line.getMontant()) != 0) {
                    continue;
                }
                boolean sameReference = line.getReference() != null && !line.getReference().isBlank()
                        && t.getDescription() != null
                        && t.getDescription().toLowerCase().contains(line.getReference().toLowerCase());
                boolean nearDate = t.getDateTransaction() != null && line.getDateTransaction() != null
                        && Math.abs(ChronoUnit.DAYS.between(t.getDateTransaction(), line.getDateTransaction()))
                            <= DATE_TOLERANCE_DAYS;
                if (sameReference || nearDate) {
                    hit = t;
                    break;
                }
            }
            if (hit != null) {
                line.setMatchedTransactionId(hit.getId());
                line.setStatus(FinanceBankStatementLine.Status.MATCHED);
                lineRepository.save(line);
                candidates.remove(hit);
                matched++;
            }
        }
        long remaining = lineRepository.countByTenantIdAndStatus(tenantId, FinanceBankStatementLine.Status.UNMATCHED);
        return Map.of("autoMatched", matched, "unmatchedLines", remaining);
    }

    /** Rapprochement manuel d'un résidu (contrôle tenant des deux côtés). */
    public FinanceBankStatementLine manualMatch(UUID tenantId, UUID lineId, UUID transactionId) {
        FinanceBankStatementLine line = lineRepository.findById(lineId)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("FinanceBankStatementLine", lineId));
        FinanceTransaction tx = transactionRepository.findById(transactionId)
                .filter(t -> tenantId.equals(t.getTenantId()) && !t.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("FinanceTransaction", transactionId));
        if (matchedTransactionIds(tenantId).contains(tx.getId())
                && !tx.getId().equals(line.getMatchedTransactionId())) {
            throw new IllegalStateException("Cette transaction est déjà rapprochée d'une autre ligne");
        }
        line.setMatchedTransactionId(tx.getId());
        line.setStatus(FinanceBankStatementLine.Status.MATCHED);
        return lineRepository.save(line);
    }

    public Map<String, Object> unmatched(UUID tenantId) {
        Set<UUID> matchedTx = matchedTransactionIds(tenantId);
        List<FinanceBankStatementLine> openLines =
                lineRepository.findByTenantIdAndStatus(tenantId, FinanceBankStatementLine.Status.UNMATCHED);
        List<FinanceTransaction> openTx = transactionRepository.findAll().stream()
                .filter(t -> tenantId.equals(t.getTenantId()))
                .filter(t -> !t.isDeleted())
                .filter(t -> !matchedTx.contains(t.getId()))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statementLines", openLines);
        result.put("transactions", openTx);
        return result;
    }

    /**
     * Grand livre : solde théorique (RECETTE − DEPENSE) vs somme des lignes
     * rapprochées du relevé ; intégrité = aucune ligne double-matcher et
     * écart explicite (le diff reste affiché, jamais masqué).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> ledger(UUID tenantId) {
        List<FinanceBankStatementLine> allLines = lineRepository.findByTenantIdOrderByDateTransactionDesc(tenantId);
        List<FinanceTransaction> allTx = transactionRepository.findAll().stream()
                .filter(t -> tenantId.equals(t.getTenantId()))
                .filter(t -> !t.isDeleted())
                .toList();

        BigDecimal recettes = sum(allTx, FinanceTransaction.TransactionType.RECETTE);
        BigDecimal depenses = sum(allTx, FinanceTransaction.TransactionType.DEPENSE);
        BigDecimal ledgerSolde = recettes.subtract(depenses);

        BigDecimal matchedStatement = allLines.stream()
                .filter(l -> l.getStatus() == FinanceBankStatementLine.Status.MATCHED)
                .map(FinanceBankStatementLine::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // intégrité : chaque transaction ne peut être référencée qu'une fois
        long matchedCount = allLines.stream()
                .filter(l -> l.getMatchedTransactionId() != null)
                .map(l -> l.getMatchedTransactionId().toString())
                .distinct().count();
        long matchedWithTx = allLines.stream()
                .filter(l -> l.getMatchedTransactionId() != null).count();
        boolean noDoubleMatch = matchedCount == matchedWithTx;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recettes", recettes);
        result.put("depenses", depenses);
        result.put("ledgerSolde", ledgerSolde);
        result.put("matchedStatementTotal", matchedStatement);
        result.put("difference", ledgerSolde.subtract(matchedStatement));
        result.put("unmatchedLines", allLines.stream()
                .filter(l -> l.getStatus() == FinanceBankStatementLine.Status.UNMATCHED).count());
        result.put("totalLines", allLines.size());
        result.put("totalTransactions", allTx.size());
        result.put("integrityOk", noDoubleMatch);
        return result;
    }

    private Set<UUID> matchedTransactionIds(UUID tenantId) {
        return lineRepository.findByTenantIdOrderByDateTransactionDesc(tenantId).stream()
                .map(FinanceBankStatementLine::getMatchedTransactionId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private BigDecimal sum(List<FinanceTransaction> txs, FinanceTransaction.TransactionType type) {
        return txs.stream()
                .filter(t -> t.getType() == type)
                .map(t -> t.getMontant() != null ? t.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
