package com.discipolat.modules.network.api;

import com.discipolat.modules.network.domain.NetworkDirectory;
import com.discipolat.modules.network.domain.NetworkDirectoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * G6.9 — Annuaire public « Églises sur Discipolat ».
 *
 * <p>Endpoint 100 % public (permitAll via /api/v1/public/**) : ne retourne QUE
 * les églises ayant explicitement opt-in ({@code is_listed = true}), et
 * uniquement les champs vitrine non sensibles : nom, ville, pays,
 * dénomination, site web, description. Sont EXCLUS : emails, téléphones,
 * pasteur, coordonnées GPS précises, tenant_id, compteurs internes.</p>
 *
 * <p>La publication reste contrôlée par l'église elle-même via le toggle
 * authentifié {@code POST /api/v1/network/directory/mine/listing}
 * (opt-in individuel, réversible à tout moment).</p>
 */
@RestController
@RequestMapping("/api/v1/public/churches")
public class PublicChurchesController {

    private final NetworkDirectoryRepository directoryRepository;

    public PublicChurchesController(NetworkDirectoryRepository directoryRepository) {
        this.directoryRepository = directoryRepository;
    }

    /** Liste vitrine des églises opt-in (tri alphabétique). */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String country,
            @RequestParam(required = false, name = "q") String query) {
        List<NetworkDirectory> entries;
        if (query != null && !query.isBlank()) {
            entries = directoryRepository
                    .findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(query.trim());
        } else if (country != null && !country.isBlank()) {
            entries = directoryRepository
                    .findByIsListedTrueAndCountryOrderByChurchNameAsc(country.trim());
        } else {
            entries = directoryRepository.findByIsListedTrueOrderByChurchNameAsc();
        }
        List<Map<String, Object>> items = entries.stream()
                .map(PublicChurchesController::toPublicView)
                .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", items.size());
        body.put("content", items);
        return ResponseEntity.ok(body);
    }

    /** Vue publique : champs vitrine uniquement — jamais de PII ni de tenant_id. */
    static Map<String, Object> toPublicView(NetworkDirectory e) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("name", e.getChurchName());
        v.put("city", e.getCity());
        v.put("country", e.getCountry());
        v.put("denomination", e.getDenomination());
        v.put("website", e.getWebsite());
        v.put("description", e.getDescription());
        v.put("listedAt", e.getListedAt());
        return v;
    }
}
