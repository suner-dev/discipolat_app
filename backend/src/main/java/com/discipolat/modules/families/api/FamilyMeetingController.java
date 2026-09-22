package com.discipolat.modules.families.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.FamilyMeeting;
import com.discipolat.modules.families.service.FamilyOSService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * G4.4 — listing tenant-wide des réunions de famille automatisées.
 * Expérimenté par FamilyMeetingPage.tsx et écouté en SSE pour le refresh live des rôles.
 */
@RestController
@RequestMapping("/api/v1/family-meetings")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')")
public class FamilyMeetingController {

    private final FamilyOSService familyOSService;

    public FamilyMeetingController(FamilyOSService familyOSService) {
        this.familyOSService = familyOSService;
    }

    @GetMapping
    public ResponseEntity<List<FamilyMeeting>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(familyOSService.getFamilyMeetings(tenantId, from, to));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<FamilyMeeting> complete(
            @PathVariable("id") UUID meetingId,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        String minutes = body == null ? null : (String) body.getOrDefault("minutes", "");
        return ResponseEntity.ok(familyOSService.completeMeeting(tenantId, actorId, meetingId, minutes));
    }
}
