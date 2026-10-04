package com.discipolat.modules.announcements.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SPEC_ONBOARDING_FLOWS (BE-4, D4) — workflow d'annonces publiques :
 * cohabite avec l'AnnouncementService historique (annonces internes
 * ScheduledAnnouncement) sans le modifier.
 * l'église rédige (DRAFT) puis soumet (PENDING_MODERATION) ; le Super Admin
 * approuve (PUBLISHED) ou refuse (REJECTED, avec note) ; le scheduler expire
 * les annonces passées. La liste publique ne comporte AUCUNE PII et jamais
 * de tenant_id.
 */
@Service
public class PublicAnnouncementService {

    private static final int PUBLIC_LIMIT = 20;

    private final PublicAnnouncementRepository announcementRepository;
    private final TenantRepository tenantRepository;
    private final CrossTenantScopeAccess crossTenant;
    private final AuditService auditService;

    public PublicAnnouncementService(PublicAnnouncementRepository announcementRepository,
                               TenantRepository tenantRepository,
                               CrossTenantScopeAccess crossTenant,
                               AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.tenantRepository = tenantRepository;
        this.crossTenant = crossTenant;
        this.auditService = auditService;
    }

    public record AnnouncementInput(String title, String description, String imageUrl,
                                    String city, String country, Instant eventAt,
                                    String linkUrl, String accessRef, Instant expiresAt) {
    }

    // ======================== TENANT (admin église) ========================

    @Transactional
    public PublicAnnouncement create(UUID tenantId, UUID actorId, AnnouncementInput input) {
        require(input.title() != null && !input.title().isBlank(), "TITLE_REQUIRED",
                "Le titre de l'annonce est requis");
        requireValidPublicUrl(input.linkUrl(), "LINK_URL_INVALID");
        requireValidPublicUrl(input.imageUrl(), "IMAGE_URL_INVALID");
        PublicAnnouncement announcement = PublicAnnouncement.builder()
                .tenantId(tenantId)
                .title(input.title().trim())
                .description(trimToNull(input.description()))
                .imageUrl(trimToNull(input.imageUrl()))
                .city(trimToNull(input.city()))
                .country(trimToNull(input.country()))
                .eventAt(input.eventAt())
                .linkUrl(trimToNull(input.linkUrl()))
                .accessRef(trimToNull(input.accessRef()))
                .expiresAt(input.expiresAt())
                .status(AnnouncementStatus.DRAFT)
                .createdBy(actorId)
                .build();
        PublicAnnouncement saved = announcementRepository.save(announcement);
        auditService.logSimple("ANNOUNCEMENT_CREATED", "PUBLIC_ANNOUNCEMENT", saved.getId());
        return saved;
    }

