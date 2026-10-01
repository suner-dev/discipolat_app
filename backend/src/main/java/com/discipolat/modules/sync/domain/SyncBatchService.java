package com.discipolat.modules.sync.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.gantt.api.TeamTaskController;
import com.discipolat.modules.inventory.api.AssetController;
import com.discipolat.modules.members.api.MemberController;
import com.discipolat.modules.members.api.SubmitDepartmentPresenceRequest;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.members.domain.MemberService;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.api.CreateSoulRequest;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * §G5.7 — Application de la file d'écriture hors-ligne du mobile, par lot.
 *
 * Idempotence : chaque item porte un {@code clientUuid} (UUID v4 généré au
 * moment de la saisie terrain, jamais régénéré) ; l'unique
 * (tenant_id, client_uuid) de {@code sync_operation} garantit qu'un rejeu
 * (réseau instable, retry) renvoie SKIPPED_DUPLICATE sans ré-exécuter.
 *
 * Sécurité : le dispatch appelle les <b>mêmes beans controlleurs</b> que le
 * chemin en ligne — leurs {@code @PreAuthorize} (classe + méthode) s'appliquent
 * donc via AOP, et les services appliquent le scope tenant. Aucun raccourci.
 *
 * Conflits : LWW horodatée — si l'entité visée a changé côté serveur APRÈS la
 * saisie terrain, l'opération est bien appliquée (dernière écriture gagne) mais
 * un {@link SyncConflict} est tracé et notifié aux responsables du tenant :
 * pas de perte silencieuse, réconciliation manuelle côté web.
 */
@Service
public class SyncBatchService {

    /** Opérations critiques éligibles au mode FIELD_OPS (toggle tenant). */
    private static final Set<String> FIELD_OPS_TYPES = Set.of(
            "QR_CHECKIN", "QR_CHECKIN_MEMBER", "ASSET_CHECKOUT", "ASSET_RETURN", "ASSET_DAMAGE_PHOTO");

    /** Rôles autorisés à pointer autrui par QR (parité MemberController#qrCheckin). */
    private static final Set<String> POINTAGE_ROLES = Set.of(
            "ADMIN", "PASTEUR", "RESPONSABLE", "CHEF_DE_FAMILLE", "FAISEUR", "MEMBRE");

    private static final Set<String> RESPONSIBLE_ROLE_KEYS = Set.of("ADMIN", "PASTEUR");

