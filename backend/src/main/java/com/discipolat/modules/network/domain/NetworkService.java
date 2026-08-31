package com.discipolat.modules.network.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service du réseau inter-églises (Discipolat Network).
 *
 * Règle de sécurité : aucune donnée privée d'une organisation n'est exposée
 * à une autre sans autorisation explicite (shared_with_public = TRUE).
 */
@Service
public class NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);

    private final NetworkResourceRepository resourceRepository;
    private final NetworkEventRepository eventRepository;
    private final NetworkEventParticipantRepository participantRepository;
    private final NetworkDirectoryRepository directoryRepository;
    private final SecurityUtils securityUtils;

    public NetworkService(NetworkResourceRepository resourceRepository,
                          NetworkEventRepository eventRepository,
                          NetworkEventParticipantRepository participantRepository,
                          NetworkDirectoryRepository directoryRepository,
                          SecurityUtils securityUtils) {
        this.resourceRepository = resourceRepository;
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.directoryRepository = directoryRepository;
        this.securityUtils = securityUtils;
    }

    // ======================== RESSOURCES ========================

    /** Liste les ressources partagées par toutes les églises (visibles publiquement). */
    public List<NetworkResource> listSharedResources() {
        return resourceRepository.findBySharedWithPublicTrueAndIsActiveTrueOrderByCreatedAtDesc();
    }

    /** Liste les ressources d'une église spécifique. */
    public List<NetworkResource> listMyResources() {
        UUID tenantId = TenantContext.requireTenantId();
        return resourceRepository.findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    /** Recherche dans les ressources partagées. */
    public List<NetworkResource> searchResources(String query) {
        return resourceRepository.search(query);
    }

    /** Ressources par catégorie. */
    public List<NetworkResource> listByCategory(String category) {
        return resourceRepository.findBySharedWithPublicTrueAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(category);
    }

    /** Crée une ressource partagée. */
    @Transactional
    public NetworkResource createResource(NetworkResource resource) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = securityUtils.getCurrentUserId();
        resource.setTenantId(tenantId);
        resource.setSharedByUserId(userId);
        resource.setSharedWithPublic(true); // Les ressources créées dans le réseau sont publiques
        resource.setDownloads(0);
        resource.setIsActive(true);
        NetworkResource saved = resourceRepository.save(resource);
        log.info("[Network] Resource created: {} by tenant {}", saved.getId(), tenantId);
        return saved;
    }

    /** Incrémente le compteur de téléchargements (accessible si public ou si propriétaire). */
    @Transactional
    public NetworkResource incrementDownloads(UUID resourceId) {
        UUID tenantId = TenantContext.requireTenantId();
        NetworkResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("NetworkResource", resourceId));
        requireReadable(resource.getTenantId(), resource.getSharedWithPublic(), "resource");
        resource.setDownloads(resource.getDownloads() + 1);
        return resourceRepository.save(resource);
    }

    /** Désactive une ressource (soft delete). */
    @Transactional
    public void deactivateResource(UUID resourceId) {
        UUID tenantId = TenantContext.requireTenantId();
        NetworkResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("NetworkResource", resourceId));
        if (!resource.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cannot deactivate another church's resource");
        }
        resource.setIsActive(false);
        resourceRepository.save(resource);
        log.info("[Network] Resource deactivated: {} by tenant {}", resourceId, tenantId);
    }

    // ======================== ÉVÉNEMENTS ========================

    /** Liste les événements publics à venir. */
    public List<NetworkEvent> listUpcomingEvents() {
        List<NetworkEvent> events = eventRepository.findBySharedWithPublicTrueAndIsActiveTrueAndStartsAtAfterOrderByStartsAtAsc(LocalDateTime.now());
        markJoinedByMe(events);
        return events;
    }

    /** Liste tous les événements publics (y passés). */
    public List<NetworkEvent> listAllPublicEvents() {
        List<NetworkEvent> events = eventRepository.findBySharedWithPublicTrueAndIsActiveTrueOrderByStartsAtDesc();
        markJoinedByMe(events);
        return events;
    }

    /** Événements d'une église spécifique. */
    public List<NetworkEvent> listMyEvents() {
        UUID tenantId = TenantContext.requireTenantId();
        List<NetworkEvent> events = eventRepository.findByTenantIdAndIsActiveTrueOrderByStartsAtDesc(tenantId);
        markJoinedByMe(events);
        return events;
    }

    /** Événements par type. */
    public List<NetworkEvent> listByEventType(String eventType) {
        List<NetworkEvent> events = eventRepository.findBySharedWithPublicTrueAndEventTypeAndIsActiveTrueOrderByStartsAtAsc(eventType);
        markJoinedByMe(events);
        return events;
    }

    /** Crée un événement inter-églises. */
    @Transactional
    public NetworkEvent createEvent(NetworkEvent event) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = securityUtils.getCurrentUserId();
        event.setTenantId(tenantId);
        event.setCreatedByUserId(userId);
        event.setSharedWithPublic(true);
        event.setCurrentParticipants(0);
        event.setIsActive(true);
        NetworkEvent saved = eventRepository.save(event);
        log.info("[Network] Event created: {} by tenant {}", saved.getId(), tenantId);
        return saved;
    }

    /**
     * RSVP : s'inscrire à un événement (accessible si public ou si propriétaire).
     * Idempotent : un utilisateur déjà inscrit ne modifie pas le compteur.
     */
    @Transactional
    public NetworkEvent joinEvent(UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        NetworkEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("NetworkEvent", eventId));
        requireReadable(event.getTenantId(), event.getSharedWithPublic(), "event");
        if (event.getMaxParticipants() != null && event.getCurrentParticipants() >= event.getMaxParticipants()) {
            throw new IllegalStateException("Cet événement est complet.");
        }
        UUID userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur introuvable dans le contexte de sécurité.");
        }
        if (!participantRepository.existsByEventIdAndUserId(eventId, userId)) {
            NetworkEventParticipant participant = new NetworkEventParticipant();
            participant.setEventId(eventId);
            participant.setUserId(userId);
            participant.setTenantId(tenantId);
            participantRepository.save(participant);
            event.setCurrentParticipants(event.getCurrentParticipants() + 1);
            event = eventRepository.save(event);
        }
        event.setJoinedByMe(true);
        return event;
    }

    /**
     * Annuler RSVP : retire la participation réelle de l'utilisateur.
     * Idempotent : annuler sans être inscrit ne décrémente pas le compteur.
     */
    @Transactional
    public NetworkEvent leaveEvent(UUID eventId) {
        NetworkEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("NetworkEvent", eventId));
        requireReadable(event.getTenantId(), event.getSharedWithPublic(), "event");
        UUID userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur introuvable dans le contexte de sécurité.");
        }
        if (participantRepository.existsByEventIdAndUserId(eventId, userId)) {
            participantRepository.deleteByEventIdAndUserId(eventId, userId);
            event.setCurrentParticipants(Math.max(0, event.getCurrentParticipants() - 1));
            event = eventRepository.save(event);
        }
        event.setJoinedByMe(false);
        return event;
    }

    /** Désactive un événement. */
    @Transactional
    public void deactivateEvent(UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        NetworkEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("NetworkEvent", eventId));
        if (!event.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cannot deactivate another church's event");
        }
        event.setIsActive(false);
        eventRepository.save(event);
    }

    // ======================== RÉPERTOIRE ========================

    /** Liste toutes les églises volontairement listées. */
    public List<NetworkDirectory> listDirectory() {
        return directoryRepository.findByIsListedTrueOrderByChurchNameAsc();
    }

    /** Répertoire par pays. */
    public List<NetworkDirectory> listDirectoryByCountry(String country) {
        return directoryRepository.findByIsListedTrueAndCountryOrderByChurchNameAsc(country);
    }

    /** Recherche dans le répertoire. */
    public List<NetworkDirectory> searchDirectory(String name) {
        return directoryRepository.findByIsListedTrueAndChurchNameContainingIgnoreCaseOrderByChurchNameAsc(name);
    }

    /** Récupère ou crée l'entrée de l'église courante. */
    @Transactional
    public NetworkDirectory getOrCreateMyDirectoryEntry() {
        UUID tenantId = TenantContext.requireTenantId();
        return directoryRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    NetworkDirectory entry = new NetworkDirectory();
                    entry.setTenantId(tenantId);
                    entry.setIsListed(false);
                    return directoryRepository.save(entry);
                });
    }

    /** Met à jour l'entrée du répertoire de l'église courante. */
    @Transactional
    public NetworkDirectory updateMyDirectoryEntry(NetworkDirectory updates) {
        UUID tenantId = TenantContext.requireTenantId();
        NetworkDirectory entry = directoryRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    NetworkDirectory newEntry = new NetworkDirectory();
                    newEntry.setTenantId(tenantId);
                    return directoryRepository.save(newEntry);
                });

        if (updates.getChurchName() != null) entry.setChurchName(updates.getChurchName());
        if (updates.getCity() != null) entry.setCity(updates.getCity());
        if (updates.getCountry() != null) entry.setCountry(updates.getCountry());
        if (updates.getDenomination() != null) entry.setDenomination(updates.getDenomination());
        if (updates.getPastorName() != null) entry.setPastorName(updates.getPastorName());
        if (updates.getContactEmail() != null) entry.setContactEmail(updates.getContactEmail());
        if (updates.getContactPhone() != null) entry.setContactPhone(updates.getContactPhone());
        if (updates.getWebsite() != null) entry.setWebsite(updates.getWebsite());
        if (updates.getMemberCount() != null) entry.setMemberCount(updates.getMemberCount());
        if (updates.getDescription() != null) entry.setDescription(updates.getDescription());
        if (updates.getLatitude() != null) entry.setLatitude(updates.getLatitude());
        if (updates.getLongitude() != null) entry.setLongitude(updates.getLongitude());

        return directoryRepository.save(entry);
    }

    /** Active/désactive le listing public de l'église. */
    @Transactional
    public NetworkDirectory toggleListing(boolean listed) {
        UUID tenantId = TenantContext.requireTenantId();
        NetworkDirectory entry = getOrCreateMyDirectoryEntry();
        if (listed && (entry.getChurchName() == null || entry.getChurchName().isBlank())) {
            throw new IllegalStateException(
                    "Le nom de l'église est obligatoire avant de publier l'entrée dans l'annuaire.");
        }
        entry.setIsListed(listed);
        if (listed) {
            entry.setListedAt(LocalDateTime.now());
        }
        return directoryRepository.save(entry);
    }

    // ======================== SÉCURITÉ ========================

    /**
     * Champ dérivé `joinedByMe` : marque les événements auxquels l'utilisateur
     * courant est inscrit (traçabilité via network_event_participants).
     */
    private void markJoinedByMe(List<NetworkEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        UUID userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }
        List<UUID> eventIds = events.stream().map(NetworkEvent::getId).toList();
        Set<UUID> joinedEventIds = participantRepository.findByEventIdInAndUserId(eventIds, userId)
                .stream()
                .map(NetworkEventParticipant::getEventId)
                .collect(Collectors.toSet());
        events.forEach(e -> e.setJoinedByMe(joinedEventIds.contains(e.getId())));
    }

    /**
     * Garde anti-IDOR : une ressource/événement d'une AUTRE église n'est lisible
     * que si elle a explicitement été partagée avec le réseau (public).
     */
    private void requireReadable(UUID ownerTenantId, boolean sharedWithPublic, String type) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!sharedWithPublic && !ownerTenantId.equals(tenantId)) {
            log.warn("[Network] Blocked cross-tenant access to private {} from tenant {}", type, tenantId);
            throw new SecurityException("Cette " + type + " n'est pas partagée avec le réseau.");
        }
    }

    // ======================== STATISTIQUES ========================

    /** Résumé du réseau. */
    public Map<String, Object> getNetworkStats() {
        UUID tenantId = TenantContext.requireTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSharedResources", resourceRepository.countByTenantIdAndIsActiveTrue(tenantId));
        stats.put("totalPublicEvents", eventRepository.countByTenantIdAndIsActiveTrue(tenantId));
        stats.put("totalListedChurches", directoryRepository.countByIsListedTrue());
        stats.put("myResources", resourceRepository.countByTenantIdAndIsActiveTrue(tenantId));
        stats.put("myEvents", eventRepository.countByTenantIdAndIsActiveTrue(tenantId));
        return stats;
    }
}
