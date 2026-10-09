package com.discipolat.common.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter using Bucket4j.
 * <p>
 * When a {@link LettuceBasedProxyManager} bean is available (Redis running), distributed
 * rate limiting is used. When Redis is unavailable, falls back to an in-memory
 * ConcurrentHashMap of Bucket4j buckets.
 */
@Service
public class PerIpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PerIpRateLimiter.class);

    @Value("${app.rate-limiting.login-capacity:10}")
    private int loginCapacity;
    @Value("${app.rate-limiting.login-refill:10}")
    private int loginRefill;
    @Value("${app.rate-limiting.login-period-minutes:1}")
    private int loginPeriodMinutes;

    @Value("${app.rate-limiting.refresh-capacity:20}")
    private int refreshCapacity;
    @Value("${app.rate-limiting.refresh-refill:20}")
    private int refreshRefill;
    @Value("${app.rate-limiting.refresh-period-minutes:1}")
    private int refreshPeriodMinutes;

    @Value("${app.rate-limiting.forgot-password-capacity:3}")
    private int forgotPasswordCapacity;
    @Value("${app.rate-limiting.forgot-password-refill:3}")
    private int forgotPasswordRefill;
    @Value("${app.rate-limiting.forgot-password-period-minutes:1}")
    private int forgotPasswordPeriodMinutes;

    @Value("${app.rate-limiting.reset-password-capacity:5}")
    private int resetPasswordCapacity;
    @Value("${app.rate-limiting.reset-password-refill:5}")
    private int resetPasswordRefill;
    @Value("${app.rate-limiting.reset-password-period-minutes:1}")
    private int resetPasswordPeriodMinutes;

    @Value("${app.rate-limiting.activate-capacity:5}")
    private int activateCapacity;
    @Value("${app.rate-limiting.activate-refill:5}")
    private int activateRefill;
    @Value("${app.rate-limiting.activate-period-minutes:1}")
    private int activatePeriodMinutes;

    @Value("${app.rate-limiting.change-password-capacity:5}")
    private int changePasswordCapacity;
    @Value("${app.rate-limiting.change-password-refill:5}")
    private int changePasswordRefill;
    @Value("${app.rate-limiting.change-password-period-minutes:1}")
    private int changePasswordPeriodMinutes;

    @Value("${app.rate-limiting.switch-role-capacity:30}")
    private int switchRoleCapacity;
    @Value("${app.rate-limiting.switch-role-refill:30}")
    private int switchRoleRefill;
    @Value("${app.rate-limiting.switch-role-period-minutes:1}")
    private int switchRolePeriodMinutes;

    @Value("${app.rate-limiting.demo-request-capacity:3}")
    private int demoRequestCapacity;
    @Value("${app.rate-limiting.demo-request-refill:3}")
    private int demoRequestRefill;
    @Value("${app.rate-limiting.demo-request-period-minutes:10}")
    private int demoRequestPeriodMinutes;

    @Value("${app.rate-limiting.register-capacity:5}")
    private int registerCapacity;
    @Value("${app.rate-limiting.register-refill:5}")
    private int registerRefill;
    @Value("${app.rate-limiting.register-period-minutes:1}")
    private int registerPeriodMinutes;

    // §G1.6 — Acceptation d'invitation publique : quota serré anti-abus (token public).
    @Value("${app.rate-limiting.invitation-accept-capacity:5}")
    private int invitationAcceptCapacity;
    @Value("${app.rate-limiting.invitation-accept-refill:5}")
    private int invitationAcceptRefill;
    @Value("${app.rate-limiting.invitation-accept-period-minutes:1}")
    private int invitationAcceptPeriodMinutes;

    // §3.3 : consultation du statut d'une demande d'inscription.
    // 3 requêtes / 5 minutes / IP : la page « Suivre ma demande » est publique
    // et/devrait devenir un oracle d'existence de compte si elle n'était pas bornée.
    @Value("${app.rate-limiting.registration-status-capacity:3}")
    private int registrationStatusCapacity;
    @Value("${app.rate-limiting.registration-status-refill:3}")
    private int registrationStatusRefill;
    @Value("${app.rate-limiting.registration-status-period-minutes:5}")
    private int registrationStatusPeriodMinutes;

    // Connexion par identité externe (Google / Microsoft). Endpoint public : le
    // credential est costly à produire mais l'endpoint est unauthentifié, donc
    // quota serré — sur le modèle de /auth/login (10/min) plutôt que du quota
    // d'invitation : ici l'utilisateur se reconnecte légitimement plusieurs fois
    // par heure.
    @Value("${app.rate-limiting.social-login-capacity:10}")
    private int socialLoginCapacity;
    @Value("${app.rate-limiting.social-login-refill:10}")
    private int socialLoginRefill;
    @Value("${app.rate-limiting.social-login-period-minutes:1}")
    private int socialLoginPeriodMinutes;

    // Rattachement d'une identité à un compte DÉJÀ connecté : même rare et
    // légitime, il modifie la sécurité du compte, donc encore plus borné.
    @Value("${app.rate-limiting.social-link-capacity:5}")
    private int socialLinkCapacity;
    @Value("${app.rate-limiting.social-link-refill:5}")
    private int socialLinkRefill;
    @Value("${app.rate-limiting.social-link-period-minutes:5}")
    private int socialLinkPeriodMinutes;

    // SPEC_ONBOARDING_FLOWS — lookup public d'un code/slug de rejointure :
    // anti-énumération des codes (12 req / min / IP), le code est court.
    @Value("${app.rate-limiting.join-lookup-capacity:12}")
    private int joinLookupCapacity;
    @Value("${app.rate-limiting.join-lookup-refill:12}")
    private int joinLookupRefill;
    @Value("${app.rate-limiting.join-lookup-period-minutes:1}")
    private int joinLookupPeriodMinutes;

    // LOT 1 §GLISE-D'ABORD (T1.1) — suggestions du sélecteur d'église (typeahead
    // public, branché sur /public/churches/suggest). L'appel est debouncé côté
    // client (300 ms) mais le champ est libre : quota moyen, borné en anti-
    // énumération. Il ne renvoie QUE des églises déjà en opt-in annuaire, donc
    // aucune donnée privée ne transite — mais le volume de requêtes est régulé.
    @Value("${app.rate-limiting.church-suggest-capacity:30}")
    private int churchSuggestCapacity;
    @Value("${app.rate-limiting.church-suggest-refill:30}")
    private int churchSuggestRefill;
    @Value("${app.rate-limiting.church-suggest-period-minutes:1}")
    private int churchSuggestPeriodMinutes;

    // LOT 1 §GLISE-D'ABORD (T1.1) — vérification d'existence par nom exact
    // (/public/churches/exists). C'est un oracle d'existence d'église (donnée
    // sensible au sens RGD, art. 9) : quota le plus serré des deux, réponse
    // binaire identique pour « inexistante » et « non listée » (cf. D2/R3).
    @Value("${app.rate-limiting.church-exists-capacity:12}")
    private int churchExistsCapacity;
    @Value("${app.rate-limiting.church-exists-refill:12}")
    private int churchExistsRefill;
    @Value("${app.rate-limiting.church-exists-period-minutes:1}")
    private int churchExistsPeriodMinutes;

    // LOT 2 §GLISE-D'ABORD (T2.2) — lecture de la landing publique d'une église
    // (/public/churches/{slug}). Ce n'est PAS un oracle d'existence : le 404 est
    // identique que la ressource soit absente, non listée ou en landing éteinte
    // (D2/R3). Quota plus large que les deux endpoints d'oracle : une page vitrine
    // se charge depuis des NAT partagés (visiteurs), il ne faut pas les bloquer.
    @Value("${app.rate-limiting.church-landing-capacity:60}")
    private int churchLandingCapacity;
    @Value("${app.rate-limiting.church-landing-refill:60}")
    private int churchLandingRefill;
    @Value("${app.rate-limiting.church-landing-period-minutes:1}")
    private int churchLandingPeriodMinutes;

    private final MeterRegistry meterRegistry;
    private final boolean usingRedis;
    private final LettuceBasedProxyManager<byte[]> redisProxyManager;
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    private Counter counterLoginTotal, counterRefreshTotal, counterForgotPasswordTotal;
    private Counter counterResetPasswordTotal, counterActivateTotal, counterChangePasswordTotal;
    private Counter counterSwitchRoleTotal;
    private Counter counterDemoRequestTotal;
    private Counter counterRegisterTotal;
    private Counter counterInvitationAcceptTotal;
    private Counter counterRegistrationStatusTotal;
    private Counter counterSocialLoginTotal;
    private Counter counterSocialLinkTotal;
    private Counter counterJoinLookupTotal;
    private Counter counterChurchSuggestTotal;
    private Counter counterChurchExistsTotal;
    private Counter counterChurchLandingTotal;
    private Counter counterLoginDenied, counterRefreshDenied, counterForgotPasswordDenied;
    private Counter counterResetPasswordDenied, counterActivateDenied, counterChangePasswordDenied;
    private Counter counterSwitchRoleDenied;
    private Counter counterDemoRequestDenied;
    private Counter counterRegisterDenied;
    private Counter counterInvitationAcceptDenied;
    private Counter counterRegistrationStatusDenied;
    private Counter counterSocialLoginDenied;
    private Counter counterSocialLinkDenied;
    private Counter counterJoinLookupDenied;
    private Counter counterChurchSuggestDenied;
    private Counter counterChurchExistsDenied;
    private Counter counterChurchLandingDenied;

    public PerIpRateLimiter(
            Optional<LettuceBasedProxyManager<byte[]>> redisProxyManager,
            MeterRegistry meterRegistry) {
        this.redisProxyManager = redisProxyManager.orElse(null);
        this.meterRegistry = meterRegistry;
        this.usingRedis = this.redisProxyManager != null;
    }

    @PostConstruct
    public void init() {
        registerMetrics();
        log.info("PerIpRateLimiter initialized — {} rate limiting",
                usingRedis ? "Redis-backed distributed" : "in-memory (no Redis)");
    }

    private void registerMetrics() {
        counterLoginTotal = buildCounter("login", "total");
        counterRefreshTotal = buildCounter("refresh", "total");
        counterForgotPasswordTotal = buildCounter("forgot_password", "total");
        counterResetPasswordTotal = buildCounter("reset_password", "total");
        counterActivateTotal = buildCounter("activate", "total");
        counterChangePasswordTotal = buildCounter("change_password", "total");
        counterSwitchRoleTotal = buildCounter("switch_role", "total");
        counterDemoRequestTotal = buildCounter("demo_request", "total");
        counterRegisterTotal = buildCounter("register", "total");
        counterInvitationAcceptTotal = buildCounter("invitation_accept", "total");
        counterRegistrationStatusTotal = buildCounter("registration_status", "total");
        counterSocialLoginTotal = buildCounter("social_login", "total");
        counterSocialLinkTotal = buildCounter("social_link", "total");
        counterJoinLookupTotal = buildCounter("join_lookup", "total");
        counterChurchSuggestTotal = buildCounter("church_suggest", "total");
        counterChurchExistsTotal = buildCounter("church_exists", "total");
        counterChurchLandingTotal = buildCounter("church_landing", "total");

        counterLoginDenied = buildCounter("login", "denied");
        counterRefreshDenied = buildCounter("refresh", "denied");
        counterForgotPasswordDenied = buildCounter("forgot_password", "denied");
        counterResetPasswordDenied = buildCounter("reset_password", "denied");
        counterActivateDenied = buildCounter("activate", "denied");
        counterChangePasswordDenied = buildCounter("change_password", "denied");
        counterSwitchRoleDenied = buildCounter("switch_role", "denied");
        counterDemoRequestDenied = buildCounter("demo_request", "denied");
        counterRegisterDenied = buildCounter("register", "denied");
        counterInvitationAcceptDenied = buildCounter("invitation_accept", "denied");
        counterRegistrationStatusDenied = buildCounter("registration_status", "denied");
        counterSocialLoginDenied = buildCounter("social_login", "denied");
        counterSocialLinkDenied = buildCounter("social_link", "denied");
        counterJoinLookupDenied = buildCounter("join_lookup", "denied");
        counterChurchSuggestDenied = buildCounter("church_suggest", "denied");
        counterChurchExistsDenied = buildCounter("church_exists", "denied");
        counterChurchLandingDenied = buildCounter("church_landing", "denied");
    }

    private Counter buildCounter(String endpoint, String result) {
        return Counter.builder("rate_limiter_requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .description("Rate limit " + result + " checks for " + endpoint)
                .register(meterRegistry);
    }

    // ======================== PUBLIC API ========================

    public RateLimitResult tryConsumeLogin(String ip) {
        return consume("login", loginCapacity, loginRefill, loginPeriodMinutes, ip,
                counterLoginTotal, counterLoginDenied);
    }

    public RateLimitResult tryConsumeRefresh(String ip) {
        return consume("refresh", refreshCapacity, refreshRefill, refreshPeriodMinutes, ip,
                counterRefreshTotal, counterRefreshDenied);
    }

    public RateLimitResult tryConsumeForgotPassword(String ip) {
        return consume("forgot_password", forgotPasswordCapacity, forgotPasswordRefill, forgotPasswordPeriodMinutes, ip,
                counterForgotPasswordTotal, counterForgotPasswordDenied);
    }

    public RateLimitResult tryConsumeResetPassword(String ip) {
        return consume("reset_password", resetPasswordCapacity, resetPasswordRefill, resetPasswordPeriodMinutes, ip,
                counterResetPasswordTotal, counterResetPasswordDenied);
    }

    public RateLimitResult tryConsumeActivate(String ip) {
        return consume("activate", activateCapacity, activateRefill, activatePeriodMinutes, ip,
                counterActivateTotal, counterActivateDenied);
    }

    public RateLimitResult tryConsumeChangePassword(String ip) {
        return consume("change_password", changePasswordCapacity, changePasswordRefill, changePasswordPeriodMinutes, ip,
                counterChangePasswordTotal, counterChangePasswordDenied);
    }

    public RateLimitResult tryConsumeSwitchRole(String ip) {
        return consume("switch_role", switchRoleCapacity, switchRoleRefill, switchRolePeriodMinutes, ip,
                counterSwitchRoleTotal, counterSwitchRoleDenied);
    }

    /** Demandes de démonstration (endpoint public landing) — quota serré anti-spam. */
    public RateLimitResult tryConsumeDemoRequest(String ip) {
        return consume("demo_request", demoRequestCapacity, demoRequestRefill, demoRequestPeriodMinutes, ip,
                counterDemoRequestTotal, counterDemoRequestDenied);
    }

    /** §G1.6 — Acceptation d'invitation (endpoint public) : 5 req / minute / IP. */
    public RateLimitResult tryConsumeInvitationAccept(String ip) {
        return consume("invitation_accept", invitationAcceptCapacity, invitationAcceptRefill, invitationAcceptPeriodMinutes, ip,
                counterInvitationAcceptTotal, counterInvitationAcceptDenied);
    }

    /** Inscriptions publiques — quota serré anti-spam par IP. */
    public RateLimitResult tryConsumeRegister(String ip) {
        return consume("register", registerCapacity, registerRefill, registerPeriodMinutes, ip,
                counterRegisterTotal, counterRegisterDenied);
    }

    /**
     * Consultation du statut d'une demande d'inscription (endpoint public) :
     * 3 requêtes / 5 minutes / IP (contrat §3.3).
     */
    /** Quota de connexion par identite externe (Google / Microsoft). */
    public RateLimitResult tryConsumeSocialLogin(String ip) {
        return consume("social_login", socialLoginCapacity, socialLoginRefill, socialLoginPeriodMinutes, ip,
                counterSocialLoginTotal, counterSocialLoginDenied);
    }

    /** Quota de rattachement d'une identite externe a un compte connecte. */
    public RateLimitResult tryConsumeSocialLink(String ip) {
        return consume("social_link", socialLinkCapacity, socialLinkRefill, socialLinkPeriodMinutes, ip,
                counterSocialLinkTotal, counterSocialLinkDenied);
    }

    public RateLimitResult tryConsumeRegistrationStatus(String ip) {
        return consume("registration_status",
                registrationStatusCapacity, registrationStatusRefill, registrationStatusPeriodMinutes, ip,
                counterRegistrationStatusTotal, counterRegistrationStatusDenied);
    }

    /** SPEC_ONBOARDING_FLOWS — lookup public code/slug : 12 req / min / IP. */
    public RateLimitResult tryConsumeJoinLookup(String ip) {
        return consume("join_lookup",
                joinLookupCapacity, joinLookupRefill, joinLookupPeriodMinutes, ip,
                counterJoinLookupTotal, counterJoinLookupDenied);
    }

    /**
     * LOT 1 §GLISE-D'ABORD (T1.1) — suggestions publiques du sélecteur d'église
     * ({@code /public/churches/suggest}) : 30 req / min / IP.
     */
    public RateLimitResult tryConsumeChurchSuggest(String ip) {
        return consume("church_suggest",
                churchSuggestCapacity, churchSuggestRefill, churchSuggestPeriodMinutes, ip,
                counterChurchSuggestTotal, counterChurchSuggestDenied);
    }

    /**
     * LOT 1 §GLISE-D'ABORD (T1.1) — test d'existence par nom exact
     * ({@code /public/churches/exists}) : 12 req / min / IP. Oracle d'existence
     * d'un lieu de culte, donc quota serré (cf. R3 anti-énumération).
     */
    public RateLimitResult tryConsumeChurchExists(String ip) {
        return consume("church_exists",
                churchExistsCapacity, churchExistsRefill, churchExistsPeriodMinutes, ip,
                counterChurchExistsTotal, counterChurchExistsDenied);
    }

    /**
     * LOT 2 §GLISE-D'ABORD (T2.2) — lecture d'une landing publique
     * ({@code /public/churches/{slug}}) : 60 req / min / IP. Le 404 uniforme
     * (absente / non listée / landing éteinte) retire tout effet d'oracle ;
     * le quota borne l'aval, pas la confidentialité.
     */
    public RateLimitResult tryConsumeChurchLanding(String ip) {
        return consume("church_landing",
                churchLandingCapacity, churchLandingRefill, churchLandingPeriodMinutes, ip,
                counterChurchLandingTotal, counterChurchLandingDenied);
    }

    /**
     * Adresse IP cliente <b>de confiance</b>, pour le rate-limiting.
     *
     * <p><b>SPEC ORGANISATION DENOMINATION V2 §7.0 / T-B0ter (faille F18).</b>
     * L'implémentation précédente prenait {@code X-Forwarded-For.split(",")[0]},
     * c'est-à-dire la valeur la plus à gauche — <b>choisie par le client</b>. Or
     * un reverse-proxy (Render, nginx) <b>ajoute</b> la chaîne : il n'écrase
     * pas ce que l'appelant a envoyé. Un en-tête forgé donnait donc un seau
     * neuf à chaque requête, rendant le rate-limit de {@code join_lookup}
     * (12/min/IP) sans effet et l'oracle d'énumération des codes exploitable.
     *
     * <p>On lit désormais la valeur la plus à <b>droite</b> : c'est celle
     * qu'a ajoutée le dernier proxy de confiance, donc celle qui n'est pas
     * sous le contrôle du client.
     *
     * <p><b>Précondition à vérifier en infrastructure</b> : ce correctif n'est
     * sûr que si le proxy de confiance est le dernier à écrire l'en-tête. Si
     * l'application est exposée <b>sans</b> proxy (accès direct au port),
     * {@code X-Forwarded-For} reste forgeable : dans ce cas seul
     * {@link HttpServletRequest#getRemoteAddr()} est fiable. La
     * configuration de déploiement est donc partie du correctif, pas une
     * détail — voir {@code infra/nginx/nginx.conf} et les en-têtes Render
     * (cf. T-B0ter, case « Config nginx vérifiée et documentée »).
     */
    public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            // On prend la DROITE de la chaîne : c'est l'entrée ajoutée par le
            // proxy de confiance, pas celle fournie par l'appelant.
            String[] hops = xff.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String candidate = hops[i].trim();
                if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    // ======================== INTERNAL ========================

    private RateLimitResult consume(String endpoint, int capacity, int refillTokens, int periodMinutes,
                                      String ip, Counter counterTotal, Counter counterDenied) {
        counterTotal.increment();

        String bucketKey = endpoint + ":" + ip;
        Refill refill = Refill.greedy(refillTokens, Duration.ofMinutes(periodMinutes));
        Bandwidth limit = Bandwidth.classic(capacity, refill);

        try {
            Bucket bucket;
            if (usingRedis) {
                byte[] key = bucketKey.getBytes(StandardCharsets.UTF_8);
                bucket = redisProxyManager.builder()
                        .build(key, BucketConfiguration.builder().addLimit(limit).build());
            } else {
                bucket = localBuckets.computeIfAbsent(bucketKey,
                        k -> Bucket.builder().addLimit(limit).build());
            }

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return RateLimitResult.allowed(probe.getRemainingTokens());
            }
            counterDenied.increment();
            return RateLimitResult.denied(probe.getNanosToWaitForRefill());
        } catch (Exception e) {
            log.warn("Rate limiting error for {}: {}", bucketKey, e.getMessage());
            return RateLimitResult.allowed(999);
        }
    }
}
