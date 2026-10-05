package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §C / T-B12 — affiliations
 * membre×rôle×nœud (0..n), découplées de l'appartenance.
 *
 * <p>Un membre peut porter un rôle-capacité sur plusieurs campus. La fin
 * d'une assignation est une transition vers {@code ENDED} (jamais une
 * purge). La résolution « ce rôle couvre-t-il ce nœud ? » suit la
 * descendance par {@code path} : une assignation de portée {@code null}
 * (tenant) ou posée sur un ANCESTRE du nœud cible couvre ce nœud.
 */
@Service
@Transactional
public class MemberRoleAssignmentService {

    private final MemberRoleAssignmentRepository assignmentRepository;
    private final OrganizationNodeRepository nodeRepository;
    private final RoleRepository roleRepository;

    public MemberRoleAssignmentService(MemberRoleAssignmentRepository assignmentRepository,
                                       OrganizationNodeRepository nodeRepository,
                                       RoleRepository roleRepository) {
        this.assignmentRepository = assignmentRepository;
        this.nodeRepository = nodeRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberRoleAssignment> listForUser(UUID tenantId, UUID userId) {
        return assignmentRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Transactional(readOnly = true)
    public List<MemberRoleAssignment> listActiveForUser(UUID tenantId, UUID userId) {
        return assignmentRepository.findByTenantIdAndUserIdAndStatus(tenantId, userId,
                MemberRoleAssignment.AssignmentStatus.ACTIVE);
    }

    /**
     * Affecte un rôle-capacité à un membre sur un nœud (ou le tenant si
     * {@code nodeId == null}). Réactive la ligne ENDED/SUSPENDED existante
     * plutôt que d'en créer une doublon.
     */
    @Transactional
    public MemberRoleAssignment assign(UUID tenantId, UUID userId, UUID roleId, UUID nodeId, UUID actorId) {
        if (!roleRepository.existsById(roleId)) {
            throw new EntityNotFoundException("Role", roleId);
        }
        if (nodeId != null) {
            OrganizationNode node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));
            if (!node.getTenantId().equals(tenantId)) {
                // Isolation stricte : un nœud d'une autre organisation est invisible.
                throw new EntityNotFoundException("OrganizationNode", nodeId);
            }
        }
        MemberRoleAssignment existing = assignmentRepository
                .findByUserIdAndRoleIdAndNodeIdAndStatus(userId, roleId, nodeId,
                        MemberRoleAssignment.AssignmentStatus.ACTIVE)
                .orElse(null);
        if (existing != null) {
            return existing; // déjà actif, idempotent
        }
        MemberRoleAssignment assignment = MemberRoleAssignment.builder()
                .tenantId(tenantId)
                .userId(userId)
                .roleId(roleId)
                .nodeId(nodeId)
                .assignedBy(actorId)
                .assignedAt(Instant.now())
                .status(MemberRoleAssignment.AssignmentStatus.ACTIVE)
                .build();
        return assignmentRepository.save(assignment);
    }

    /**
     * Met fin à une assignation (status = ENDED, pas de suppression
     * physique). L'assignation doit appartenir au tenant courant.
     */
    @Transactional
    public void end(UUID tenantId, UUID assignmentId) {
        MemberRoleAssignment assignment = assignmentRepository.findByTenantIdAndId(tenantId, assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("MemberRoleAssignment", assignmentId));
        if (assignment.getStatus() == MemberRoleAssignment.AssignmentStatus.ENDED) {
            return;
        }
        assignment.setStatus(MemberRoleAssignment.AssignmentStatus.ENDED);
        assignment.setEndedAt(Instant.now());
        assignmentRepository.save(assignment);
    }

    /**
     * T-B12 — Rôle(s) que le membre porte COUVRANT le nœud {@code targetNodeId} :
     * assignation de portée tenant ({@code nodeId == null}) ou posée sur un
     * ancêtre du nœud cible (descendance par {@code path}). Alimente la
     * résolution d'autorisation dans {@link AuthorizationService}.
     */
    @Transactional(readOnly = true)
    public Set<UUID> roleIdsCoveringNode(UUID tenantId, UUID userId, UUID targetNodeId) {
        List<MemberRoleAssignment> active = listActiveForUser(tenantId, userId);
        if (active.isEmpty()) {
            return Set.of();
        }
        Optional<OrganizationNode> target = targetNodeId == null
                ? Optional.empty() : nodeRepository.findById(targetNodeId);
        String targetPath = target.map(OrganizationNode::getPath).orElse(null);

        return active.stream().filter(a -> coversNode(a, targetPath)).map(MemberRoleAssignment::getRoleId)
                .collect(Collectors.toSet());
    }

    private boolean coversNode(MemberRoleAssignment a, String targetPath) {
        // Portée tenant : couvre tout le tenant.
        if (a.getNodeId() == null) {
            return true;
        }
        // Pas de nœud cible demandé → seules les assignations tenant comptent.
        if (targetPath == null) {
            return false;
        }
        String ancestorPath = nodeRepository.findById(a.getNodeId())
                .map(OrganizationNode::getPath).orElse(null);
        if (ancestorPath == null) {
            return false;
        }
        // Un nœud couvre ses descendants (path préfixe) et lui-même.
        return targetPath.equals(ancestorPath) || targetPath.startsWith(ancestorPath + ".");
    }
}
