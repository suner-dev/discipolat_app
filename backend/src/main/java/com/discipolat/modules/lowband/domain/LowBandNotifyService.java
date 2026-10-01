package com.discipolat.modules.lowband.domain;

import com.discipolat.modules.core.domain.OutboxEvent;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.domain.SpaceMembership;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppMessage;
import com.discipolat.modules.whatsapp.domain.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * §G5.9 — WhatsApp SORTANT : abonné de l'outbox (G2.8) qui diffuse vers les
 * numéros OPT-IN les événements membre-first : dress code à venir (G3.4),
 * désignations (rôles vivants G4.4), tâches, alertes stock santé (G3.11).
 *
 * Règles de sécurité :
 * - toggle tenant {@code low_band_enabled} obligatoire (§G1.2) ;
 * - uniquement les utilisateurs {@code whatsapp_opt_in = true} ;
 * - AUCUNE donnée sensible (pastoral, finance personnelle) ne sort par ce canal :
 *   titres + invitation à consulter l'app ;
 * - best-effort absolu : un échec d'envoi ne casse jamais la consommation outbox.
 */
@Service
public class LowBandNotifyService {

    private static final Logger log = LoggerFactory.getLogger(LowBandNotifyService.class);

    private final ObjectProvider<WhatsAppService> whatsAppProvider;
    private final LowBandPortalService portalService;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;

    public LowBandNotifyService(ObjectProvider<WhatsAppService> whatsAppProvider,
                                LowBandPortalService portalService,
                                UserRepository userRepository,
                                PersonRepository personRepository,
                                SpaceMembershipRepository spaceMembershipRepository) {
        this.whatsAppProvider = whatsAppProvider;
        this.portalService = portalService;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.spaceMembershipRepository = spaceMembershipRepository;
    }

    /** Consommateur outbox — appelé pour chaque événement abonné (idempotent processed_event). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consume(OutboxEvent event) {
        try {
            if (event.getTenantId() == null) return;
            if (!portalService.isLowBandEnabled(event.getTenantId())) return;
            WhatsAppService wa = whatsAppProvider.getIfAvailable();
            if (wa == null) return;
            Map<String, Object> payload = event.getPayloadJson() != null
                    ? event.getPayloadJson() : new HashMap<>();
            switch (event.getEventType()) {
                case "DressCodePublished" -> dressCodePublished(event.getTenantId(), payload);
                case "RoleAssigned", "PastorAppointed" -> designation(event.getTenantId(), payload);
                case "TaskAssigned" -> taskAssigned(event.getTenantId(), payload);
                case "StockLowAlert", "MedicineExpiring" -> healthAlert(event.getTenantId(), payload);
                default -> { }
            }
        } catch (Exception e) {
            // Jamais bloquant pour l'outbox : le canal temps réel/web reste la voie principale.
            log.warn("[G5.9] Diffusion WhatsApp échouée ({}): {}", event.getEventType(), e.getMessage());
        }
    }

    private void dressCodePublished(UUID tenantId, Map<String, Object> payload) {
        String title = str(payload.get("title"), "Nouvelle tenue de culte");
        String spaceId = str(payload.get("spaceId"), null);
        List<User> audience;
        if (spaceId != null) {
            // Audience de l'espace uniquement (scope §55) — membres actifs du space.
            audience = spaceMembersAsUsers(tenantId, UUID.fromString(spaceId));
        } else {
            audience = userRepository.findByTenantIdAndWhatsappOptInTrue(tenantId);
        }
        broadcast(tenantId, audience,
                "👕 " + title + "\nRépondez #tenue pour voir les détails.");
    }

    private void designation(UUID tenantId, Map<String, Object> payload) {
        String personId = str(payload.get("personId"), null);
        if (personId == null) return;
        personRepository.findById(UUID.fromString(personId))
                .filter(p -> tenantId.equals(p.getTenantId()))
                .flatMap(p -> userByEmail(tenantId, p.getEmailNormalized()))
                .filter(this::isOptedIn)
                .ifPresent(u -> {
                    send(tenantId, u, "Nouvelle désignation",
                            "🎉 Félicitations ! Votre rôle vient d'être mis à jour sur Discipolat.");
                    portalService.journal(tenantId, "WHATSAPP", u.getPhone(), "OUTBOUND", "DESIGNATION");
                });
    }

    private void taskAssigned(UUID tenantId, Map<String, Object> payload) {
        String assigneeId = str(payload.get("assigneeId"), null);
        if (assigneeId == null) return;
        userRepository.findById(UUID.fromString(assigneeId))
                .filter(u -> tenantId.equals(u.getTenantId()))
                .filter(this::isOptedIn)
                .ifPresent(u -> {
                    send(tenantId, u, "Nouvelle tâche",
                            "📋 Une tâche vous attend sur Discipolat.");
                    portalService.journal(tenantId, "WHATSAPP", u.getPhone(), "OUTBOUND", "TASK");
                });
    }

    /** Alertes santé (stock bas, médicaments proches péremption) → responsables du tenant. */
    private void healthAlert(UUID tenantId, Map<String, Object> payload) {
        String detail = str(payload.get("message"), str(payload.get("item"), "Alerte stock santé"));
        List<User> responsibles = userRepository.findByTenantId(tenantId).stream()
                .filter(u -> !u.isDeleted())
                .filter(u -> "ADMIN".equalsIgnoreCase(String.valueOf(u.getRole()))
                        || "PASTEUR".equalsIgnoreCase(String.valueOf(u.getRole()))
                        || "ADMIN".equalsIgnoreCase(String.valueOf(u.getActiveRole()))
                        || "PASTEUR".equalsIgnoreCase(String.valueOf(u.getActiveRole())))
                .filter(this::isOptedIn)
                .toList();
        broadcast(tenantId, responsibles, "⚠️ Alerte santé : " + detail);
    }

