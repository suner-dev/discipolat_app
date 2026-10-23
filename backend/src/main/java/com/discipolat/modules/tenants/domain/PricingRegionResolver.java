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
        // Afrique centrale & de l'Ouest — zone CEMAC/UEMOA + Guinée/Ghana/Nigeria.
        // Tarif de référence en Dollars américains (canal Mobile Money + carte).
        for (String country : new String[]{
                "CM", "GA", "TD", "CG", "GQ", "CF", "CD", "ST",
                "CI", "SN", "ML", "BF", "NE", "TG", "BJ", "GW", "GM", "GH", "NG", "LR", "SL", "MR"}) {
            map.put(country, "AF");
        }
        // Afrique de l'Est — Corne de l'Afrique + Grands Lacs + océan Indien.
        for (String country : new String[]{"KE", "TZ", "UG", "RW", "ET", "SO", "DJ", "BI", "SS", "ER", "MU", "SC"}) {
            map.put(country, "AF_E");
        }
        // Afrique australe — SADC.
        for (String country : new String[]{"ZA", "ZW", "ZM", "MZ", "BW", "SZ", "LS", "MG", "AO", "NA"}) {
            map.put(country, "AF_S");
        }
        // Afrique du Nord — Maghreb + Égypte (devises à 2 décimales, canal carte naturel).
        for (String country : new String[]{"MA", "DZ", "TN", "LY", "EG", "SD"}) {
            map.put(country, "NA");
        }
        // Europe — zone euro : facturée en Euros.
        for (String country : new String[]{
                "FR", "DE", "ES", "IT", "PT", "NL", "BE", "LU", "AT", "IE", "FI", "GR",
                "SK", "SI", "HR", "CY", "MT", "EE", "LV", "LT", "SM", "VA", "AD", "MC"}) {
            map.put(country, "EU");
        }
        // Europe hors zone euro : facturée en Dollars américains.
        for (String country : new String[]{
                "GB", "CH", "SE", "NO", "DK", "PL", "CZ", "HU", "RO", "BG", "IS",
                "RS", "BA", "MK", "AL", "ME", "XK", "MD", "UA", "BY", "GE", "AM", "AZ", "TR"}) {
            map.put(country, "EU_D");
        }
        // Asie — Dollars américains.
        for (String country : new String[]{
                "CN", "IN", "ID", "PH", "VN", "JP", "KR", "SG", "MY", "TH", "MM", "KH", "LA",
                "NP", "BD", "LK", "PK", "MN", "TW", "HK", "MO", "BN", "TL", "KZ", "UZ",
                "TM", "TJ", "KG", "QA", "AE", "SA", "KW", "BH", "OM", "JO", "LB", "IQ", "IR", "YE"}) {
            map.put(country, "ASIA");
        }
        // Amérique du Nord — Dollars américains.
        for (String country : new String[]{"US", "CA", "MX", "GL"}) {
            map.put(country, "AM_N");
        }
        // Amérique du Sud + Amérique centrale & Caraïbes — Dollars américains.
        for (String country : new String[]{
                "BR", "AR", "CO", "PE", "CL", "VE", "EC", "UY", "PY", "BO", "GY", "SR",
                "GT", "HN", "SV", "NI", "CR", "PA", "CU", "DO", "HT", "JM", "TT", "BS",
                "BB", "BZ", "GTM", "PR", "SV"}) {
            map.put(country, "AM_S");
        }
        // Océanie — Dollars américains.
        map.put("AU", "OC");
        map.put("NZ", "OC");
        for (String country : new String[]{"FJ", "PG", "SB", "VU", "WS", "TO", "FM", "MH", "PW", "NR", "KI", "TV"}) {
            map.put(country, "OC");
        }
        // Alias historiques conservés pour ne rien casser (en-têtes/query déjà émis).
        map.put("UK", "EU_D");
        return Map.copyOf(map);
    }

    /**
     * Référentiel des régions tarifaires (clé → clé i18n {@code pricing.region.*}).
     *
     * <p>Grille monde demandée : Afrique centrale/ouest (AF), australe (AF_S), de
     * l'est (AF_E) et du Nord (NA) ; Asie (ASIA) ; Amériques Nord (AM_N) et Sud
     * (AM_S) — toutes en Dollars américains ; Europe zone euro (EU) en Euros et
     * Europe hors zone euro (EU_D) en Dollars. Les codes legacy (UK, US, CA,
     * LATAM, OC) restent reconnus pour la rétro-compatibilité.</p>
     */
    private static final Map<String, String> PRICING_REGIONS = Map.ofEntries(
            Map.entry("AF", "pricing.region.AF"),
            Map.entry("AF_E", "pricing.region.AF_E"),
            Map.entry("AF_S", "pricing.region.AF_S"),
            Map.entry("NA", "pricing.region.NA"),
            Map.entry("EU", "pricing.region.EU"),
            Map.entry("EU_D", "pricing.region.EU_D"),
            Map.entry("ASIA", "pricing.region.ASIA"),
            Map.entry("AM_N", "pricing.region.AM_N"),
            Map.entry("AM_S", "pricing.region.AM_S"),
            Map.entry("OC", "pricing.region.OC"),
            // Codes legacy toujours résolus (affichés si un appelant les emploie).
            Map.entry("UK", "pricing.region.EU_D"),
            Map.entry("US", "pricing.region.AM_N"),
            Map.entry("CA", "pricing.region.AM_N"),
            Map.entry("LATAM", "pricing.region.AM_S"));
}