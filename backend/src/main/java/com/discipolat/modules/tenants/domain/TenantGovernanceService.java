package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
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

    /** Blocage temporaire : le tenant devient SUSPENDED (garde de requêtes active). */
    public Tenant block(UUID tenantId, String reason) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            throw new DomainException("Tenant déjà bloqué", HttpStatus.CONFLICT, "TENANT_ALREADY_BLOCKED");
        }
        if (tenant.getStatus() == TenantStatus.CANCELLED) {
            throw new DomainException("Tenant banni — utiliser la réintégration",
                    HttpStatus.CONFLICT, "TENANT_BANNED");
        }
        return changeStatus(tenant, TenantStatus.SUSPENDED, "TENANT_BLOCKED", reason);
    }

    /** Déblocage : SUSPENDED → ACTIVE. */
    public Tenant unblock(UUID tenantId, String reason) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.SUSPENDED) {
            throw new DomainException("Seul un tenant bloqué se débloque",
                    HttpStatus.CONFLICT, "TENANT_NOT_BLOCKED");
        }
        return changeStatus(tenant, TenantStatus.ACTIVE, "TENANT_UNBLOCKED", reason);
    }

    /** Bannissement : statut CANCELLED (suppression logique, données conservées). */
    public Tenant ban(UUID tenantId, String reason) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() == TenantStatus.CANCELLED) {
            throw new DomainException("Tenant déjà banni", HttpStatus.CONFLICT, "TENANT_ALREADY_BANNED");
        }
        return changeStatus(tenant, TenantStatus.CANCELLED, "TENANT_BANNED", reason);
    }

    /** Réintégration après bannissement : CANCELLED → ACTIVE. */
    public Tenant unban(UUID tenantId, String reason) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.CANCELLED) {
            throw new DomainException("Seul un tenant banni se réintègre",
                    HttpStatus.CONFLICT, "TENANT_NOT_BANNED");
        }
        return changeStatus(tenant, TenantStatus.ACTIVE, "TENANT_UNBANNED", reason);
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

    private Tenant changeStatus(Tenant tenant, TenantStatus newStatus, String action, String reason) {
        TenantStatus previous = tenant.getStatus();
        tenant.setStatus(newStatus);
        Tenant saved = tenantRepository.save(tenant);
        // Invalid immédiat du cache de statut (TenantStatusGuard) + propagation
        // multi-instance, comme TenantService.deactivate/reactivate.
        eventPublisher.publishEvent(new TenantStatusChangedEvent(saved.getId(), previous, newStatus));
        propagationPublisher.publishStatusChanged("TENANT", saved.getId(),
                previous.name(), newStatus.name(),
                action + ": " + saved.getName() + (reason == null || reason.isBlank() ? "" : " — " + reason.trim()));
        auditService.logSimple(action, "TENANT", saved.getId());
        return saved;
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
