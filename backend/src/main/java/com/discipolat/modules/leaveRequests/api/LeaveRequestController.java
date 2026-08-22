package com.discipolat.modules.leaveRequests.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.leaveRequests.domain.LeaveRequest;
import com.discipolat.modules.leaveRequests.domain.LeaveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService service;

    public LeaveRequestController(LeaveRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LeaveRequest>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut) {
        Page<LeaveRequest> result = service.list(PageRequest.of(page, size), statut);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequest> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        LeaveRequest req = service.create(
                (String) body.get("type"),
                LocalDate.parse((String) body.get("dateDebut")),
                LocalDate.parse((String) body.get("dateFin")),
                (String) body.get("motif"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<LeaveRequest> approve(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.approve(id, userId));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<LeaveRequest> reject(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.reject(id, userId, body.get("commentaire")));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequest> cancel(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.cancel(id, userId));
    }
}
