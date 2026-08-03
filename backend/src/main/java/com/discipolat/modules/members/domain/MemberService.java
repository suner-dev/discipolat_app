package com.discipolat.modules.members.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.api.MemberDashboardResponse;
import com.discipolat.modules.members.api.UpdateMemberProfileRequest;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Espace Membre — Phase 1 : le membre consulte et met à jour ses informations,
 * sa famille de disciple, son encadrement et ses départements.
 */
@Service
@Transactional
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final UserRepository userRepository;
    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;
    private final SecurityUtils securityUtils;

    public MemberService(UserRepository userRepository,
                         SoulRepository soulRepository,
                         FamilyRepository familyRepository,
                         DepartmentRepository departmentRepository,
                         MemberDepartmentRepository memberDepartmentRepository,
                         SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public MemberDashboardResponse getMyDashboard() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        List<Soul> souls = soulRepository.findAllByUserId(userId).stream()
                .filter(s -> !s.isDeleted())
                .toList();
        Soul soul = souls.isEmpty() ? null : souls.get(0);

        // Profil personnel (compte utilisateur)
        MemberDashboardResponse.MemberUserInfo userInfo = new MemberDashboardResponse.MemberUserInfo(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhone(), user.getPhotoUrl(), user.getDateNaissance(), user.getSituationFamiliale());

        // Âge (date de naissance du compte, sinon de l'âme)
        LocalDate birth = user.getDateNaissance() != null ? user.getDateNaissance()
                : (soul != null ? soul.getDateNaissance() : null);
        Integer age = birth != null ? Period.between(birth, LocalDate.now()).getYears() : null;

        // Statut : simple membre, faiseur de disciples ou chef de famille
        boolean estFaiseur = user.getRoles().contains(UserRole.FAISEUR);
        String statutMembre;
        if (user.getRoles().contains(UserRole.CHEF_DE_FAMILLE)) statutMembre = "CHEF_DE_FAMILLE";
        else if (estFaiseur) statutMembre = "FAISEUR";
        else statutMembre = "MEMBRE";

        // Profil disciple (âme liée au compte)
        MemberDashboardResponse.MemberSoulInfo soulInfo = null;
        if (soul != null) {
            soulInfo = new MemberDashboardResponse.MemberSoulInfo(
                    soul.getId(), soul.getProfession(), soul.getNiveauEtude(), soul.getNbEnfants(),
                    soul.getDateIntegration(), soul.getStatut().name());
        }
        String dateArriveeEglise = soul != null && soul.getDateIntegration() != null
                ? soul.getDateIntegration().toString() : null;

        // Famille de disciple + chef de famille
        MemberDashboardResponse.MemberFamilyInfo familleInfo = null;
        if (soul != null && soul.getFamilleId() != null) {
            Family family = familyRepository.findById(soul.getFamilleId()).orElse(null);
            if (family != null && !family.isDeleted()) {
                String chefNom = family.getChefFamilleId() != null
                        ? userRepository.findById(family.getChefFamilleId())
                                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                        : null;
                familleInfo = new MemberDashboardResponse.MemberFamilyInfo(
                        family.getId(), family.getNom(), family.getChefFamilleId(), chefNom);
            }
        }

        // Faiseur de disciples (encadrant direct)
        MemberDashboardResponse.PersonneInfo faiseurInfo = null;
        if (soul != null && soul.getFaiseurId() != null) {
            faiseurInfo = userRepository.findById(soul.getFaiseurId())
                    .filter(u -> !u.isDeleted())
                    .map(u -> new MemberDashboardResponse.PersonneInfo(
                            u.getId(), u.getFirstName() + " " + u.getLastName()))
                    .orElse(null);
        }

        // Départements du membre (chorale, audiovisuel, ...)
        List<MemberDashboardResponse.MemberDepartmentInfo> departements = new ArrayList<>();
        if (soul != null) {
            for (MemberDepartment md : memberDepartmentRepository.findBySoulId(soul.getId())) {
                Department dept = departmentRepository.findById(md.getDepartmentId()).orElse(null);
                if (dept == null || dept.isDeleted()) continue;
                String responsableNom = dept.getResponsableId() != null
                        ? userRepository.findById(dept.getResponsableId())
                                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                        : null;
                departements.add(new MemberDashboardResponse.MemberDepartmentInfo(
                        dept.getId(), dept.getNom(), dept.getDescription(),
                        dept.getResponsableId(), responsableNom));
            }
        }

        return new MemberDashboardResponse(userInfo, soulInfo, age, statutMembre, estFaiseur,
                dateArriveeEglise, familleInfo, faiseurInfo, departements);
    }

    /**
     * Mise à jour du profil du membre connecté (compte + âme liée).
     * Seuls les champs fournis sont modifiés (PATCH-like via PUT partiel).
     */
    public MemberDashboardResponse updateMyProfile(UpdateMemberProfileRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (request.phone() != null) user.setPhone(request.phone());
        if (request.dateNaissance() != null) user.setDateNaissance(request.dateNaissance());
        if (request.situationFamiliale() != null) user.setSituationFamiliale(request.situationFamiliale());
        if (request.photoUrl() != null) user.setPhotoUrl(request.photoUrl());
        user.markUpdated();
        userRepository.save(user);

        // Profil disciple : l'âme liée au compte, si elle existe
        List<Soul> souls = soulRepository.findAllByUserId(userId).stream()
                .filter(s -> !s.isDeleted())
                .toList();
        if (!souls.isEmpty()) {
            Soul soul = souls.get(0);
            if (request.profession() != null) soul.setProfession(request.profession());
            if (request.niveauEtude() != null) soul.setNiveauEtude(request.niveauEtude());
            if (request.nbEnfants() != null) soul.setNbEnfants(request.nbEnfants());
            if (request.situationFamiliale() != null) soul.setSituationFamiliale(request.situationFamiliale());
            soulRepository.save(soul);
        } else {
            log.warn("Aucune âme liée au compte {} — profil disciple non mis à jour", userId);
        }

        return getMyDashboard();
    }
}
