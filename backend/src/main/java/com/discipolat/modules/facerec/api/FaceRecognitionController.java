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
}
