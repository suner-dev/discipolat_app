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

    // ========== ACCOUNTS (V236) ==========

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> listAccounts() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.listAccounts(tenantId));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Map<String, Object>> getAccount(@PathVariable UUID id) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.getAccount(tenantId, id));
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody Map<String, Object> body) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        UUID actorId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createAccount(tenantId, actorId, body));
    }

    // ========== DONATIONS (V236) ==========

    @GetMapping("/donations")
    public ResponseEntity<List<Map<String, Object>>> listDonations() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.listDonations(tenantId));
    }

    @PostMapping("/donations")
    public ResponseEntity<Map<String, Object>> createDonation(@RequestBody Map<String, Object> body) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        UUID actorId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createDonation(tenantId, actorId, body));
    }

    // ========== TONTINES (V236) ==========

    @GetMapping("/tontines")
    public ResponseEntity<List<Map<String, Object>>> listTontines() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.listTontines(tenantId));
    }

    @GetMapping("/tontines/{id}")
    public ResponseEntity<Map<String, Object>> getTontine(@PathVariable UUID id) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.getTontine(tenantId, id));
    }

    @PostMapping("/tontines")
    public ResponseEntity<Map<String, Object>> createTontine(@RequestBody Map<String, Object> body) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        UUID actorId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createTontine(tenantId, actorId, body));
    }

    @GetMapping("/tontines/{tontineId}/members")
    public ResponseEntity<List<Map<String, Object>>> listTontineMembers(@PathVariable UUID tontineId) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.listTontineMembers(tenantId, tontineId));
    }

    @GetMapping("/tontines/{tontineId}/payouts")
    public ResponseEntity<List<Map<String, Object>>> listTontinePayouts(@PathVariable UUID tontineId) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.listTontinePayouts(tenantId, tontineId));
    }

    // ========== REPORTS (V236) ==========

    @GetMapping("/reports/summary")
    public ResponseEntity<Map<String, Object>> reportSummary() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.reportSummary(tenantId));
    }

    @GetMapping("/reports/by-category")
    public ResponseEntity<List<Map<String, Object>>> reportByCategory() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.reportByCategory(tenantId));
    }

    @GetMapping("/reports/cash-flow")
    public ResponseEntity<List<Map<String, Object>>> reportCashFlow() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.reportCashFlow(tenantId));
    }

    // ========== TRANSACTION DETAIL / RECONCILE (V236) ==========

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Map<String, Object>> getTransaction(@PathVariable UUID id) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.getTransaction(tenantId, id));
    }

    @GetMapping("/transactions/unreconciled")
    public ResponseEntity<List<Map<String, Object>>> listUnreconciled() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.listUnreconciledTransactions(tenantId));
    }

    @PostMapping("/transactions/{id}/reconcile")
    public ResponseEntity<Map<String, Object>> reconcileTransaction(@PathVariable UUID id) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.requireTenantId();
        return ResponseEntity.ok(financeService.reconcileTransaction(tenantId, id));
    }
}
