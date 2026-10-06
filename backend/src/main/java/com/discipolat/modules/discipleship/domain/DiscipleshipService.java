package com.discipolat.modules.discipleship.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;

/**
 * Discipleship — V233.
 *
 * <p>Implémente le contrat du mobile (mobile/lib/features/discipleship) :
 * parcours, étapes, progression, assignations mentor, réunions, rapports.
 *
 * <p><b>Isolation multi-tenant</b> : chaque repository porte un
 * {@code tenantId} explicite (le filtre Hibernate n'est actif qu'en contexte
 * HTTP). Aucun IDOR : tout objet chargé par id est revérifié contre le tenant.
 */
@Service
@Transactional
public class DiscipleshipService {

    private static final int MAX_PAGE_SIZE = 100;
    /** Plafond serveur du classement de mentors (§2.1 : `limit`). */
    private static final int MAX_TOP_MENTORS = 50;
    private static final int DEFAULT_TOP_MENTORS = 10;
    /**
     * Garde-fou d'agrégation : on ne charge jamais plus que ce nombre
     * d'assignations par mentor, pour que le classement reste une requête bornée
     * et non un chargement intégral de la table.
     */
    private static final int MAX_ASSIGNMENTS_PER_MENTOR = 200;

    private final DiscipleshipJourneyRepository journeyRepository;
    private final DiscipleshipStageRepository stageRepository;
    private final StageRequirementRepository requirementRepository;
    private final StageRewardRepository rewardRepository;
    private final DiscipleProgressRepository progressRepository;
    private final DiscipleProgressRequirementRepository progressRequirementRepository;
    private final MentorAssignmentRepository assignmentRepository;
    private final MentorMeetingRepository meetingRepository;
    /** Résolution des noms d'utilisateur attendus par les modèles mobiles. */
    private final com.discipolat.modules.users.domain.UserRepository userRepository;

    public DiscipleshipService(DiscipleshipJourneyRepository journeyRepository,
                               DiscipleshipStageRepository stageRepository,
                               StageRequirementRepository requirementRepository,
                               StageRewardRepository rewardRepository,
                               DiscipleProgressRepository progressRepository,
                               DiscipleProgressRequirementRepository progressRequirementRepository,
                               MentorAssignmentRepository assignmentRepository,
                               MentorMeetingRepository meetingRepository,
                               com.discipolat.modules.users.domain.UserRepository userRepository) {
        this.journeyRepository = journeyRepository;
        this.stageRepository = stageRepository;
        this.requirementRepository = requirementRepository;
        this.rewardRepository = rewardRepository;
        this.progressRepository = progressRepository;
        this.progressRequirementRepository = progressRequirementRepository;
        this.assignmentRepository = assignmentRepository;
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
    }