    private List<User> spaceMembersAsUsers(UUID tenantId, UUID spaceId) {
        List<SpaceMembership> members =
                spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(tenantId, spaceId, "ACTIVE");
        return members.stream()
                .map(SpaceMembership::getPersonId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(pid -> personRepository.findById(pid)
                        .filter(p -> tenantId.equals(p.getTenantId()))
                        .map(Person::getEmailNormalized)
                        .flatMap(email -> userByEmail(tenantId, email))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .filter(this::isOptedIn)
                .toList();
    }

    private void broadcast(UUID tenantId, List<User> audience, String body) {
        int sent = 0;
        for (User u : audience) {
            if (!isOptedIn(u)) continue;
            send(tenantId, u, null, body);
            sent++;
        }
        if (sent > 0) {
            portalService.journal(tenantId, "WHATSAPP", "broadcast", "OUTBOUND",
                    body + " (" + sent + " destinataires)");
        }
    }

    private void send(UUID tenantId, User u, String ignoredTitle, String body) {
        WhatsAppService wa = whatsAppProvider.getIfAvailable();
        if (wa == null || u.getPhone() == null || u.getPhone().isBlank()) return;
        // sendText gère already le filetage (QUEUED si le pont Meta n'est pas configuré).
        wa.sendText(tenantId, u.getPhone(), body, null, null, WhatsAppMessage.Kind.NOTIFICATION);
    }

    private boolean isOptedIn(User u) {
        return u.isWhatsappOptIn()
                && u.getPhone() != null && !u.getPhone().isBlank();
    }

    private Optional<User> userByEmail(UUID tenantId, String email) {
        if (email == null) return Optional.empty();
        // findByEmail n'existe plus dans main (requête sensible a la casse levant
        // IncorrectResultSize sur les comptes soft-deleted partageant l'email) :
        // on utilise la resolution scopee tenant fournie par main.
        return userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email);
    }

    private static String str(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }
}
