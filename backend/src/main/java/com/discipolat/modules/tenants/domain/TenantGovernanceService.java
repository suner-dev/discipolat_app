package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.discipolat.common.exception.DomainException;

/**
 * SPEC_ONBOARDING_FLOWS (BE-5, D5) — gouvernance plateforme par le Super Admin :
 * blocage (SUSPENDED), bannissement (CANCELLED), réactivation, avertissements
 * et litiges. Le statut n'est jamais supprimé de {@code tenants} : la raison
 * est tracée ici (warning) et dans le journal d'audit chaîné.
 *
 * <p>Les tables {@code tenant_warnings}/{@code tenant_disputes} sont GLOBALES
 * (comme {@code tenants}) : aucun filtre tenant, accès réservé console
 * plateforme.
 */
@Service
@Transactional
public class TenantGovernanceService {

    /**
     * Longueur maximale du message persisté — colonne
     * {@code tenant_warnings.message VARCHAR(1000)} ({@code TenantWarning:32}).
     */
    static final int MAX_MESSAGE_LENGTH = 1000;

    /**
     * Marge réservée pour le préfixe d'action qu'on concatène au motif
     * (« Église bannie : … », « Église réintégrée : … »).
     *
     * <p>Ce n'est pas une précaution théorique : la première version limitait
     * le <i>motif</i> à 1000 puis préfixait le message, si bien qu'un motif
     * de 999 caractères produisait une ligne plus longue que la colonne et
     * que l'action de gouvernance entière était annulée par une violation de
     * longueur — un bannissement impossible à effectuer, avec une erreur 500
     * au lieu d'un refus explicite. La marge est dimensionnée au plus long
     * libellé utilisé ({@code "Église réintégrée : "} = 20 caractères) arrondi
     * à 32, pour qu'ajouter une traduction ne casse rien.
     */
    static final int LABEL_HEADROOM = 32;

    /** Longueur maximale du MOTIF seul (donc toujours insérable). */
    static final int MAX_REASON_LENGTH = MAX_MESSAGE_LENGTH - LABEL_HEADROOM;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(TenantGovernanceService.class);

    private final TenantRepository tenantRepository;
    private final TenantWarningRepository warningRepository;
    private final TenantDisputeRepository disputeRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final ApplicationEventPublisher eventPublisher;

    public TenantGovernanceService(TenantRepository tenantRepository,
                                   TenantWarningRepository warningRepository,
                                   TenantDisputeRepository disputeRepository,
                                   AuditService auditService,
                                   EntityPropagationPublisher propagationPublisher,
                                   ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.warningRepository = warningRepository;
        this.disputeRepository = disputeRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.eventPublisher = eventPublisher;
    }

    // ======================== BLOCAGE / BANNISSEMENT ========================

