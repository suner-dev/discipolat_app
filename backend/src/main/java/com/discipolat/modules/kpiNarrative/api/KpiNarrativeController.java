package com.discipolat.modules.kpiNarrative.api;

import com.discipolat.modules.kpiNarrative.domain.KpiNarrative;
import com.discipolat.modules.kpiNarrative.domain.KpiNarrativeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kpi-narrative")
public class KpiNarrativeController {

    private final KpiNarrativeService service;

    public KpiNarrativeController(KpiNarrativeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<KpiNarrative>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<KpiNarrative>> listByType(@PathVariable String type) {
        return ResponseEntity.ok(service.listByType(KpiNarrative.TypeKPI.valueOf(type)));
    }

    @GetMapping("/période/{période}")
    public ResponseEntity<List<KpiNarrative>> listByPériode(@PathVariable String période) {
        return ResponseEntity.ok(service.listByPériode(période));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<KpiNarrative> generate(@RequestBody Map<String, Object> body) {
        KpiNarrative.TypeKPI type = KpiNarrative.TypeKPI.valueOf((String) body.get("typeKPI"));
        double actuel = body.get("valeurActuelle") != null ? ((Number) body.get("valeurActuelle")).doubleValue() : 0;
        double précédent = body.get("valeurPrécédente") != null ? ((Number) body.get("valeurPrécédente")).doubleValue() : 0;
        UUID deptId = body.get("départementId") != null ? UUID.fromString((String) body.get("départementId")) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = body.get("contexte") instanceof Map ? (Map<String, Object>) body.get("contexte") : Map.of();
        return ResponseEntity.ok(service.generate(type, actuel, précédent, deptId, ctx));
    }

    @PostMapping("/generate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<KpiNarrative>> generateAll(@RequestBody Map<String, Object[]> body) {
        Map<KpiNarrative.TypeKPI, double[]> kpisData = new java.util.HashMap<>();
        for (Map.Entry<String, Object[]> entry : body.entrySet()) {
            try {
                KpiNarrative.TypeKPI type = KpiNarrative.TypeKPI.valueOf(entry.getKey());
                Object[] vals = entry.getValue();
                double[] doubles = new double[Math.min(vals.length, 2)];
                for (int i = 0; i < doubles.length; i++) {
                    doubles[i] = vals[i] instanceof Number ? ((Number) vals[i]).doubleValue() : 0;
                }
                kpisData.put(type, doubles);
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(service.generateAll(kpisData));
    }
}
