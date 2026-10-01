package com.discipolat.modules.families.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.families.domain.*;
import com.discipolat.modules.families.repository.*;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.people.repository.RoleAssignmentRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * G4.1/G4.2 — Famille OS : dashboard, visites, réceptions, réunions, journal,
 * recherche/ajout de membres — avec garde d'accès RÉELLE par famille
 * (l'acteur doit être chef, adjoint, membre/âme rattachée, faiseur d'une âme
 * de la famille, ou ADMIN/PASTEUR) et minimisation des données (§ G4.2 :
 * la recherche ne expose ni email, ni téléphone, ni adresse).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FamilyOSService {

    private final FamilyVisitRepository familyVisitRepository;
    private final FamilyReceptionRepository familyReceptionRepository;
    private final FamilyMeetingRepository familyMeetingRepository;
    private final FamilyActivityRepository familyActivityRepository;
    private final FamilyRepository familyRepository;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final NotificationService notificationService;
    private final OutboxPublisher outboxPublisher;

    // ========== ACCÈS (G4.1 — paroisse = périmètre, pas simple rôle) ==========

    /**
     * Vérifie que l'acteur a accès en LECTURE à la famille, et retourne la famille.
     * ADMIN/PASTEUR (rôles tenant) → accès ; chef/adjoint/membre rattaché/faiseur → accès.
     */
    Family requireFamilyAccess(UUID tenantId, UUID familyId, UUID actorId) {
        Family family = loadFamily(tenantId, familyId);
        if (hasSuperRole()) return family;
        if (actorId != null && (actorId.equals(family.getChefFamilleId())
                || actorId.equals(family.getChefAdjointId())
                || actorId.equals(family.getUserId()))) {
            return family;
        }
        boolean linked = actorId != null && soulRepository.findAllByFamilleIdAndDeletedFalse(familyId).stream()
                .anyMatch(s -> actorId.equals(s.getUserId()) || actorId.equals(s.getFaiseurId()));
        if (linked) return family;
        throw new AccessDeniedException("Accès refusé à cette famille");
    }

    /**
     * Vérifie l'accès en ÉCRITURE (visites, réceptions, réunions, ajouts) :
     * chefs, adjoint, ADMIN/PASTEUR, ou faiseur d'une âme de la famille.
     */
    Family requireFamilyWriteAccess(UUID tenantId, UUID familyId, UUID actorId) {
        Family family = loadFamily(tenantId, familyId);
        if (hasSuperRole()) return family;
        if (actorId != null && (actorId.equals(family.getChefFamilleId())
                || actorId.equals(family.getChefAdjointId()))) {
            return family;
        }
        boolean faiseur = actorId != null && soulRepository.findAllByFamilleIdAndDeletedFalse(familyId).stream()
                .anyMatch(s -> actorId.equals(s.getFaiseurId()));
        if (faiseur) return family;
        throw new AccessDeniedException("Accès en écriture refusé pour cette famille");
    }

    private Family loadFamily(UUID tenantId, UUID familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new EntityNotFoundException("Family", familyId));
        if (!tenantId.equals(family.getTenantId()) || family.isDeleted()) {
            throw new EntityNotFoundException("Family", familyId);
        }
        return family;
    }

    private boolean hasSuperRole() {
        List<String> roles = securityUtils.getAllUserRoles();
        return roles != null && (roles.contains("ADMIN") || roles.contains("PASTEUR"));
    }

    // ========== DASHBOARD ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getFamilyDashboard(UUID tenantId, UUID familyId) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("familyId", familyId);

        // Upcoming visits (next 7 days)
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        List<FamilyVisit> upcomingVisits = familyVisitRepository.findByFamilyIdAndVisitDateBetweenAndDeletedFalse(
                familyId, today, nextWeek);
        dashboard.put("upcomingVisits", upcomingVisits.stream()
                .map(this::visitToSummary)
                .toList());

        // Souls in follow-up (with next_action_date <= today)
        List<FamilyVisit> followUps = familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId).stream()
                .filter(v -> v.getNextActionDate() != null && !v.getNextActionDate().isAfter(today))
                .filter(v -> !"COMPLETED".equals(v.getStatus()) && !"CANCELLED".equals(v.getStatus()))
                .toList();
        dashboard.put("followUps", followUps.stream()
                .map(this::visitToSummary)
                .toList());

        // Recent receptions (last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<FamilyReception> recentReceptions = familyReceptionRepository.findByFamilyIdAndReceptionDateBetween(
                familyId, thirtyDaysAgo, LocalDate.now());
        dashboard.put("recentReceptions", recentReceptions);

        // Upcoming meetings
        List<FamilyMeeting> upcomingMeetings = familyMeetingRepository.findByFamilyIdAndMeetingDateBetween(
                familyId, today, today.plusDays(30));
        dashboard.put("upcomingMeetings", upcomingMeetings);

        // Overdue follow-ups
        long overdueCount = familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId).stream()
                .filter(v -> v.getNextActionDate() != null && v.getNextActionDate().isBefore(LocalDate.now()))
                .filter(v -> !"COMPLETED".equals(v.getStatus()) && !"CANCELLED".equals(v.getStatus()))
                .count();
        dashboard.put("overdueFollowUps", overdueCount);

        return dashboard;
    }

    // ========== VISITS ==========

    public FamilyVisit createVisit(UUID tenantId, UUID actorId, FamilyVisit visit) {
        requireFamilyWriteAccess(tenantId, visit.getFamilyId(), actorId);
        visit.setTenantId(tenantId);
        visit.setFaiseurId(actorId);
        visit.setCreatedBy(actorId);
        FamilyVisit saved = familyVisitRepository.save(visit);

        // Also create FamilyActivity entry
        FamilyActivity activity = FamilyActivity.builder()
                .tenantId(tenantId)
                .familyId(visit.getFamilyId())
                .activityType("VISIT")
                .referenceId(saved.getId())
                .title("Visite: " + getSoulName(visit.getSoulId()))
                .description(visit.getSubject())
                .activityDate(visit.getVisitDate())
                .status(visit.getStatus())
                .createdBy(actorId)
                .build();
        familyActivityRepository.save(activity);

        return saved;
    }

    public FamilyVisit updateVisit(UUID tenantId, UUID actorId, UUID familyId, UUID visitId, FamilyVisit updates) {
        FamilyVisit visit = familyVisitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("FamilyVisit", visitId));
        if (!visit.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cross-tenant");
        }
        if (!familyId.equals(visit.getFamilyId())) {
            throw new EntityNotFoundException("FamilyVisit", visitId);
        }
        requireFamilyWriteAccess(tenantId, familyId, actorId);

        if (updates.getVisitDate() != null) visit.setVisitDate(updates.getVisitDate());
        if (updates.getVisitType() != null) visit.setVisitType(updates.getVisitType());
        if (updates.getSubject() != null) visit.setSubject(updates.getSubject());
        if (updates.getReport() != null) visit.setReport(updates.getReport());
        if (updates.getDecisions() != null) visit.setDecisions(updates.getDecisions());
        if (updates.getNextActionDate() != null) visit.setNextActionDate(updates.getNextActionDate());
        if (updates.getNextActionType() != null) visit.setNextActionType(updates.getNextActionType());
        if (updates.getStatus() != null) visit.setStatus(updates.getStatus());

        FamilyVisit saved = familyVisitRepository.save(visit);

        // Update FamilyActivity
        familyActivityRepository.findByReferenceId(visitId).ifPresent(a -> {
            a.setTitle("Visite: " + getSoulName(visit.getSoulId()));
            a.setDescription(visit.getSubject());
            a.setActivityDate(visit.getVisitDate());
            a.setStatus(visit.getStatus());
            familyActivityRepository.save(a);
        });

        return saved;
    }

    @Transactional(readOnly = true)
    public List<FamilyVisit> getFamilyVisits(UUID tenantId, UUID familyId, LocalDate from, LocalDate to) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        if (from != null && to != null) {
            return familyVisitRepository.findByFamilyIdAndVisitDateBetweenAndDeletedFalse(familyId, from, to);
        }
        return familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId);
    }

    // ========== RECEPTIONS ==========

    public FamilyReception createReception(UUID tenantId, UUID actorId, FamilyReception reception) {
        requireFamilyWriteAccess(tenantId, reception.getFamilyId(), actorId);
        reception.setTenantId(tenantId);
        reception.setCreatedBy(actorId);
        FamilyReception saved = familyReceptionRepository.save(reception);

        // Also create FamilyActivity
        FamilyActivity activity = FamilyActivity.builder()
                .tenantId(tenantId)
                .familyId(reception.getFamilyId())
                .activityType("RECEPTION")
                .referenceId(saved.getId())
                .title("Réception: " + getSoulName(reception.getSoulId()))
                .description(reception.getNotes())
                .activityDate(reception.getReceptionDate())
                .status("COMPLETED")
                .createdBy(actorId)
                .build();
        familyActivityRepository.save(activity);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<FamilyReception> getFamilyReceptions(UUID tenantId, UUID familyId, LocalDate from, LocalDate to) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        if (from != null && to != null) {
            return familyReceptionRepository.findByFamilyIdAndReceptionDateBetween(familyId, from, to);
        }
        return familyReceptionRepository.findByFamilyIdAndDeletedFalse(familyId);
    }

    // ========== MEETINGS ==========

    public FamilyMeeting createMeeting(UUID tenantId, UUID actorId, FamilyMeeting meeting) {
        requireFamilyWriteAccess(tenantId, meeting.getFamilyId(), actorId);
        meeting.setTenantId(tenantId);
        meeting.setCreatedBy(actorId);
        return familyMeetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public List<FamilyMeeting> getFamilyMeetings(UUID tenantId, UUID familyId, LocalDate from, LocalDate to) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        if (from != null && to != null) {
            return familyMeetingRepository.findByFamilyIdAndMeetingDateBetween(familyId, from, to);
        }
        return familyMeetingRepository.findByFamilyIdAndDeletedFalse(familyId);
    }

        @Transactional(readOnly = true)
    public List<FamilyMeeting> getFamilyMeetings(UUID tenantId, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return familyMeetingRepository.findAllByTenantIdAndDeletedFalseOrderByMeetingDateDesc(tenantId)
                    .stream().filter(m -> !m.getMeetingDate().isBefore(from) && !m.getMeetingDate().isAfter(to))
                    .toList();
        }
        return familyMeetingRepository.findAllByTenantIdAndDeletedFalseOrderByMeetingDateDesc(tenantId);
    }

    public FamilyMeeting completeMeeting(UUID tenantId, UUID actorId, UUID meetingId, String minutes) {
        FamilyMeeting meeting = familyMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new EntityNotFoundException("FamilyMeeting", meetingId));
        if (!tenantId.equals(meeting.getTenantId())) {
            throw new EntityNotFoundException("FamilyMeeting", meetingId);
        }
        meeting.setStatus("COMPLETED");
        if (minutes != null && !minutes.isBlank()) {
            String existing = meeting.getDescription() == null ? "" : meeting.getDescription();
            meeting.setDescription(existing + "\n\n[Clôture] " + minutes);
        }
        return familyMeetingRepository.save(meeting);
    }

    // ========== UNIFIED ACTIVITY LIST ==========

    @Transactional(readOnly = true)
    public Page<FamilyActivity> getFamilyActivities(UUID tenantId, UUID familyId, int page, int size) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("activityDate").descending());
        // G4.1 FIX — le journal était fuitté tout le tenant ; désormais scopé sur LA famille.
        return familyActivityRepository.findByTenantIdAndFamilyIdOrderByActivityDateDesc(tenantId, familyId, pageable);
    }

    @Transactional(readOnly = true)
    public List<FamilyActivity> getFamilyActivitiesByDateRange(UUID tenantId, UUID familyId, LocalDate from, LocalDate to) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        return familyActivityRepository.findByFamilyIdAndActivityDateBetween(familyId, from, to);
    }

    // ========== MEMBERS (G4.2) ==========

    @Transactional(readOnly = true)
    public List<Soul> getFamilySouls(UUID tenantId, UUID familyId) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());
        return soulRepository.findAllByFamilleIdAndDeletedFalse(familyId);
    }

    // ========== HELPERS ==========

    /** Nom lisible d'une âme (fallback utilisateur lié) — G4.1 : l'id est un Soul, pas un User. */
    private String getSoulName(UUID soulId) {
        if (soulId == null) return "Inconnu";
        Optional<Soul> soul = soulRepository.findById(soulId);
        if (soul.isPresent()) {
            return soul.get().getNomComplet();
        }
        return userRepository.findById(soulId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Inconnu");
    }

    private Map<String, Object> visitToSummary(FamilyVisit v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("soulId", v.getSoulId());
        m.put("soulName", getSoulName(v.getSoulId()));
        m.put("visitDate", v.getVisitDate());
        m.put("visitType", v.getVisitType());
        m.put("subject", v.getSubject());
        m.put("status", v.getStatus());
        m.put("nextActionDate", v.getNextActionDate());
        return m;
    }

    private UUID currentUserIdOrNull() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ========== G4.2: SEARCH & ADD MEMBERS ==========

    /**
     * Recherche les âmes NON AFFECTÉES pouvant rejoindre la famille, scopée selon
     * « visible_people_scope » du tenant (CHURCH par défaut, CAMPUS si configuré).
     * Minimisation : ne retourne que {soulId, userId, prenom, nom} — jamais les
     * coordonnées complètes de toute l'église.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchSoulsForFamily(UUID tenantId, UUID familyId, String search, String scope) {
        requireFamilyAccess(tenantId, familyId, currentUserIdOrNull());

        List<Soul> candidates = soulRepository.findByTenantId(tenantId).stream()
                .filter(s -> !s.isDeleted())
                // non affectées : aucune famille (les membres d'AUTRES familles ne sont pas proposés)
                .filter(s -> s.getFamilleId() == null)
                .toList();

        if ("CAMPUS".equalsIgnoreCase(scope)) {
            Set<UUID> campusUserIds = resolveCampusUserIds(tenantId, familyId);
            if (campusUserIds != null) {
                // Une âme appartient au campus si son compte utilisateur ou son faiseur y appartient.
                candidates = candidates.stream()
                        .filter(s -> (s.getUserId() != null && campusUserIds.contains(s.getUserId()))
                                || (s.getFaiseurId() != null && campusUserIds.contains(s.getFaiseurId())))
                        .toList();
            }
            // campus non résolvable → repli CHURCH (documenté : pas de membership CAMPUS pour le chef)
        }

        String q = search == null || search.isBlank() ? null : search.toLowerCase();
        return candidates.stream()
                .filter(s -> q == null
                        || (s.getPrenom() != null && s.getPrenom().toLowerCase().contains(q))
                        || (s.getNom() != null && s.getNom().toLowerCase().contains(q)))
                .limit(50)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("soulId", s.getId());
                    m.put("userId", s.getUserId());
                    m.put("prenom", s.getPrenom());
                    m.put("nom", s.getNom());
                    return m;
                })
                .toList();
    }

    /**
     * Users du campus de la famille : périmètre CAMPUS (sous-arbre) du chef de famille
     * (et de son adjoint). Retourne null si aucun campus connu → l'appelant retombe sur CHURCH.
     */
    private Set<UUID> resolveCampusUserIds(UUID tenantId, UUID familyId) {
        Family family = loadFamily(tenantId, familyId);
        Set<UUID> unitIds = new HashSet<>();
        for (UUID chef : Arrays.asList(family.getChefFamilleId(), family.getChefAdjointId())) {
            if (chef == null) continue;
            for (TenantMembership tm : tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(
                    chef, tenantId, MembershipStatus.ACTIVE)) {
                if (tm.getScopeType() == MembershipScopeType.CAMPUS && tm.getScopeId() != null) {
                    unitIds.add(tm.getScopeId());
                    orgNodeRepository.findDescendantsByNodeId(tenantId, tm.getScopeId())
                            .forEach(n -> unitIds.add(n.getId()));
                }
            }
        }
        if (unitIds.isEmpty()) return null;

        Set<UUID> userIds = tenantMembershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE)
                .stream()
                .filter(tm -> tm.getScopeId() != null && unitIds.contains(tm.getScopeId()))
                .map(TenantMembership::getUserId)
                .collect(Collectors.toSet());
        for (UUID unitId : unitIds) {
            roleAssignmentRepository.findByTenantIdAndOrganizationUnitIdAndStatus(tenantId, unitId, "ACTIVE")
                    .forEach(ra -> userIds.add(ra.getPersonId()));
        }
        return userIds;
    }

    /**
     * Ajout RÉEL d'un membre à la famille (G4.2) : rattache l'âme, notifie
     * l'intéressé et le chef de famille, journalise et publie l'événement temps réel.
     */
    public Family addSoulToFamily(UUID tenantId, UUID actorId, UUID familyId, UUID soulId, UUID faiseurId) {
        Family family = requireFamilyWriteAccess(tenantId, familyId, actorId);

        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", soulId));
        if (!tenantId.equals(soul.getTenantId()) || soul.isDeleted()) {
            throw new EntityNotFoundException("Soul", soulId);
        }
        if (familyId.equals(soul.getFamilleId())) {
            return family; // idempotent : déjà membre de cette famille
        }

        if (faiseurId != null && !faiseurId.equals(soul.getFaiseurId())) {
            User faiseur = userRepository.findById(faiseurId)
                    .orElseThrow(() -> new EntityNotFoundException("User", faiseurId));
            if (!tenantId.equals(faiseur.getTenantId())) {
                throw new SecurityException("Cross-tenant");
            }
            soul.setFaiseurId(faiseurId);
        }

        // G4.2 — rattachement réel de l'âme à la famille (fix : la version
        // précédente créait seulement une activité sans jamais modifier l'âme).
        // Le bloc de validation du faiseur ci-dessus couvre déjà la pose de
        // faiseurId (en plus strict : il vérifie le tenant du faiseur). On ne
        // fait qu'un seul save ici pour éviter la double écriture issue de
        // l'union de fusion main + Develop1 (l'ancienne version sauvait deux fois).
        soul.setFamilleId(familyId);
        soulRepository.save(soul);

        // FamilyActivity for tracking
        FamilyActivity activity = FamilyActivity.builder()
                .tenantId(tenantId)
                .familyId(familyId)
                .activityType("RECEPTION")
                .referenceId(soulId)
                .title("Ajout membre: " + soul.getPrenom() + " " + soul.getNom())
                .description("Nouveau membre ajouté à la famille")
                .activityDate(LocalDate.now())
                .status("COMPLETED")
                .createdBy(actorId)
                .build();
        familyActivityRepository.save(activity);

        // Notification in-app : l'intéressé (si lié à un compte) + le chef de famille
        String message = "Vous avez été ajouté à la famille « " + family.getNom() + " ».";
        if (soul.getUserId() != null) {
            safeNotify(soul.getUserId(), TypeNotification.MEMBRE_AJOUTE, familyId, message);
        }
        if (family.getChefFamilleId() != null
                && !family.getChefFamilleId().equals(soul.getUserId())
                && !family.getChefFamilleId().equals(actorId)) {
            safeNotify(family.getChefFamilleId(), TypeNotification.MEMBRE_AJOUTE, familyId,
                    soul.getNomComplet() + " a rejoint la famille « " + family.getNom() + " ».");
        }

        // Temps réel (firehose G5.8) : les écrans famille rafraîchissent.
        outboxPublisher.publish(tenantId, "FAMILY", familyId, "FamilyMemberAdded",
                Map.of("familyId", familyId.toString(),
                        "soulId", soulId.toString(),
                        "actorId", actorId != null ? actorId.toString() : "system"));

        // Retour de l'entité déjà chargée et contrôlée par requireFamilyWriteAccess
        // (même garantie que le findById().orElseThrow() de main : jamais null).
        return family;
    }

    private void safeNotify(UUID destinataireId, TypeNotification type, UUID familyId, String message) {
        try {
            notificationService.create(destinataireId, type, CanalNotification.IN_APP,
                    type == TypeNotification.MEMBRE_AJOUTE ? "Nouveau membre dans votre famille" : "Famille",
                    message, familyId, "FAMILY");
        } catch (Exception e) {
            log.warn("Notification famille {} échouée pour {} : {}", familyId, destinataireId, e.getMessage());
        }
    }
}
