package com.discipolat.modules.referrals.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.referrals.domain.Referral;
import com.discipolat.modules.referrals.domain.ReferralService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
public class ReferralController {

    private final ReferralService service;

    public ReferralController(ReferralService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<Referral>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Referral> result = service.list(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Referral> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stats() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.getStats(userId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Referral> create(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Referral r = service.create(body.get("nomComplet"), body.get("telephone"),
                body.get("email"), body.get("notes"), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Referral> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }
}
