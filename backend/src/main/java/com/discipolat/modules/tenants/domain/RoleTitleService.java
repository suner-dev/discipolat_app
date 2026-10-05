package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §B / T-B11 — <b>intitulés</b> par scope.
 *
 * <p>Résout le libellé AFFICHÉ d'un rôle-capacité : intitulé du NŒUD →
 * intitulé par DÉFAUT du tenant → label global du {@link Role}. La
 * résolution ne touche <b>jamais</b> les permissions (V3-B) : seul le nom
 * change, deux églises peuvent nommer le même rôle différemment.
 */
@Service
@Transactional
public class RoleTitleService {

    private final RoleTitleRepository titleRepository;
    private final RoleRepository roleRepository;

    public RoleTitleService(RoleTitleRepository titleRepository, RoleRepository roleRepository) {
        this.titleRepository = titleRepository;
        this.roleRepository = roleRepository;
    }

    /** Tous les intitulés d'un rôle (défaut tenant + par nœud). */
    @Transactional(readOnly = true)
    public List<RoleTitle> listForRole(UUID tenantId, UUID roleId) {
        return titleRepository.findByTenantIdAndRoleId(tenantId, roleId);
    }

    /**
     * Intitulé résolu pour un couple (rôle, nœud) : nœud → défaut tenant →
     * label global du rôle. Ne renvoie jamais null (repli sur le rôle).
     */
    @Transactional(readOnly = true)
    public String getEffectiveLabel(UUID tenantId, UUID roleId, UUID nodeId) {
        if (nodeId != null) {
            Optional<RoleTitle> forNode = titleRepository.findByTenantIdAndRoleIdAndNodeId(tenantId, roleId, nodeId);
            if (forNode.isPresent()) return forNode.get().getLabel();
        }
        Optional<RoleTitle> tenantDefault = titleRepository.findByTenantIdAndRoleIdAndNodeIdIsNull(tenantId, roleId);
        if (tenantDefault.isPresent()) return tenantDefault.get().getLabel();
        return roleRepository.findById(roleId)
                .map(Role::getLabel)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));
    }

    /**
     * Upsert d'un intitulé : {@code nodeId == null} pose le défaut du tenant,
     * sinon l'intitulé pour CE nœud. Idempotent (mise à jour si déjà présent).
     */
    @Transactional
    public RoleTitle upsert(UUID tenantId, UUID roleId, UUID nodeId, String label, String labelPlural) {
        if (label == null || label.isBlank()) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Intitulé requis", "ROLE_TITLE_LABEL_REQUIRED");
        }
        // La capacité (permissions) ne bouge pas ; on vérifie juste que le rôle existe.
        if (!roleRepository.existsById(roleId)) {
            throw new EntityNotFoundException("Role", roleId);
        }
        RoleTitle title = (nodeId == null
                ? titleRepository.findByTenantIdAndRoleIdAndNodeIdIsNull(tenantId, roleId)
                : titleRepository.findByTenantIdAndRoleIdAndNodeId(tenantId, roleId, nodeId))
                .orElseGet(() -> RoleTitle.builder()
                        .tenantId(tenantId).roleId(roleId).nodeId(nodeId).build());
        title.setLabel(label.trim());
        title.setLabelPlural(labelPlural == null || labelPlural.isBlank() ? null : labelPlural.trim());
        return titleRepository.save(title);
    }

    @Transactional
    public void delete(UUID tenantId, UUID roleId, UUID nodeId) {
        if (nodeId == null) {
            titleRepository.findByTenantIdAndRoleIdAndNodeIdIsNull(tenantId, roleId)
                    .ifPresent(titleRepository::delete);
        } else {
            titleRepository.findByTenantIdAndRoleIdAndNodeId(tenantId, roleId, nodeId)
                    .ifPresent(titleRepository::delete);
        }
    }
}
