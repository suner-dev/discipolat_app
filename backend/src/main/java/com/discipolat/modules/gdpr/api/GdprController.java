package com.discipolat.modules.gdpr.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.gdpr.domain.GdprRequest;
import com.discipolat.modules.gdpr.domain.GdprService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gdpr")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
public class GdprController {

    private final GdprService gdprService;
    private final SecurityUtils securityUtils;

    public GdprController(GdprService gdprService, SecurityUtils securityUtils) {
        this.gdprService = gdprService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/export")
    public ResponseEntity<GdprRequestResponse> requestDataExport(@RequestBody Map<String, UUID> body) {
        UUID userId = body.get("userId");
        GdprRequest request = gdprService.requestDataExport(userId);
        return ResponseEntity.ok(GdprRequestResponse.from(request));
    }

    @PostMapping("/delete")
    public ResponseEntity<GdprRequestResponse> requestDataDeletion(@RequestBody Map<String, UUID> body) {
        UUID userId = body.get("userId");
        GdprRequest request = gdprService.requestDataDeletion(userId);
        return ResponseEntity.ok(GdprRequestResponse.from(request));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<GdprRequestResponse>> listRequests(
            @RequestParam(required = false) UUID userId) {
        List<GdprRequest> requests;
        if (userId != null) {
            requests = gdprService.getRequestsByUser(userId);
        } else {
            requests = gdprService.getRequestsForTenant();
        }
        List<GdprRequestResponse> response = requests.stream()
                .map(GdprRequestResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/process")
    public ResponseEntity<GdprRequestResponse> processRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        UUID processorId = securityUtils.getCurrentUserId();
        GdprRequest request = gdprService.processDataRequest(id, processorId, notes);
        return ResponseEntity.ok(GdprRequestResponse.from(request));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<GdprRequestResponse> rejectRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        UUID processorId = securityUtils.getCurrentUserId();
        GdprRequest request = gdprService.rejectRequest(id, processorId, notes);
        return ResponseEntity.ok(GdprRequestResponse.from(request));
    }
}