    @Transactional
    public PublicAnnouncement update(UUID id, UUID tenantId, AnnouncementInput input) {
        PublicAnnouncement announcement = requireOwned(id, tenantId);
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new DomainException("Dépublier avant modification",
                    HttpStatus.CONFLICT, "ANNOUNCEMENT_LOCKED");
        }
        // F8 : validation aussi en modification, pas seulement à la création —
        // sinon un `PUT` contourne la règle posée sur le `POST`.
        requireValidPublicUrl(input.linkUrl(), "LINK_URL_INVALID");
        requireValidPublicUrl(input.imageUrl(), "IMAGE_URL_INVALID");
        if (input.title() != null && !input.title().isBlank()) announcement.setTitle(input.title().trim());
        announcement.setDescription(trimToNull(input.description()));
        announcement.setImageUrl(trimToNull(input.imageUrl()));
        announcement.setCity(trimToNull(input.city()));
        announcement.setCountry(trimToNull(input.country()));
        announcement.setEventAt(input.eventAt());
        announcement.setLinkUrl(trimToNull(input.linkUrl()));
        announcement.setAccessRef(trimToNull(input.accessRef()));
        announcement.setExpiresAt(input.expiresAt());
        return announcementRepository.save(announcement);
    }

    @Transactional
    public PublicAnnouncement submit(UUID id, UUID tenantId) {
        PublicAnnouncement announcement = requireOwned(id, tenantId);
        if (announcement.getStatus() != AnnouncementStatus.DRAFT
                && announcement.getStatus() != AnnouncementStatus.REJECTED) {
            throw new DomainException("Seules les annonces en brouillon ou refusées se soumettent",
                    HttpStatus.CONFLICT, "ANNOUNCEMENT_STATE_INVALID");
        }
        announcement.setStatus(AnnouncementStatus.PENDING_MODERATION);
        announcement.setModerationNote(null);
        return announcementRepository.save(announcement);
    }

    /** Republie directement par l'admin église si elle était publiée (retrait = DRAFT). */
    @Transactional
    public PublicAnnouncement unpublish(UUID id, UUID tenantId) {
        PublicAnnouncement announcement = requireOwned(id, tenantId);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new DomainException("Annonce non publiée",
                    HttpStatus.CONFLICT, "ANNOUNCEMENT_STATE_INVALID");
        }
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setPublishedAt(null);
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void delete(UUID id, UUID tenantId) {
        PublicAnnouncement announcement = requireOwned(id, tenantId);
        announcementRepository.delete(announcement);
        auditService.logSimple("ANNOUNCEMENT_DELETED", "PUBLIC_ANNOUNCEMENT", id);
    }

    @Transactional(readOnly = true)
    public List<PublicAnnouncement> listForTenant(UUID tenantId) {
        return announcementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    // ======================== PLATEFORME (modération) ========================

    @Transactional(readOnly = true)
    public List<PublicAnnouncement> moderationQueue(String status) {
        AnnouncementStatus filter = status == null || status.isBlank()
                ? AnnouncementStatus.PENDING_MODERATION
                : parseStatus(status);
        // Filtre suspendu : la console plateforme traverse tous les tenants.
        return crossTenant.call(() -> announcementRepository
                .findByStatusOrderByCreatedAtDesc(filter));
    }

    @Transactional
    public PublicAnnouncement approve(UUID id, UUID moderatorId) {
        PublicAnnouncement announcement = requireInStates(id, AnnouncementStatus.PENDING_MODERATION);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(Instant.now());
        announcement.setModeratedBy(moderatorId);
        announcement.setModeratedAt(Instant.now());
        announcement.setModerationNote(null);
        PublicAnnouncement saved = announcementRepository.save(announcement);
        auditService.logSimple("ANNOUNCEMENT_PUBLISHED", "PUBLIC_ANNOUNCEMENT", id);
        return saved;
    }

    @Transactional
    public PublicAnnouncement reject(UUID id, UUID moderatorId, String note) {
        PublicAnnouncement announcement = requireInStates(id, AnnouncementStatus.PENDING_MODERATION);
        announcement.setStatus(AnnouncementStatus.REJECTED);
        announcement.setModeratedBy(moderatorId);
        announcement.setModeratedAt(Instant.now());
        announcement.setModerationNote(trimToNull(note));
        PublicAnnouncement saved = announcementRepository.save(announcement);
        auditService.logSimple("ANNOUNCEMENT_REJECTED", "PUBLIC_ANNOUNCEMENT", id);
        return saved;
    }

    // ======================== PUBLIC (landing) ========================

    /**
     * Vitrine publique du carrousel du landing.
     *
     * <p><b>SPF ORGANISATION DENOMINATION V2 §3 / D8 et §7.1 / T-B6 (faille F19).</b>
     * Le code de rejointure n'est <b>jamais</b> publié. La version initiale
     * renvoyait {@code accessRef} tel quel : une église qui collait son code
     * dans une annonce s'ouvrait à quiconque lisait la page d'accueil, sans
     * contrôle, et le code y restait même après rotation ou désactivation.
     *
     * <p>La carte renvoie à la place le <b>lien d'invitation</b> {@code /j/<slug>} :
     * c'est la même porte, sans exposer le secret. Le lien mène à la page de
     * rejointure où le mode {@code OPEN}/{@code APPROVAL} du code continue de
     * régner — l'exigence de validation n'est pas contournée, elle est
     * déplacée là où elle a lieu.
     *
     * <p>La réponse ne contient par ailleurs <b>ni tenant_id, ni email, ni nom de
     * membre</b> : uniquement de quoi identifier l'organisatrice et
     * l'événement.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicFeed() {
        List<PublicAnnouncement> visible = crossTenant.call(() -> announcementRepository.findVisible(
                AnnouncementStatus.PUBLISHED, Instant.now(), PageRequest.of(0, PUBLIC_LIMIT)));
        if (visible.isEmpty()) {
            return List.of();
        }
        Map<UUID, Tenant> churches = tenantRepository.findAllById(visible.stream()
                        .map(PublicAnnouncement::getTenantId).distinct().toList())
                .stream()
                .filter(t -> t.getStatus() == com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE)
                .collect(Collectors.toMap(Tenant::getId, t -> t, (a, b) -> a));
        return visible.stream()
                .filter(a -> churches.containsKey(a.getTenantId()))
                .map(a -> {
                    Tenant church = churches.get(a.getTenantId());
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("title", a.getTitle());
                    view.put("description", a.getDescription());
                    view.put("churchName", church.getName());
                    view.put("city", a.getCity());
                    view.put("country", a.getCountry());
                    view.put("eventAt", a.getEventAt());
                    view.put("imageUrl", a.getImageUrl());
                    view.put("linkUrl", a.getLinkUrl());
                    // D8 (F19) : on publie le LIEN d'invitation, jamais le code.
                    // `accessRef` n'est volontairement pas repris ici.
                    view.put("invitePath", church.getSlug() == null ? null : "/j/" + church.getSlug());
                    return view;
                })
                .toList();
    }

    // ======================== SCHEDULER ========================

    /** Quotidien à 03h40 : les PUBLISHED dont la date de fin est passée deviennent EXPIRED. */
    @Scheduled(cron = "0 40 3 * * *")
    @Transactional
    public void expireStaleAnnouncements() {
        List<PublicAnnouncement> stale = crossTenant.call(
                () -> announcementRepository.findStale(AnnouncementStatus.PUBLISHED, Instant.now()));
        for (PublicAnnouncement announcement : stale) {
            announcement.setStatus(AnnouncementStatus.EXPIRED);
        }
        if (!stale.isEmpty()) {
            announcementRepository.saveAll(stale);
            auditService.logSimple("ANNOUNCEMENTS_EXPIRED", "PLATFORM", null);
        }
    }

    // ======================== INTERNAL ========================

    private PublicAnnouncement requireOwned(UUID id, UUID tenantId) {
        PublicAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new DomainException("Annonce introuvable",
                        HttpStatus.NOT_FOUND, "ANNOUNCEMENT_NOT_FOUND"));
        if (tenantId == null || !announcement.getTenantId().equals(tenantId)) {
            throw new DomainException("Annonce introuvable",
                    HttpStatus.NOT_FOUND, "ANNOUNCEMENT_NOT_FOUND");
        }
        return announcement;
    }

    private PublicAnnouncement requireInStates(UUID id, AnnouncementStatus... expected) {
        PublicAnnouncement announcement = crossTenant.call(
                () -> announcementRepository.findById(id).orElse(null));
        if (announcement == null) {
            throw new DomainException("Annonce introuvable",
                    HttpStatus.NOT_FOUND, "ANNOUNCEMENT_NOT_FOUND");
        }
        for (AnnouncementStatus status : expected) {
            if (announcement.getStatus() == status) {
                return announcement;
            }
        }
        throw new DomainException("État d'annonce invalide : " + announcement.getStatus(),
                HttpStatus.CONFLICT, "ANNOUNCEMENT_STATE_INVALID");
    }

    private AnnouncementStatus parseStatus(String raw) {
        try {
            return AnnouncementStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Statut inconnu : " + raw,
                    HttpStatus.BAD_REQUEST, "ANNOUNCEMENT_STATUS_INVALID");
        }
    }

    private void require(boolean condition, String code, String message) {
        if (!condition) {
            throw new DomainException(message, HttpStatus.BAD_REQUEST, code);
        }
    }

    /**
     * N'autorise que {@code http}/{@code https} pour les URL fournies par une
     * église et rendues sur la page d'accueil publique.
     *
     * <p><b>SPF ORGANISATION DENOMINATION V2 §7.1 / T-B6 (faille F8).</b> Ces
     * deux champs sont saisis par un administrateur d'église puis rendus sur
     * le <b>domaine public</b> : {@code SectionAnnouncements.tsx:86-90} les pose
     * en {@code href}/{@code src}. Sans contrainte de schéma, une valeur
     * {@code javascript:…} s'exécute dans l'origine du site dès qu'un visiteur
     * clique — et la CSP de {@code render.yaml:199} autorise
     * {@code 'unsafe-inline'}, qui ne bloque pas ce cas.
     *
     * <p>La validation est faite <b>ici</b>, côté serveur : c'est le seul point
     * où l'on est sûr que la règle s'applique à tous les chemins (create, update,
     * imports futurs). Une validation purement front-end serait contournable.
     *
     * <p>{@code null} / vide = champ non fourni, ce qui est légitime.
     */
    private void requireValidPublicUrl(String rawUrl, String errorCode) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        String url = rawUrl.trim();
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new DomainException(
                    "Lien non autorisé : seules les adresses http:// ou https:// sont acceptées",
                    HttpStatus.BAD_REQUEST, errorCode);
        }
        try {
            java.net.URI parsed = new java.net.URI(url);
            if (parsed.getHost() == null || parsed.getHost().isBlank()) {
                throw new DomainException("Lien non autorisé : hôte introuvable",
                        HttpStatus.BAD_REQUEST, errorCode);
            }
        } catch (java.net.URISyntaxException malformed) {
            throw new DomainException("Lien non autorisé : adresse invalide",
                    HttpStatus.BAD_REQUEST, errorCode);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
