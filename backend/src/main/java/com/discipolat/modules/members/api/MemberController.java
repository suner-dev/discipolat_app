package com.discipolat.modules.members.api;

import com.discipolat.common.infrastructure.qr.QrImageService;
import com.discipolat.modules.members.domain.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@PreAuthorize("isAuthenticated()")
public class MemberController {

    private final MemberService memberService;
    private final QrImageService qrImageService;

    public MemberController(MemberService memberService, QrImageService qrImageService) {
        this.memberService = memberService;
        this.qrImageService = qrImageService;
    }

    // ============================================================
    // Espace Membre — Phase 3 : progression, événements, stats
    // ============================================================

    /** Progression spirituelle et statistiques personnelles du membre connecté. */
    @GetMapping("/me/progression")
    public ResponseEntity<Map<String, Object>> myProgression() {
        return ResponseEntity.ok(memberService.getMyProgression());
    }

    /** Événements à venir pour le membre connecté (départements + famille + plateforme). */
    @GetMapping("/me/events")
    public ResponseEntity<List<Map<String, Object>>> myEvents() {
        return ResponseEntity.ok(memberService.getMyUpcomingEvents());
    }

    /** Notes du faiseur visibles par le membre connecté. */
    @GetMapping("/me/notes")
    public ResponseEntity<List<Map<String, Object>>> myNotes() {
        return ResponseEntity.ok(memberService.getMyNotes());
    }

    // ============================================================
    // Espace Membre — Phase 1 (dashboard + profil)
    // ============================================================

    /** Dashboard agrégé du membre connecté (tout utilisateur authentifié). */
    @GetMapping("/me/dashboard")
    public ResponseEntity<MemberDashboardResponse> myDashboard() {
        return ResponseEntity.ok(memberService.getMyDashboard());
    }

    /** Mise à jour du profil du membre connecté (compte + âme liée). */
    @PutMapping("/me/profile")
    public ResponseEntity<MemberDashboardResponse> updateProfile(
            @Valid @RequestBody UpdateMemberProfileRequest request) {
        return ResponseEntity.ok(memberService.updateMyProfile(request));
    }

    // ============================================================
    // Espace Membre — Phase 2 : présences hebdomadaires
    // ============================================================

    /** Historique des présences du membre connecté. */
    @GetMapping("/me/presences")
    public ResponseEntity<List<MemberPresenceResponse>> myPresences() {
        return ResponseEntity.ok(memberService.getMyPresences());
    }

    /** Saisie / mise à jour de la présence hebdomadaire du membre connecté. */
    @PostMapping("/me/presences")
    public ResponseEntity<MemberPresenceResponse> submitPresence(
            @Valid @RequestBody SubmitPresenceRequest request) {
        return ResponseEntity.ok(memberService.submitMyPresence(request));
    }

    // ============================================================
    // Espace Membre — Phase 2 : demandes (suggestions, rendez-vous, signalements)
    // ============================================================

    /** Demandes envoyées par le membre connecté. */
    @GetMapping("/me/requests")
    public ResponseEntity<List<MemberRequestResponse>> myRequests() {
        return ResponseEntity.ok(memberService.getMyRequests());
    }

    /** Envoi d'une suggestion, d'un rendez-vous ou d'un signalement. */
    @PostMapping("/me/requests")
    public ResponseEntity<MemberRequestResponse> createRequest(
            @Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.ok(memberService.createRequest(request));
    }

    /** Boîte de réception scopée : pasteur/admin tout, responsable ses départements, chef de famille sa famille. */
    @GetMapping("/requests/inbox")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<MemberRequestResponse>> inbox() {
        return ResponseEntity.ok(memberService.getRequestsInbox());
    }

    /** Traitement d'une demande (statut + réponse) par le récepteur autorisé. */
    @PatchMapping("/requests/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<MemberRequestResponse> updateRequestStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMemberRequestStatus request) {
        return ResponseEntity.ok(memberService.updateRequestStatus(id, request));
    }

