package com.discipolat.modules.facerec.api;

import com.discipolat.modules.facerec.domain.FaceRecognitionService;
import com.discipolat.modules.facerec.domain.FaceTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pointage par reconnaissance faciale — enrôlement et identification.
 *
 * Les photos transitent en base64 dans le corps JSON et ne sont JAMAIS
 * stockées : seule l'empreinte perceptuelle 256 bits est conservée.
 */
@RestController
@RequestMapping("/api/v1/face")
public class FaceRecognitionController {

    private final FaceRecognitionService service;

    public FaceRecognitionController(FaceRecognitionService service) {
        this.service = service;
    }

    /** Enrôle le visage d'un utilisateur (mise à jour si déjà enrôlé). */
    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<FaceTemplate> enroll(@RequestBody EnrollRequest body) throws Exception {
        return ResponseEntity.ok(service.enroll(
                body.userId(),
                body.soulId(),
                body.displayName(),
                decode(body.imageBase64())));
    }

    /** Identifie un visage parmi les gabarits actifs. */
    @PostMapping("/identify")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> identify(@RequestBody IdentifyRequest body) throws Exception {
        FaceRecognitionService.IdentificationResult result =
                service.identify(decode(body.imageBase64()));
        FaceTemplate t = result.template();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("matched", result.matched());
        payload.put("confidence", Math.round(result.confidence() * 1000) / 1000.0);
        payload.put("message", result.message());
        if (t != null) {
            payload.put("templateId", t.getId());
            payload.put("userId", t.getUserId());
            payload.put("soulId", t.getSoulId());
            payload.put("displayName", t.getDisplayName());
        }
        return ResponseEntity.ok(payload);
    }

    /** Liste des gabarits enrôlés (recherche facultative par nom). */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<FaceTemplate>> templates(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.list(q));
    }

    /** Statistiques d'enrôlement. */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }

    /** Désactive un gabarit (droit à l'effacement RGPD). */
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** P13 — Batch enrollment : enrôle plusieurs visages en une requête. */
    @PostMapping("/enroll-batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> enrollBatch(@RequestBody List<EnrollRequest> bodies) {
        int success = 0;
        int failed = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (EnrollRequest body : bodies) {
            try {
                FaceTemplate t = service.enroll(body.userId(), body.soulId(), body.displayName(), decode(body.imageBase64()));
                results.add(Map.of("displayName", body.displayName, "status", "OK", "templateId", t.getId()));
                success++;
            } catch (Exception e) {
                results.add(Map.of("displayName", body.displayName, "status", "ERROR", "error", e.getMessage()));
                failed++;
            }
        }
        return ResponseEntity.ok(Map.of("total", bodies.size(), "success", success, "failed", failed, "details", results));
    }

    /** P13 — Identifie avec seuil de confiance configurable. */
    @PostMapping("/identify-configurable")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> identifyConfigurable(
            @RequestBody IdentifyWithThresholdRequest body) throws Exception {
        FaceRecognitionService.IdentificationResult result =
                service.identify(decode(body.imageBase64()));
        double effectiveConfidence = result.confidence();
        boolean matched = effectiveConfidence >= body.minConfidence();

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("matched", matched);
        payload.put("confidence", Math.round(effectiveConfidence * 1000) / 1000.0);
        payload.put("threshold", body.minConfidence());
        payload.put("message", matched ? result.message() : "Confiance insuffisante (" + Math.round(effectiveConfidence * 100) + "% < " + Math.round(body.minConfidence() * 100) + "%)");
        if (result.template() != null && matched) {
            payload.put("templateId", result.template().getId());
            payload.put("userId", result.template().getUserId());
            payload.put("displayName", result.template().getDisplayName());
        }
        return ResponseEntity.ok(payload);
    }

    private static byte[] decode(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("imageBase64 est obligatoire");
        }
        // Tolère le préfixe data:image/jpeg;base64,
        int comma = base64.indexOf(',');
        String raw = comma >= 0 ? base64.substring(comma + 1) : base64;
        return Base64.getDecoder().decode(raw);
    }

    public record EnrollRequest(UUID userId, UUID soulId, String displayName, String imageBase64) {
    }

    public record IdentifyRequest(String imageBase64) {
    }

    public record IdentifyWithThresholdRequest(String imageBase64, double minConfidence) {
    }
}
