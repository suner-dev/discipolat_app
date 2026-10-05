package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TenantMembershipService {

    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public TenantMembershipService(TenantMembershipRepository membershipRepository,
                                    UserRepository userRepository,
                                    TenantRepository tenantRepository,
                                    RoleRepository roleRepository,
                                    AuditService auditService,
                                    EntityPropagationPublisher propagationPublisher) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    /**
     * Ajoute un utilisateur à un tenant avec un rôle donné
     */
    public TenantMembership addMembership(UUID userId, UUID tenantId, String roleKey, UUID invitedBy) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // Vérifier que le tenant existe
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));

        // Résoudre le rôle
        Role role = roleRepository.findByTenantIdAndKey(tenantId, roleKey.toUpperCase())
                .orElseGet(() -> roleRepository.findByTenantIdIsNullAndKey(roleKey.toUpperCase())
                        .orElseThrow(() -> new EntityNotFoundException("Role", "key", roleKey)));

        // Vérifier si l'appartenance existe déjà.
        // F17 : lecture par LISTE — une organisation peut porter plusieurs lignes
        // (périmètres distincts). On refuse si une ligne ACTIVE existe déjà ;
        // sinon on réactive la ligne « principale » inactive plutôt qu'une
        // création en doublon.
        List<TenantMembership> existingAll = membershipRepository.findAllByUserIdAndTenantId(userId, tenantId);
        boolean alreadyActive = existingAll.stream().anyMatch(m -> m.getStatus() == MembershipStatus.ACTIVE);
        if (alreadyActive) {
            throw new BusinessRuleException(
                    "L'utilisateur est déjà membre actif de ce tenant", "MEMBERSHIP_EXISTS");
        }
        if (!existingAll.isEmpty()) {
            TenantMembership membership = pickPrimary(existingAll);
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
                Map.of("userId", userId, "tenantId", tenantId, "role", roleKey),
                "Membership créé: user=" + userId + " tenant=" + tenantId + " role=" + roleKey);

        return membership;
    }

    /**
     * Change le rôle d'un utilisateur dans un tenant
     */
    public TenantMembership changeRole(UUID userId, UUID tenantId, String newRoleKey, UUID changedBy) {
        // F17 : sélection déterministe de la ligne principale (liste, pas Optional).
        TenantMembership membership = pickPrimary(membershipRepository.findAllByUserIdAndTenantId(userId, tenantId));
        if (membership == null) {
            throw new EntityNotFoundException("TenantMembership", "userId:tenantId", userId + ":" + tenantId);
        }

        Role newRole = roleRepository.findByTenantIdAndKey(tenantId, newRoleKey.toUpperCase())
                .orElseGet(() -> roleRepository.findByTenantIdIsNullAndKey(newRoleKey.toUpperCase())
                        .orElseThrow(() -> new EntityNotFoundException("Role", "key", newRoleKey)));

        Role oldRoleEntity = membership.getRole();
        String oldRoleKey = oldRoleEntity != null ? oldRoleEntity.getKey() : null;

        membership.setRole(newRole);
        membership = membershipRepository.save(membership);

        auditService.log(
                changedBy != null ? changedBy : userId,
                tenantId,
                "MEMBERSHIP_ROLE_CHANGED",
                "TENANT_MEMBERSHIP",
                membership.getId(),
                "SUCCESS",
                Payloads.of("oldRole", oldRoleKey, "newRole", newRoleKey),
                null, null, null
        );

        return membership;
    }

    /**
     * Désactive (soft delete) une appartenance
     */
    public void removeMembership(UUID userId, UUID tenantId, UUID removedBy) {
        // F17 : ligne principale déterministe ; on révoque la portée TENANT en
        // priorité, puis la plus ancienne.
        TenantMembership membership = pickPrimary(membershipRepository.findAllByUserIdAndTenantId(userId, tenantId));
        if (membership == null) {
            throw new EntityNotFoundException("TenantMembership", "userId:tenantId", userId + ":" + tenantId);
        }

        membership.setStatus(MembershipStatus.REVOKED);
        membershipRepository.save(membership);

        auditService.log(
                removedBy != null ? removedBy : userId,
                tenantId,
                "MEMBERSHIP_REVOKED",
                "TENANT_MEMBERSHIP",
                membership.getId(),
                "SUCCESS",
                Payloads.of("role", membership.getRole() != null ? membership.getRole().getKey() : null),
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
        // F17 : on résout la ligne principale puis on filtre ACTIVE.
        return Optional.ofNullable(pickPrimary(membershipRepository.findAllByUserIdAndTenantId(userId, tenantId)))
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

    @Transactional(readOnly = true)
    public List<TenantMembership> findByUserId(UUID userId) {
        return membershipRepository.findByUserId(userId);
    }

    /**
     * F17 — Sélectionne l'appartenance « principale » d'un utilisateur dans un
     * tenant quand plusieurs lignes coexistent (périmètres distincts, spec V2
     * §1.3 mode LÉGER). Ordre de priorité : ligne ACTIVE &gt; portée {@code TENANT}
     * &gt; la plus ancienne. Retourne {@code null} si la liste est vide.
     *
     * <p>Ce helper remplace les lectures {@code findByUserIdAndTenantId} renvoyant
     * un {@code Optional}, qui lèveraient
     * {@code IncorrectResultSizeDataAccessException} (500) dès qu'un membre porte
     * deux appartenances dans la même organisation.
     */
    private static TenantMembership pickPrimary(List<TenantMembership> all) {
        if (all == null || all.isEmpty()) {
            return null;
        }
        return all.stream()
                .min(Comparator
                        .comparing((TenantMembership m) -> m.getStatus() == MembershipStatus.ACTIVE ? 0 : 1)
                        .thenComparing(m -> m.getScopeType() == MembershipScopeType.TENANT ? 0 : 1)
                        .thenComparing(m -> m.getJoinedAt() == null ? Instant.EPOCH : m.getJoinedAt()))
                .orElse(null);
    }

    private void auditMembership(TenantMembership membership, String action, UUID actorId) {
        auditService.log(
                actorId != null ? actorId : membership.getUserId(),
                membership.getTenantId(),
                action,
                "TENANT_MEMBERSHIP",
                membership.getId(),
                "SUCCESS",
                Payloads.of("userId", membership.getUserId(), "role", membership.getRole() != null ? membership.getRole().getKey() : null),
                null, null, null
        );
    }
}