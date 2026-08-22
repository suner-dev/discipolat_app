package com.discipolat.modules.twin.api;

import com.discipolat.modules.twin.domain.DigitalTwinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/twin")
public class DigitalTwinController {

    private final DigitalTwinService service;

    public DigitalTwinController(DigitalTwinService service) {
        this.service = service;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> snapshot() {
        return ResponseEntity.ok(service.snapshot());
    }

    /** Simulateur « et si » : projections de croissance selon les hypothèses. */
    @PostMapping("/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> simulate(@RequestBody Map<String, Object> body) {
        double faiseurMultiplier = doubleOf(body.get("faiseurMultiplier"), 1.0);
        int retentionGain = intOf(body.get("retentionGain"), 0);
        double pipelineBoost = doubleOf(body.get("pipelineBoost"), 1.0);
        int months = intOf(body.get("months"), 12);
        return ResponseEntity.ok(service.simulate(faiseurMultiplier, retentionGain, pipelineBoost, months));
    }

    private static double doubleOf(Object v, double fallback) {
        if (v instanceof Number n) return n.doubleValue();
        try {
            return v != null ? Double.parseDouble(v.toString()) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int intOf(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue();
        try {
            return v != null ? Integer.parseInt(v.toString()) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
