package com.discipolat.modules.members.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
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
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

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
    private final com.discipolat.modules.souls.domain.SoulDepartmentRepository soulDepartmentRepository;
    private final MemberPresenceRepository memberPresenceRepository;
    private final MemberRequestRepository memberRequestRepository;
    private final EventRepository eventRepository;
    private final SecurityUtils securityUtils;
    private final EntityAttachmentService attachmentService;

    public MemberService(UserRepository userRepository,
                         SoulRepository soulRepository,
                         FamilyRepository familyRepository,
                         DepartmentRepository departmentRepository,
                         MemberDepartmentRepository memberDepartmentRepository,
                         com.discipolat.modules.souls.domain.SoulDepartmentRepository soulDepartmentRepository,
                         MemberPresenceRepository memberPresenceRepository,
                         MemberRequestRepository memberRequestRepository,
                         EventRepository eventRepository,
                         SecurityUtils securityUtils,
                         EntityAttachmentService attachmentService) {
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.memberPresenceRepository = memberPresenceRepository;
        this.memberRequestRepository = memberRequestRepository;
        this.eventRepository = eventRepository;
        this.securityUtils = securityUtils;
        this.attachmentService = attachmentService;
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

    // ============================================================
    // PHASE 2b — Saisie des présences par le responsable (département)
    // ============================================================

    /**
     * Fiche de présence du département pour une semaine :
     * liste les membres du département avec leur présence enregistrée (si saisie).
     * Seul le responsable du département (ou pasteur/admin) peut y accéder.
     */
    @Transactional(readOnly = true)
    public List<DepartmentPresenceRecord> getDepartmentPresenceSheet(UUID deptId, LocalDate semaine) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        verifyDepartmentAccess(currentUserId, deptId);

        LocalDate lundi = lundiDe(semaine != null ? semaine : LocalDate.now());
        return departmentMembers(deptId).stream()
                .map(soul -> {
                    // Priorité à la saisie par soul_id (responsable), repli sur user_id (membre)
                    MemberPresence presence = memberPresenceRepository.findBySoulIdAndSemaine(soul.getId(), lundi)
                            .or(() -> soul.getUserId() != null
                                    ? memberPresenceRepository.findByUserIdAndSemaine(soul.getUserId(), lundi)
                                    : java.util.Optional.empty())
                            .orElse(null);
                    boolean saisie = presence != null;
                    return new DepartmentPresenceRecord(
                            soul.getId(),
                            soul.getUserId(),
                            soul.getNomComplet(),
                            soul.getTelephone(),
                            soul.getStatut() != null ? soul.getStatut().name() : null,
                            soul.getFamilleId() != null
                                    ? familyRepository.findById(soul.getFamilleId()).map(Family::getNom).orElse(null)
                                    : null,
                            soul.getFamilleId(),
                            soul.getDateIntegration(),
                            saisie,
                            saisie ? presence.getPresent() : null,
                            saisie ? presence.getPresences() : null,
                            saisie ? presence.getNotes() : null,
                            saisie ? presence.getTypeProgramme() : null,
                            saisie ? presence.getSousProgramme() : null);
                })
                .toList();
    }

    /**
     * Saisie groupée des présences d'un département pour une semaine.
     * Le responsable coche présent/absent pour chaque membre.
     */
    public List<MemberPresenceResponse> submitDepartmentPresences(
            UUID deptId, SubmitDepartmentPresenceRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        verifyDepartmentAccess(currentUserId, deptId);

        LocalDate lundi = lundiDe(request.semaine());
        List<DepartmentPresenceRecord> sheet = getDepartmentPresenceSheet(deptId, lundi);
        Map<UUID, DepartmentPresenceRecord> bySoulId = sheet.stream()
                .collect(java.util.stream.Collectors.toMap(
                        DepartmentPresenceRecord::soulId, r -> r, (a, b) -> a));

        List<MemberPresenceResponse> results = new ArrayList<>();
        for (SubmitDepartmentPresenceItem item : request.presences()) {
            DepartmentPresenceRecord record = bySoulId.get(item.soulId());
            if (record == null) {
                throw new BadRequestException(
                        "Ce membre n'appartient pas au département sélectionné : " + item.soulId());
            }
            // Âme sans compte utilisateur : présence pointée par soul_id (gérée par le responsable)
            UUID userId = record.userId();
            MemberPresence presence = memberPresenceRepository.findBySoulIdAndSemaine(record.soulId(), lundi)
                    .or(() -> userId != null
                            ? memberPresenceRepository.findByUserIdAndSemaine(userId, lundi)
                            : java.util.Optional.empty())
                    .orElseGet(() -> MemberPresence.builder()
                            .soulId(record.soulId())
                            .userId(userId)
                            .semaine(lundi)
                            .build());
            presence.setSoulId(record.soulId());
            presence.setPresent(item.present());
            presence.setTypeProgramme(request.typeProgramme());
            presence.setSousProgramme(request.sousProgramme());
            // On garde la structure existante (programme label -> booléen) pour la compatibilité
            Map<String, Boolean> presences = new LinkedHashMap<>();
            if (request.typeProgramme() != null && !request.typeProgramme().isBlank()) {
                presences.put(request.typeProgramme(), item.present());
            } else if (item.presences() != null) {
                presences.putAll(item.presences());
            } else {
                presences.put("Présent", item.present());
            }
            presence.setPresences(presences);
            if (item.notes() != null) presence.setNotes(item.notes());

            // Nom du membre : depuis le compte s'il existe, sinon depuis la fiche du département
            String nom = userId != null ? fullName(userId) : record.nom();
            results.add(MemberPresenceResponse.from(memberPresenceRepository.save(presence), nom));
        }
        return results;
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

        // Super-utilisateurs (pasteur/admin actifs) : toutes les présences (même sans âme liée au compte)
        if (securityUtils.isSuperUser()) {
            return memberPresenceRepository.findAllByOrderBySemaineDesc()
                    .stream()
                    .map(p -> MemberPresenceResponse.from(p, presenceNom(p)))
                    .toList();
        }

        List<UUID> soulIds = scopedSoulIds(user);
        if (soulIds.isEmpty()) return List.of();

        return memberPresenceRepository.findBySoulIdInOrderBySemaineDesc(soulIds)
                .stream()
                .map(p -> MemberPresenceResponse.from(p, presenceNom(p)))
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

        MemberRequest saved = memberRequestRepository.save(req);
        attachmentService.replace(EntityAttachment.EntityType.MEMBER_REQUEST, saved.getId(), request.fichierIds());
        return toResponse(saved);
    }

    /** Boîte de réception scopée par rôle ACTIF : super-utilisateurs tout, responsable ses départements, chef ses familles. */
    @Transactional(readOnly = true)
    public List<MemberRequestResponse> getRequestsInbox() {
        UUID userId = securityUtils.getCurrentUserId();

        List<MemberRequest> requests = new ArrayList<>();
        if (securityUtils.isSuperUser()) {
            requests.addAll(memberRequestRepository.findAllByOrderByCreatedAtDesc());
        } else {
            if (securityUtils.hasActiveRole("RESPONSABLE")) {
                List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                        .stream().map(Department::getId).toList();
                if (!deptIds.isEmpty()) {
                    requests.addAll(memberRequestRepository
                            .findByCibleAndDepartmentIdInOrderByCreatedAtDesc(MemberRequest.Cible.RESPONSABLE, deptIds));
                }
            }
            if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
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

    /** Met à jour le statut d'une demande (seul le récepteur ciblé du rôle actif peut traiter). */
    public MemberRequestResponse updateRequestStatus(UUID requestId, UpdateMemberRequestStatus payload) {
        UUID userId = securityUtils.getCurrentUserId();

        MemberRequest req = memberRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("MemberRequest", requestId));

        // Permission : super-utilisateurs partout, sinon le récepteur scopé du rôle ACTIF
        // (un faiseur/chef actif qui possède RESPONSABLE ne traite pas les demandes RESPONSABLE)
        if (!securityUtils.isSuperUser()) {
            boolean allowed = switch (req.getCible()) {
                case RESPONSABLE -> securityUtils.hasActiveRole("RESPONSABLE")
                        && req.getDepartmentId() != null
                        && departmentRepository.findByResponsableId(userId)
                                .stream().map(Department::getId).anyMatch(req.getDepartmentId()::equals);
                case CHEF_DE_FAMILLE -> securityUtils.hasActiveRole("CHEF_DE_FAMILLE")
                        && req.getFamilyId() != null
                        && familyRepository.findByChefFamilleId(userId)
                                .stream().map(Family::getId).anyMatch(req.getFamilyId()::equals);
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

    /** Nom du membre d'une présence : depuis le compte, sinon depuis l'âme (soul_id). */
    private String presenceNom(MemberPresence p) {
        if (p.getUserId() != null) {
            String nom = fullName(p.getUserId());
            if (!"Membre".equals(nom)) return nom;
        }
        return p.getSoulId() != null
                ? soulRepository.findById(p.getSoulId())
                        .map(Soul::getNomComplet)
                        .orElse("Membre")
                : "Membre";
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

    /**
     * Vérifie que l'utilisateur courant peut gérer le département :
     * super-utilisateurs partout, sinon le rôle ACTIF doit être RESPONSABLE
     * (un faiseur/chef actif qui possède le rôle RESPONSABLE n'y a pas accès)
     * et le département doit lui appartenir.
     */
    private void verifyDepartmentAccess(UUID userId, UUID deptId) {
        if (securityUtils.isSuperUser()) {
            return;
        }
        if (!securityUtils.hasActiveRole("RESPONSABLE")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La gestion du département requiert le mode Responsable");
        }
        boolean gere = departmentRepository.findByResponsableId(userId).stream()
                .anyMatch(d -> d.getId().equals(deptId));
        if (!gere) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne gérez pas ce département");
        }
    }

    /** Âmes actives membres d'un département (via soul_departments). */
    private List<Soul> departmentMembers(UUID deptId) {
        return soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)
                .stream()
                .map(sd -> soulRepository.findById(sd.getSoulId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparing(Soul::getNomComplet))
                .toList();
    }

    /** Âmes sous la responsabilité du rôle ACTIF courant (pour la visibilité des présences). */
    private List<UUID> scopedSoulIds(User user) {
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(user.getId())
                    .stream().map(Department::getId).toList();
            return deptIds.isEmpty() ? List.of()
                    : memberDepartmentRepository.findByDepartmentIdIn(deptIds)
                            .stream().map(MemberDepartment::getSoulId).distinct().toList();
        }
        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
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
        return MemberRequestResponse.from(r, auteurNom, traiteParNom, deptNom, famNom,
                attachmentService.itemsFor(EntityAttachment.EntityType.MEMBER_REQUEST, r.getId()));
    }

    // ============================================================
    // Phase 3 : progression, événements, notes
    // ============================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getMyProgression() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        Map<String, Object> result = new LinkedHashMap<>();

        // Soul info
        Soul soul = soulRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (soul != null) {
            result.put("etatSpirituel", soul.getEtatSpirituel());
            result.put("niveauCroissance", soul.getNiveauCroissance());
            result.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
            result.put("typeDisciple", soul.getTypeDisciple() != null ? soul.getTypeDisciple().name() : null);
        }

        // Attendance stats from presences
        List<MemberPresence> presences = memberPresenceRepository.findByUserIdOrderBySemaineDesc(userId);
        int totalWeeks = presences.size();
        long weeksPresent = presences.stream()
                .filter(p -> p.getPresences() != null && p.getPresences().containsValue(true))
                .count();
        double tauxPresence = totalWeeks > 0 ? Math.round((double) weeksPresent / totalWeeks * 1000.0) / 10.0 : 0.0;
        result.put("totalSemaines", totalWeeks);
        result.put("semainesPresents", weeksPresent);
        result.put("tauxPresence", tauxPresence);

        // Streak: consecutive weeks with presence
        int streak = 0;
        LocalDate checkDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < 26; i++) { // Max 26 weeks (~6 months)
            String weekStr = checkDate.minusWeeks(i).toString();
            boolean found = presences.stream().anyMatch(p ->
                    weekStr.equals(p.getSemaine()) && p.getPresences() != null && p.getPresences().containsValue(true));
            if (found) streak++; else break;
        }
        result.put("streak", streak);

        // Department count (via member_department or soul_department)
        long deptCount = 0;
        if (soul != null) {
            deptCount = soulDepartmentRepository.findBySoulId(soul.getId()).stream()
                    .filter(sd -> Boolean.TRUE.equals(sd.isActif())).count();
        }
        result.put("nombreDepartements", deptCount);

        // Family info
        result.put("famille", soul != null && soul.getFamilleId() != null ?
                familyRepository.findById(soul.getFamilleId()).map(Family::getNom).orElse(null) : null);

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyUpcomingEvents() {
        UUID userId = securityUtils.getCurrentUserId();
        Soul soul = soulRepository.findAllByUserId(userId).stream()
                .filter(s -> !s.isDeleted())
                .findFirst()
                .orElse(null);
        if (soul == null) return List.of();

        // Événements de la famille du membre
        List<Event> familleEvents = soul.getFamilleId() != null
                ? eventRepository.findByFamilleIdAndStatutAndDeletedFalse(soul.getFamilleId(), "PLANIFIE")
                : List.of();

        // Événements des départements du membre
        List<UUID> departmentIds = memberDepartmentRepository.findBySoulId(soul.getId()).stream()
                .map(MemberDepartment::getDepartmentId)
                .distinct()
                .toList();
        List<Event> departementEvents = departmentIds.isEmpty()
                ? List.of()
                : eventRepository.findByDepartmentIdInAndDeletedFalse(departmentIds);

        LocalDateTime now = LocalDateTime.now();
        Map<UUID, Event> byId = new LinkedHashMap<>();
        for (Event ev : familleEvents) if (ev.getDateDebut() != null && ev.getDateDebut().isAfter(now)) byId.put(ev.getId(), ev);
        for (Event ev : departementEvents) if (ev.getDateDebut() != null && ev.getDateDebut().isAfter(now)) byId.put(ev.getId(), ev);

        List<Map<String, Object>> result = byId.values().stream()
                .sorted(Comparator.comparing(Event::getDateDebut))
                .map(ev -> {
                    Map<String, Object> em = new LinkedHashMap<>();
                    em.put("id", ev.getId());
                    em.put("titre", ev.getTitre());
                    em.put("typeEvenement", ev.getTypeEvenement());
                    em.put("lieu", ev.getLieu());
                    em.put("dateDebut", ev.getDateDebut().toString());
                    em.put("dateFin", ev.getDateFin() != null ? ev.getDateFin().toString() : null);
                    em.put("statut", ev.getStatut());
                    em.put("familleId", ev.getFamilleId());
                    em.put("departmentId", ev.getDepartmentId());
                    em.put("nbInscrits", ev.getNbInscrits());
                    em.put("limitePlaces", ev.getLimitePlaces());
                    return em;
                })
                .toList();
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyNotes() {
        UUID userId = securityUtils.getCurrentUserId();
        Soul soul = soulRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (soul == null) return List.of();

        List<Map<String, Object>> notes = new ArrayList<>();
        if (soul.getNotesPasteur() != null && !soul.getNotesPasteur().isBlank()) {
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("type", "PASTEUR");
            note.put("contenu", soul.getNotesPasteur());
            note.put("date", soul.getUpdatedAt() != null ? soul.getUpdatedAt().toString() : null);
            notes.add(note);
        }
        return notes;
    }

    /** Enregistre la présence d'un membre via scan QR code. */
    public void recordPresenceByQr(UUID soulId) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("Soul", soulId));
        if (soul.getUserId() == null) {
            throw new IllegalStateException("Ce membre n'a pas de compte utilisateur lié");
        }
        LocalDate monday = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        MemberPresence existing = memberPresenceRepository
                .findByUserIdAndSemaine(soul.getUserId(), monday)
                .orElse(null);
        if (existing != null) {
            if (existing.getPresences() == null) existing.setPresences(new java.util.LinkedHashMap<>());
            existing.getPresences().put("QR_CHECKIN", true);
            memberPresenceRepository.save(existing);
        } else {
            memberPresenceRepository.save(MemberPresence.builder()
                    .userId(soul.getUserId())
                    .semaine(monday)
                    .presences(new java.util.LinkedHashMap<>(java.util.Map.of("QR_CHECKIN", true)))
                    .build());
        }
    }
}