    private final SyncOperationRepository operationRepository;
    private final SyncConflictRepository conflictRepository;
    private final TenantSettingsService tenantSettingsService;
    private final TenantMembershipRepository membershipRepository;
    private final MemberController memberController;
    private final AssetController assetController;
    private final TeamTaskController teamTaskController;
    private final SoulService soulService;
    private final MemberService memberService;
    private final MemberPresenceRepository presenceRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public SyncBatchService(SyncOperationRepository operationRepository,
                            SyncConflictRepository conflictRepository,
                            TenantSettingsService tenantSettingsService,
                            TenantMembershipRepository membershipRepository,
                            MemberController memberController,
                            AssetController assetController,
                            TeamTaskController teamTaskController,
                            SoulService soulService,
                            MemberService memberService,
                            MemberPresenceRepository presenceRepository,
                            NotificationService notificationService,
                            ObjectMapper objectMapper) {
        this.operationRepository = operationRepository;
        this.conflictRepository = conflictRepository;
        this.tenantSettingsService = tenantSettingsService;
        this.membershipRepository = membershipRepository;
        this.memberController = memberController;
        this.assetController = assetController;
        this.teamTaskController = teamTaskController;
        this.soulService = soulService;
        this.memberService = memberService;
        this.presenceRepository = presenceRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /** Un item de la file mobile. {@code at} = horodatation locale LWW. */
    public record ClientOp(String clientUuid, String type, Map<String, Object> payload,
                           LocalDateTime at, String photoBase64, String photoMime, String photoName) {}

    public record BatchResult(List<Map<String, Object>> results, int applied, int duplicates,
                              int failed, int conflicts) {}

    @Transactional
    public BatchResult applyBatch(List<ClientOp> ops, Authentication auth) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        String mode = offlineMode(tenantId);
        final LocalDateTime replayStartedAt = LocalDateTime.now();

        List<Map<String, Object>> results = new ArrayList<>();
        int applied = 0, duplicates = 0, failed = 0, conflicts = 0;

        for (ClientOp op : ops) {
            if (op.clientUuid() == null || op.clientUuid().isBlank()
                    || op.type() == null || op.type().isBlank()) {
                results.add(itemResult(op.clientUuid(), "FAILED", "clientUuid et type requis", false));
                failed++;
                continue;
            }
            // Toggle tenant (§1105) : LECTURE = aucune écriture hors-ligne admise.
            if ("LECTURE".equalsIgnoreCase(mode)) {
                results.add(itemResult(op.clientUuid(), "REJECTED_OFFLINE_MODE",
                        "Le tenant autorise seulement la lecture hors-ligne", false));
                failed++;
                continue;
            }
            if ("FIELD_OPS".equalsIgnoreCase(mode) && !FIELD_OPS_TYPES.contains(op.type())) {
                results.add(itemResult(op.clientUuid(), "REJECTED_OFFLINE_MODE",
                        "Mode FIELD_OPS : opération hors périmètre critique", false));
                failed++;
                continue;
            }
            // Idempotence : rejeu d'un item déjà tracé → doublon ignoré.
            if (operationRepository.findByTenantIdAndClientUuid(tenantId, op.clientUuid()).isPresent()) {
                results.add(itemResult(op.clientUuid(), "SKIPPED_DUPLICATE", null, false));
                duplicates++;
                continue;
            }

            boolean conflict = false;
            String status;
            String error = null;
            UUID operationId = null;
            try {
                conflict = dispatch(op, auth, tenantId, replayStartedAt);
                status = conflict ? "CONFLICT_LWW" : "APPLIED";
                SyncOperation trace = new SyncOperation();
                trace.setTenantId(tenantId);
                trace.setActorUserId(actorId);
                trace.setClientUuid(op.clientUuid());
                trace.setOpType(op.type());
                trace.setStatus(status);
                trace.setAppliedAt(LocalDateTime.now());
                operationId = operationRepository.save(trace).getId();
            } catch (Exception e) {
                status = "FAILED";
                error = e.getClass().getSimpleName() + ": " + e.getMessage();
                try {
                    SyncOperation trace = new SyncOperation();
                    trace.setTenantId(tenantId);
                    trace.setActorUserId(actorId);
                    trace.setClientUuid(op.clientUuid());
                    trace.setOpType(op.type());
                    trace.setStatus(status);
                    trace.setError(truncate(error));
                    trace.setAppliedAt(LocalDateTime.now());
                    operationId = operationRepository.save(trace).getId();
                } catch (Exception ignored) {
                    // la trace d'échec est best-effort ; le résultat le signale déjà
                }
                failed++;
            }
            if ("APPLIED".equals(status) || "CONFLICT_LWW".equals(status)) applied++;
            if (conflict) {
                conflicts++;
                recordConflict(op, tenantId, operationId);
            }
            results.add(itemResult(op.clientUuid(), status, error, conflict));
        }
        return new BatchResult(results, applied, duplicates, failed, conflicts);
    }

    /** Liste des conflits non résolus du tenant (réconciliation web §1111). */
    public List<SyncConflict> openConflicts() {
        return conflictRepository.findByTenantIdAndResolvedFalseOrderByCreatedAtDesc(
                TenantContext.requireTenantId());
    }

