package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour la gestion des appartenances utilisateur-tenant.
 * Permet à un utilisateur d'appartenir à plusieurs tenants avec des rôles différents.
 */
@Service
@Transactional
public class TenantMembershipService {

    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public TenantMembershipService(TenantMembershipRepository membershipRepository,
                                   UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   AuditService auditService,
                                   EntityPropagationPublisher propagationPublisher) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    /**
     * Ajoute un utilisateur à un tenant avec un rôle donné
     */
    public TenantMembership addMembership(UUID userId, UUID tenantId, String role, UUID invitedBy) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // Vérifier que le tenant existe
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));

        // Vérifier si l'appartenance existe déjà
        Optional<TenantMembership> existing = membershipRepository.findByUserIdAndTenantId(userId, tenantId);
        if (existing.isPresent()) {
            TenantMembership membership = existing.get();
            if (membership.getStatus() == MembershipStatus.ACTIVE) {
                throw new BusinessRuleException(
                        "L'utilisateur est déjà membre actif de ce tenant", "MEMBERSHIP_EXISTS");
            }
            // Réactiver si inactif/pending
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setRole(role);
            membership.setInvitedBy(invitedBy);
            membership = membershipRepository.save(membership);
            auditMembership(membership, "MEMBERSHIP_REACTIVATED", invitedBy);
            return membership;
        }

        TenantMembership membership = TenantMembership.builder()
                .userId(userId)
                .tenantId(tenantId)
                .role(role)
                .status(MembershipStatus.ACTIVE)
                .invitedBy(invitedBy)
                .build();

        membership = membershipRepository.save(membership);

        // Mettre à jour le tenant_id de l'utilisateur si c'est son premier tenant
        // (pour compatibilité arrière avec l'ancien modèle single-tenant)
        if (user.getTenantId() == null) {
            user.setTenantId(tenantId);
            userRepository.save(user);
        }

        auditMembership(membership, "MEMBERSHIP_CREATED", invitedBy);
        propagationPublisher.publishCreated("TENANT_MEMBERSHIP", membership.getId(),
                Map.of("userId", userId, "tenantId", tenantId, "role", role),
                "Membership créé: user=" + userId + " tenant=" + tenantId + " role=" + role);

        return membership;
    }

    /**
     * Change le rôle d'un utilisateur dans un tenant
     */
    public TenantMembership changeRole(UUID userId, UUID tenantId, String newRole, UUID changedBy) {
        TenantMembership membership = membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("TenantMembership", userId + ":" + tenantId));

        String oldRole = membership.getRole();
        membership.setRole(newRole);
        membership = membershipRepository.save(membership);

        auditService.log(
                changedBy != null ? changedBy : userId,
                tenantId,
                "MEMBERSHIP_ROLE_CHANGED",
                "TENANT_MEMBERSHIP",
                membership.getId(),
                "SUCCESS",
                Map.of("oldRole", oldRole, "newRole", newRole),
                null, null, null
        );

        return membership;
    }

    /**
     * Désactive (soft delete) une appartenance
     */
    public void removeMembership(UUID userId, UUID tenantId, UUID removedBy) {
        TenantMembership membership = membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("TenantMembership", userId + ":" + tenantId));

        membership.setStatus(MembershipStatus.REVOKED);
        membershipRepository.save(membership);

        auditService.log(
                removedBy != null ? removedBy : userId,
                tenantId,
                "MEMBERSHIP_REVOKED",
                "TENANT_MEMBERSHIP",
                membership.getId(),
                "SUCCESS",
                Map.of("role", membership.getRole()),
                null, null, null
        );
    }

    /**
     * Récupère toutes les appartenances actives d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<TenantMembership> getUserMemberships(UUID userId) {
        return membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
    }

    /**
     * Récupère l'appartenance active d'un utilisateur pour un tenant
     */
    @Transactional(readOnly = true)
    public Optional<TenantMembership> getMembership(UUID userId, UUID tenantId) {
        return membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE);
    }

    /**
     * Vérifie si un utilisateur a accès à un tenant
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, UUID tenantId) {
        return membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
    }

    /**
     * Récupère tous les membres d'un tenant
     */
    @Transactional(readOnly = true)
    public List<TenantMembership> getTenantMembers(UUID tenantId) {
        return membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
    }

    /**
     * Compte les membres actifs d'un tenant
     */
    @Transactional(readOnly = true)
    public long countActiveMembers(UUID tenantId) {
        return membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
    }

    private void auditMembership(TenantMembership membership, String action, UUID actorId) {
        auditService.log(
                actorId != null ? actorId : membership.getUserId(),
                membership.getTenantId(),
                action,
                "TENANT_MEMBERSHIP",
                membership.getId(),
                "SUCCESS",
                Map.of("userId", membership.getUserId(), "role", membership.getRole()),
                null, null, null
        );
    }
}