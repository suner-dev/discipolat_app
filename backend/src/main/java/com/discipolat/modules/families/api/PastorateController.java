package com.discipolat.modules.families.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.PastorateAppointment;
import com.discipolat.modules.families.domain.PastorateTransfer;
import com.discipolat.modules.families.service.PastorateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pastorate")
@PreAuthorize("hasRole('PASTOR_PRINCIPAL')")
public class PastorateController {

    private final PastorateService pastorateService;

    public PastorateController(PastorateService pastorateService) {
        this.pastorateService = pastorateService;
    }

    // ========== APPOINTMENTS ==========

    @GetMapping("/appointments")
    public ResponseEntity<List<PastorateAppointment>> getAppointments(
            @RequestParam(required = false) UUID pastorId,
            @RequestParam(required = false) UUID orgUnitId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(pastorateService.getAppointments(tenantId, pastorId, orgUnitId));
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<PastorateAppointment> getAppointment(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(pastorateService.getAppointment(tenantId, id));
    }

    @PostMapping("/appointments")
    public ResponseEntity<PastorateAppointment> createAppointment(@RequestBody PastorateAppointment appointment) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(pastorateService.createAppointment(tenantId, actorId, appointment));
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<PastorateAppointment> updateAppointment(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(pastorateService.updateAppointment(tenantId, actorId, id, updates));
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> endAppointment(@PathVariable UUID id, @RequestParam String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        pastorateService.endAppointment(tenantId, actorId, id, reason);
        return ResponseEntity.noContent().build();
    }

    // ========== TRANSFERS ==========

    @GetMapping("/transfers")
    public ResponseEntity<List<PastorateTransfer>> getTransfers(
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(pastorateService.getTransfers(tenantId, status));
    }

    @PostMapping("/transfers")
    public ResponseEntity<PastorateTransfer> createTransfer(
            @RequestParam UUID pastorId,
            @RequestParam UUID toOrgUnitId,
            @RequestParam String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(pastorateService.createTransfer(tenantId, actorId, pastorId, toOrgUnitId, reason));
    }

    @PostMapping("/transfers/{id}/approve")
    public ResponseEntity<PastorateTransfer> approveTransfer(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(pastorateService.approveTransfer(tenantId, actorId, id));
    }

    @PostMapping("/transfers/{id}/reject")
    public ResponseEntity<PastorateTransfer> rejectTransfer(@PathVariable UUID id, @RequestParam String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(pastorateService.rejectTransfer(tenantId, actorId, id, reason));
    }

    // ========== HISTORY ==========

    @GetMapping("/pastors/{pastorId}/history")
    public ResponseEntity<Map<String, Object>> getPastorHistory(@PathVariable UUID pastorId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(pastorateService.getPastorHistory(tenantId, pastorId));
    }
}