    public SyncConflict resolveConflict(UUID conflictId, String note) {
        UUID tenantId = TenantContext.requireTenantId();
        SyncConflict conflict = conflictRepository.findById(conflictId)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException(
                        "SyncConflict", conflictId));
        conflict.setResolved(true);
        conflict.setResolvedBy(SecurityUtils.getCurrentUserId());
        conflict.setResolvedAt(LocalDateTime.now());
        conflict.setResolutionNote(truncate(note));
        return conflictRepository.save(conflict);
    }

    // ==================== dispatch ====================

    /** @return true si un conflit LWW a été détecté (opération quand même appliquée). */
    private boolean dispatch(ClientOp op, Authentication auth, UUID tenantId, LocalDateTime replayStartedAt) {
        Map<String, Object> p = op.payload() != null ? op.payload() : Map.of();
        switch (op.type()) {
            case "QR_CHECKIN" -> {
                requireAnyRole(auth, POINTAGE_ROLES);
                memberController.qrCheckin(Map.of("code", String.valueOf(p.get("code"))));
                return false;
            }
            case "QR_CHECKIN_MEMBER" -> {
                // Pointage de sa propre présence (MEMBRE, sans rôle de pointage).
                requireAnyRole(auth, Set.of("MEMBRE"));
                UUID soulId = memberService.mySoulId();
                if (soulId == null) {
                    throw new IllegalStateException("Aucune âme liée à ce compte");
                }
                memberController.qrCheckin(Map.of("soulId", soulId.toString()));
                return false;
            }
            case "ASSET_CHECKOUT" -> {
                UUID itemId = uuid(p.get("itemId"));
                assetController.checkout(itemId, new AssetController.CheckoutRequest(
                        uuid(p.get("memberId")), uuid(p.get("spaceId")), uuid(p.get("eventId")),
                        null, (String) p.getOrDefault("condition", "GOOD"),
                        (String) p.get("notes")));
                return false;
            }
            case "ASSET_RETURN" -> {
                UUID itemId = uuid(p.get("itemId"));
                assetController.returnAsset(itemId, new AssetController.ReturnRequest(
                        (String) p.getOrDefault("condition", "GOOD"), (String) p.get("notes")));
                return false;
            }
            case "ASSET_DAMAGE_PHOTO" -> {
                UUID itemId = uuid(p.get("itemId"));
                if (op.photoBase64() == null || op.photoBase64().isBlank()) {
                    throw new IllegalArgumentException("photoBase64 requise");
                }
                byte[] bytes = java.util.Base64.getDecoder().decode(op.photoBase64());
                try {
                    assetController.uploadDamagePhoto(itemId,
                            new InMemoryMultipartFile(op.photoName(), op.photoMime(), bytes));
                } catch (IOException e) {
                    throw new IllegalStateException("Écriture photo de dommage impossible", e);
                }
                return false;
            }
            case "PRESENCE_SUBMIT" -> {
                UUID departmentId = uuid(p.get("departmentId"));
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.valueToTree(p.get("request"));
                SubmitDepartmentPresenceRequest request;
                try {
                    request = objectMapper.treeToValue(node, SubmitDepartmentPresenceRequest.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new IllegalArgumentException("payload presence invalide", e);
                }
                memberController.submitDepartmentPresences(departmentId, request);
                // LWW : une présence du tenant a-t-elle été modifiée APRÈS la saisie
                // terrain (par le web ou un autre poste) ? Dans ce cas la saisie
                // terrain est plus ancienne → conflit à réconcilier, pas de silence.
                if (op.at() != null) {
                    return !presenceRepository
                            .findByTenantIdAndUpdatedAtAfter(tenantId, op.at()).isEmpty();
                }
                return false;
            }
            case "TASK_STATUS" -> {
                teamTaskController.updateStatus(uuid(p.get("taskId")),
                        Map.of("statut", String.valueOf(p.get("statut"))));
                return false;
            }
            case "TASK_PROGRESS" -> {
                teamTaskController.updateProgression(uuid(p.get("taskId")),
                        Map.of("progression", p.get("progression") instanceof Number n ? n.intValue() : 0));
                return false;
            }
            case "NEW_VISITOR" -> {
                CreateSoulRequest req = objectMapper.convertValue(p.get("visitor"), CreateSoulRequest.class);
                soulService.create(req);
                return false;
            }
            default -> throw new IllegalArgumentException("Type d'opération inconnu : " + op.type());
        }
    }

    /** Toggle offline_mode du tenant (§1105-5). */
    private String offlineMode(UUID tenantId) {
        TenantSettings settings = tenantSettingsService.getSettings(tenantId);
        return settings != null && settings.getOfflineMode() != null ? settings.getOfflineMode() : "LECTURE";
    }

    private void recordConflict(ClientOp op, UUID tenantId, UUID operationId) {
        try {
            SyncConflict conflict = new SyncConflict();
            conflict.setTenantId(tenantId);
            conflict.setOperationId(operationId);
            conflict.setClientUuid(op.clientUuid());
            conflict.setEntityType(op.type());
            conflict.setEntityId(String.valueOf(op.payload() != null ? op.payload().get("itemId") : null));
            conflict.setClientValue(op.payload() != null ? objectMapper.writeValueAsString(op.payload()) : null);
            conflict.setClientOpAt(op.at());
            conflictRepository.save(conflict);
            notifyResponsibles(tenantId, op);
        } catch (Exception ignored) {
            // un échec de traçabilité ne doit pas faire échouer la sync
        }
    }

    private void notifyResponsibles(UUID tenantId, ClientOp op) {
        List<UUID> responsables = membershipRepository
                .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                .filter(m -> {
                    String roleKey = m.getRole() != null ? m.getRole().getKey() : m.getRoleLegacy();
                    return roleKey != null && RESPONSIBLE_ROLE_KEYS.contains(roleKey.toUpperCase());
                })
                .map(TenantMembership::getUserId)
                .distinct()
                .collect(Collectors.toList());
        for (UUID id : responsables) {
            try {
                notificationService.create(id, TypeNotification.OFFLINE_CONFLIT,
                        CanalNotification.IN_APP, "Conflit de synchronisation hors-ligne",
                        "L'opération terrain " + op.type() + " (" + op.clientUuid()
                                + ") a été appliquée (dernière écriture gagne) mais divergeait"
                                + " d'une modification serveur plus récente — réconciliation nécessaire.",
                        null, "SYNC_OPERATION");
            } catch (Exception ignored) {
                // best-effort, comme AccessRequestService
            }
        }
    }

    private static Map<String, Object> itemResult(String clientUuid, String status, String error, boolean conflict) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientUuid", clientUuid);
        m.put("status", status);
        if (error != null) m.put("error", error);
        m.put("conflict", conflict);
        return m;
    }

    private static void requireAnyRole(Authentication auth, Set<String> roles) {
        if (auth == null || auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(a -> roles.contains(a.replace("ROLE_", "")))) {
            throw new org.springframework.security.access.AccessDeniedException("Rôle insuffisant");
        }
    }

    private static UUID uuid(Object v) {
        if (v == null) return null;
        if (v instanceof UUID u) return u;
        String s = String.valueOf(v);
        return s.isBlank() || "null".equals(s) ? null : UUID.fromString(s);
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }

    /** Photo remontée en base64 dans le batch → vue MultipartFile pour le contrôleur existant. */
    static final class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] bytes;

        InMemoryMultipartFile(String filename, String contentType, byte[] bytes) {
            this.name = filename != null ? filename : "photo.jpg";
            this.contentType = contentType != null ? contentType : "image/jpeg";
            this.bytes = bytes;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
        @Override public void transferTo(java.nio.file.Path dest) throws IOException {
            java.nio.file.Files.write(dest, bytes);
        }
    }
}
