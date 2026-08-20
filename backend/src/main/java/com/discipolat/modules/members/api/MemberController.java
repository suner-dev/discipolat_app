package com.discipolat.modules.members.api;

import com.discipolat.modules.members.domain.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@PreAuthorize("isAuthenticated()")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
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

    /** Enregistre la présence via scan QR code (dispo pour tout responsable). */
    @PostMapping("/qr-checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> qrCheckin(@RequestBody Map<String, String> body) {
        String soulIdStr = body.get("soulId");
        if (soulIdStr == null || soulIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "soulId is required"));
        }
        UUID soulId = UUID.fromString(soulIdStr);
        memberService.recordPresenceByQr(soulId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Présence enregistrée", "soulId", soulId.toString()));
    }
}
