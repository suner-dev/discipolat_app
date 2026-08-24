package com.discipolat.modules.members.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.members.domain.GeofencePing;
import com.discipolat.modules.members.domain.GeofencePingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Geofencing attendance — records presence when a member enters
 * a configurable GPS zone (church location).
 *
 * P20 — compléments : auto check-in par distance, historique GPS,
 * mode basse consommation (échantillonnage réduit).
 */
@RestController
@RequestMapping("/api/v1/geofencing")
@PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE', 'MEMBRE')")
@RequiredArgsConstructor
public class GeofencingController {

    private final SecurityUtils securityUtils;
    private final GeofencePingRepository pingRepository;

    /**
     * Verify if a member is within the geofence and record presence.
     * POST /geofencing/check-in
     * Body: { "latitude": 48.8566, "longitude": 2.3522, "accuracy": 10, "powerMode": "NORMAL" }
     */
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody GeofenceCheckInRequest request) {
        return ResponseEntity.ok(record(request.latitude(), request.longitude(), request.accuracy(),
                request.powerMode(), GeofencePing.Kind.CHECK_IN));
    }

    /**
     * P20 — Auto check-in : le client envoie sa position périodiquement ;
     * le serveur décide si l'utilisateur est dans la zone et enregistre.
     */
    @PostMapping("/auto-check-in")
    public ResponseEntity<Map<String, Object>> autoCheckIn(@RequestBody GeofenceCheckInRequest request) {
        Map<String, Object> result = record(request.latitude(), request.longitude(), request.accuracy(),
                request.powerMode(), GeofencePing.Kind.AUTO_CHECK_IN);
        if (!Boolean.TRUE.equals(result.get("inZone"))) {
            result.put("status", "OUT_OF_ZONE");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Check-out when leaving the geofence area.
     */
    @PostMapping("/check-out")
    public ResponseEntity<Map<String, Object>> checkOut(@RequestBody(required = false) GeofenceCheckInRequest request) {
        double lat = request != null ? request.latitude() : 0;
        double lng = request != null ? request.longitude() : 0;
        double acc = request != null ? request.accuracy() : 0;
        String mode = request != null ? request.powerMode() : "NORMAL";
        UUID tenantId = TenantContext.getTenantId();
        UUID memberId = securityUtils.getCurrentUserId();
        GeofencePing ping = new GeofencePing();
        ping.setTenantId(tenantId);
        ping.setUserId(memberId);
        ping.setLatitude(lat);
        ping.setLongitude(lng);
        ping.setAccuracy(acc);
        ping.setKind(GeofencePing.Kind.CHECK_OUT);
        ping.setPowerMode(mode != null ? mode : "NORMAL");
        pingRepository.save(ping);
        return ResponseEntity.ok(Map.of(
            "status", "CHECKED_OUT",
            "message", "Sortie enregistrée",
            "timestamp", LocalDateTime.now().toString(),
            "memberId", memberId.toString()
        ));
    }

    /**
     * P20 — Historique GPS de l'utilisateur courant (100 derniers pings).
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> history() {
        UUID tenantId = TenantContext.getTenantId();
        UUID memberId = securityUtils.getCurrentUserId();
        List<Map<String, Object>> out = pingRepository
                .findTop100ByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, memberId)
                .stream().map(GeofencingController::toMap).toList();
        return ResponseEntity.ok(out);
    }

    /**
     * P20 — Historique global (superviseurs).
     */
    @GetMapping("/history/all")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE')")
    public ResponseEntity<List<Map<String, Object>>> historyAll() {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> out = pingRepository
                .findTop50ByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(GeofencingController::toMap).toList();
        return ResponseEntity.ok(out);
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

    // ------------------------------------------------------------------

    private Map<String, Object> record(double lat, double lng, double accuracy,
                                       String powerMode, GeofencePing.Kind kind) {
        UUID tenantId = TenantContext.getTenantId();
        UUID memberId = securityUtils.getCurrentUserId();

        Map<String, Object> config = getConfig().getBody();
        double centerLat = ((Number) config.getOrDefault("latitude", 0)).doubleValue();
        double centerLng = ((Number) config.getOrDefault("longitude", 0)).doubleValue();
        int radius = ((Number) config.getOrDefault("radiusMeters", 200)).intValue();

        int distance = (int) Math.round(haversineMeters(lat, lng, centerLat, centerLng));
        boolean inZone = distance <= radius;

        GeofencePing ping = new GeofencePing();
        ping.setTenantId(tenantId);
        ping.setUserId(memberId);
        ping.setLatitude(lat);
        ping.setLongitude(lng);
        ping.setAccuracy(accuracy);
        ping.setDistanceMeters(distance);
        ping.setInZone(inZone);
        ping.setKind(kind);
        ping.setPowerMode(powerMode != null ? powerMode : "NORMAL");
        pingRepository.save(ping);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", inZone ? "CHECKED_IN" : "OUT_OF_ZONE");
        result.put("inZone", inZone);
        result.put("distanceMeters", distance);
        result.put("radiusMeters", radius);
        result.put("message", inZone ? "Présence enregistrée avec succès" : "Hors zone — présence non comptabilisée");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("memberId", memberId.toString());
        return result;
    }

    /** Distance Haversine en mètres. */
    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static Map<String, Object> toMap(GeofencePing p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        m.put("latitude", p.getLatitude());
        m.put("longitude", p.getLongitude());
        m.put("distanceMeters", p.getDistanceMeters());
        m.put("inZone", p.isInZone());
        m.put("kind", p.getKind());
        m.put("powerMode", p.getPowerMode());
        m.put("createdAt", p.getCreatedAt().toString());
        return m;
    }
}

record GeofenceCheckInRequest(
    double latitude,
    double longitude,
    double accuracy,
    String powerMode
) {}
