package com.discipolat.modules.members.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.api.*;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Espace Membre — Phase 2 : présences hebdomadaires et demandes
 * (suggestions, rendez-vous, signalements) avec visibilité scopée :
 * pasteur voit tout, responsable voit les demandes/présences de ses
 * départements, chef de famille voit celles de sa famille.
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
    private final MemberPresenceRepository memberPresenceRepository;
    private final MemberRequestRepository memberRequestRepository;
    private final SecurityUtils securityUtils;

    public MemberService(UserRepository userRepository,
                         SoulRepository soulRepository,
                         FamilyRepository familyRepository,
                         DepartmentRepository departmentRepository,
                         MemberDepartmentRepository memberDepartmentRepository,
                         MemberPresenceRepository memberPresenceRepository,
                         MemberRequestRepository memberRequestRepository,
                         SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.memberPresenceRepository = memberPresenceRepository;
        this.memberRequestRepository = memberRequestRepository;
        this.securityUtils = securityUtils;
    }

    // ============================================================
    // PHASE 1 — Dashboard & profil
    // ============================================================

    @Transactional(readOnly = true)
    public MemberDashboardResponse getMyDashboard() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        List<Soul> souls = soulRepository.findAllByUserId(userId).stream()
                .filter(s -> !s.isDeleted())
                .toList();
        Soul soul = souls.isEmpty() ? null : souls.get(0);

        MemberDashboardResponse.MemberUserInfo userInfo = new MemberDashboardResponse.MemberUserInfo(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhone(), user.getPhotoUrl(), user.getDateNaissance(), user.getSituationFamiliale());

        LocalDate birth = user.getDateNaissance() != null ? user.getDateNaissance()
                : (soul != null ? soul.getDateNaissance() : null);
        Integer age = birth != null ? Period.between(birth, LocalDate.now()).getYears() : null;

        boolean estFaiseur = user.getRoles().contains(UserRole.FAISEUR);
        String statutMembre;
        if (user.getRoles().contains(UserRole.CHEF_DE_FAMILLE)) statutMembre = "CHEF_DE_FAMILLE";
        else if (estFaiseur) statutMembre = "FAISEUR";
        else statutMembre = "MEMBRE";

        MemberDashboardResponse.MemberSoulInfo soulInfo = null;
        if (soul != null) {
            soulInfo = new MemberDashboardResponse.MemberSoulInfo(
                    soul.getId(), soul.getProfession(), soul.getNiveauEtude(), soul.getNbEnfants(),
                    soul.getDateIntegration(), soul.getStatut().name());
        }
        String dateArriveeEglise = soul != null && soul.getDateIntegration() != null
                ? soul.getDateIntegration().toString() : null;

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

        MemberDashboardResponse.PersonneInfo faiseurInfo = null;
        if (soul != null && soul.getFaiseurId() != null) {
            faiseurInfo = userRepository.findById(soul.getFaiseurId())
                    .filter(u -> !u.isDeleted())
                    .map(u -> new MemberDashboardResponse.PersonneInfo(
                            u.getId(), u.getFirstName() + " " + u.getLastName()))
                    .orElse(null);
        }

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

    // ============================================================
    // PHASE 2 — Présences hebdomadaires
    // ============================================================

    /** Historique des présences saisies par le membre connecté. */
    @Transactional(readOnly = true)
    public List<MemberPresenceResponse> getMyPresences() {
        UUID userId = securityUtils.getCurrentUserId();
        String nom = fullName(userId);
        return memberPresenceRepository.findByUserIdOrderBySemaineDesc(userId)
                .stream().map(p -> MemberPresenceResponse.from(p, nom)).toList();
    }

    /** Crée ou met à jour la présence de la semaine pour le membre connecté. */
    public MemberPresenceResponse submitMyPresence(SubmitPresenceRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        LocalDate lundi = lundiDe(request.semaine());

        MemberPresence presence = memberPresenceRepository.findByUserIdAndSemaine(userId, lundi)
                .orElseGet(() -> MemberPresence.builder()
                        .userId(userId)
                        .semaine(lundi)
                        .build());

        UUID soulId = currentSoulId(userId);
        if (soulId != null) presence.setSoulId(soulId);
        presence.setPresences(request.presences());
        presence.setNotes(request.notes());
        presence.setTypeProgramme(request.typeProgramme());
        presence.setSousProgramme(request.sousProgramme());
        if (request.typeProgramme() != null && !request.presences().isEmpty()) {
            presence.setPresent(request.presences().values().stream().allMatch(Boolean.TRUE::equals));
        }

        return MemberPresenceResponse.from(memberPresenceRepository.save(presence), fullName(userId));
    }

    /**
     * Présences récentes des membres sous la responsabilité du rôle courant :
     * pasteur/admin = tous, responsable = membres de ses départements,
     * chef de famille = membres de ses familles.
     */
    @Transactional(readOnly = true)
    public List<MemberPresenceResponse> getScopedPresences() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // Pasteur / admin : toutes les présences (même sans âme liée au compte)
        if (user.getRoles().contains(UserRole.PASTEUR) || user.getRoles().contains(UserRole.ADMIN)) {
            return memberPresenceRepository.findAllByOrderBySemaineDesc()
                    .stream()
                    .map(p -> MemberPresenceResponse.from(p, fullName(p.getUserId())))
                    .toList();
        }

        List<UUID> soulIds = scopedSoulIds(user);
        if (soulIds.isEmpty()) return List.of();

        return memberPresenceRepository.findBySoulIdInOrderBySemaineDesc(soulIds)
                .stream()
                .map(p -> MemberPresenceResponse.from(p, fullName(p.getUserId())))
                .toList();
    }

    // ============================================================
    // PHASE 2 — Demandes : suggestions, rendez-vous, signalements
    // ============================================================

    /** Demandes envoyées par le membre connecté. */
    @Transactional(readOnly = true)
    public List<MemberRequestResponse> getMyRequests() {
        UUID userId = securityUtils.getCurrentUserId();
        return memberRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    /** Crée une demande : cible PASTEUR (église), RESPONSABLE (département) ou CHEF_DE_FAMILLE (famille). */
    public MemberRequestResponse createRequest(CreateMemberRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        MemberRequest req = MemberRequest.builder()
                .userId(userId)
                .type(request.type())
                .cible(request.cible())
                .message(request.message())
                .statut(MemberRequest.Statut.OUVERT)
                .build();

        // Portée automatique selon la cible
        if (request.cible() == MemberRequest.Cible.RESPONSABLE) {
            UUID deptId = firstDepartmentId(userId);
            if (deptId == null) {
                throw new BadRequestException(
                        "Vous n'êtes pas encore rattaché(e) à un département. Contactez votre faiseur ou le pasteur.");
            }
            req.setDepartmentId(deptId);
        } else if (request.cible() == MemberRequest.Cible.CHEF_DE_FAMILLE) {
            Soul soul = currentSoul(userId);
            if (soul == null || soul.getFamilleId() == null) {
                throw new BadRequestException(
                        "Vous n'êtes pas encore rattaché(e) à une famille de disciple. Contactez votre faiseur ou le pasteur.");
            }
            req.setFamilyId(soul.getFamilleId());
        }

        return toResponse(memberRequestRepository.save(req));
    }

    /** Boîte de réception scopée par rôle : pasteur/admin tout, responsable ses départements, chef ses familles. */
    @Transactional(readOnly = true)
    public List<MemberRequestResponse> getRequestsInbox() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // Accumulation pour les rôles multiples (ex : responsable aussi chef de famille)
        List<MemberRequest> requests = new ArrayList<>();
        if (user.getRoles().contains(UserRole.PASTEUR) || user.getRoles().contains(UserRole.ADMIN)) {
            requests.addAll(memberRequestRepository.findAllByOrderByCreatedAtDesc());
        } else {
            if (user.getRoles().contains(UserRole.RESPONSABLE)) {
                List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                        .stream().map(Department::getId).toList();
                if (!deptIds.isEmpty()) {
                    requests.addAll(memberRequestRepository
                            .findByCibleAndDepartmentIdInOrderByCreatedAtDesc(MemberRequest.Cible.RESPONSABLE, deptIds));
                }
            }
            if (user.getRoles().contains(UserRole.CHEF_DE_FAMILLE)) {
                List<UUID> famIds = familyRepository.findByChefFamilleId(userId)
                        .stream().map(Family::getId).toList();
                if (!famIds.isEmpty()) {
                    requests.addAll(memberRequestRepository
                            .findByCibleAndFamilyIdInOrderByCreatedAtDesc(MemberRequest.Cible.CHEF_DE_FAMILLE, famIds));
                }
            }
        }

        return requests.stream()
                .sorted(Comparator.comparing(MemberRequest::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    /** Met à jour le statut d'une demande (seul le récepteur ciblé peut traiter). */
    public MemberRequestResponse updateRequestStatus(UUID requestId, UpdateMemberRequestStatus payload) {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        MemberRequest req = memberRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("MemberRequest", requestId));

        // Permission : pasteur/admin partout, sinon le récepteur scopé
        if (!user.getRoles().contains(UserRole.PASTEUR) && !user.getRoles().contains(UserRole.ADMIN)) {
            boolean allowed = switch (req.getCible()) {
                case RESPONSABLE -> {
                    List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                            .stream().map(Department::getId).toList();
                    yield req.getDepartmentId() != null && deptIds.contains(req.getDepartmentId());
                }
                case CHEF_DE_FAMILLE -> {
                    List<UUID> famIds = familyRepository.findByChefFamilleId(userId)
                            .stream().map(Family::getId).toList();
                    yield req.getFamilyId() != null && famIds.contains(req.getFamilyId());
                }
                case PASTEUR -> false;
            };
            if (!allowed) throw new org.springframework.security.access.AccessDeniedException("Vous ne pouvez pas traiter cette demande");
        }

        req.setStatut(payload.statut());
        if (payload.reponse() != null && !payload.reponse().isBlank()) {
            req.setReponse(payload.reponse());
        }
        req.setTraitePar(userId);
        req.setDateTraitement(java.time.LocalDateTime.now());

        return toResponse(memberRequestRepository.save(req));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String fullName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Membre");
    }

    private Soul currentSoul(UUID userId) {
        return soulRepository.findAllByUserId(userId).stream()
                .filter(s -> !s.isDeleted())
                .findFirst().orElse(null);
    }

    private UUID currentSoulId(UUID userId) {
        Soul soul = currentSoul(userId);
        return soul != null ? soul.getId() : null;
    }

    private UUID firstDepartmentId(UUID userId) {
        Soul soul = currentSoul(userId);
        if (soul == null) return null;
        return memberDepartmentRepository.findBySoulId(soul.getId()).stream()
                .findFirst()
                .map(MemberDepartment::getDepartmentId)
                .orElse(null);
    }

    /** Lundi de la semaine contenant la date donnée. */
    private LocalDate lundiDe(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Âmes sous la responsabilité du rôle courant (pour la visibilité des présences). */
    private List<UUID> scopedSoulIds(User user) {
        if (user.getRoles().contains(UserRole.RESPONSABLE)) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(user.getId())
                    .stream().map(Department::getId).toList();
            return deptIds.isEmpty() ? List.of()
                    : memberDepartmentRepository.findByDepartmentIdIn(deptIds)
                            .stream().map(MemberDepartment::getSoulId).distinct().toList();
        }
        if (user.getRoles().contains(UserRole.CHEF_DE_FAMILLE)) {
            List<UUID> famIds = familyRepository.findByChefFamilleId(user.getId())
                    .stream().map(Family::getId).toList();
            return famIds.isEmpty() ? List.of()
                    : soulRepository.findByFamilleIdIn(famIds).stream()
                            .filter(s -> !s.isDeleted())
                            .map(Soul::getId).toList();
        }
        return List.of();
    }

    private MemberRequestResponse toResponse(MemberRequest r) {
        String auteurNom = userRepository.findById(r.getUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        String traiteParNom = r.getTraitePar() != null
                ? userRepository.findById(r.getTraitePar())
                        .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                : null;
        String deptNom = r.getDepartmentId() != null
                ? departmentRepository.findById(r.getDepartmentId()).map(Department::getNom).orElse(null)
                : null;
        String famNom = r.getFamilyId() != null
                ? familyRepository.findById(r.getFamilyId()).map(Family::getNom).orElse(null)
                : null;
        return MemberRequestResponse.from(r, auteurNom, traiteParNom, deptNom, famNom);
    }
}