    /**
     * Blocage temporaire : le tenant devient SUSPENDED (garde de requêtes active).
     *
     * <p><b>F11 — le motif est OBLIGATOIRE et PERSISTÉ.</b> L'IHM de gouvernance
     * annonce « chaque action est auditée » et propose un champ « motif » ; or
     * l'ancien service l'acceptait vide et ne l'écrivait nulle part : un
     * bannissement restait inexpliquable après coup. Le motif est désormais
     * rejeté s'il est vide (400) et conservé dans {@code tenant_warnings}
     * (gravité {@code FORMAL} pour un blocage, {@code FINAL} pour un
     * bannissement) ainsi que dans le journal d'audit chaîné.
     */
    public Tenant block(UUID tenantId, String reason, UUID actorId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            throw new DomainException("Tenant déjà bloqué", HttpStatus.CONFLICT, "TENANT_ALREADY_BLOCKED");
        }
        if (tenant.getStatus() == TenantStatus.CANCELLED) {
            throw new DomainException("Tenant banni — utiliser la réintégration",
                    HttpStatus.CONFLICT, "TENANT_BANNED");
        }
        String motive = requireReason(reason);
        Tenant changed = changeStatus(tenant, TenantStatus.SUSPENDED, "TENANT_BLOCKED", motive, actorId);
        recordGovernanceReason(tenantId, actorId, TenantWarning.Severity.FORMAL,
                "Église bloquée : " + motive);
        return changed;
    }

    /** Déblocage : SUSPENDED → ACTIVE. */
    public Tenant unblock(UUID tenantId, String reason, UUID actorId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.SUSPENDED) {
            throw new DomainException("Seul un tenant bloqué se débloque",
                    HttpStatus.CONFLICT, "TENANT_NOT_BLOCKED");
        }
        String motive = requireReason(reason);
        Tenant changed = changeStatus(tenant, TenantStatus.ACTIVE, "TENANT_UNBLOCKED", motive, actorId);
        recordGovernanceReason(tenantId, actorId, TenantWarning.Severity.INFO,
                "Église réactivée : " + motive);
        return changed;
    }

    /**
     * Bannissement : statut CANCELLED (suppression logique, données conservées).
     *
     * <p>Action quasi <b>irréversible</b> pour l'église : le motif est donc
     * exigé (F11) et tracé à gravité {@code FINAL}.
     */
    public Tenant ban(UUID tenantId, String reason, UUID actorId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() == TenantStatus.CANCELLED) {
            throw new DomainException("Tenant déjà banni", HttpStatus.CONFLICT, "TENANT_ALREADY_BANNED");
        }
        String motive = requireReason(reason);
        Tenant changed = changeStatus(tenant, TenantStatus.CANCELLED, "TENANT_BANNED", motive, actorId);
        recordGovernanceReason(tenantId, actorId, TenantWarning.Severity.FINAL,
                "Église bannie : " + motive);
        return changed;
    }

    /** Réintégration après bannissement : CANCELLED → ACTIVE. */
    public Tenant unban(UUID tenantId, String reason, UUID actorId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.CANCELLED) {
            throw new DomainException("Seul un tenant banni se réintègre",
                    HttpStatus.CONFLICT, "TENANT_NOT_BANNED");
        }
        String motive = requireReason(reason);
        Tenant changed = changeStatus(tenant, TenantStatus.ACTIVE, "TENANT_UNBANNED", motive, actorId);
        recordGovernanceReason(tenantId, actorId, TenantWarning.Severity.INFO,
                "Église réintégrée : " + motive);
        return changed;
    }

    // ======================== AVERTISSEMENTS ========================

    public TenantWarning warn(UUID tenantId, String message, TenantWarning.Severity severity, UUID actorId) {
        requireTenant(tenantId);
        if (message == null || message.isBlank()) {
            throw new DomainException("Message d'avertissement requis",
                    HttpStatus.BAD_REQUEST, "WARNING_MESSAGE_REQUIRED");
        }
        TenantWarning warning = TenantWarning.builder()
                .tenantId(tenantId)
                .message(message.trim())
                .severity(severity == null ? TenantWarning.Severity.INFO : severity)
                .createdBy(actorId)
                .build();
        TenantWarning saved = warningRepository.save(warning);
        auditService.logSimple("TENANT_WARNED_" + saved.getSeverity().name(), "TENANT", tenantId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TenantWarning> listWarnings(UUID tenantId) {
        requireTenant(tenantId);
        return warningRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    // ======================== LITIGES ========================

    public TenantDispute openDispute(UUID tenantId, String subject, String description, UUID actorId) {
        requireTenant(tenantId);
        if (subject == null || subject.isBlank()) {
            throw new DomainException("Objet de litige requis",
                    HttpStatus.BAD_REQUEST, "DISPUTE_SUBJECT_REQUIRED");
        }
        TenantDispute dispute = TenantDispute.builder()
                .tenantId(tenantId)
                .subject(subject.trim())
                .description(description == null || description.isBlank() ? null : description.trim())
                .status(TenantDispute.DisputeStatus.OPEN)
                .openedBy(actorId)
                .build();
        TenantDispute saved = disputeRepository.save(dispute);
        auditService.logSimple("TENANT_DISPUTE_OPENED", "TENANT", tenantId);
        return saved;
    }

    public TenantDispute updateDispute(UUID disputeId, TenantDispute.DisputeStatus status,
                                       String resolution, UUID actorId) {
        TenantDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new EntityNotFoundException("TenantDispute", disputeId));
        if (status != null) {
            dispute.setStatus(status);
        }
        if (resolution != null && !resolution.isBlank()) {
            dispute.setResolution(resolution.trim());
        }
        if (dispute.getStatus() == TenantDispute.DisputeStatus.CLOSED) {
            dispute.setClosedAt(Instant.now());
            dispute.setClosedBy(actorId);
        } else {
            dispute.setClosedAt(null);
            dispute.setClosedBy(null);
        }
        TenantDispute saved = disputeRepository.save(dispute);
        auditService.logSimple("TENANT_DISPUTE_" + saved.getStatus().name(), "TENANT", saved.getTenantId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TenantDispute> listDisputes(UUID tenantId) {
        requireTenant(tenantId);
        return disputeRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<TenantDispute> listOpenDisputes() {
        return disputeRepository.findByStatusOrderByCreatedAtDesc(TenantDispute.DisputeStatus.OPEN);
    }

    // ======================== INTERNAL ========================

    /**
     * Applique un changement de statut, l'audite ET conserve le motif (F11).
     *
     * <p>L'audit reçoit désormais le motif dans ses métadonnées : l'ancien
     * appel {@code logSimple(action, "TENANT", id)} n'enregistrait que le nom
     * de l'action, ce qui rendait la décision inexplicable à la relecture.
     * Le journal chaîné par hash conserve ces métadonnées.
     */
    private Tenant changeStatus(Tenant tenant, TenantStatus newStatus, String action,
                                String reason, UUID actorId) {
        TenantStatus previous = tenant.getStatus();
        tenant.setStatus(newStatus);
        Tenant saved = tenantRepository.save(tenant);
        // Invalid immédiat du cache de statut (TenantStatusGuard) + propagation
        // multi-instance, comme TenantService.deactivate/reactivate.
        eventPublisher.publishEvent(new TenantStatusChangedEvent(saved.getId(), previous, newStatus));
        propagationPublisher.publishStatusChanged("TENANT", saved.getId(),
                previous.name(), newStatus.name(),
                action + ": " + saved.getName() + (reason == null || reason.isBlank() ? "" : " — " + reason.trim()));
        auditService.log(actorId, saved.getId(), action, "TENANT", saved.getId(),
                "SUCCESS",
                Payloads.of("tenantName", saved.getName(),
                        "previousStatus", previous.name(),
                        "newStatus", newStatus.name(),
                        "reason", reason),
                null, null, null);
        return saved;
    }

    /**
     * Motif obligatoire (F11, D18) : une action de gouvernance sans motif
     * n'est pas traçable, donc refusée.
     */
    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("Un motif est obligatoire pour cette action",
                    HttpStatus.BAD_REQUEST, "GOVERNANCE_REASON_REQUIRED");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new DomainException("Motif trop long (" + MAX_REASON_LENGTH + " caractères maximum)",
                    HttpStatus.BAD_REQUEST, "GOVERNANCE_REASON_TOO_LONG");
        }
        return trimmed;
    }

    /**
     * Trace le motif de gouvernance dans {@code tenant_warnings} (F11).
     *
     * <p><b>Atomicité plutôt que best-effort.</b> Une première version
     * absorbait l'échec — mais alors un bannissement pouvait être appliqué sans
     * motif conservé, exactement l'état inexpliquable que la spec veut
     * supprimer. Ici l'exception est journalisée puis <b>relancée</b> : les
     * deux écritures sont dans la même transaction, donc l'action de
     * gouvernance est annulée avec elle. On préfère une décision non
     * appliquée à une décision appliquée sans justification.
     *
     * <p>Le {@code log.warn} sert au diagnostic : il conserve la cause racine
     * même quand la transaction est annulée et que la stack trace remonte au
     * client sous forme d'erreur 500.
     */
    private void recordGovernanceReason(UUID tenantId, UUID actorId,
                                        TenantWarning.Severity severity, String message) {
        // Garde-fou STRUCTUREL : `MAX_REASON_LENGTH` réserve déjà la marge du
        // préfixe, mais cette assertion protège du futur refactor — quelqu'un
        // qui rallonge un libellé, ou qui passe un message composé à la main,
        // obtiendra ici une erreur explicite en développement plutôt qu'une
        // violation de colonne en production (500 sur un bannissement).
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalStateException(
                    "Message de gouvernance trop long pour tenant_warnings.message ("
                            + message.length() + " > " + MAX_MESSAGE_LENGTH
                            + "). Motif limité à " + MAX_REASON_LENGTH
                            + " caractères : allonger un libellé ? Augmenter "
                            + "LABEL_HEADROOM ou migrer la colonne.");
        }
        try {
            warningRepository.save(TenantWarning.builder()
                    .tenantId(tenantId)
                    .message(message)
                    .severity(severity)
                    .createdBy(actorId)
                    .build());
        } catch (RuntimeException traceFailure) {
            log.warn("Trace de gouvernance non écrite pour le tenant {} : {}", tenantId, traceFailure.getMessage());
            throw traceFailure;
        }
    }

    private Tenant requireTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new DomainException("Identifiant de tenant requis",
                    HttpStatus.BAD_REQUEST, "TENANT_ID_REQUIRED");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
    }
}
