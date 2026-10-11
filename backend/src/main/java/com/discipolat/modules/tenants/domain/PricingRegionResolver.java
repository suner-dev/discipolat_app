package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.enums.PricingRegionSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Résolution de la région tarifaire (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T2.4).
 *
 * <p>Ordre strict (contrat §2.2) : en-tête de confiance → paramètre d'URL →
 * région du tenant → région par défaut.</p>
 *
 * <p><b>Deux gardes-fous délibérés.</b> L'en-tête {@code X-Discipolat-Region} n'est
 * accepté que si {@code trust_region_header} est activé : un navigateur en
 * cross-origin ne peut pas le forger, mais un reverse-proxy peut, et dans un SaaS
 * multi-tenant un client qui choisirait lui-même sa région choisirait du coup son
 * tarif. De même, le paramètre {@code ?region=} n'est lu que si
 * {@code allow_region_query_override} est actif — et il ne sert qu'à l'<b>affichage</b>
 * (comparer deux devis), jamais à fixer le prix facturé.</p>
 *
 * <p>La géolocalisation par IP n'est pas implémentée en v1 (dépendance externe et
 * charge RGPD disproportionnés pour un gain marginal sur une grille tarifaire).</p>
 */
@Component
public class PricingRegionResolver {

    private static final Map<String, String> COUNTRY_TO_REGION = buildCountryMap();

    private final PricingSettingsService settingsService;

    public PricingRegionResolver(PricingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * @param request       requête courante, ou {@code null} hors contexte web
     * @param queryRegion   région demandée explicitement ({@code ?region=}), ou {@code null}
     * @param tenantCountry pays du tenant, ou {@code null}
     * @return région normalisée, ou {@link Optional#empty()} si aucune source ne répond
     */
    public Optional<String> resolve(HttpServletRequest request, String queryRegion, String tenantCountry) {
        SaasPlanPricingSettings settings = settingsService.current();

        if (settings.getRegionResolution() == PricingRegionSource.HEADER
                && Boolean.TRUE.equals(settings.getTrustRegionHeader())
                && request != null) {
            String headerName = settings.getRegionHeaderName();
            if (headerName != null && !headerName.isBlank()) {
                String fromHeader = normalize(request.getHeader(headerName));
                if (fromHeader != null && isKnownRegion(fromHeader)) {
                    return Optional.of(fromHeader);
                }
            }
        }

        if (Boolean.TRUE.equals(settings.getAllowRegionQueryOverride())
                && settings.getRegionResolution() != PricingRegionSource.TENANT) {
            String fromQuery = normalize(queryRegion);
            if (fromQuery != null && isKnownRegion(fromQuery)) {
                return Optional.of(fromQuery);
            }
        }

        String fromTenant = normalize(regionOfCountry(tenantCountry));
        if (fromTenant != null) {
            return Optional.of(fromTenant);
        }

        String fallback = normalize(settings.getDefaultRegion());
        return fallback != null ? Optional.of(fallback) : Optional.empty();
    }

    /** Région par défaut du paramétrage, sans coût de résolution. */
    public Optional<String> defaultRegion() {
        String region = normalize(settingsService.current().getDefaultRegion());
        return region == null ? Optional.empty() : Optional.of(region);
    }

    /** Région déduite du pays (ISO-3166 alpha-2, insensible à la casse). */
    public static String regionOfCountry(String country) {
        if (country == null || country.isBlank()) return null;
        String code = country.trim().toUpperCase(Locale.ROOT);
        if (code.length() == 3) {
            // Un tenant peut déclarer une région directement (ex. « AF »).
            return isKnownRegion(code) ? code : null;
        }
        return COUNTRY_TO_REGION.get(code);
    }

    /** Le code est-il une région connue du référentiel ? */
    public static boolean isKnownRegion(String region) {
        return region != null && PRICING_REGIONS.containsKey(normalize(region));
    }

    /** Clés i18n des régions, pour que le client ne réinvente pas les libellés. */
    public static Map<String, String> regions() {
        return PRICING_REGIONS;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String code = raw.trim().toUpperCase(Locale.ROOT);
        return code.isEmpty() ? null : code;
    }

    private static Map<String, String> buildCountryMap() {
        Map<String, String> map = new LinkedHashMap<>();
        // Afrique de l'Ouest — zone CEMAC/UEM, devise XAF/XOF.
        for (String country : new String[]{"CM", "GA", "TD", "CG", "GQ", "BI"}) map.put(country, "AF");
        for (String country : new String[]{"CI", "SN", "ML", "BF", "NE", "TG", "BJ", "GW", "GM", "GH", "NG", "LR", "SL"}) {
            map.put(country, "AF");
        }
        // Afrique du Nord / Est — devis à 2 décimales, canal carte plus naturel.
        for (String country : new String[]{"MA", "DZ", "TN", "LY", "EG", "SD", "ZA", "KE", "TZ", "UG", "RW", "ET"}) {
            map.put(country, "NA");
        }
        for (String country : new String[]{"FR", "DE", "ES", "IT", "PT", "NL", "BE", "LU", "AT", "IE", "CH", "DK", "SE", "NO", "FI", "GR", "PL", "CZ", "SK", "HU", "RO", "BG", "HR", "SI", "SK"}) {
            map.put(country, "EU");
        }
        map.put("GB", "UK");
        map.put("US", "US");
        map.put("CA", "CA");
        map.put("MX", "LATAM");
        map.put("BR", "LATAM");
        map.put("AR", "LATAM");
        map.put("CO", "LATAM");
        map.put("CN", "ASIA");
        map.put("IN", "ASIA");
        map.put("ID", "ASIA");
        map.put("PH", "ASIA");
        map.put("VN", "ASIA");
        map.put("AU", "OC");
        map.put("NZ", "OC");
        return Map.copyOf(map);
    }

    /** Référentiel des régions tarifaires (clé → clé i18n {@code pricing.region.*}). */
    private static final Map<String, String> PRICING_REGIONS = Map.ofEntries(
            Map.entry("AF", "pricing.region.AF"),
            Map.entry("NA", "pricing.region.NA"),
            Map.entry("EU", "pricing.region.EU"),
            Map.entry("UK", "pricing.region.UK"),
            Map.entry("US", "pricing.region.US"),
            Map.entry("CA", "pricing.region.CA"),
            Map.entry("LATAM", "pricing.region.LATAM"),
            Map.entry("ASIA", "pricing.region.ASIA"),
            Map.entry("OC", "pricing.region.OC"));
}