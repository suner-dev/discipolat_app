package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * G5.4 (§55-2) — « Demander l'accès » : un utilisateur bloqué par une garde
 * (403 frontend résolu depuis les permissions serveur §G4.4) émet une demande
 * RÉELLE : ligne {@code access_request} tracée + notification IN_APP envoyée
 * aux responsables du tenant (adhésions actives aux rôles ADMIN / PASTEUR).
 *
 * Règles :
 * - tenant et demandeur TOUJOURS résolus serveur (jamais fournis par le client) ;
 * - permissionKey validé par motif (refus de n'importe quelle chaîne) ;
 * - anti-spam : même (user, permission) sur une fenêtre de 5 minutes → renvoi
 *   ALREADY_SENT sans nouvelle notification.
 */
@Service
@Transactional
public class AccessRequestService {

    /** Clés d'accès « responsable » à notifier (miroir Security Matrix §G1.x). */
    private static final Set<String> RESPONSIBLE_ROLE_KEYS = Set.of("ADMIN", "PASTEUR");

    private static final Pattern PERMISSION_KEY_PATTERN = Pattern.compile("^[A-Z0-9_]{2,100}$");
    private static final long COOLDOWN_SECONDS = 5 * 60;

    private final AccessRequestRepository accessRequestRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AccessRequestService(AccessRequestRepository accessRequestRepository,
                                TenantMembershipRepository membershipRepository,
                                UserRepository userRepository,
                                NotificationService notificationService) {
        this.accessRequestRepository = accessRequestRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public record AccessRequestOutcome(String status, int notifiedResponsibles, UUID requestId) {}

    public AccessRequestOutcome request(UUID tenantId, UUID userId,
                                        String permissionKey, String resourceLabel, String reason) {
        if (tenantId == null || userId == null) {
            throw new IllegalArgumentException("Contexte d'authentification requis");
        }
        String key = permissionKey == null ? "" : permissionKey.trim().toUpperCase();
        if (!PERMISSION_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Clé de permission invalide");
        }

        // Anti-spam : une demande déjà émise sur la fenêtre de cooldown.
        Instant since = Instant.now().minusSeconds(COOLDOWN_SECONDS);
        Optional<AccessRequest> recent = accessRequestRepository
                .findFirstByTenantIdAndUserIdAndPermissionKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                        tenantId, userId, key, since);
        if (recent.isPresent()) {
            return new AccessRequestOutcome("ALREADY_SENT", 0, recent.get().getId());
        }

        User requester = userRepository.findById(userId).orElse(null);
        String requesterName = requester != null
                ? ((requester.getFirstName() != null ? requester.getFirstName() + " " : "")
                        + (requester.getLastName() != null ? requester.getLastName() : "")).trim()
                : userId.toString();
        if (requesterName.isEmpty()) {
            requesterName = userId.toString();
        }

        AccessRequest request = accessRequestRepository.save(AccessRequest.builder()
                .tenantId(tenantId)
                .userId(userId)
                .permissionKey(key)
                .resourceLabel(truncate(resourceLabel, 200))
                .reason(truncate(reason, 500))
                .status("NOTIFIED")
                .notifiedCount(0)
                .createdAt(Instant.now())
                .build());

        // Responsables à notifier : adhésions ACTIVES du tenant au rôle ADMIN ou PASTEUR.
        List<UUID> responsibleUserIds = membershipRepository
                .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                .filter(m -> {
                    String roleKey = m.getRole() != null ? m.getRole().getKey() : m.getRoleLegacy();
                    return roleKey != null && RESPONSIBLE_ROLE_KEYS.contains(roleKey.toUpperCase());
                })
                .map(TenantMembership::getUserId)
                .filter(id -> !id.equals(userId))
                .distinct()
                .collect(Collectors.toList());

        String when = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()).format(Instant.now());
        String titre = "Demande d'accès : " + key;
        String message = "%s demande l'accès « %s »%s (%s). Traitée via l'éditeur de rôles et permissions."
                .formatted(requesterName, key,
                        request.getResourceLabel() != null && !request.getResourceLabel().isBlank()
                                ? " — " + request.getResourceLabel() : "",
                        when);

        int notified = 0;
        for (UUID responsableId : responsibleUserIds) {
            try {
                notificationService.create(responsableId, TypeNotification.DEMANDE_ACCES,
                        CanalNotification.IN_APP, titre, message, request.getId(), "ACCESS_REQUEST");
                notified++;
            } catch (Exception ignored) {
                // Un échec de notification ponctuel ne doit pas faire échouer la demande tracée.
            }
        }

        request.setNotifiedCount(notified);
        accessRequestRepository.save(request);
        return new AccessRequestOutcome("NOTIFIED", notified, request.getId());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
