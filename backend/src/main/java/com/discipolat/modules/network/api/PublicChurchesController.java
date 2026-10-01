package com.discipolat.modules.network.api;

import com.discipolat.modules.network.domain.NetworkDirectory;
import com.discipolat.modules.network.domain.NetworkDirectoryRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;

    public PublicChurchesController(NetworkDirectoryRepository directoryRepository,
                                    TenantRepository tenantRepository,
                                    TenantSettingsRepository tenantSettingsRepository) {
        this.directoryRepository = directoryRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
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
        // Le slug public vient du tenant (NetworkDirectory ne le stocke pas). Une
        // seule lecture en bloc, bornee aux entrees deja listees ; un tenant supprime
        // donne simplement une carte sans lien d'inscription.
        List<UUID> tenantIds = entries.stream().map(NetworkDirectory::getTenantId).distinct().toList();
        Map<UUID, String> slugs = tenantRepository.findAllById(tenantIds)
                .stream()
                .filter(t -> t.getSlug() != null && !t.getSlug().isBlank())
                .collect(Collectors.toMap(Tenant::getId, Tenant::getSlug, (a, b) -> a));
        // PORT Develop1 (§G6.9) — la fiche vitrine (slogan / logo / couverture) est
        // dans TenantSettings. Une seule lecture en bloc la aussi.
        Map<UUID, TenantSettings> settings = tenantSettingsRepository.findAllForTenants(tenantIds)
                .stream()
                .filter(ts -> ts.getTenant() != null && ts.getTenant().getId() != null)
                .collect(Collectors.toMap(ts -> ts.getTenant().getId(), ts -> ts, (a, b) -> a));
        List<Map<String, Object>> items = entries.stream()
                .map(e -> toPublicView(e, slugs.get(e.getTenantId()), settings.get(e.getTenantId())))
                .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", items.size());
        body.put("content", items);
        return ResponseEntity.ok(body);
    }

    /** Vue publique : champs vitrine uniquement — jamais de PII ni de tenant_id. */
    static Map<String, Object> toPublicView(NetworkDirectory e) {
        return toPublicView(e, null);
    }

    /**
     * Vue publique enrichie du slug. Le slug n'est PAS un identifiant interne :
     * c'est l'anse public utilise par les liens d'inscription (« s'inscrire AU NOM
     * d'une église », Develop1 §G3.1 : /register?tenant=<slug>). Le `tenant_id`
     * reste, lui, strictement exclus de cette reponse.
     */
    static Map<String, Object> toPublicView(NetworkDirectory e, String slug) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("name", e.getChurchName());
        if (slug != null && !slug.isBlank()) {
            v.put("slug", slug);
        }
        v.put("city", e.getCity());
        v.put("country", e.getCountry());
        v.put("denomination", e.getDenomination());
        v.put("website", e.getWebsite());
        v.put("description", e.getDescription());
        v.put("listedAt", e.getListedAt());
        return v;
    }

    /**
     * Vue publique enrichie du slug et des champs de vitrine Develop1 (§G6.9) :
     * slogan, logo et couverture proviennent des réglages du tenant, pas de
     * l'annuaire reseau. Rien de sensible ne transite — jamais email, telephone
     * ni adresse, et le nom marketing ({@code businessName}) prime sur le nom
     * technique quand il existe.
     */
    static Map<String, Object> toPublicView(NetworkDirectory e, String slug, TenantSettings ts) {
        Map<String, Object> v = toPublicView(e, slug);
        if (ts != null) {
            if (ts.getBusinessName() != null && !ts.getBusinessName().isBlank()) {
                v.put("name", ts.getBusinessName());
            }
            if (ts.getSlogan() != null && !ts.getSlogan().isBlank()) {
                v.put("slogan", ts.getSlogan());
            }
            if (ts.getLogoUrl() != null && !ts.getLogoUrl().isBlank()) {
                v.put("logoUrl", ts.getLogoUrl());
            }
            if (ts.getCoverUrl() != null && !ts.getCoverUrl().isBlank()) {
                v.put("coverUrl", ts.getCoverUrl());
            }
            if ((e.getCity() == null || e.getCity().isBlank()) && ts.getCity() != null) {
                v.put("city", ts.getCity());
            }
            if ((e.getCountry() == null || e.getCountry().isBlank()) && ts.getCountry() != null) {
                v.put("country", ts.getCountry());
            }
            if ((e.getWebsite() == null || e.getWebsite().isBlank()) && ts.getWebsite() != null) {
                v.put("website", ts.getWebsite());
            }
            if ((e.getDescription() == null || e.getDescription().isBlank()) && ts.getDescription() != null) {
                v.put("description", ts.getDescription());
            }
        }
        return v;
    }

    static Map<String, Object> toPublicView(NetworkDirectory e, String slug, java.util.Optional<TenantSettings> ts) {
        return toPublicView(e, slug, ts.orElse(null));
    }
}
