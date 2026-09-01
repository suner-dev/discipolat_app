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

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
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
}
