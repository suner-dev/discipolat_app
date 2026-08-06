package com.discipolat.modules.souls.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Périmètre de l'espace métier courant (rôle ACTIF).
 *
 * Source unique de vérité pour le scoping des données par espace :
 * - super-utilisateurs (Admin / Pasteur actifs) : tout est visible ;
 * - FAISEUR actif : ses disciples, leurs familles, lui-même comme faiseur ;
 * - CHEF_DE_FAMILLE actif : les âmes de la famille qu'il gère, ses faiseurs ;
 * - RESPONSABLE actif : les membres de SES départements (via soul_departments),
 *   les familles de ces membres et leurs faiseurs.
 *
 * Un utilisateur multi-rôles n'accède qu'aux données de l'espace courant :
 * changer de rôle = changer complètement de périmètre de données.
 */
@Service
@Transactional(readOnly = true)
public class WorkspaceScopeService {

    private final SecurityUtils securityUtils;
    private final SoulRepository soulRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    public WorkspaceScopeService(SecurityUtils securityUtils,
                                 SoulRepository soulRepository,
                                 SoulDepartmentRepository soulDepartmentRepository,
                                 DepartmentRepository departmentRepository,
                                 FamilyRepository familyRepository,
                                 UserRepository userRepository) {
        this.securityUtils = securityUtils;
        this.soulRepository = soulRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
    }

    /** Super-utilisateurs (Admin / Pasteur actifs) : accès à toutes les données. */
    public boolean isSuperUser() {
        return securityUtils.isSuperUser();
    }

    /** Ids des âmes accessibles dans l'espace métier courant. */
    public Set<UUID> accessibleSoulIds() {
        UUID userId = securityUtils.getCurrentUserId();
        Set<UUID> ids = new HashSet<>();

        if (securityUtils.hasActiveRole("FAISEUR")) {
            soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted())
                    .map(Soul::getId)
                    .forEach(ids::add);
        }
        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            userRepository.findById(userId).ifPresent(u -> {
                if (u.getFamilleGereeId() != null) {
                    soulRepository.findAllByFamilleId(u.getFamilleGereeId()).stream()
                            .filter(s -> !s.isDeleted())
                            .map(Soul::getId)
                            .forEach(ids::add);
                }
            });
        }
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                    .stream().map(Department::getId).toList();
            if (!deptIds.isEmpty()) {
                soulDepartmentRepository.findByDepartmentIdIn(deptIds).stream()
                        .filter(SoulDepartment::isActif)
                        .map(SoulDepartment::getSoulId)
                        .forEach(ids::add);
            }
        }
        return ids;
    }

    /** Ids des familles accessibles dans l'espace métier courant. */
    public Set<UUID> accessibleFamilyIds() {
        UUID userId = securityUtils.getCurrentUserId();
        Set<UUID> familyIds = new HashSet<>();

        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            userRepository.findById(userId).ifPresent(u -> {
                if (u.getFamilleGereeId() != null) familyIds.add(u.getFamilleGereeId());
            });
        }
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                    .stream().map(Department::getId).toList();
            if (!deptIds.isEmpty()) {
                List<UUID> soulIds = soulDepartmentRepository.findByDepartmentIdIn(deptIds).stream()
                        .filter(SoulDepartment::isActif)
                        .map(SoulDepartment::getSoulId)
                        .distinct()
                        .toList();
                if (!soulIds.isEmpty()) {
                    soulRepository.findAllById(soulIds).stream()
                            .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                            .map(Soul::getFamilleId)
                            .forEach(familyIds::add);
                }
            }
        }
        if (securityUtils.hasActiveRole("FAISEUR")) {
            soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                    .map(Soul::getFamilleId)
                    .forEach(familyIds::add);
        }
        return familyIds;
    }

    /** Ids des utilisateurs-faiseurs accessibles dans l'espace métier courant. */
    public Set<UUID> accessibleFaiseurIds() {
        UUID userId = securityUtils.getCurrentUserId();
        Set<UUID> ids = new HashSet<>();

        if (securityUtils.hasActiveRole("FAISEUR")) {
            ids.add(userId);
        }
        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            userRepository.findById(userId).ifPresent(u -> {
                if (u.getFamilleGereeId() != null) {
                    soulRepository.findAllByFamilleId(u.getFamilleGereeId()).stream()
                            .filter(s -> !s.isDeleted() && s.getFaiseurId() != null)
                            .map(Soul::getFaiseurId)
                            .forEach(ids::add);
                }
            });
        }
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<Soul> souls = soulRepository.findAllById(accessibleSoulIds());
            souls.stream().map(Soul::getFaiseurId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(ids::add);
        }
        return ids;
    }

    public boolean canAccessSoul(UUID soulId) {
        return soulId != null && accessibleSoulIds().contains(soulId);
    }

    public boolean canAccessFamily(UUID familleId) {
        return familleId != null && accessibleFamilyIds().contains(familleId);
    }

    public boolean canAccessFaiseur(UUID faiseurId) {
        return faiseurId != null && accessibleFaiseurIds().contains(faiseurId);
    }
}
