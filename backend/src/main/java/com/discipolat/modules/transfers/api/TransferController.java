package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TransferType;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferWorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API des demandes de transfert (moteur de workflow configurable).
 * Accessible à tous les rôles authentifiés ; les permissions d'initiation,
 * de validation et de visibilité sont gérées par le moteur selon le rôle
 * ACTIF et la configuration du workflow.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
public class TransferController {

    private final TransferWorkflowService workflowService;

    public TransferController(TransferWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
    public ResponseEntity<PageResponse<TransferResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransferStatus statut,
            @RequestParam(required = false) TransferType type) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransferResponse> response = workflowService.findAll(statut, type, pageable)
                .map(workflowService::toResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    /** Types de transfert que l'utilisateur courant peut initier (configuration active). */
    @GetMapping("/configurations")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
    public ResponseEntity<List<TransferConfigurationResponse>> configurations() {
        return ResponseEntity.ok(workflowService.getConfigurations());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
    public ResponseEntity<TransferDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')")
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request) {
        TransferRequest req = workflowService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')")
    public ResponseEntity<TransferResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateTransferRequest request) {
        return ResponseEntity.ok(toResponse(workflowService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')")
    public ResponseEntity<TransferResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(workflowService.submit(id)));
    }

    @PostMapping("/{id}/decide")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE')")
    public ResponseEntity<TransferResponse> decide(@PathVariable UUID id,
                                                   @Valid @RequestBody DecideRequest request) {
        return ResponseEntity.ok(toResponse(workflowService.decide(id, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')")
    public ResponseEntity<TransferResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(workflowService.cancel(id)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<TransferResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(workflowService.archive(id)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
    public ResponseEntity<List<TransferHistoryResponse>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getHistory(id));
    }

    @GetMapping("/{id}/decisions")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
    public ResponseEntity<List<Map<String, Object>>> decisions(@PathVariable UUID id) {
        List<Map<String, Object>> decisions = workflowService.getDecisions(id).stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(), "validateurId", d.getValidateurId(),
                        "roleValidateur", d.getRoleValidateur() != null ? d.getRoleValidateur() : "",
                        "decision", d.getDecision().name(),
                        "motivation", d.getMotivation() != null ? d.getMotivation() : "",
                        "etapeOrdre", d.getEtapeOrdre(), "createdAt", d.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(decisions);
    }

    private TransferResponse toResponse(TransferRequest req) {
        return workflowService.toResponse(req);
    }
}
