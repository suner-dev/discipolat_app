package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.api.DepartmentAssignmentRequest;
import com.discipolat.modules.departments.api.DepartmentCreateMemberRequest;
import com.discipolat.modules.departments.api.DepartmentPositionRequest;
import com.discipolat.modules.departments.api.DepartmentTaskRequest;
import com.discipolat.modules.departments.api.DepartmentTeamRequest;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Department Management System — cœur métier de l'espace Responsable.
 * <p>
 * Sous-départements/équipes (hiérarchie récursive), postes,
 * affectations de membres avec traçabilité, tâches et charge de
 * travail, journal d'activité et indicateurs de gestion.
 * <p>
 * Tous les accès passent par {@link DepartmentService#findById} qui
 * vérifie que le département appartient à l'espace métier du rôle actif.
 */
@Service
@Transactional
public class DepartmentManagementService {

    private final DepartmentService departmentService;
    private final DepartmentTeamRepository teamRepository;
    private final DepartmentPositionRepository positionRepository;
    private final DepartmentAssignmentRepository assignmentRepository;
    private final DepartmentTaskRepository taskRepository;
    private final DepartmentActivityRepository activityRepository;
    private final SoulRepository soulRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final SoulService soulService;

    public DepartmentManagementService(DepartmentService departmentService,
                                       DepartmentTeamRepository teamRepository,
                                       DepartmentPositionRepository positionRepository,
                                       DepartmentAssignmentRepository assignmentRepository,
                                       DepartmentTaskRepository taskRepository,
                                       DepartmentActivityRepository activityRepository,
                                       SoulRepository soulRepository,
                                       SoulDepartmentRepository soulDepartmentRepository,
                                       UserRepository userRepository,
                                       SecurityUtils securityUtils,
                                       NotificationService notificationService,
                                       SoulService soulService) {
        this.departmentService = departmentService;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.activityRepository = activityRepository;
        this.soulRepository = soulRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
        this.soulService = soulService;
    }

    /** Vérifie que le département existe et appartient à l'espace métier de l'utilisateur. */
    private void assertCanManage(UUID departmentId) {
        departmentService.findById(departmentId);
    }

    // ========================================================================
    // JOURNAL D'ACTIVITÉ
    // ========================================================================

    private String actorName() {
        return userRepository.findById(securityUtils.getCurrentUserId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);
    }

    private void record(UUID departmentId, String action, String entityType, UUID entityId, String details) {
        DepartmentActivity activity = DepartmentActivity.builder()
                .departmentId(departmentId)
                .actorId(securityUtils.getCurrentUserId())
                .actorNom(actorName())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        activityRepository.save(activity);
    }

    // ========================================================================
    // ÉQUIPES / SOUS-DÉPARTEMENTS
    // ========================================================================

    public List<Map<String, Object>> getTeams(UUID departmentId) {
        assertCanManage(departmentId);
        List<DepartmentTeam> teams = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId);

        // Archivage automatique des équipes temporaires dont la période est terminée
        LocalDate today = LocalDate.now();
        for (DepartmentTeam team : teams) {
            if (team.getType() == DepartmentTeam.TeamType.EQUIPE_TEMPORAIRE
                    && team.getStatut() == DepartmentTeam.TeamStatus.ACTIVE
                    && team.getDateFin() != null && team.getDateFin().isBefore(today)) {
                team.setStatut(DepartmentTeam.TeamStatus.ARCHIVED);
                teamRepository.save(team);
                deactivateTeamAssignments(team.getId());
                record(departmentId, "TEAM_ARCHIVED_AUTO", "TEAM", team.getId(),
                        "Équipe temporaire « " + team.getNom() + " » archivée automatiquement (période terminée)");
            }
        }
        teams = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId);

        // Chargement groupé des noms (évite N+1 sur chef/adjoint)
        Map<UUID, String> userNames = new HashMap<>();
        List<UUID> userIds = teams.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getChefId(), t.getAdjointId()))
                .filter(Objects::nonNull).distinct().toList();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userNames.put(u.getId(), u.getFirstName() + " " + u.getLastName()));
        }

        Map<UUID, Long> counts = assignmentRepository.findByDepartmentIdAndActifTrue(departmentId).stream()
                .collect(Collectors.groupingBy(DepartmentAssignment::getTeamId, Collectors.counting()));
        return teams.stream().map(t -> toTeamMap(t, counts, userNames)).toList();
    }

    private Map<String, Object> toTeamMap(DepartmentTeam t, Map<UUID, Long> counts, Map<UUID, String> userNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("nom", t.getNom());
        m.put("parentId", t.getParentId());
        m.put("type", t.getType().name());
        m.put("chefId", t.getChefId());
        m.put("chefNom", t.getChefId() != null ? userNames.get(t.getChefId()) : null);
        m.put("adjointId", t.getAdjointId());
        m.put("adjointNom", t.getAdjointId() != null ? userNames.get(t.getAdjointId()) : null);
        m.put("objectif", t.getObjectif());
        m.put("description", t.getDescription());
        m.put("dateDebut", t.getDateDebut() != null ? t.getDateDebut().toString() : null);
        m.put("dateFin", t.getDateFin() != null ? t.getDateFin().toString() : null);
        m.put("statut", t.getStatut().name());
        m.put("nbMembres", counts.getOrDefault(t.getId(), 0L));
        return m;
    }

    private void validateParent(UUID departmentId, UUID parentId, UUID selfId) {
        if (parentId == null) return;
        DepartmentTeam parent = teamRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", parentId));
        if (!parent.getDepartmentId().equals(departmentId)) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "L'équipe parente doit appartenir au même département", "PARENT_DEPARTMENT_MISMATCH");
        }
        // Anti-cycle : le parent ne doit pas être l'équipe elle-même ni un de ses descendants
        UUID cursor = parentId;
        Set<UUID> seen = new HashSet<>();
        while (cursor != null) {
            if (cursor.equals(selfId)) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "Impossible : créerait une boucle dans la hiérarchie des équipes", "TEAM_CYCLE");
            }
            if (!seen.add(cursor)) break;
            cursor = teamRepository.findById(cursor).map(DepartmentTeam::getParentId).orElse(null);
        }
    }

    public Map<String, Object> createTeam(UUID departmentId, DepartmentTeamRequest request) {
        assertCanManage(departmentId);
        validateParent(departmentId, request.parentId(), null);
        DepartmentTeam team = DepartmentTeam.builder()
                .departmentId(departmentId)
                .parentId(request.parentId())
                .nom(request.nom().trim())
                .type(request.type() != null ? request.type() : DepartmentTeam.TeamType.EQUIPE_PERMANENTE)
                .chefId(request.chefId())
                .adjointId(request.adjointId())
                .objectif(request.objectif())
                .description(request.description())
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .statut(DepartmentTeam.TeamStatus.ACTIVE)
                .build();
        team = teamRepository.save(team);
        record(departmentId, "TEAM_CREATED", "TEAM", team.getId(),
                "Équipe « " + team.getNom() + " » créée");
        return toTeamMap(team, assignmentRepository.findByDepartmentIdAndActifTrue(departmentId).stream()
                .collect(Collectors.groupingBy(DepartmentAssignment::getTeamId, Collectors.counting())), Map.of());
    }

    public Map<String, Object> updateTeam(UUID departmentId, UUID teamId, DepartmentTeamRequest request) {
        assertCanManage(departmentId);
        DepartmentTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", teamId));
        if (!team.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : équipe hors de votre espace métier");
        }
        validateParent(departmentId, request.parentId(), teamId);
        team.setNom(request.nom().trim());
        team.setParentId(request.parentId());
        if (request.type() != null) team.setType(request.type());
        team.setChefId(request.chefId());
        team.setAdjointId(request.adjointId());
        team.setObjectif(request.objectif());
        team.setDescription(request.description());
        team.setDateDebut(request.dateDebut());
        team.setDateFin(request.dateFin());
        team = teamRepository.save(team);
        record(departmentId, "TEAM_UPDATED", "TEAM", teamId,
                "Équipe « " + team.getNom() + " » modifiée");
        return toTeamMap(team, assignmentRepository.findByDepartmentIdAndActifTrue(departmentId).stream()
                .collect(Collectors.groupingBy(DepartmentAssignment::getTeamId, Collectors.counting())), Map.of());
    }

    /** Archivage (récursif : la sous-hiérarchie est archivée, les affectations clôturées). */
    public void archiveTeam(UUID departmentId, UUID teamId) {
        assertCanManage(departmentId);
        DepartmentTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", teamId));
        if (!team.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : équipe hors de votre espace métier");
        }
        archiveTeamRecursive(team);
        record(departmentId, "TEAM_ARCHIVED", "TEAM", teamId,
                "Équipe « " + team.getNom() + " » archivée");
    }

    private void archiveTeamRecursive(DepartmentTeam team) {
        team.setStatut(DepartmentTeam.TeamStatus.ARCHIVED);
        teamRepository.save(team);
        deactivateTeamAssignments(team.getId());
        for (DepartmentTeam child : teamRepository.findByParentId(team.getId())) {
            archiveTeamRecursive(child);
        }
    }

    /** Clôture les affectations actives d'une équipe (fin datée du jour). */
    private void deactivateTeamAssignments(UUID teamId) {
        for (DepartmentAssignment assignment : assignmentRepository.findByTeamId(teamId)) {
            if (assignment.isActif()) {
                assignment.setActif(false);
                if (assignment.getDateFin() == null) {
                    assignment.setDateFin(LocalDate.now());
                }
                assignmentRepository.save(assignment);
            }
        }
    }

    // ========================================================================
    // POSTES
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPositions(UUID departmentId) {
        assertCanManage(departmentId);
        Map<UUID, Long> counts = assignmentRepository.findByDepartmentIdAndActifTrue(departmentId).stream()
                .filter(a -> a.getPositionId() != null)
                .collect(Collectors.groupingBy(DepartmentAssignment::getPositionId, Collectors.counting()));
        return positionRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("nom", p.getNom());
                    m.put("description", p.getDescription());
                    m.put("competencesRequises", p.getCompetencesRequises());
                    m.put("statut", p.getStatut().name());
                    m.put("nbMembres", counts.getOrDefault(p.getId(), 0L));
                    return m;
                })
                .toList();
    }

    public Map<String, Object> createPosition(UUID departmentId, DepartmentPositionRequest request) {
        assertCanManage(departmentId);
        DepartmentPosition position = DepartmentPosition.builder()
                .departmentId(departmentId)
                .nom(request.nom().trim())
                .description(request.description())
                .competencesRequises(request.competencesRequises())
                .statut(DepartmentPosition.PositionStatus.ACTIVE)
                .build();
        position = positionRepository.save(position);
        record(departmentId, "POSITION_CREATED", "POSITION", position.getId(),
                "Poste « " + position.getNom() + " » créé");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", position.getId());
        m.put("nom", position.getNom());
        m.put("description", position.getDescription());
        m.put("competencesRequises", position.getCompetencesRequises());
        m.put("statut", position.getStatut().name());
        m.put("nbMembres", 0L);
        return m;
    }

    public Map<String, Object> updatePosition(UUID departmentId, UUID positionId, DepartmentPositionRequest request) {
        assertCanManage(departmentId);
        DepartmentPosition position = findPosition(departmentId, positionId);
        position.setNom(request.nom().trim());
        position.setDescription(request.description());
        position.setCompetencesRequises(request.competencesRequises());
        position = positionRepository.save(position);
        record(departmentId, "POSITION_UPDATED", "POSITION", positionId,
                "Poste « " + position.getNom() + " » modifié");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", position.getId());
        m.put("nom", position.getNom());
        m.put("description", position.getDescription());
        m.put("competencesRequises", position.getCompetencesRequises());
        m.put("statut", position.getStatut().name());
        return m;
    }

    public void archivePosition(UUID departmentId, UUID positionId) {
        assertCanManage(departmentId);
        DepartmentPosition position = findPosition(departmentId, positionId);
        position.setStatut(DepartmentPosition.PositionStatus.ARCHIVED);
        positionRepository.save(position);
        record(departmentId, "POSITION_ARCHIVED", "POSITION", positionId,
                "Poste « " + position.getNom() + " » archivé");
    }

    private DepartmentPosition findPosition(UUID departmentId, UUID positionId) {
        DepartmentPosition position = positionRepository.findById(positionId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentPosition", positionId));
        if (!position.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : poste hors de votre espace métier");
        }
        return position;
    }

    // ========================================================================
    // AFFECTATIONS MEMBRE -> ÉQUIPE / POSTE
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssignments(UUID departmentId) {
        assertCanManage(departmentId);
        Map<UUID, String> teamNames = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentTeam::getId, DepartmentTeam::getNom));
        Map<UUID, String> positionNames = positionRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentPosition::getId, DepartmentPosition::getNom));
        List<DepartmentAssignment> assignments = assignmentRepository.findByDepartmentId(departmentId);
        Map<UUID, String> soulNames = soulNames(assignments.stream().map(DepartmentAssignment::getMemberId).toList());
        return assignments.stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("memberId", a.getMemberId());
                    m.put("memberNom", soulNames.get(a.getMemberId()));
                    m.put("teamId", a.getTeamId());
                    m.put("teamNom", a.getTeamId() != null ? teamNames.get(a.getTeamId()) : null);
                    m.put("positionId", a.getPositionId());
                    m.put("positionNom", a.getPositionId() != null ? positionNames.get(a.getPositionId()) : null);
                    m.put("role", a.getRole().name());
                    m.put("dateDebut", a.getDateDebut() != null ? a.getDateDebut().toString() : null);
                    m.put("dateFin", a.getDateFin() != null ? a.getDateFin().toString() : null);
                    m.put("actif", a.isActif());
                    return m;
                })
                .toList();
    }

    /** Noms complets groupés (évite le N+1 sur soulRepository.findById). */
    private Map<UUID, String> soulNames(List<UUID> soulIds) {
        List<UUID> distinctIds = soulIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) return Map.of();
        return soulRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Soul::getId, Soul::getNomComplet));
    }

    public Map<String, Object> assignMember(UUID departmentId, DepartmentAssignmentRequest request) {
        assertCanManage(departmentId);
        Soul soul = soulRepository.findById(request.memberId())
                .orElseThrow(() -> new EntityNotFoundException("Soul", request.memberId()));

        // Une affectation doit avoir une équipe ou un poste
        if (request.teamId() == null && request.positionId() == null) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Une affectation doit comporter une équipe ou un poste", "ASSIGNMENT_EMPTY");
        }

        // Isolation : un responsable n'affecte que des membres de SON département.
        // Règle tolérante : un département encore vide peut être constitué librement ;
        // dès qu'il contient des membres, l'affectation d'un membre extérieur est refusée.
        assertMemberBelongsToDepartment(departmentId, request.memberId(), soul);

        // Anti-doublon : pas de seconde affectation active sur la même équipe
        if (request.teamId() != null && assignmentRepository.existsByMemberIdAndTeamIdAndActifTrue(request.memberId(), request.teamId())) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    soul.getNomComplet() + " est déjà affecté à cette équipe", "ALREADY_ASSIGNED");
        }
        // Anti-doublon sur un poste : une seule affectation « sans équipe » par poste
        if (request.teamId() == null && request.positionId() != null) {
            boolean hasTeamlessPosition = assignmentRepository.findByDepartmentIdAndMemberIdAndActifTrue(departmentId, request.memberId()).stream()
                    .anyMatch(a -> a.getTeamId() == null && request.positionId().equals(a.getPositionId()));
            if (hasTeamlessPosition) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        soul.getNomComplet() + " a déjà une affectation sur ce poste", "ALREADY_ASSIGNED");
            }
        }
        if (request.teamId() != null) {
            DepartmentTeam team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", request.teamId()));
            if (!team.getDepartmentId().equals(departmentId)) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "L'équipe doit appartenir au département", "TEAM_DEPARTMENT_MISMATCH");
            }
        }
        if (request.positionId() != null) findPosition(departmentId, request.positionId());

        DepartmentAssignment assignment = DepartmentAssignment.builder()
                .departmentId(departmentId)
                .memberId(request.memberId())
                .teamId(request.teamId())
                .positionId(request.positionId())
                .role(request.role() != null ? request.role() : DepartmentAssignment.AssignmentRole.MEMBRE)
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .actif(true)
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        assignment = assignmentRepository.save(assignment);
        record(departmentId, "MEMBER_ASSIGNED", "ASSIGNMENT", assignment.getId(),
                soul.getNomComplet() + " affecté à " + describeAssignment(request, assignment));
        notifyMemberAddedToUser(departmentId, soul, "affecté");
        return getAssignmentMap(assignment);
    }

    private String describeAssignment(DepartmentAssignmentRequest request, DepartmentAssignment assignment) {
        String team = request.teamId() != null
                ? teamRepository.findById(request.teamId()).map(DepartmentTeam::getNom).orElse("?")
                : "aucune équipe";
        String position = request.positionId() != null
                ? positionRepository.findById(request.positionId()).map(DepartmentPosition::getNom).orElse("?")
                : "aucun poste";
        return team + " · " + position;
    }

    /** Met fin à une affectation (date de fin = aujourd'hui). */
    public Map<String, Object> endAssignment(UUID departmentId, UUID assignmentId) {
        assertCanManage(departmentId);
        DepartmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentAssignment", assignmentId));
        if (!assignment.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : affectation hors de votre espace métier");
        }
        assignment.setActif(false);
        if (assignment.getDateFin() == null) {
            assignment.setDateFin(LocalDate.now());
        }
        assignmentRepository.save(assignment);
        record(departmentId, "ASSIGNMENT_ENDED", "ASSIGNMENT", assignmentId,
                "Fin d'affectation de " + soulName(assignment.getMemberId()));
        return getAssignmentMap(assignment);
    }

    private Map<String, Object> getAssignmentMap(DepartmentAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("memberId", a.getMemberId());
        m.put("memberNom", soulName(a.getMemberId()));
        m.put("teamId", a.getTeamId());
        m.put("teamNom", a.getTeamId() != null ? teamRepository.findById(a.getTeamId()).map(DepartmentTeam::getNom).orElse(null) : null);
        m.put("positionId", a.getPositionId());
        m.put("positionNom", a.getPositionId() != null ? positionRepository.findById(a.getPositionId()).map(DepartmentPosition::getNom).orElse(null) : null);
        m.put("role", a.getRole().name());
        m.put("dateDebut", a.getDateDebut() != null ? a.getDateDebut().toString() : null);
        m.put("dateFin", a.getDateFin() != null ? a.getDateFin().toString() : null);
        m.put("actif", a.isActif());
        return m;
    }

    // ========================================================================
    // TÂCHES & CHARGE DE TRAVAIL
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTasks(UUID departmentId, DepartmentTask.TaskStatus statut, UUID teamId) {
        assertCanManage(departmentId);
        List<DepartmentTask> tasks = statut != null
                ? taskRepository.findByDepartmentIdAndStatut(departmentId, statut)
                : (teamId != null
                        ? taskRepository.findByDepartmentIdAndTeamId(departmentId, teamId)
                        : taskRepository.findByDepartmentIdOrderByEcheanceAsc(departmentId));

        // Chargement groupé des noms (évite N+1)
        Map<UUID, String> teamNames = new HashMap<>();
        List<UUID> teamIds = tasks.stream().map(DepartmentTask::getTeamId).filter(Objects::nonNull).distinct().toList();
        if (!teamIds.isEmpty()) {
            teamRepository.findAllById(teamIds).forEach(t -> teamNames.put(t.getId(), t.getNom()));
        }
        Map<UUID, String> soulNames = new HashMap<>();
        List<UUID> soulIds = tasks.stream().map(DepartmentTask::getAssignedTo).filter(Objects::nonNull).distinct().toList();
        if (!soulIds.isEmpty()) {
            soulRepository.findAllById(soulIds).forEach(s -> soulNames.put(s.getId(), s.getNomComplet()));
        }
        return tasks.stream().map(t -> toTaskMap(t, teamNames, soulNames)).toList();
    }

    private Map<String, Object> toTaskMap(DepartmentTask t, Map<UUID, String> teamNames, Map<UUID, String> soulNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("titre", t.getTitre());
        m.put("description", t.getDescription());
        m.put("teamId", t.getTeamId());
        m.put("teamNom", t.getTeamId() != null ? teamNames.get(t.getTeamId()) : null);
        m.put("assignedTo", t.getAssignedTo());
        m.put("assigneeNom", t.getAssignedTo() != null ? soulNames.get(t.getAssignedTo()) : null);
        m.put("priorite", t.getPriorite().name());
        m.put("statut", t.getStatut().name());
        m.put("dateDebut", t.getDateDebut() != null ? t.getDateDebut().toString() : null);
        m.put("echeance", t.getEcheance() != null ? t.getEcheance().toString() : null);
        m.put("avancement", t.getAvancement());
        m.put("enRetard", t.isOverdue());
        return m;
    }

    /** Version mono-tâche (création/mise à jour) — chargement direct des noms. */
    private Map<String, Object> toTaskMap(DepartmentTask t) {
        Map<UUID, String> teamNames = Map.of();
        Map<UUID, String> soulNames = Map.of();
        if (t.getTeamId() != null) {
            teamNames = teamRepository.findAllById(List.of(t.getTeamId())).stream()
                    .collect(Collectors.toMap(DepartmentTeam::getId, DepartmentTeam::getNom));
        }
        if (t.getAssignedTo() != null) {
            soulNames = soulRepository.findAllById(List.of(t.getAssignedTo())).stream()
                    .collect(Collectors.toMap(Soul::getId, Soul::getNomComplet));
        }
        return toTaskMap(t, teamNames, soulNames);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTaskStats(UUID departmentId) {
        assertCanManage(departmentId);
        List<DepartmentTask> all = taskRepository.findByDepartmentIdOrderByEcheanceAsc(departmentId);
        long aFaire = all.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.A_FAIRE).count();
        long enCours = all.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.EN_COURS).count();
        long bloquees = all.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.BLOQUEE).count();
        long terminees = all.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.TERMINEE).count();
        long validees = all.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.VALIDEE).count();
        long annulees = all.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.ANNULEE).count();
        long ouvertes = all.stream().filter(DepartmentTask::isOpen).count();
        long enRetard = all.stream().filter(DepartmentTask::isOverdue).count();

        // Charge de travail par membre (tâches ouvertes)
        Map<UUID, Long> openByMember = all.stream()
                .filter(DepartmentTask::isOpen)
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(DepartmentTask::getAssignedTo, Collectors.counting()));
        Map<UUID, Long> overdueByMember = all.stream()
                .filter(DepartmentTask::isOverdue)
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(DepartmentTask::getAssignedTo, Collectors.counting()));
        Map<UUID, String> memberNames = soulNames(openByMember.keySet().stream().toList());
        List<Map<String, Object>> charge = openByMember.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("memberId", e.getKey());
                    m.put("memberNom", memberNames.get(e.getKey()));
                    m.put("tachesOuvertes", e.getValue());
                    m.put("enRetard", overdueByMember.getOrDefault(e.getKey(), 0L));
                    return m;
                })
                .sorted((a, b) -> Long.compare((long) b.get("tachesOuvertes"), (long) a.get("tachesOuvertes")))
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", (long) all.size());
        stats.put("aFaire", aFaire);
        stats.put("enCours", enCours);
        stats.put("bloquees", bloquees);
        stats.put("terminees", terminees);
        stats.put("validees", validees);
        stats.put("annulees", annulees);
        stats.put("ouvertes", ouvertes);
        stats.put("enRetard", enRetard);
        stats.put("chargeParMembre", charge);
        return stats;
    }

    public Map<String, Object> createTask(UUID departmentId, DepartmentTaskRequest request) {
        assertCanManage(departmentId);
        if (request.teamId() != null) {
            DepartmentTeam team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", request.teamId()));
            if (!team.getDepartmentId().equals(departmentId)) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "L'équipe doit appartenir au département", "TEAM_DEPARTMENT_MISMATCH");
            }
        }
        DepartmentTask task = DepartmentTask.builder()
                .departmentId(departmentId)
                .teamId(request.teamId())
                .titre(request.titre().trim())
                .description(request.description())
                .assignedTo(request.assignedTo())
                .priorite(request.priorite() != null ? request.priorite() : DepartmentTask.TaskPriority.MOYENNE)
                .statut(request.statut() != null ? request.statut() : DepartmentTask.TaskStatus.A_FAIRE)
                .dateDebut(request.dateDebut())
                .echeance(request.echeance())
                .avancement(request.avancement() != null ? Math.max(0, Math.min(100, request.avancement())) : 0)
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        task = taskRepository.save(task);
        record(departmentId, "TASK_CREATED", "TASK", task.getId(),
                "Tâche « " + task.getTitre() + " » créée");
        notifyAssignee(task);
        return toTaskMap(task);
    }

    public Map<String, Object> updateTask(UUID departmentId, UUID taskId, DepartmentTaskRequest request) {
        assertCanManage(departmentId);
        DepartmentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentTask", taskId));
        if (!task.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : tâche hors de votre espace métier");
        }
        if (request.teamId() != null) {
            DepartmentTeam team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", request.teamId()));
            if (!team.getDepartmentId().equals(departmentId)) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "L'équipe doit appartenir au département", "TEAM_DEPARTMENT_MISMATCH");
            }
        }
        task.setTitre(request.titre().trim());
        task.setDescription(request.description());
        task.setTeamId(request.teamId());
        UUID previousAssignee = task.getAssignedTo();
        task.setAssignedTo(request.assignedTo());
        if (request.priorite() != null) task.setPriorite(request.priorite());
        if (request.statut() != null) task.setStatut(request.statut());
        task.setDateDebut(request.dateDebut());
        task.setEcheance(request.echeance());
        if (request.avancement() != null) task.setAvancement(Math.max(0, Math.min(100, request.avancement())));
        task = taskRepository.save(task);
        record(departmentId, "TASK_UPDATED", "TASK", taskId,
                "Tâche « " + task.getTitre() + " » mise à jour (statut : " + task.getStatut().name() + ")");
        // Notification uniquement lors d'une (ré)assignation effective — pas à chaque mise à jour.
        if (request.assignedTo() != null && !request.assignedTo().equals(previousAssignee)) {
            notifyAssignee(task);
        }
        return toTaskMap(task);
    }

    /**
     * Annulation d'une tâche : passage au statut ANNULEE (suppression logique).
     * La tâche reste dans l'historique et les statistiques (compteur « annulées »)
     * afin de préserver l'audit — cohérent avec l'archivage logique utilisé
     * partout ailleurs dans le module.
     */
    public void deleteTask(UUID departmentId, UUID taskId) {
        assertCanManage(departmentId);
        DepartmentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentTask", taskId));
        if (!task.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : tâche hors de votre espace métier");
        }
        task.setStatut(DepartmentTask.TaskStatus.ANNULEE);
        taskRepository.save(task);
        record(departmentId, "TASK_CANCELLED", "TASK", taskId,
                "Tâche « " + task.getTitre() + " » annulée");
    }

    // ========================================================================
    // MEMBRES DU DÉPARTEMENT (dossier de gestion)
    // ========================================================================

    /** Liste riche des membres du département (profil + affectations courantes). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMembersManagement(UUID departmentId) {
        assertCanManage(departmentId);
        List<SoulDepartment> links = soulDepartmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        List<Soul> souls = soulRepository.findAllById(links.stream().map(SoulDepartment::getSoulId).toList());
        Map<UUID, Soul> byId = souls.stream().collect(Collectors.toMap(Soul::getId, s -> s));

        Map<UUID, String> teamNames = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentTeam::getId, DepartmentTeam::getNom));
        Map<UUID, String> positionNames = positionRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentPosition::getId, DepartmentPosition::getNom));

        List<DepartmentAssignment> activeAssignments = assignmentRepository.findByDepartmentIdAndActifTrue(departmentId);

        List<Map<String, Object>> members = new ArrayList<>();
        for (SoulDepartment link : links) {
            Soul soul = byId.get(link.getSoulId());
            if (soul == null) continue;
            List<Map<String, Object>> assignements = activeAssignments.stream()
                    .filter(a -> a.getMemberId().equals(soul.getId()))
                    .map(a -> {
                        Map<String, Object> am = new LinkedHashMap<>();
                        am.put("id", a.getId());
                        am.put("teamId", a.getTeamId());
                        am.put("teamNom", a.getTeamId() != null ? teamNames.get(a.getTeamId()) : null);
                        am.put("positionId", a.getPositionId());
                        am.put("positionNom", a.getPositionId() != null ? positionNames.get(a.getPositionId()) : null);
                        am.put("role", a.getRole().name());
                        am.put("dateDebut", a.getDateDebut() != null ? a.getDateDebut().toString() : null);
                        am.put("dateFin", a.getDateFin() != null ? a.getDateFin().toString() : null);
                        return am;
                    })
                    .toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", soul.getId());
            m.put("nom", soul.getNom());
            m.put("prenom", soul.getPrenom());
            m.put("nomComplet", soul.getNomComplet());
            m.put("telephone", soul.getTelephone());
            m.put("email", soul.getEmail());
            m.put("profession", soul.getProfession());
            m.put("statut", soul.getStatut().name());
            m.put("typeDisciple", soul.getTypeDisciple().name());
            m.put("familleId", soul.getFamilleId());
            m.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
            m.put("dateAffectation", link.getDateAffectation() != null ? link.getDateAffectation().toString() : null);
            m.put("assignments", assignements);
            members.add(m);
        }
        members.sort(Comparator.comparing(a -> ((String) a.get("nomComplet"))));
        return members;
    }

    /**
     * Recherche de candidats à l'ajout dans le département : personnes déjà
     * inscrites sur la plateforme et non encore rattachées au département.
     * La recherche respecte le périmètre métier du rôle actif.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findCandidates(UUID departmentId, String query) {
        assertCanManage(departmentId);
        List<UUID> existing = soulDepartmentRepository.findByDepartmentIdAndActifTrue(departmentId).stream()
                .map(SoulDepartment::getSoulId).toList();
        String q = query == null ? "" : query.trim().toLowerCase();
        // Recherche en base (LIKE) : évite de charger toute la table puis filtrer en mémoire.
        // 30 résultats pour compenser l'exclusion des membres déjà rattachés (fenêtre de 20).
        Page<Soul> page = securityUtils.isSuperUser()
                ? soulRepository.search(q, PageRequest.of(0, 30))
                : soulRepository.searchIn(accessibleSoulIds(), q, PageRequest.of(0, 30));
        return page.getContent().stream()
                .filter(s -> !existing.contains(s.getId()))
                .limit(20)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("nomComplet", s.getNomComplet());
                    m.put("telephone", s.getTelephone());
                    m.put("email", s.getEmail());
                    m.put("statut", s.getStatut().name());
                    m.put("typeDisciple", s.getTypeDisciple().name());
                    m.put("dejaDansDept", false);
                    return m;
                })
                .toList();
    }

    /** Âmes accessibles à l'espace métier courant (rôle actif). */
    private List<UUID> accessibleSoulIds() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            List<UUID> deptIds = departmentService.findResponsableDepartments(currentUserId);
            return soulDepartmentRepository.findByDepartmentIdIn(deptIds).stream()
                    .filter(SoulDepartment::isActif).map(SoulDepartment::getSoulId).distinct().toList();
        }
        if (securityUtils.hasActiveRole("FAISEUR")) {
            return soulRepository.findAllByFaiseurId(currentUserId).stream().map(Soul::getId).toList();
        }
        if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            return userRepository.findById(currentUserId)
                    .flatMap(u -> Optional.ofNullable(u.getFamilleGereeId()))
                    .map(familleId -> soulRepository.findAllByFamilleId(familleId).stream().map(Soul::getId).toList())
                    .orElse(List.of());
        }
        return List.of();
    }

    /**
     * Ajoute une personne déjà inscrite au département. Crée le lien
     * soul_departments (traçabilité : qui, quand, depuis quel rôle),
     * consigne l'activité et notifie les responsables si l'action vient
     * d'un pasteur/administrateur.
     */
    public Map<String, Object> addMember(UUID departmentId, UUID memberId) {
        assertCanManage(departmentId);
        Soul soul = soulRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", memberId));
        if (soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, departmentId)) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    soul.getNomComplet() + " fait déjà partie de ce département", "MEMBER_ALREADY_IN_DEPARTMENT");
        }
        soulDepartmentRepository.save(SoulDepartment.builder()
                .soulId(memberId)
                .departmentId(departmentId)
                .actif(true)
                .createdBy(securityUtils.getCurrentUserId())
                .origine("MANUEL")
                .build());
        String role = securityUtils.getCurrentUserRole();
        record(departmentId, "MEMBER_ADDED", "SOUL", memberId,
                soul.getNomComplet() + " ajouté au département depuis le rôle " + role);
        notifyDepartmentResponsables(departmentId, soul, TypeNotification.MEMBRE_AJOUTE,
                "Nouveau membre dans votre département",
                soul.getNomComplet() + " a été ajouté au département.");
        notifyMemberAddedToUser(departmentId, soul, "ajouté");
        return memberSummary(soul, departmentId);
    }

    /**
     * Crée un nouveau membre (âme) et l'affecte au département en une seule
     * opération — le responsable n'a pas besoin d'un outil extérieur.
     */
    public Map<String, Object> createMember(UUID departmentId, DepartmentCreateMemberRequest request) {
        assertCanManage(departmentId);
        Soul soul = Soul.builder()
                .nom(request.nom().trim())
                .prenom(request.prenom())
                .email(request.email())
                .telephone(request.telephone())
                .adresse(request.adresse())
                .dateNaissance(request.dateNaissance())
                .profession(request.profession())
                .typeDisciple(request.typeDisciple() != null ? request.typeDisciple()
                        : com.discipolat.common.enums.TypeDisciple.NOUVEL_ARRIVANT)
                .dateIntegration(request.dateIntegration() != null ? request.dateIntegration() : LocalDate.now())
                .dateConversion(request.dateConversion())
                .statut(request.statut() != null ? request.statut()
                        : com.discipolat.common.enums.StatutAme.EN_INTEGRATION)
                .faiseurId(request.faiseurId() != null ? request.faiseurId() : securityUtils.getCurrentUserId())
                .familleId(request.familleId())
                .situationFamiliale(request.situationFamiliale())
                .etatSpirituel(request.etatSpirituel() != null ? request.etatSpirituel() : "NOUVEAU_CONVERTI")
                .niveauCroissance(request.niveauCroissance() != null ? request.niveauCroissance() : 1)
                .build();
        soul = soulRepository.save(soul);
        soulDepartmentRepository.save(SoulDepartment.builder()
                .soulId(soul.getId())
                .departmentId(departmentId)
                .actif(true)
                .createdBy(securityUtils.getCurrentUserId())
                .origine("MANUEL")
                .build());
        record(departmentId, "MEMBER_CREATED", "SOUL", soul.getId(),
                "Membre « " + soul.getNomComplet() + " » créé et ajouté au département");
        notifyDepartmentResponsables(departmentId, soul, TypeNotification.MEMBRE_AJOUTE,
                "Nouveau membre dans votre département",
                soul.getNomComplet() + " a été créé et ajouté au département.");
        notifyMemberAddedToUser(departmentId, soul, "ajouté");
        return memberSummary(soul, departmentId);
    }

    /**
     * Retire un membre du département : désactivation du lien soul_departments,
     * clôture des affectations actives et traçabilité.
     */
    public void removeMember(UUID departmentId, UUID memberId) {
        assertCanManage(departmentId);
        Soul soul = soulRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", memberId));
        for (SoulDepartment link : soulDepartmentRepository.findBySoulIdAndDepartmentId(memberId, departmentId)) {
            link.setActif(false);
            link.setDateDesaffectation(java.time.LocalDateTime.now());
            soulDepartmentRepository.save(link);
        }
        for (DepartmentAssignment assignment : assignmentRepository.findByDepartmentIdAndMemberIdAndActifTrue(departmentId, memberId)) {
            assignment.setActif(false);
            if (assignment.getDateFin() == null) assignment.setDateFin(LocalDate.now());
            assignmentRepository.save(assignment);
        }
        record(departmentId, "MEMBER_REMOVED", "SOUL", memberId,
                soul.getNomComplet() + " retiré du département");
        notifyDepartmentResponsables(departmentId, soul, TypeNotification.MEMBRE_RETIRE,
                "Membre retiré du département",
                soul.getNomComplet() + " a été retiré du département.");
    }

    private Map<String, Object> memberSummary(Soul soul, UUID departmentId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", soul.getId());
        m.put("nom", soul.getNom());
        m.put("prenom", soul.getPrenom());
        m.put("nomComplet", soul.getNomComplet());
        m.put("telephone", soul.getTelephone());
        m.put("email", soul.getEmail());
        m.put("profession", soul.getProfession());
        m.put("statut", soul.getStatut() != null ? soul.getStatut().name() : null);
        m.put("typeDisciple", soul.getTypeDisciple() != null ? soul.getTypeDisciple().name() : null);
        m.put("familleId", soul.getFamilleId());
        m.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
        return m;
    }

    // ========================================================================
    // NOTIFICATIONS
    // ========================================================================

    /** Notifie le titulaire du compte utilisateur lié à l'âme (si présent). */
    private void notifyMemberAddedToUser(UUID departmentId, Soul soul, String action) {
        if (soul.getUserId() == null) return;
        String deptNom = departmentService.findById(departmentId).getNom();
        notificationService.create(
                soul.getUserId(), TypeNotification.MEMBRE_AJOUTE, CanalNotification.IN_APP,
                "Affectation au département " + deptNom,
                "Vous avez été " + action + " au département " + deptNom + ".",
                departmentId, "DEPARTMENT");
    }

    /** Notifie les responsables du département (ajout/retrait par pasteur/administrateur). */
    private void notifyDepartmentResponsables(UUID departmentId, Soul soul, TypeNotification type,
                                              String titre, String message) {
        UUID responsableId = departmentService.findById(departmentId).getResponsableId();
        if (responsableId == null) return;
        UUID actorId = securityUtils.getCurrentUserId();
        if (responsableId.equals(actorId)) return;
        notificationService.create(
                responsableId, type, CanalNotification.IN_APP,
                titre,
                message + " Responsable : vous pouvez consulter le profil et compléter ses informations.",
                soul.getId(), "SOUL");
    }

    /** Notifie le titulaire du compte lié à l'âme assignée à une tâche. */
    private void notifyAssignee(DepartmentTask task) {
        if (task.getAssignedTo() == null) return;
        soulRepository.findById(task.getAssignedTo()).ifPresent(soul -> {
            if (soul.getUserId() == null) return;
            String deptNom = departmentService.findById(task.getDepartmentId()).getNom();
            notificationService.create(
                    soul.getUserId(), TypeNotification.TACHE_ASSIGNEE, CanalNotification.IN_APP,
                    "Nouvelle tâche — " + deptNom,
                    "Une tâche vous a été assignée : « " + task.getTitre() + " »"
                            + (task.getEcheance() != null ? " (échéance : " + task.getEcheance() + ")" : "") + ".",
                    task.getId(), "TASK");
        });
    }

    // ========================================================================
    // ACTIVITÉ / VUE D'ENSEMBLE
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivity(UUID departmentId) {
        assertCanManage(departmentId);
        return activityRepository.findTop50ByDepartmentIdOrderByCreatedAtDesc(departmentId).stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("action", a.getAction());
                    m.put("details", a.getDetails());
                    m.put("actorNom", a.getActorNom());
                    m.put("entityType", a.getEntityType());
                    m.put("entityId", a.getEntityId());
                    m.put("createdAt", a.getCreatedAt().toString());
                    return m;
                })
                .toList();
    }

    /** Vue d'ensemble pour l'écran de gestion : tout le nécessaire en un appel. */
    @Transactional(readOnly = true)
    public Map<String, Object> getManagementOverview(UUID departmentId) {
        assertCanManage(departmentId);
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("departmentId", departmentId);
        overview.put("teams", getTeams(departmentId));
        overview.put("positions", getPositions(departmentId));
        overview.put("assignments", getAssignments(departmentId));
        overview.put("taskStats", getTaskStats(departmentId));
        overview.put("activity", getActivity(departmentId));

        long actives = teamRepository.countByDepartmentIdAndStatut(departmentId, DepartmentTeam.TeamStatus.ACTIVE);
        long positions = positionRepository.countByDepartmentIdAndStatut(departmentId, DepartmentPosition.PositionStatus.ACTIVE);
        List<DepartmentAssignment> activeAssignments = assignmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        long affectations = activeAssignments.size();
        long membresAffectes = activeAssignments.stream()
                .map(DepartmentAssignment::getMemberId).distinct().count();

        Map<String, Object> org = new LinkedHashMap<>();
        org.put("equipesActives", actives);
        org.put("postesActifs", positions);
        org.put("affectationsActives", affectations);
        org.put("membresAffectes", membresAffectes);
        overview.put("org", org);
        return overview;
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    /**
     * Isolation RBAC : le membre à affecter doit appartenir au département.
     * Règle tolérante pour la constitution initiale : si le département ne
     * contient encore aucun membre rattaché, l'affectation reste possible
     * (organisation d'un département vide) ; sinon le membre doit figurer
     * dans soul_departments (lien âme ↔ département actif).
     */
    private void assertMemberBelongsToDepartment(UUID departmentId, UUID memberId, Soul soul) {
        if (securityUtils.isSuperUser()) return;
        if (soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, departmentId)) return;
        if (soulDepartmentRepository.countByDepartmentIdAndActifTrue(departmentId) == 0) return; // département vide : constitution libre
        throw new com.discipolat.common.domain.BusinessRuleException(
                soul.getNomComplet() + " ne fait pas partie de ce département — affectation refusée",
                "MEMBER_NOT_IN_DEPARTMENT");
    }

    private String soulName(UUID soulId) {
        if (soulId == null) return null;
        return soulRepository.findById(soulId).map(Soul::getNomComplet).orElse(null);
    }
}
