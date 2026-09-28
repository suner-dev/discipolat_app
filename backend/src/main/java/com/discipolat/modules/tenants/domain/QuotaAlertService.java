package com.discipolat.modules.tenants.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.notifications.domain.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Constat M3 — alerte in-app lorsqu'un quota de tenant est dépassé.
 *
 * <p>Avant ce correctif, un dépassement de quota produisait un simple
 * {@code 403} : l'église ne savait pas qu'elle approchait de sa limite, et le
 * super admin non plus. Personne n'était prévenu, la découverte se faisait à
 * l'échec.
 *
 * <p><b>Jamais bloquant</b> : l'alerte est best-effort. Si elle échoue, le
 * dépassement de quota reste un refus propre — l'utilisateur n'a pas à subir une
 * erreur technique parce qu'une notification n'a pas pu être créée.
 *
 * <p><b>Destinataires</b> : les membres actifs du tenant portant le rôle
 * {@code TENANT_OWNER} ou {@code TENANT_ADMIN} — les seuls susceptibles
 * d'agrandir le plan.
 */
@Service
public class QuotaAlertService {

    private static final Logger log = LoggerFactory.getLogger(QuotaAlertService.class);

    private static final Set<String> ADMIN_ROLE_KEYS = Set.of("TENANT_OWNER", "TENANT_ADMIN");

    private final TenantMembershipRepository membershipRepository;
    private final NotificationService notificationService;

    public QuotaAlertService(TenantMembershipRepository membershipRepository,
                             NotificationService notificationService) {
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    /**
     * Notifie les administrateurs qu'un quota de la ressource donnée est atteint.
     *
     * @param tenantId  tenant concerné
     * @param resource  nom technique de la ressource (`spaces`, `events`, `users`…)
     * @param used      consommation observée
     * @param limit     limite du plan
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alertQuotaExceeded(UUID tenantId, String resource, long used, long limit) {
        if (tenantId == null) {
            return;
        }
        String label = humanize(resource);
        String title = "Quota atteint : " + label;
        String message = "Votre église a atteint la limite de son plan pour « " + label + " » "
                + "(" + used + " / " + limit + "). Pensez à agrandir votre abonnement.";

        int notified = 0;
        for (UUID administratorId : administratorsOf(tenantId)) {
            try {
                notificationService.create(tenantId, administratorId,
                        TypeNotification.INFORMATION, CanalNotification.IN_APP,
                        title, message, tenantId, "QUOTA");
                notified++;
            } catch (RuntimeException notificationFailure) {
                // Une notification ratée ne doit pas interrompre l'alerte des autres.
                log.warn("Notification de quota non remise à {} : {}",
                        administratorId, notificationFailure.getMessage());
            }
        }
        log.info("Quota {} dépassé pour le tenant {} ({}/{}) : {} administrateur(s) alerté(s)",
                resource, tenantId, used, limit, notified);
    }

    private Set<UUID> administratorsOf(UUID tenantId) {
        Set<UUID> recipients = new LinkedHashSet<>();
        for (TenantMembership membership : membershipRepository
                .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE)) {
            String roleKey = membership.getRole() != null
                    ? membership.getRole().getKey()
                    : membership.getRoleLegacy();
            if (roleKey != null && ADMIN_ROLE_KEYS.contains(roleKey.trim().toUpperCase(Locale.ROOT))) {
                recipients.add(membership.getUserId());
            }
        }
        return recipients;
    }

    private String humanize(String resource) {
        if (resource == null || resource.isBlank()) {
            return "ressource";
        }
        return switch (resource.trim().toLowerCase(Locale.ROOT)) {
            case "spaces" -> "espaces";
            case "events" -> "événements";
            case "churches" -> "églises";
            case "departments" -> "départements";
            case "campuses" -> "campus";
            case "groups" -> "groupes";
            case "users" -> "utilisateurs";
            case "storage" -> "stockage";
            case "ai_credits" -> "crédits IA";
            case "courses" -> "formations";
            case "messages" -> "messages";
            default -> resource;
        };
    }
}
