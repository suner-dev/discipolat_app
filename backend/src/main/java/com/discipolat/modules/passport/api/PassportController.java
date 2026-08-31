package com.discipolat.modules.passport.api;

import com.discipolat.modules.passport.domain.PassportService;
import com.discipolat.modules.passport.domain.PassportVerification;
import com.discipolat.modules.passport.domain.SpiritualPassport;
import com.discipolat.modules.passport.domain.SpiritualPassportEntry;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST du Passeport Spirituel (authentifiée).
 *
 * Sécurité :
 * - toutes les routes exigent une authentification ;
 * - émission/révocation réservées ADMIN/PASTEUR ;
 * - consultation d'un passeport tiers réservée aux responsables ;
 * - ajout d'entrées réservé aux rôles habilités (RBAC + tenant courant).
 */
@RestController
@RequestMapping("/api/v1/passports")
@PreAuthorize("isAuthenticated()")
public class PassportController {

    private final PassportService service;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    public PassportController(PassportService service) {
        this.service = service;
    }

    /** Émet le passeport d'un membre (idempotent). */
    @PostMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<PassportResponse> issuePassport(@PathVariable UUID memberId) {
        SpiritualPassport passport = service.issuePassport(memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(passport));
    }

    /** Mon passeport spirituel. */
    @GetMapping("/mine")
    public ResponseEntity<PassportResponse> getMyPassport() {
        return ResponseEntity.ok(toResponse(service.getMyPassport()));
    }

    /** Passeport d'un membre du tenant courant (usage pastoral). */
    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<PassportResponse> getMemberPassport(@PathVariable UUID memberId) {
        return ResponseEntity.ok(toResponse(service.getMemberPassport(memberId)));
    }

    /** Ajoute une entrée d'historique (baptême, formation, service…). */
    @PostMapping("/{passportId}/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<SpiritualPassportEntry> addEntry(@PathVariable UUID passportId,
                                                           @RequestBody SpiritualPassportEntry entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addEntry(passportId, entry));
    }

    /** Révoque un passeport. */
    @PostMapping("/{passportId}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<PassportResponse> revoke(@PathVariable UUID passportId,
                                                   @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(toResponse(service.revokePassport(passportId, reason)));
    }

    /** Entrées d'historique d'un passeport. */
    @GetMapping("/{passportId}/entries")
    public ResponseEntity<List<SpiritualPassportEntry>> getEntries(@PathVariable UUID passportId) {
        return ResponseEntity.ok(service.getEntries(passportId));
    }

    /**
     * QR de vérification : URL publique + PNG (base64) généré avec ZXing.
     * Le QR contient uniquement l'URL de vérification (aucune donnée privée).
     */
    @GetMapping("/{passportId}/qr")
    public ResponseEntity<Map<String, Object>> getQr(@PathVariable UUID passportId) {
        SpiritualPassport passport = service.getPassportForCurrentUser(passportId);
        String verificationUrl = publicBaseUrl + "/verify/passport/" + passport.getPassportCode();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passportCode", passport.getPassportCode());
        result.put("verificationUrl", verificationUrl);
        result.put("qrPngBase64", generateQrPngBase64(verificationUrl));
        return ResponseEntity.ok(result);
    }

    /** Historique de vérification (traçabilité) — responsables de l'église. */
    @GetMapping("/verifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<PassportVerification>> getVerifications(@RequestParam String code) {
        return ResponseEntity.ok(service.getVerifications(code));
    }

    // ======================== HELPERS ========================

    private PassportResponse toResponse(SpiritualPassport passport) {
        List<SpiritualPassportEntry> entries = service.getEntries(passport.getId());
        return new PassportResponse(passport.getId(), passport.getPassportCode(), passport.getStatus(),
                passport.getIssuedAt(), passport.getExpiresAt(), passport.getPayloadHash(), entries);
    }

    private String generateQrPngBase64(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 256, 256);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Génération du QR impossible", e);
        }
    }

    /** DTO de réponse du passeport (sans la signature brute). */
    public record PassportResponse(
            UUID id,
            String passportCode,
            String status,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            String payloadHash,
            List<SpiritualPassportEntry> entries) {
    }
}
