package com.discipolat.modules.network.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.network.domain.NetworkDirectory;
import com.discipolat.modules.network.domain.NetworkDirectoryRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    private final PerIpRateLimiter rateLimiter;

    public PublicChurchesController(NetworkDirectoryRepository directoryRepository,
                                    TenantRepository tenantRepository,
                                    TenantSettingsRepository tenantSettingsRepository,
                                    PerIpRateLimiter rateLimiter) {
        this.directoryRepository = directoryRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.rateLimiter = rateLimiter;
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

    // ==================================================================
    // LOT 1 §GLISE-D'ABORD (T1.1) — ajouts pour le sélecteur d'église.
    // Ces deux endpoints sont NEUFS ; list() et le contrat /public/churches
    // ne bougent pas (A3/A4). Anti-énumération : rate-limit par IP +
    // Cache-Control no-store, et « non listée » répond identiquement à
    // « inexistante » (D2/R3).
    // ==================================================================

    /** Minimum de 2 caractères pour une suggestion (sinon trop d'ambiguïté / coût). */
    private static final int SUGGEST_MIN_Q = 2;
    /** Nombre maximum de suggestions renvoyées. */
    private static final int SUGGEST_LIMIT = 10;
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_ROBOTS_TAG = "X-Robots-Tag";

    /**
     * Suggestions de nom d'église pour l'auto-complétion du sélecteur : top 10 des
     * églises EN OPT-IN ANNUAIRE dont le nom contient {@code q} (insensible à la
     * casse). Projection MINIMALE ({@code name, slug?, city?, country?}) — jamais
     * les autres champs vitrine, jamais de PII ni de tenant_id.
     */
    @GetMapping("/suggest")
    public ResponseEntity<Map<String, Object>> suggest(
            @RequestParam(required = false, name = "q") String query,
            HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeChurchSuggest(clientIp);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .cacheControl(CacheControl.noStore())
                    .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                    .header(HEADER_RATE_LIMIT_REMAINING, "0")
                    .body(Map.of("total", 0, "items", List.of()));
        }
        List<Map<String, Object>> items = new ArrayList<>();
        if (query != null && query.trim().length() >= SUGGEST_MIN_Q) {
            List<NetworkDirectory> entries = directoryRepository
                    .findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(query.trim())
                    .stream().limit(SUGGEST_LIMIT).toList();
            Map<UUID, String> slugs = slugsFor(entries);
            Map<UUID, TenantSettings> settings = settingsFor(entries);
            for (NetworkDirectory e : entries) {
                items.add(toSuggestView(e, slugs.get(e.getTenantId()), settings.get(e.getTenantId())));
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", items.size());
        body.put("items", items);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(body);
    }

    /**
     * Test d'existence par NOM EXACT pour le sélecteur (« cette église existe-t-elle ? »).
     * Réponse binaire STRICTEMENT identique pour « inexistante » et « existante mais
     * non listée » (D2/R3) : on ne confirme que ce qui est déjà public. Si confirmé,
     * on renvoie le slug public et le nom affiché, rien de plus.
     */
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> exists(
            @RequestParam(required = false, name = "q") String query,
            HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeChurchExists(clientIp);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .cacheControl(CacheControl.noStore())
                    .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                    .header(HEADER_RATE_LIMIT_REMAINING, "0")
                    .body(Map.of("found", false));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            body.put("found", false);
        } else {
            List<NetworkDirectory> matches = directoryRepository
                    .findTop10ByIsListedTrueAndChurchNameIgnoreCaseOrderByChurchNameAsc(query.trim());
            if (matches.isEmpty()) {
                // Inexistante OU non listée : même réponse, aucune distinction (R3).
                body.put("found", false);
            } else {
                NetworkDirectory e = matches.get(0);
                Map<UUID, String> slugs = slugsFor(List.of(e));
                Map<UUID, TenantSettings> settings = settingsFor(List.of(e));
                body.put("found", true);
                String slug = slugs.get(e.getTenantId());
                if (slug != null && !slug.isBlank()) {
                    body.put("slug", slug);
                }
                body.put("name", displayName(e, settings.get(e.getTenantId())));
            }
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(body);
    }

    /**
     * LOT 2 §GLISE-D'ABORD (T2.2) — projection publique de la landing d'une église
     * ({@code GET /api/v1/public/churches/{slug}}). DOUBLE CONSENTEMENT RGPD (art. 9) :
     * ne répond 200 que si l'église est EN MÊME TEMPS listée à l'annuaire
     * ({@code isListed}) ET opt-in de sa page publique ({@code landingEnabled}).
     * Sinon {@code 404} STRICTEMENT indistinguable (slug inconnu / non listée /
     * landing éteinte) — aucun oracle d'existence (D2/R3). La réponse est une
     * liste blanche marketing : jamais {@code tenant_id}, e-mail, téléphone,
     * adresse, mentions fiscales, compteurs internes ni données membres (R2).
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> landing(
            @PathVariable String slug,
            HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeChurchLanding(clientIp);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .cacheControl(CacheControl.noStore())
                    .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                    .header(HEADER_RATE_LIMIT_REMAINING, "0")
                    .build();
        }
        // Triple 404 indistinguable : slug inconnu, non listé, ou landing éteinte.
        // Un seul chemin de retour « introuvable », corps et en-têtes identiques.
        ResponseEntity<Map<String, Object>> notFound = ResponseEntity.notFound()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_ROBOTS_TAG, "noindex, nofollow")
                .build();
        if (slug == null || slug.isBlank()) {
            return notFound;
        }
        Tenant tenant = tenantRepository.findBySlug(slug.trim()).orElse(null);
        if (tenant == null) {
            return notFound;
        }
        NetworkDirectory entry = directoryRepository.findByTenantId(tenant.getId()).orElse(null);
        boolean listed = entry != null && Boolean.TRUE.equals(entry.getIsListed());
        if (!listed) {
            return notFound;
        }
        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenant.getId()).orElse(null);
        if (ts == null || !Boolean.TRUE.equals(ts.getLandingEnabled())) {
            return notFound;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_ROBOTS_TAG, "noindex, nofollow")
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(toLandingView(tenant, entry, ts));
    }

    // ---- helpers additifs (partagés par /suggest et /exists, list() intacte) ----

    private Map<UUID, String> slugsFor(List<NetworkDirectory> entries) {
        List<UUID> tenantIds = entries.stream().map(NetworkDirectory::getTenantId).distinct().toList();
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        return tenantRepository.findAllById(tenantIds).stream()
                .filter(t -> t.getSlug() != null && !t.getSlug().isBlank())
                .collect(Collectors.toMap(Tenant::getId, Tenant::getSlug, (a, b) -> a));
    }

    private Map<UUID, TenantSettings> settingsFor(List<NetworkDirectory> entries) {
        List<UUID> tenantIds = entries.stream().map(NetworkDirectory::getTenantId).distinct().toList();
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        return tenantSettingsRepository.findAllForTenants(tenantIds).stream()
                .filter(ts -> ts.getTenant() != null && ts.getTenant().getId() != null)
                .collect(Collectors.toMap(ts -> ts.getTenant().getId(), ts -> ts, (a, b) -> a));
    }

    /** Nom affiché public : le nom marketing ({@code businessName}) prime sur le nom technique. */
    private static String displayName(NetworkDirectory e, TenantSettings ts) {
        if (ts != null && ts.getBusinessName() != null && !ts.getBusinessName().isBlank()) {
            return ts.getBusinessName();
        }
        return e.getChurchName();
    }

    /** Projection minimale pour l'auto-complétion : name, slug?, city?, country? uniquement. */
    static Map<String, Object> toSuggestView(NetworkDirectory e, String slug, TenantSettings ts) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("name", displayName(e, ts));
        if (slug != null && !slug.isBlank()) {
            v.put("slug", slug);
        }
        String city = e.getCity();
        String country = e.getCountry();
        if (ts != null) {
            if ((city == null || city.isBlank()) && ts.getCity() != null) city = ts.getCity();
            if ((country == null || country.isBlank()) && ts.getCountry() != null) country = ts.getCountry();
        }
        if (city != null && !city.isBlank()) v.put("city", city);
        if (country != null && !country.isBlank()) v.put("country", country);
        return v;
    }

    /**
     * LOT 2 §GLISE-D'ABORD (T2.2) — projection LISTE BLANCHE de la landing publique.
     * N'utilise que des champs marketing déjà destinés à la vitrine. Sont exclus,
     * et jamais lus ici : {@code tenant_id}, e-mail, téléphone, adresse, raison
     * sociale légale, mentions/numéro fiscal, CSS/HTML libres, config UI ou
     * d'intégration, compteurs internes, données membres (R2 / RGPD art. 9).
     */
    static Map<String, Object> toLandingView(Tenant tenant, NetworkDirectory entry, TenantSettings ts) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("slug", tenant.getSlug());
        v.put("name", displayName(entry, ts));
        v.put("landingEnabled", true);
        putIfPresent(v, "slogan", ts.getSlogan());
        putIfPresent(v, "description", ts.getDescription());
        putIfPresent(v, "logoUrl", ts.getLogoUrl());
        putIfPresent(v, "logoDarkUrl", ts.getLogoDarkUrl());
        putIfPresent(v, "coverUrl", ts.getCoverUrl());
        putIfPresent(v, "faviconUrl", ts.getFaviconUrl());
        putIfPresent(v, "website", ts.getWebsite());
        // Localité marketing (ville/pays), jamais l'adresse postale complète.
        putIfPresent(v, "city", ts.getCity() != null && !ts.getCity().isBlank() ? ts.getCity() : entry.getCity());
        putIfPresent(v, "country", ts.getCountry() != null && !ts.getCountry().isBlank() ? ts.getCountry() : entry.getCountry());
        putIfPresent(v, "locale", ts.getLocale());
        // Palette scopée (T2.4 lira ceci via applyScopedBranding, jamais le global).
        Map<String, Object> branding = new LinkedHashMap<>();
        putIfPresent(branding, "primaryColor", ts.getPrimaryColor());
        putIfPresent(branding, "secondaryColor", ts.getSecondaryColor());
        putIfPresent(branding, "accentColor", ts.getAccentColor());
        putIfPresent(branding, "surfaceColor", ts.getSurfaceColor());
        putIfPresent(branding, "backgroundColor", ts.getBackgroundColor());
        putIfPresent(branding, "textPrimaryColor", ts.getTextPrimaryColor());
        putIfPresent(branding, "textSecondaryColor", ts.getTextSecondaryColor());
        putIfPresent(branding, "primaryFont", ts.getPrimaryFont());
        putIfPresent(branding, "headingFont", ts.getHeadingFont());
        if (!branding.isEmpty()) {
            v.put("branding", branding);
        }
        // Composition demandée par l'église (nullable) : absente = sections par défaut.
        if (ts.getLandingSections() != null) {
            v.put("sections", ts.getLandingSections());
        }
        return v;
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
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
