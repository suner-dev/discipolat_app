package com.discipolat.modules.map.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.map.api.MapPointResponse;
import com.discipolat.modules.map.api.UpdateCoordinatesRequest;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cartographie des disciples et familles : les points visibles dépendent
 * du rôle courant (pasteur voit tout, responsable ses départements,
 * chef de famille sa famille, faiseur ses disciples).
 */
@Service
@Transactional
public class MapService {

    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public MapService(SoulRepository soulRepository,
                      FamilyRepository familyRepository,
                      DepartmentRepository departmentRepository,
                      MemberDepartmentRepository memberDepartmentRepository,
                      UserRepository userRepository,
                      SecurityUtils securityUtils) {
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<MapPointResponse> getMapPoints() {
        UUID userId = securityUtils.getCurrentUserId();

        // Pasteur / admin actifs : tout
        if (securityUtils.isSuperUser()) {
            return allPoints();
        }

        Set<UUID> soulIds = new HashSet<>();
        Set<UUID> familyIds = new HashSet<>();

        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            // Union dédupliquée : source canonique familleGereeId + repli DB (chefFamilleId)
            List<UUID> famIds = new ArrayList<>();
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getFamilleGereeId() != null) famIds.add(user.getFamilleGereeId());
            familyRepository.findByChefFamilleId(userId).stream()
                    .map(Family::getId).forEach(famIds::add);
            List<UUID> distinctFamIds = famIds.stream().distinct().toList();
            familyIds.addAll(distinctFamIds);
            soulIds.addAll(soulRepository.findByFamilleIdIn(distinctFamIds).stream()
                    .filter(s -> !s.isDeleted()).map(Soul::getId).toList());
        }

        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                    .stream().map(Department::getId).toList();
            if (!deptIds.isEmpty()) {
                List<UUID> deptSoulIds = memberDepartmentRepository.findByDepartmentIdIn(deptIds)
                        .stream().map(MemberDepartment::getSoulId).toList();
                soulIds.addAll(deptSoulIds);
                // familles des membres du département (et non toutes les familles)
                familyIds.addAll(soulRepository.findAllById(deptSoulIds).stream()
                        .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                        .map(Soul::getFamilleId).toList());
            }
        }

        if (securityUtils.hasActiveRole("FAISEUR")) {
            soulIds.addAll(soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted()).map(Soul::getId).toList());
            // familles des disciples du faiseur
            familyIds.addAll(soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted() && s.getFamilleId() != null)
                    .map(Soul::getFamilleId).toList());
        }

        return buildPoints(soulIds, familyIds);
    }

    @Transactional(readOnly = true)
    public MapPointResponse updateSoulCoordinates(UUID soulId, UpdateCoordinatesRequest request) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", soulId));

        // Seuls pasteur/admin actifs partout, responsable actif sur ses départements
        boolean allowed = securityUtils.isSuperUser()
                || (securityUtils.hasActiveRole("RESPONSABLE") && isResponsableDe(soul));
        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez pas modifier la position de ce disciple");
        }

        soul.setLatitude(request.latitude());
        soul.setLongitude(request.longitude());
        if (request.zone() != null && !request.zone().isBlank()) {
            soul.setZone(request.zone());
        }
        soulRepository.save(soul);

        return toSoulPoint(soul, familyRepository.findById(soul.getFamilleId()).orElse(null),
                departmentName(soul));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private boolean isResponsableDe(Soul soul) {
        UUID userId = securityUtils.getCurrentUserId();
        List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                .stream().map(Department::getId).toList();
        if (deptIds.isEmpty()) return false;
        return memberDepartmentRepository.findBySoulId(soul.getId()).stream()
                .map(MemberDepartment::getDepartmentId)
                .anyMatch(deptIds::contains);
    }

    private List<MapPointResponse> allPoints() {
        List<MapPointResponse> points = new ArrayList<>();
        for (Soul soul : soulRepository.findAll()) {
            if (soul.isDeleted()) continue;
            Family fam = soul.getFamilleId() != null
                    ? familyRepository.findById(soul.getFamilleId()).orElse(null)
                    : null;
            points.add(toSoulPoint(soul, fam, departmentName(soul)));
        }
        for (Family family : familyRepository.findAll()) {
            if (family.isDeleted()) continue;
            if (family.getLatitude() == null || family.getLongitude() == null) continue;
            points.add(new MapPointResponse(
                    family.getId(), "FAMILY", family.getNom(),
                    family.getLatitude(), family.getLongitude(), family.getZone(),
                    family.getStatut().name(),
                    null, null, null));
        }
        return points;
    }

    private List<MapPointResponse> buildPoints(Set<UUID> soulIds, Set<UUID> familyIds) {
        List<MapPointResponse> points = new ArrayList<>();
        if (soulIds != null && !soulIds.isEmpty()) {
            for (Soul soul : soulRepository.findAllById(soulIds)) {
                if (soul.isDeleted() || soul.getLatitude() == null) continue;
                Family fam = soul.getFamilleId() != null
                        ? familyRepository.findById(soul.getFamilleId()).orElse(null)
                        : null;
                points.add(toSoulPoint(soul, fam, departmentName(soul)));
            }
        }
        if (familyIds != null && !familyIds.isEmpty()) {
            for (Family family : familyRepository.findAllById(familyIds)) {
                if (family.isDeleted() || family.getLatitude() == null) continue;
                points.add(new MapPointResponse(
                        family.getId(), "FAMILY", family.getNom(),
                        family.getLatitude(), family.getLongitude(), family.getZone(),
                        family.getStatut().name(),
                        null, null, null));
            }
        }
        return points;
    }

    private MapPointResponse toSoulPoint(Soul soul, Family fam, String deptNom) {
        return new MapPointResponse(
                soul.getId(), "SOUL", soul.getNomComplet(),
                soul.getLatitude(), soul.getLongitude(), soul.getZone(),
                soul.getStatut().name(),
                fam != null ? fam.getNom() : null,
                deptNom,
                soul.getNiveauCroissance());
    }

    private String departmentName(Soul soul) {
        UUID deptId = memberDepartmentRepository.findBySoulId(soul.getId()).stream()
                .findFirst().map(MemberDepartment::getDepartmentId).orElse(null);
        return departmentName(deptId);
    }

    private String departmentName(UUID deptId) {
        if (deptId == null) return null;
        return departmentRepository.findById(deptId)
                .map(Department::getNom).orElse(null);
    }
}
