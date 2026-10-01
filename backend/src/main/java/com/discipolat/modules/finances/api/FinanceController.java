package com.discipolat.modules.finances.api;

import com.discipolat.modules.finances.domain.FinanceService;
import com.discipolat.modules.finances.domain.FinanceTransaction;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API de l'outil métier FINANCES (recettes, dépenses, budget).
 * Réservé aux super-utilisateurs (ADMIN / PASTEUR) ; le module entier est
 * activable/désactivable (ModuleGateFilter → FINANCES, 403 si désactivé).
 */
@RestController
@RequestMapping("/api/v1/finances")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
public class FinanceController {

    private final FinanceService financeService;
    /** §G6.4 — chemin critique « finance : rapprochement ». */
    private final com.discipolat.modules.finances.service.FinanceReconciliationService reconciliationService;

    public FinanceController(FinanceService financeService,
                             com.discipolat.modules.finances.service.FinanceReconciliationService reconciliationService) {
        this.financeService = financeService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> listTransactions(
            @RequestParam(required = false) FinanceTransaction.TransactionType type,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(financeService.listTransactions(type, categorie, debut, fin));
    }

    @PostMapping("/transactions")
    public ResponseEntity<Map<String, Object>> createTransaction(
            @Valid @RequestBody FinanceTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financeService.createTransaction(request));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<Map<String, Object>> updateTransaction(@PathVariable UUID id,
                                                                 @Valid @RequestBody FinanceTransactionRequest request) {
        return ResponseEntity.ok(financeService.updateTransaction(id, request));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID id) {
        financeService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(@RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(financeService.stats(annee != null ? annee : LocalDate.now().getYear()));
    }

    /** P0 #6 — Statistiques financières avec conversion multi-devise. */
    @GetMapping("/stats/currency")
    public ResponseEntity<Map<String, Object>> statsWithCurrency(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(financeService.statsWithCurrency(
                annee != null ? annee : LocalDate.now().getYear(), currency));
    }

    @GetMapping("/budgets")
    public ResponseEntity<List<Map<String, Object>>> listBudgets(@RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(financeService.listBudgets(annee != null ? annee : LocalDate.now().getYear()));
    }

    @PostMapping("/budgets")
    public ResponseEntity<Map<String, Object>> upsertBudget(@Valid @RequestBody FinanceBudgetRequest request) {
        return ResponseEntity.ok(financeService.upsertBudget(request));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        financeService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    // ========== RAPPROCHEMENT (§G6.4 — chemin critique « finance ») ==========

    /** Import d'un relevé bancaire (idempotent par externalKey) + rapprochement auto immédiat. */
    @PostMapping("/reconciliation/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> importStatement(
            @RequestBody Map<String, List<com.discipolat.modules.finances.service.FinanceReconciliationService.StatementLineInput>> body) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                reconciliationService.importStatement(tenantId, body.get("lines")));
    }

    /** Relance le rapprochement automatique sur les lignes encore ouvertes. */
    @PostMapping("/reconciliation/auto")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> autoMatch() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(reconciliationService.autoMatch(tenantId));
    }

    /** Résidus à traiter manuellement : lignes du relevé + écritures jamais rapprochées. */
    @GetMapping("/reconciliation/unmatched")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> unmatched() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(reconciliationService.unmatched(tenantId));
    }

    /** Rapprochement manuel d'une ligne avec une écriture (tenant contrôlé). */
    @PostMapping("/reconciliation/match")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<com.discipolat.modules.finances.domain.FinanceBankStatementLine> manualMatch(
            @RequestBody Map<String, String> body) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(reconciliationService.manualMatch(tenantId,
                UUID.fromString(body.get("lineId")), UUID.fromString(body.get("transactionId"))));
    }

    /** Grand livre : soldes, écart rapproché/relevé, intégrité (jamais masqué). */
    @GetMapping("/reconciliation/ledger")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> ledger() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(reconciliationService.ledger(tenantId));
    }
}