    /** Présences récentes des membres sous la responsabilité du rôle courant. */
    @GetMapping("/presences/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<MemberPresenceResponse>> scopedPresences() {
        return ResponseEntity.ok(memberService.getScopedPresences());
    }

    // ============================================================
    // Saisie des présences par le responsable (département)
    // ============================================================

    /** Fiche de présence du département : membres + présence de la semaine (saisie responsable). */
    @GetMapping("/departments/{deptId}/presences")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<DepartmentPresenceRecord>> departmentPresenceSheet(
            @PathVariable UUID deptId,
            @RequestParam(required = false) LocalDate semaine) {
        return ResponseEntity.ok(memberService.getDepartmentPresenceSheet(deptId, semaine));
    }

    /** Saisie groupée des présences du département pour la semaine. */
    @PostMapping("/departments/{deptId}/presences")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<MemberPresenceResponse>> submitDepartmentPresences(
            @PathVariable UUID deptId,
            @Valid @RequestBody SubmitDepartmentPresenceRequest request) {
        return ResponseEntity.ok(memberService.submitDepartmentPresences(deptId, request));
    }

    /** Enregistre la présence via scan QR code (dispo pour tous les rôles).
     *  Accepte {@code soulId} (UUID) ou {@code code} (contenu scanné {@code discipolat:soul:<uuid>}). */
    @PostMapping("/qr-checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<Map<String, Object>> qrCheckin(@RequestBody Map<String, String> body) {
        String raw = body.getOrDefault("soulId", body.get("code"));
        UUID soulId = parseSoulCode(raw);
        if (soulId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "soulId ou code QR valide requis"));
        }
        memberService.recordPresenceByQr(soulId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Présence enregistrée", "soulId", soulId.toString()));
    }

    /** Scan terrain : la photo du QR du membre est remontée, décodée (ZXing) puis convertie en présence. */
    @PostMapping("/qr-checkin/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> qrCheckinScan(@RequestParam("file") MultipartFile file) throws IOException {
        java.util.Optional<String> decoded = qrImageService.decodeQrImage(file.getBytes());
        if (decoded.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun QR lisible sur la photo"));
        }
        UUID soulId = parseSoulCode(decoded.get());
        if (soulId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "QR non reconnu", "scanned", decoded.get()));
        }
        memberService.recordPresenceByQr(soulId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Présence enregistrée", "soulId", soulId.toString()));
    }

    /** QR de présentation du membre connecté, à faire scanner par le responsable. */
    @GetMapping("/me/qr-code")
    public ResponseEntity<Map<String, String>> myQrCode() throws IOException {
        UUID soulId = memberService.mySoulId();
        if (soulId == null) {
            return ResponseEntity.notFound().build();
        }
        String data = "discipolat:soul:" + soulId;
        return ResponseEntity.ok(Map.of(
                "soulId", soulId.toString(),
                "data", data,
                "qrPngDataUrl", qrImageService.renderPngDataUrl(data)));
    }

    /**
     * Identifie une âme depuis la photo de son QR (distribution de kit par scan
     * de « l'âme », G5.6) — sans enregistrer de présence.
     */
    @PostMapping("/qr-resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> qrResolve(@RequestParam("file") MultipartFile file) throws IOException {
        java.util.Optional<String> decoded = qrImageService.decodeQrImage(file.getBytes());
        if (decoded.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun QR lisible sur la photo"));
        }
        UUID soulId = parseSoulCode(decoded.get());
        if (soulId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "QR non reconnu", "scanned", decoded.get()));
        }
        return memberService.resolveSoulForQr(soulId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Extrait l'UUID d'un contenu QR de membre (tolère {@code discipolat:soul:<uuid>} ou UUID brut). */
    private static UUID parseSoulCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        int idx = trimmed.lastIndexOf("discipolat:soul:");
        if (idx >= 0) {
            trimmed = trimmed.substring(idx + "discipolat:soul:".length());
        }
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
