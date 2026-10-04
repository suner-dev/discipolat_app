package com.discipolat.modules.tenants.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.tenants.domain.JoinCodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — résolution PUBLIQUE d'un code ou slug de
 * rejointure (page /join, lien vanity /j/&lt;slug&gt;, mobile).
 *
 * <p>Protocole anti-énumération identique à {@code /auth/registration-status}
 * (contrat §3.3) : rate-limit par IP dur (12/min), {@code Cache-Control:
 * no-store}, et réponse « vitrine » — nom de l'église et mode d'adhésion
 * uniquement, jamais de tenant_id, email ou donnée personnelle.</p>
 */
@RestController
@RequestMapping("/api/v1/public/join")
public class PublicJoinController {

    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final JoinCodeService joinCodeService;
    private final PerIpRateLimiter rateLimiter;

    public PublicJoinController(JoinCodeService joinCodeService, PerIpRateLimiter rateLimiter) {
        this.joinCodeService = joinCodeService;
        this.rateLimiter = rateLimiter;
    }

    public record LookupRequest(String code, String slug) {
    }

    /** Variante GET (déjà permitAll via /api/v1/public/**) pour les liens simples. */
    @GetMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolve(@RequestParam(required = false) String code,
                                                       @RequestParam(required = false) String slug,
                                                       HttpServletRequest httpRequest) {
        return lookupInternal(code, slug, httpRequest);
    }

    /** POST autorisé en public (cf. SecurityConfig) — la saisie ne transite pas en URL. */
    @PostMapping("/lookup")
    public ResponseEntity<Map<String, Object>> lookup(@RequestBody LookupRequest request,
                                                      HttpServletRequest httpRequest) {
        return lookupInternal(request.code(), request.slug(), httpRequest);
    }

    private ResponseEntity<Map<String, Object>> lookupInternal(String code, String slug,
                                                               HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeJoinLookup(clientIp);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .cacheControl(CacheControl.noStore())
                    .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                    .header(HEADER_RATE_LIMIT_REMAINING, "0")
                    .body(Map.of("found", false, "reason", "RATE_LIMITED"));
        }
        JoinCodeService.JoinLookup lookup = (code != null && !code.isBlank())
                ? joinCodeService.lookupByCode(code)
                : joinCodeService.lookupBySlug(slug);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("found", lookup.found());
        if (lookup.found()) {
            body.put("churchName", lookup.churchName());
            body.put("orgNodeLabel", lookup.orgNodeLabel());
            body.put("slug", lookup.slug());
            body.put("joinMode", lookup.joinMode() == null ? null : lookup.joinMode().name());
            body.put("requiresApproval", lookup.requiresApproval());
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(body);
    }
}