    /** Nom lisible d'un utilisateur ; repli stable si absent. */
    private String userName(UUID userId) {
        if (userId == null) return "—";
        return userRepository.findById(userId)
                .map(u -> {
                    String full = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                            + (u.getLastName() == null ? "" : u.getLastName())).trim();
                    return full.isEmpty() ? (u.getEmail() != null ? u.getEmail() : "—") : full;
                })
                .orElse("—");
    }

    private Pageable clamp(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(p, s);
    }

    // ==================== JOURNEYS ====================

    /**
     * Le filtre {@code isActive} est honoré. Les deux branches du ternaire
     * précédent appelaient la MÊME requête « isActive = true » : demander
     * {@code isActive=false} renvoyait malgré tout les parcours actifs, ce qui
     * rendait le paramètre muet (et les parcours archivés invisibles).
     */
    public List<Map<String, Object>> listJourneys(UUID tenantId, Boolean isActive) {
        List<DiscipleshipJourney> journeys;
        if (isActive == null) {
            journeys = journeyRepository.findByTenantIdOrderByNameAsc(tenantId);
        } else if (isActive) {
            journeys = journeyRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
        } else {
            journeys = journeyRepository.findByTenantIdAndIsActiveFalseOrderByNameAsc(tenantId);
        }
        return journeys.stream().map(this::journeyView).toList();
    }

    public Map<String, Object> getJourney(UUID tenantId, Long id) {
        return journeyView(requireJourney(tenantId, id));
    }

    public Map<String, Object> createJourney(UUID tenantId, Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name est requis");
        DiscipleshipJourney journey = DiscipleshipJourney.builder()
                .tenantId(tenantId)
                .name(name.trim())
                .description((String) body.get("description"))
                .type(optionalEnum(DiscipleshipJourney.JourneyType.class, body.get("type"), DiscipleshipJourney.JourneyType.CUSTOM))
                .totalStages(intVal(body.get("totalStages"), 0))
                .startDate(body.get("startDate") != null ? Instant.parse((String) body.get("startDate")) : Instant.now())
                .endDate(body.get("endDate") != null ? Instant.parse((String) body.get("endDate")) : null)
                .createdBy(SecurityUtils.getCurrentUserId())
                .isActive(true)
                .build();
        return journeyView(journeyRepository.save(journey));
    }

    private DiscipleshipJourney requireJourney(UUID tenantId, Long id) {
        return journeyRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("DiscipleshipJourney", "id", String.valueOf(id)));
    }

    private Map<String, Object> journeyView(DiscipleshipJourney j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("name", j.getName());
        m.put("description", j.getDescription());
        m.put("type", j.getType().name());
        m.put("totalStages", j.getTotalStages());
        m.put("startDate", j.getStartDate().toString());
        m.put("endDate", j.getEndDate() != null ? j.getEndDate().toString() : null);
        m.put("createdById", j.getCreatedBy());
        m.put("isActive", j.isActive());
        m.put("createdAt", j.getCreatedAt().toString());
        m.put("updatedAt", j.getUpdatedAt() != null ? j.getUpdatedAt().toString() : null);
        return m;
    }

    // ==================== STAGES ====================

    public List<Map<String, Object>> listStages(UUID tenantId, Long journeyId) {
        requireJourney(tenantId, journeyId);
        return stageRepository.findByTenantIdAndJourneyIdOrderByOrderAsc(tenantId, journeyId)
                .stream().map(s -> stageView(tenantId, s)).toList();
    }

    public Map<String, Object> getStage(UUID tenantId, Long id) {
        return stageView(tenantId, requireStage(tenantId, id));
    }

    public Map<String, Object> createStage(UUID tenantId, Map<String, Object> body) {
        String name = requireText(body, "name");
        Long journeyId = longVal(body.get("journeyId"));
        // Le parcours parent est validé dans le tenant : une étape ne peut pas
        // être rattachée à un parcours inexistant ou d'un autre tenant.
        requireJourney(tenantId, journeyId);
        DiscipleshipStage stage = DiscipleshipStage.builder()
                .tenantId(tenantId)
                .journeyId(journeyId)
                .order(intVal(body.get("order"), 0))
                .name(name.trim())
                .description((String) body.get("description"))
                .color((String) body.get("color"))
                .icon((String) body.get("icon"))
                .durationDays((Integer) body.get("durationDays"))
                .isOptional(boolVal(body.get("isOptional"), false))
                .build();
        return stageView(tenantId, stageRepository.save(stage));
    }

    private DiscipleshipStage requireStage(UUID tenantId, Long id) {
        return stageRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("DiscipleshipStage", "id", String.valueOf(id)));
    }

    private Map<String, Object> stageView(UUID tenantId, DiscipleshipStage s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("journeyId", s.getJourneyId());
        m.put("order", s.getOrder());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("color", s.getColor());
        m.put("icon", s.getIcon());
        m.put("durationDays", s.getDurationDays());
        m.put("isOptional", s.isOptional());
        m.put("createdAt", s.getCreatedAt().toString());
        m.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        List<Map<String, Object>> reqs = requirementRepository.findByTenantIdAndStageIdOrderByOrderAsc(tenantId, s.getId())
                .stream().map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("stageId", r.getStageId());
                    rm.put("type", r.getType().name());
                    rm.put("description", r.getDescription());
                    rm.put("referenceId", r.getReferenceId());
                    rm.put("referenceName", r.getReferenceName());
                    rm.put("isRequired", r.isRequired());
                    rm.put("order", r.getOrder());
                    return rm;
                }).toList();
        m.put("requirements", reqs);
        List<Map<String, Object>> rewards = rewardRepository.findByTenantIdAndStageId(tenantId, s.getId())
                .stream().map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("stageId", r.getStageId());
                    rm.put("type", r.getType().name());
                    rm.put("name", r.getName());
                    rm.put("description", r.getDescription());
                    rm.put("icon", r.getIcon());
                    rm.put("imageUrl", r.getImageUrl());
                    rm.put("points", r.getPoints());
                    rm.put("badgeId", r.getBadgeId());
                    rm.put("badgeName", r.getBadgeName());
                    return rm;
                }).toList();
        m.put("rewards", rewards);
        return m;
    }

    // ==================== PROGRESS ====================

    /**
     * Les filtres journeyId / discipleId / status sont COMBINÉS en base.
     *
     * <p>La version précédente testait les filtres en cascade (si journeyId,
     * puis discipleId, puis status) : les combinaisons tombaient dans une page
     * unique filtrée en mémoire — donc pagination fausse (une page de 20 pouvait
     * n'en rendre que 2) — et {@code status} était purement et simplement
     * IGNORÉ dès qu'un autre filtre était présent. Le repository porte maintenant
     * une requête dérivée par combinaison.
     */
    public List<Map<String, Object>> listProgress(UUID tenantId, int page, int size, Long journeyId, UUID discipleId, String status) {
        Pageable p = clamp(page, size);
        DiscipleProgress.ProgressStatus parsedStatus =
                status == null ? null : requireEnum(DiscipleProgress.ProgressStatus.class, status);
        Page<DiscipleProgress> result = progressRepository.findAll(
                tenantId, journeyId, discipleId, parsedStatus, p);
        return result.getContent().stream().map(pr -> progressView(tenantId, pr)).toList();
    }

    public Map<String, Object> getProgress(UUID tenantId, Long id) {
        return progressView(tenantId, requireProgress(tenantId, id));
    }

    public Map<String, Object> createProgress(UUID tenantId, Map<String, Object> body) {
        UUID discipleId = uuidVal(requireText(body, "discipleId"));
        Long journeyId = longVal(body.get("journeyId"));
        requireJourney(tenantId, journeyId);
        // R4 : le disciple doit exister ET être membre du tenant. Sans cette
        // revérification, un appel pouvait rattacher une progression à un
        // utilisateur d'un autre tenant (la FK ne garantit que l'existence).
        requireTenantUser(tenantId, discipleId);
        // Un disciple ne peut avoir qu'une progression par parcours : sinon la
        // liste en renvoie plusieurs lignes concurrentes pour la même paire.
        progressRepository.findByTenantIdAndDiscipleIdAndJourneyId(tenantId, discipleId, journeyId)
                .ifPresent(existing -> {
                    throw new com.discipolat.common.domain.BusinessRuleException(
                            "Ce disciple a déjà une progression sur ce parcours", "DISCIPLESHIP_PROGRESS_EXISTS");
                });
        Long currentStageId = body.get("currentStageId") != null ? longVal(body.get("currentStageId")) : null;
        if (currentStageId != null) requireStage(tenantId, currentStageId);
        DiscipleProgress progress = DiscipleProgress.builder()
                .tenantId(tenantId)
                .discipleId(discipleId)
                .journeyId(journeyId)
                .currentStageId(currentStageId)
                .completedStages(intVal(body.get("completedStages"), 0))
                .totalStages(intVal(body.get("totalStages"), 0))
                .completedRequirements(intVal(body.get("completedRequirements"), 0))
                .totalRequirements(intVal(body.get("totalRequirements"), 0))
                .status(DiscipleProgress.ProgressStatus.NOT_STARTED)
                .build();
        return progressView(tenantId, progressRepository.save(progress));
    }

    /**
     * Vérifie qu'un utilisateur appartient bien au tenant (R4).
     *
     * <p>Une clé étrangère ne garantit que l'EXISTENCE de la ligne : sans ce
     * contrôle, un identifiant devinable ou simplement connu d'un autre tenant
     * permettait de l'attacher à ses données.
     */
    private void requireTenantUser(UUID tenantId, UUID userId) {
        if (userRepository.findByIdWithActiveMembershipInTenant(userId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("User", "id", String.valueOf(userId));
        }
    }

    /** Parse un UUID en rejetant proprement une valeur absente ou invalide. */
    private static UUID uuidVal(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Identifiant invalide : \"" + value + "\"");
        }
    }

    public Map<String, Object> updateRequirementProgress(UUID tenantId, Long progressId, Long requirementId, Map<String, Object> body) {
        DiscipleProgress progress = requireProgress(tenantId, progressId);
        DiscipleProgressRequirement pr = progressRequirementRepository
                .findByTenantIdAndProgressIdAndRequirementId(tenantId, progressId, requirementId)
                .orElseGet(() -> DiscipleProgressRequirement.builder()
                        .tenantId(tenantId)
                        .progressId(progressId)
                        .requirementId(requirementId)
                        .build());
        String status = (String) body.get("status");
        if (status != null) pr.setStatus(requireEnum(DiscipleProgressRequirement.RequirementStatus.class, status));
        pr.setEvidence((String) body.get("evidence"));
        pr.setNotes((String) body.get("notes"));
        if (body.get("verifiedById") != null) pr.setVerifiedBy(UUID.fromString(String.valueOf(body.get("verifiedById"))));
        if (pr.getStatus() == DiscipleProgressRequirement.RequirementStatus.COMPLETED
                || pr.getStatus() == DiscipleProgressRequirement.RequirementStatus.VERIFIED) {
            pr.setCompletedAt(Instant.now());
        }
        progressRequirementRepository.save(pr);
        return progressView(tenantId, progress);
    }

    public Map<String, Object> completeStage(UUID tenantId, Long progressId, Long stageId) {
        DiscipleProgress progress = requireProgress(tenantId, progressId);
        requireStage(tenantId, stageId);
        progress.setCompletedStages(progress.getCompletedStages() + 1);
        progress.setLastActivityAt(Instant.now());
        if (progress.getTotalStages() > 0 && progress.getCompletedStages() >= progress.getTotalStages()) {
            progress.setStatus(DiscipleProgress.ProgressStatus.COMPLETED);
            progress.setCompletedAt(Instant.now());
        } else {
            progress.setStatus(DiscipleProgress.ProgressStatus.IN_PROGRESS);
        }
        progressRepository.save(progress);
        return progressView(tenantId, progress);
    }

    private DiscipleProgress requireProgress(UUID tenantId, Long id) {
        return progressRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("DiscipleProgress", "id", String.valueOf(id)));
    }

    /**
     * Vue d'une progression, alignée sur le modèle mobile {@code DiscipleProgress}.
     *
     * <p>Deux écarts de la version précédente sont corrigés :
     * <ul>
     *   <li>les champs libellés {@code discipleName}, {@code journeyName},
     *       {@code currentStageName} et {@code currentStageOrder} étaient
     *       ABSENTS alors que le modèle mobile les déclare {@code required} :
     *       le {@code fromJson} du client échouait sur chaque lecture ;</li>
     *   <li>{@code requirementProgress} était renvoyé comme une LISTE alors que
     *       le modèle mobile attend une MAP indexée par {@code requirementId}
     *       ({@code Map<int, RequirementProgress>}). La désérialisation Dart
     *       convertit la clé en {@code int} : un tableau y levait une
     *       exception de type.</li>
     * </ul>
     */
    private Map<String, Object> progressView(UUID tenantId, DiscipleProgress pr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pr.getId());
        m.put("discipleId", pr.getDiscipleId());
        m.put("discipleName", userName(pr.getDiscipleId()));
        m.put("journeyId", pr.getJourneyId());
        m.put("journeyName", journeyRepository.findByTenantIdAndId(tenantId, pr.getJourneyId())
                .map(DiscipleshipJourney::getName).orElse("—"));
        m.put("currentStageId", pr.getCurrentStageId() != null ? pr.getCurrentStageId() : 0);
        DiscipleshipStage currentStage = pr.getCurrentStageId() == null ? null
                : stageRepository.findByTenantIdAndId(tenantId, pr.getCurrentStageId()).orElse(null);
        m.put("currentStageName", currentStage != null ? currentStage.getName() : "");
        m.put("currentStageOrder", currentStage != null ? currentStage.getOrder() : 0);
        m.put("completedStages", pr.getCompletedStages());
        m.put("totalStages", pr.getTotalStages());
        m.put("completedRequirements", pr.getCompletedRequirements());
        m.put("totalRequirements", pr.getTotalRequirements());
        m.put("startedAt", pr.getStartedAt() != null ? pr.getStartedAt().toString() : null);
        m.put("lastActivityAt", pr.getLastActivityAt() != null ? pr.getLastActivityAt().toString() : null);
        m.put("completedAt", pr.getCompletedAt() != null ? pr.getCompletedAt().toString() : null);
        m.put("status", pr.getStatus().name());
        m.put("nextMilestoneDate", pr.getNextMilestoneDate() != null ? pr.getNextMilestoneDate().toString() : null);
        m.put("nextMilestoneName", pr.getNextMilestoneName());
        m.put("createdAt", pr.getCreatedAt().toString());
        m.put("updatedAt", pr.getUpdatedAt() != null ? pr.getUpdatedAt().toString() : null);
        m.put("requirementProgress", requirementProgressMap(tenantId, pr));
        return m;
    }

    /**
     * Avancement par exigence, sous forme de MAP indexée par
     * {@code requirementId} — forme attendue par le modèle mobile.
     */
    private Map<String, Map<String, Object>> requirementProgressMap(UUID tenantId, DiscipleProgress pr) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (DiscipleProgressRequirement r
                : progressRequirementRepository.findByTenantIdAndProgressId(tenantId, pr.getId())) {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("requirementId", r.getRequirementId());
            rm.put("requirementName", requirementName(tenantId, r.getRequirementId()));
            rm.put("status", r.getStatus().name());
            rm.put("completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : null);
            rm.put("evidence", r.getEvidence());
            rm.put("notes", r.getNotes());
            rm.put("verifiedById", r.getVerifiedBy());
            rm.put("verifiedByName", r.getVerifiedBy() == null ? null : userName(r.getVerifiedBy()));
            out.put(String.valueOf(r.getRequirementId()), rm);
        }
        return out;
    }

    /** Libellé d'une exigence de parcours (repli stable si introuvable). */
    private String requirementName(UUID tenantId, Long requirementId) {
        return requirementRepository.findByTenantIdAndId(tenantId, requirementId)
                .map(r -> r.getDescription() != null && !r.getDescription().isBlank()
                        ? r.getDescription()
                        : (r.getReferenceName() != null ? r.getReferenceName() : r.getType().name()))
                .orElse("Exigence " + requirementId);
    }

    // ==================== ASSIGNMENTS ====================

    public List<Map<String, Object>> listAssignments(UUID tenantId, int page, int size, UUID mentorId, UUID discipleId, Long journeyId, String status) {
        Pageable p = clamp(page, size);
        Page<MentorAssignment> result;
        if (mentorId != null) {
            result = assignmentRepository.findByTenantIdAndMentorId(tenantId, mentorId, p);
        } else if (discipleId != null) {
            result = assignmentRepository.findByTenantIdAndDiscipleId(tenantId, discipleId, p);
        } else if (journeyId != null) {
            result = assignmentRepository.findByTenantIdAndJourneyId(tenantId, journeyId, p);
        } else if (status != null) {
            result = assignmentRepository.findByTenantIdAndStatus(tenantId, requireEnum(MentorAssignment.AssignmentStatus.class, status), p);
        } else {
            result = assignmentRepository.findByTenantId(tenantId, p);
        }
        return result.getContent().stream().map(this::assignmentView).toList();
    }

    public Map<String, Object> createAssignment(UUID tenantId, Map<String, Object> body) {
        UUID mentorId = uuidVal(requireText(body, "mentorId"));
        UUID discipleId = uuidVal(requireText(body, "discipleId"));
        Long journeyId = longVal(body.get("journeyId"));
        // R4 : mentor, disciple et parcours sont revérifiés dans le tenant.
        requireTenantUser(tenantId, mentorId);
        requireTenantUser(tenantId, discipleId);
        requireJourney(tenantId, journeyId);
        MentorAssignment a = MentorAssignment.builder()
                .tenantId(tenantId)
                .mentorId(mentorId)
                .discipleId(discipleId)
                .journeyId(journeyId)
                .status(MentorAssignment.AssignmentStatus.ACTIVE)
                .meetingFrequencyDays(intVal(body.get("meetingFrequencyDays"), 7))
                .notes((String) body.get("notes"))
                .build();
        return assignmentView(assignmentRepository.save(a));
    }

    public Map<String, Object> endAssignment(UUID tenantId, Long id) {
        MentorAssignment a = requireAssignment(tenantId, id);
        a.setStatus(MentorAssignment.AssignmentStatus.ENDED);
        a.setEndedAt(Instant.now());
        return assignmentView(assignmentRepository.save(a));
    }

    private MentorAssignment requireAssignment(UUID tenantId, Long id) {
        return assignmentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("MentorAssignment", "id", String.valueOf(id)));
    }

    /** `mentorName`/`discipleName` sont `required` côté mobile (R7). */
    private Map<String, Object> assignmentView(MentorAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("mentorId", a.getMentorId());
        m.put("mentorName", userName(a.getMentorId()));
        m.put("discipleId", a.getDiscipleId());
        m.put("discipleName", userName(a.getDiscipleId()));
        m.put("journeyId", a.getJourneyId());
        m.put("assignedAt", a.getAssignedAt().toString());
        m.put("endedAt", a.getEndedAt() != null ? a.getEndedAt().toString() : null);
        m.put("status", a.getStatus().name());
        m.put("notes", a.getNotes());
        m.put("meetingFrequencyDays", a.getMeetingFrequencyDays());
        m.put("lastMeetingAt", a.getLastMeetingAt() != null ? a.getLastMeetingAt().toString() : null);
        m.put("nextMeetingAt", a.getNextMeetingAt() != null ? a.getNextMeetingAt().toString() : null);
        return m;
    }

    // ==================== MEETINGS ====================

    public List<Map<String, Object>> listMeetings(UUID tenantId, int page, int size, Long assignmentId, UUID mentorId, String status) {
        Pageable p = clamp(page, size);
        Page<MentorMeeting> result;
        if (assignmentId != null) {
            result = meetingRepository.findByTenantIdAndAssignmentId(tenantId, assignmentId, p);
        } else if (mentorId != null) {
            result = meetingRepository.findByTenantIdAndMentorId(tenantId, mentorId, p);
        } else if (status != null) {
            result = meetingRepository.findByTenantIdAndStatus(tenantId, requireEnum(MentorMeeting.MeetingStatus.class, status), p);
        } else {
            result = meetingRepository.findByTenantId(tenantId, p);
        }
        return result.getContent().stream().map(this::meetingView).toList();
    }

    public Map<String, Object> scheduleMeeting(UUID tenantId, Map<String, Object> body) {
        // L'assignation est revérifiée dans le tenant (R4) : sans cela, on
        // pouvait créer une réunion rattachée à l'assignation d'un autre tenant.
        MentorAssignment assignment = requireAssignment(tenantId, longVal(body.get("assignmentId")));
        UUID mentorId = body.get("mentorId") != null
                ? uuidVal(requireText(body, "mentorId")) : assignment.getMentorId();
        UUID discipleId = body.get("discipleId") != null
                ? uuidVal(requireText(body, "discipleId")) : assignment.getDiscipleId();
        requireTenantUser(tenantId, mentorId);
        requireTenantUser(tenantId, discipleId);
        MentorMeeting mt = MentorMeeting.builder()
                .tenantId(tenantId)
                .assignmentId(assignment.getId())
                .mentorId(mentorId)
                .discipleId(discipleId)
                .scheduledAt(body.get("scheduledAt") != null ? Instant.parse((String) body.get("scheduledAt")) : Instant.now())
                .location((String) body.get("location"))
                .status(MentorMeeting.MeetingStatus.SCHEDULED)
                .isGroup(boolVal(body.get("isGroup"), false))
                .build();
        return meetingView(meetingRepository.save(mt));
    }

    public Map<String, Object> completeMeeting(UUID tenantId, Long id, Map<String, Object> body) {
        MentorMeeting mt = requireMeeting(tenantId, id);
        mt.setStatus(MentorMeeting.MeetingStatus.COMPLETED);
        mt.setActualAt(Instant.now());
        if (body.get("notes") != null) mt.setNotes((String) body.get("notes"));
        if (body.get("actionItems") != null) mt.setActionItems((String) body.get("actionItems"));
        if (body.get("nextSteps") != null) mt.setNextSteps((String) body.get("nextSteps"));
        if (body.get("durationMinutes") != null) mt.setDurationMinutes(intVal(body.get("durationMinutes"), null));
        return meetingView(meetingRepository.save(mt));
    }

    private MentorMeeting requireMeeting(UUID tenantId, Long id) {
        return meetingRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("MentorMeeting", "id", String.valueOf(id)));
    }

    private Map<String, Object> meetingView(MentorMeeting mt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mt.getId());
        m.put("assignmentId", mt.getAssignmentId());
        m.put("mentorId", mt.getMentorId());
        m.put("mentorName", userName(mt.getMentorId()));
        m.put("discipleId", mt.getDiscipleId());
        m.put("discipleName", userName(mt.getDiscipleId()));
        m.put("scheduledAt", mt.getScheduledAt().toString());
        m.put("actualAt", mt.getActualAt() != null ? mt.getActualAt().toString() : null);
        m.put("status", mt.getStatus().name());
        m.put("notes", mt.getNotes());
        m.put("actionItems", mt.getActionItems());
        m.put("nextSteps", mt.getNextSteps());
        m.put("durationMinutes", mt.getDurationMinutes());
        m.put("location", mt.getLocation());
        m.put("isGroup", mt.isGroup());
        m.put("createdAt", mt.getCreatedAt().toString());
        m.put("updatedAt", mt.getUpdatedAt() != null ? mt.getUpdatedAt().toString() : null);
        return m;
    }

    // ==================== REPORTS ====================

    /**
     * Rapport d'un parcours.
     *
     * <p>Le payload couvre INTÉGRALEMENT le modèle mobile
     * {@code DiscipleshipReport} (R7) : sans {@code journeyName},
     * {@code averageCompletion}, {@code completedMeetings},
     * {@code stageDistribution}, {@code statusDistribution} et
     * {@code topMentors}, le {@code fromJson} du client lèverait une exception
     * sur des champs {@code required}.
     */
    public Map<String, Object> getReport(UUID tenantId, Long journeyId) {
        DiscipleshipJourney journey = requireJourney(tenantId, journeyId);
        long total = progressRepository.countByTenantIdAndJourneyId(tenantId, journeyId);
        long active = progressRepository.countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, DiscipleProgress.ProgressStatus.IN_PROGRESS);
        long completed = progressRepository.countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, DiscipleProgress.ProgressStatus.COMPLETED);
        long stalled = progressRepository.countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, DiscipleProgress.ProgressStatus.STALLED);
        long totalMeetings = meetingRepository.countByTenantIdAndJourneyId(tenantId, journeyId);
        long completedMeetings = meetingRepository.countByTenantIdAndJourneyIdAndStatus(
                tenantId, journeyId, MentorMeeting.MeetingStatus.COMPLETED);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("journeyId", journeyId);
        m.put("journeyName", journey.getName());
        m.put("totalDisciples", total);
        m.put("activeDisciples", active);
        m.put("completedDisciples", completed);
        m.put("stalledDisciples", stalled);
        // Pourcentage d'avancement MOYEN, sur la même formule que le modèle
        // mobile `completionPercentage` : 0 quand le total est inconnu, jamais
        // de division par zéro.
        m.put("averageCompletion", averageCompletionPercentage(tenantId, journeyId));
        m.put("totalMeetings", totalMeetings);
        m.put("completedMeetings", completedMeetings);
        m.put("stageDistribution", stageDistribution(tenantId, journeyId));
        m.put("statusDistribution", statusDistribution(tenantId, journeyId, journey));
        m.put("topMentors", getTopMentors(tenantId, journeyId, DEFAULT_TOP_MENTORS));
        m.put("generatedAt", Instant.now().toString());
        return m;
    }

    /** Avancement moyen en % (0 si aucun disciple ou totals nuls). */
    private double averageCompletionPercentage(UUID tenantId, Long journeyId) {
        double sum = 0d;
        int counted = 0;
        for (DiscipleProgress p : progressRepository
                .findByTenantIdAndJourneyId(tenantId, journeyId, Pageable.unpaged()).getContent()) {
            int total = p.getTotalStages() > 0 ? p.getTotalStages() : p.getCompletedStages();
            if (total > 0) {
                sum += (p.getCompletedStages() * 100d) / total;
                counted++;
            }
        }
        return counted == 0 ? 0d : Math.round((sum / counted) * 100d) / 100d;
    }

    /** Nb de disciples par étape atteinte ; les clés sont les libellés d'étape. */
    private Map<String, Integer> stageDistribution(UUID tenantId, Long journeyId) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (DiscipleshipStage stage : stageRepository
                .findByTenantIdAndJourneyIdOrderByOrderAsc(tenantId, journeyId)) {
            dist.put(String.valueOf(stage.getOrder()) + " - " + stage.getName(), 0);
        }
        for (Object[] row : progressRepository.countByCurrentStage(tenantId, journeyId)) {
            Long stageId = row[0] == null ? null : (Long) row[0];
            int count = ((Number) row[1]).intValue();
            String key = stageId == null
                    ? "Non démarré"
                    : stageRepository.findByTenantIdAndId(tenantId, stageId)
                        .map(s -> String.valueOf(s.getOrder()) + " - " + s.getName())
                        .orElse("Étape " + stageId);
            dist.merge(key, count, Integer::sum);
        }
        return dist;
    }

    /** Nb de disciples par statut — couvre TOUS les statuts, même à zéro. */
    private Map<String, Integer> statusDistribution(UUID tenantId, Long journeyId, DiscipleshipJourney journey) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (DiscipleProgress.ProgressStatus s : DiscipleProgress.ProgressStatus.values()) {
            dist.put(s.name(), 0);
        }
        for (DiscipleProgress.ProgressStatus s : DiscipleProgress.ProgressStatus.values()) {
            dist.put(s.name(), (int) progressRepository
                    .countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, s));
        }
        return dist;
    }

    /**
     * Classement des mentors d'un parcours, agrégé par mentor.
     *
     * <p>La version précédente listait les ASSIGNATIONS et renvoyait des
     * compteurs en dur ({@code discipleCount: 1}, {@code meetingCount: 0}) :
     * un mentor suivi de 5 disciples apparaissait cinq fois, toujours classé 1/0.
     * On agrège réellement, et on joint le nom du mentor attendu par le modèle
     * mobile {@code TopMentor}.
     */
    public List<Map<String, Object>> getTopMentors(UUID tenantId, Long journeyId, int limit) {
        requireJourney(tenantId, journeyId);
        int max = Math.min(Math.max(limit, 1), MAX_TOP_MENTORS);
        Page<MentorAssignment> page = assignmentRepository.findByTenantIdAndJourneyId(
                tenantId, journeyId, PageRequest.of(0, max * MAX_ASSIGNMENTS_PER_MENTOR, Sort.by("assignedAt")));

        Map<UUID, int[]> counts = new LinkedHashMap<>();
        for (MentorAssignment a : page.getContent()) {
            counts.computeIfAbsent(a.getMentorId(), k -> new int[2]);
            if (a.getStatus() == MentorAssignment.AssignmentStatus.ENDED) continue;
            counts.get(a.getMentorId())[0]++;
            counts.get(a.getMentorId())[1] += meetingCountForAssignment(tenantId, a.getId());
        }

        return counts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("mentorId", e.getKey());
                    m.put("mentorName", mentorName(e.getKey()));
                    m.put("discipleCount", e.getValue()[0]);
                    m.put("meetingCount", e.getValue()[1]);
                    return m;
                })
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(x -> (Integer) x.get("meetingCount")).reversed()
                        .thenComparing(x -> (Integer) x.get("discipleCount")).reversed())
                .limit(max)
                .toList();
    }

    private int meetingCountForAssignment(UUID tenantId, Long assignmentId) {
        long count = meetingRepository.countByTenantIdAndAssignmentId(tenantId, assignmentId);
        return (int) Math.min(count, Integer.MAX_VALUE);
    }

    /** Nom du mentor, ou un repli stable si l'utilisateur est absent/archivé. */
    private String mentorName(UUID mentorId) {
        return userName(mentorId);
    }

    // ==================== helpers ====================

    /**
     * Parse un enum en REJETANT les valeurs inconnues (400 via
     * GlobalExceptionHandler), au lieu de retomber silencieusement sur une
     * valeur par défaut.
     *
     * <p>Le repli silencieux était un défaut de fond : un filtre
     * {@code ?status=TYPO} renvoyait 200 avec le jeu de données NON filtré,
     * ce que l'appelant ne peut pas distinguer d'un résultat correct.
     */
    /**
     * Parse un enum fourni à la CRÉATION, en tolérant l'absence de la valeur
     * (repli sur {@code def}) mais en rejetant une valeur INCONNUE.
     *
     * <p>Point d'équilibre volontaire : une création sans {@code type} reste
     * valide (comportement d'origine), tandis qu'un {@code type: "FOO"} échoue
     * en 400 au lieu d'être silencieusement enregistré en CUSTOM.
     */
    private static <T extends Enum<T>> T optionalEnum(Class<T> cls, Object raw, T def) {
        if (raw == null || String.valueOf(raw).isBlank()) return def;
        return requireEnum(cls, String.valueOf(raw));
    }

    private static <T extends Enum<T>> T requireEnum(Class<T> cls, String value) {
        try {
            return Enum.valueOf(cls, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valeur invalide pour "
                    + cls.getSimpleName() + " : \"" + value + "\". Attendu : "
                    + Arrays.stream(cls.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", ")));
        }
    }

    private static long longVal(Object o) { return o == null ? 0 : Long.parseLong(String.valueOf(o)); }
    private static int intVal(Object o, int def) { return o == null ? def : Integer.parseInt(String.valueOf(o)); }
    private static int intVal(Object o, Integer def) { return o == null || o == "" ? def : Integer.parseInt(String.valueOf(o)); }
    private static boolean boolVal(Object o, boolean def) { return o == null ? def : Boolean.parseBoolean(String.valueOf(o)); }

    private static String requireText(Map<String, Object> body, String field) {
        Object raw = body.get(field);
        String value = raw == null ? null : String.valueOf(raw).trim();
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(field + " est requis");
        }
        return value;
    }
}
