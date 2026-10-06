package com.discipolat.modules.relations.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.relations.api.dto.MemberRelationView;
import com.discipolat.modules.relations.domain.MemberRelation;
import com.discipolat.modules.relations.domain.MemberRelationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * « Mon encadrement » — V231. Self-service : tout membre déclare ses
 * autorités enregistrées (pasteur, supérieur, mentor, parrain…) selon le
 * paramétrage de son église ; le supérieur reçoit une notification et le
 * membre apparaît dans sa liste « ses membres ».
 *
 * <p>Le tenant vient <b>toujours</b> du contexte (jamais d'un paramètre
 * d requête) et l'utilisateur courant de la sécurité.
 */
@RestController
@RequestMapping("/api/v1/relations")
public class RelationController {

    private final MemberRelationService service;

    public RelationController(MemberRelationService service) {
        this.service = service;
    }

    /** Types de rattachement <b>actifs</b> pour cette église (dictionnaire). */
    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, String>>> types() {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, String>> out = service.typeCatalog(tenantId).stream()
                .filter(MemberRelationService.RelationType::actif)
                .map(t -> Map.of("code", t.code(), "label", t.label()))
                .toList();
        return ResponseEntity.ok(out);
    }

    /** Mes rattachements : sortantes (mes encadrants) + entrantes (mes membres). */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> myRelations() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.summary(tenantId, userId, userId));
    }

    /**
     * « Ses membres » — paginé et borné : un encadrement peut dépasser
     * le plafond de rattachements entrants.
     */
    @GetMapping("/me/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> myMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listMembersOf(tenantId, userId, page, size));
    }

    /** Déclarer un encadrant (par id ou email enregistré) — ACTIVE + notif immédiate. */
    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberRelationView> declare(@RequestBody DeclareRelationRequest body) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        MemberRelation relation = service.declare(tenantId, userId, body.toUserId(), body.toEmail(),
                body.relationType(), body.note(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.viewOf(tenantId, relation, userId));
    }

    /**
     * Déclarer un encadrement pour le compte d'un membre (secrétaire qui
     * saisit le pasteur d'un ancien) — ADMIN / PASTEUR uniquement.
     */
    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<MemberRelationView> declareFor(@PathVariable UUID userId,
                                                         @RequestBody DeclareRelationRequest body) {
        UUID tenantId = TenantContext.getTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        MemberRelation relation = service.declare(tenantId, userId, body.toUserId(), body.toEmail(),
                body.relationType(), body.note(), actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.viewOf(tenantId, relation, actorId));
    }

    /**
     * Retirer un rattachement : autorisé au déclarant, à l'encadrant
     * concerné et aux modérateurs (ADMIN/PASTEUR) — le contrôle est fait
     * en service sur les troiscas, pas seulement dans l'annotation.
     */
    @DeleteMapping("/me/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberRelationView> revoke(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        MemberRelation relation = service.revoke(tenantId, id, userId);
        return ResponseEntity.ok(service.viewOf(tenantId, relation, userId));
    }

    /** Rattachements d'un autre membre — même garde que la fiche /users/{id}/detail. */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> relationsOf(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.summary(tenantId, userId, SecurityUtils.getCurrentUserId()));
    }

    public record DeclareRelationRequest(UUID toUserId, String toEmail, String relationType, String note) {}
}
