package com.discipolat.modules.map.api;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Kingdom Mapping — cartographie territoriale.
 *
 * Heatmap de densité des âmes : grille géographique (cellules ~0,005° ≈ 500 m)
 * + agrégats par zone nommée. Alimente les cartes à chaleur web et mobile.
 */
@RestController
@RequestMapping("/api/v1/map")
public class KingdomMappingController {

    private static final double CELL_SIZE = 0.005; // ≈ 500 m

    private final SoulRepository soulRepository;

    public KingdomMappingController(SoulRepository soulRepository) {
        this.soulRepository = soulRepository;
    }

    /** Heatmap : cellules de densité avec intensité 0-100 par type de disciple. */
    @GetMapping("/heatmap")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> heatmap(
            @RequestParam(required = false) String typeDisciple) {
        List<Soul> souls = soulRepository.findByDeletedFalseAndLatitudeIsNotNullAndLongitudeIsNotNull()
                .stream()
                .filter(s -> typeDisciple == null || s.getTypeDisciple() != null
                        && s.getTypeDisciple().name().equalsIgnoreCase(typeDisciple))
                .toList();

        // Clustering en cellules
        Map<String, List<Soul>> cells = new LinkedHashMap<>();
        for (Soul soul : souls) {
            String key = cellKey(soul.getLatitude(), soul.getLongitude());
            cells.computeIfAbsent(key, k -> new ArrayList<>()).add(soul);
        }

        int maxDensity = cells.values().stream().mapToInt(List::size).max().orElse(1);

        List<Map<String, Object>> heat = new ArrayList<>();
        for (Map.Entry<String, List<Soul>> e : cells.entrySet()) {
            List<Soul> cellSouls = e.getValue();
            Soul first = cellSouls.get(0);
            Map<StatutAme, Long> byStatut = cellSouls.stream()
                    .collect(Collectors.groupingBy(Soul::getStatut, Collectors.counting()));

            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("cell", e.getKey());
            cell.put("centerLat", round3(first.getLatitude()));
            cell.put("centerLng", round3(first.getLongitude()));
            cell.put("count", cellSouls.size());
            cell.put("intensity", Math.round(cellSouls.size() * 100.0 / maxDensity));
            cell.put("byStatut", byStatut.entrySet().stream()
                    .collect(Collectors.toMap(x -> x.getKey().name(), Map.Entry::getValue)));
            cell.put("zones", cellSouls.stream()
                    .map(Soul::getZone).filter(Objects::nonNull).distinct().limit(5).toList());
            heat.add(cell);
        }
        return ResponseEntity.ok(heat);
    }

    /** Secteurs : découpage par zone nommée avec stats d'occupation. */
    @GetMapping("/sectors")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> sectors() {
        List<Soul> souls = soulRepository.findByDeletedFalse();

        Map<String, List<Soul>> byZone = souls.stream()
                .filter(s -> s.getZone() != null && !s.getZone().isBlank())
                .collect(Collectors.groupingBy(Soul::getZone));

        List<Map<String, Object>> sectors = new ArrayList<>();
        for (Map.Entry<String, List<Soul>> e : byZone.entrySet()) {
            List<Soul> zoneSouls = e.getValue();
            long actifs = zoneSouls.stream().filter(s -> s.getStatut() == StatutAme.ACTIF).count();
            long decroches = zoneSouls.stream().filter(s -> s.getStatut() == StatutAme.DECROCHE).count();
            double avgLat = zoneSouls.stream().filter(s -> s.getLatitude() != null)
                    .mapToDouble(Soul::getLatitude).average().orElse(0);
            double avgLng = zoneSouls.stream().filter(s -> s.getLongitude() != null)
                    .mapToDouble(Soul::getLongitude).average().orElse(0);

            Map<String, Object> sector = new LinkedHashMap<>();
            sector.put("zone", e.getKey());
            sector.put("total", zoneSouls.size());
            sector.put("actifs", actifs);
            sector.put("decroches", decroches);
            sector.put("healthPercent", zoneSouls.isEmpty() ? 0
                    : Math.round((zoneSouls.size() - decroches) * 100.0 / zoneSouls.size()));
            if (avgLat != 0 || avgLng != 0) {
                sector.put("centerLat", round4(avgLat));
                sector.put("centerLng", round4(avgLng));
            }
            sectors.add(sector);
        }
        sectors.sort((a, b) -> Integer.compare((int) b.get("total"), (int) a.get("total")));
        return ResponseEntity.ok(sectors);
    }

    private static String cellKey(Double lat, Double lng) {
        long latCell = Math.round(lat / CELL_SIZE);
        long lngCell = Math.round(lng / CELL_SIZE);
        return latCell + ":" + lngCell;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
