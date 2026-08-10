package com.discipolat.modules.transfers.api;

import com.discipolat.modules.transfers.domain.TransferAdminService;
import com.discipolat.modules.transfers.domain.TransferWorkflowConfig;
import com.discipolat.modules.transfers.domain.TransferWorkflowStep;
import com.discipolat.modules.transfers.domain.TransferWorkflowStepRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administration du workflow de transfert (réservé au pasteur et à l'admin).
 * Le circuit de validation est entièrement configurable ici, sans code.
 */
@RestController
@RequestMapping("/api/v1/admin/transfers/workflows")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class TransferAdminController {

    private final TransferAdminService adminService;
    private final TransferWorkflowStepRepository stepRepository;

    public TransferAdminController(TransferAdminService adminService,
                                   TransferWorkflowStepRepository stepRepository) {
        this.adminService = adminService;
        this.stepRepository = stepRepository;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowConfigResponse>> findAll() {
        List<WorkflowConfigResponse> configs = adminService.findAll().stream()
                .map(c -> WorkflowConfigResponse.from(c,
                        stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(c.getId())))
                .toList();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowConfigResponse> findById(@PathVariable UUID id) {
        TransferWorkflowConfig config = adminService.findById(id);
        return ResponseEntity.ok(WorkflowConfigResponse.from(config,
                stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(id)));
    }

    @PostMapping
    public ResponseEntity<WorkflowConfigResponse> create(@Valid @RequestBody WorkflowConfigRequest request) {
        TransferWorkflowConfig config = adminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WorkflowConfigResponse.from(config,
                        stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(config.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowConfigResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody WorkflowConfigRequest request) {
        TransferWorkflowConfig config = adminService.update(id, request);
        return ResponseEntity.ok(WorkflowConfigResponse.from(config,
                stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(id)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<WorkflowConfigResponse> toggle(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        TransferWorkflowConfig config = adminService.toggle(id, body.getOrDefault("actif", true));
        return ResponseEntity.ok(WorkflowConfigResponse.from(config,
                stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        adminService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Configuration supprimée"));
    }
}
