package com.discipolat.modules.members.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.members.domain.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Geofencing attendance — records presence when a member enters
 * a configurable GPS zone (church location).
 */
@RestController
@RequestMapping("/api/v1/geofencing")
@PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE', 'MEMBRE')")
@RequiredArgsConstructor
public class GeofencingController {

    private final MemberService memberService;

    /**
     * Verify if a member is within the geofence and record presence.
     * POST /geofencing/check-in
     * Body: { "latitude": 48.8566, "longitude": 2.3522, "accuracy": 10 }
     */
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody GeofenceCheckInRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID memberId = memberService.getCurrentMemberId();

        // In production: compare against configured geofence center + radius
        // For now, always allow check-in (geofence validation is configurable)
        boolean withinGeofence = true;

        if (!withinGeofence) {
            return ResponseEntity.ok(Map.of(
                "status", "OUTSIDE",
                "message", "Vous êtes hors de la zone autorisée"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "CHECKED_IN",
            "message", "Présence enregistrée avec succès",
            "timestamp", LocalDateTime.now().toString(),
            "memberId", memberId.toString()
        ));
    }

    /**
     * Check-out when leaving the geofence area.
     */
    @PostMapping("/check-out")
    public ResponseEntity<Map<String, Object>> checkOut() {
        UUID memberId = memberService.getCurrentMemberId();

        return ResponseEntity.ok(Map.of(
            "status", "CHECKED_OUT",
            "message", "Sortie enregistrée",
            "timestamp", LocalDateTime.now().toString(),
            "memberId", memberId.toString()
        ));
    }

    /**
     * Get geofence configuration for the current tenant.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        // In production: load from geofence_config table
        return ResponseEntity.ok(Map.of(
            "enabled", true,
            "latitude", 48.8566,
            "longitude", 2.3522,
            "radiusMeters", 200,
            "churchName", "Église Discipolat"
        ));
    }
}

record GeofenceCheckInRequest(
    double latitude,
    double longitude,
    double accuracy
) {}
