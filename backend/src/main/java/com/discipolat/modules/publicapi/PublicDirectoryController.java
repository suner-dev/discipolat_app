package com.discipolat.modules.publicapi;

import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * §G6.9 — Annuaire public « Églises sur Discipolat ».
 *
 * <p><b>Arbitrage du port Develop1.</b> Cette classe arrive de Develop1 telle quelle,
 * sauf son chemin : main exposait deja {@code GET /api/v1/public/churches} via
 * {@code PublicChurchesController} (source = annuaire reseau {@code NetworkDirectory},
 * reponse {@code {total, content}}). Deux {@code @GetMapping} sur le MEME chemin
 * produiraient une {@code Ambiguous mapping} bloquant le demarrage. La vitrine
 * Develop1 (source = {@code TenantSettings}, toggle {@code public_directory_enabled})
 * est donc publiee sur {@code /api/v1/public/directory}, et ses champs marketing
 * (slogan, logo, couverture, nom commercial) sont en plus fusionnes dans la réponse
 * de {@code /churches} — les deux capacites coexistent, aucune n'est retiree.</p>
 *
 * <p>Liste vitrine des églises ayant <b>explicitement opté</b> ({@code public_directory_enabled}
 * mis par le tenant admin) et dont le tenant est {@code ACTIVE}. Endpoint GET public
 * (sous {@code /api/v1/public/**} → {@code permitAll}, aucune donnée d'authentification
 * requise). Minimization stricte : seules des informations marketing publiques sont
 * exposées — jamais de contact privé, de membres, de finance ni de donnée pastorale.
 */
@RestController
@RequestMapping("/api/v1/public/directory")
public class PublicDirectoryController {

    private final TenantSettingsRepository tenantSettingsRepository;

    public PublicDirectoryController(TenantSettingsRepository tenantSettingsRepository) {
        this.tenantSettingsRepository = tenantSettingsRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam(required = false) String country) {
        List<Map<String, Object>> churches = tenantSettingsRepository.findPublicDirectoryEntries().stream()
                .filter(ts -> country == null || country.isBlank()
                        || country.equalsIgnoreCase(ts.getTenant().getCountry()))
                .map(this::toPublicCard)
                .toList();
        return ResponseEntity.ok(churches);
    }

    /** Fiche vitrine minimale — uniquement des champs destinés au public. */
    private Map<String, Object> toPublicCard(TenantSettings ts) {
        Tenant t = ts.getTenant();
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("slug", t.getSlug());
        card.put("name", ts.getBusinessName() != null && !ts.getBusinessName().isBlank()
                ? ts.getBusinessName() : t.getName());
        card.put("slogan", ts.getSlogan());
        card.put("description", ts.getDescription());
        card.put("logoUrl", ts.getLogoUrl());
        card.put("coverUrl", ts.getCoverUrl());
        card.put("city", ts.getCity() != null ? ts.getCity() : null);
        card.put("country", ts.getCountry() != null ? ts.getCountry() : t.getCountry());
        card.put("website", ts.getWebsite());
        return card;
    }
}
