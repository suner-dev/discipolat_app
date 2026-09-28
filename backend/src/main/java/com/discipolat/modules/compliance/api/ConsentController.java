package com.discipolat.modules.compliance.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.compliance.domain.ConsentLog;
import com.discipolat.modules.compliance.domain.ComplianceService;
import com.discipolat.modules.compliance.domain.LegalDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Consentements de l'utilisateur connecté : dépôt, retrait et historique
 * (preuve RGPD art. 7 — version du document + horodatage + trace technique).
 */
@RestController
@RequestMapping("/api/v1/compliance/consents")
public class ConsentController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("CGU", "PRIVACY", "CONSENT_ART9", "WHATSAPP", "MARKETING", "PHOTO");

    private final ComplianceService complianceService;
    private final LegalDocumentService legalDocumentService;

    public ConsentController(ComplianceService complianceService, LegalDocumentService legalDocumentService) {
        this.complianceService = complianceService;
        this.legalDocumentService = legalDocumentService;
    }

    public record ConsentRequest(
            @NotBlank(message = "type is required") String type,
            boolean granted,
            String policyVersion,
            String details) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> record(@Valid @RequestBody ConsentRequest request,
                                                      HttpServletRequest httpRequest) {
        String type = request.type().trim().toUpperCase();
        if (!ALLOWED_TYPES.contains(type)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Type de consentement inconnu", "allowed", ALLOWED_TYPES));
        }
        UUID userId = SecurityUtils.getCurrentUserId();
        String version = request.policyVersion();
        // Résoudre la version courante du document légal si le type y correspond
        if (version == null && LegalDocumentService.MANDATORY_CODES.contains(type)) {
            try {
                version = String.valueOf(legalDocumentService.getLatest(type, "fr").getVersion());
            } catch (RuntimeException ignored) {
                // document non publié → version laissée vide, le consentement reste journalisé
            }
        }
        complianceService.logConsent(userId, type, request.granted(), request.details(),
                version, PerIpRateLimiter.extractClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of(
                "message", request.granted() ? "Consentement enregistré" : "Consentement retiré",
                "type", type,
                "granted", request.granted(),
                "policyVersion", version != null ? version : ""));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Map<String, Object>>> myConsents() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> rows = complianceService.consentsOf(userId).stream()
                .map(this::toRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    private Map<String, Object> toRow(ConsentLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", log.getTypeConsentement());
        row.put("granted", log.isAccorde());
        row.put("policyVersion", log.getPolicyVersion());
        row.put("details", log.getDetails());
        row.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
        return row;
    }
}
