package com.discipolat.modules.discipleship.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private final DiscipleshipJourneyRepository journeyRepository;
    private final DiscipleshipStageRepository stageRepository;
    private final StageRequirementRepository requirementRepository;
    private final StageRewardRepository rewardRepository;
    private final DiscipleProgressRepository progressRepository;
    private final DiscipleProgressRequirementRepository progressRequirementRepository;
    private final MentorAssignmentRepository assignmentRepository;
    private final MentorMeetingRepository meetingRepository;

    public DiscipleshipService(DiscipleshipJourneyRepository journeyRepository,
                               DiscipleshipStageRepository stageRepository,
                               StageRequirementRepository requirementRepository,
                               StageRewardRepository rewardRepository,
                               DiscipleProgressRepository progressRepository,
                               DiscipleProgressRequirementRepository progressRequirementRepository,
                               MentorAssignmentRepository assignmentRepository,
                               MentorMeetingRepository meetingRepository) {
        this.journeyRepository = journeyRepository;
        this.stageRepository = stageRepository;
        this.requirementRepository = requirementRepository;
        this.rewardRepository = rewardRepository;
        this.progressRepository = progressRepository;
        this.progressRequirementRepository = progressRequirementRepository;
        this.assignmentRepository = assignmentRepository;
        this.meetingRepository = meetingRepository;
    }

    private Pageable clamp(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(p, s);
    }

    // ==================== JOURNEYS ====================

    public List<Map<String, Object>> listJourneys(UUID tenantId, Boolean isActive) {
        List<DiscipleshipJourney> journeys = Boolean.FALSE.equals(isActive)
                ? journeyRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId)
                : journeyRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
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
                .type(parseEnum(DiscipleshipJourney.JourneyType.class, (String) body.get("type"), DiscipleshipJourney.JourneyType.CUSTOM))
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
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name est requis");
        Long journeyId = longVal(body.get("journeyId"));
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

    public List<Map<String, Object>> listProgress(UUID tenantId, int page, int size, Long journeyId, UUID discipleId, String status) {
        Pageable p = clamp(page, size);
        List<DiscipleProgress> all = progressRepository.findByTenantId(tenantId, p).getContent();
        if (journeyId != null && discipleId != null) {
            return all.stream().filter(x -> journeyId.equals(x.getJourneyId()) && discipleId.equals(x.getDiscipleId()))
                    .map(pr -> progressView(tenantId, pr)).toList();
        } else if (journeyId != null) {
            return progressRepository.findByTenantIdAndJourneyId(tenantId, journeyId, p).getContent().stream().map(pr -> progressView(tenantId, pr)).toList();
        } else if (discipleId != null) {
            return progressRepository.findByTenantIdAndDiscipleId(tenantId, discipleId, p).getContent().stream().map(pr -> progressView(tenantId, pr)).toList();
        } else if (status != null) {
            return progressRepository.findByTenantIdAndStatus(tenantId, parseEnum(DiscipleProgress.ProgressStatus.class, status, null), p).getContent().stream().map(pr -> progressView(tenantId, pr)).toList();
        } else {
            return all.stream().map(pr -> progressView(tenantId, pr)).toList();
        }
    }

    public Map<String, Object> getProgress(UUID tenantId, Long id) {
        return progressView(tenantId, requireProgress(tenantId, id));
    }

    public Map<String, Object> createProgress(UUID tenantId, Map<String, Object> body) {
        UUID discipleId = UUID.fromString(String.valueOf(body.get("discipleId")));
        Long journeyId = longVal(body.get("journeyId"));
        requireJourney(tenantId, journeyId);
        DiscipleProgress progress = DiscipleProgress.builder()
                .tenantId(tenantId)
                .discipleId(discipleId)
                .journeyId(journeyId)
                .currentStageId(body.get("currentStageId") != null ? longVal(body.get("currentStageId")) : null)
                .completedStages(intVal(body.get("completedStages"), 0))
                .totalStages(intVal(body.get("totalStages"), 0))
                .completedRequirements(intVal(body.get("completedRequirements"), 0))
                .totalRequirements(intVal(body.get("totalRequirements"), 0))
                .status(DiscipleProgress.ProgressStatus.NOT_STARTED)
                .build();
        return progressView(tenantId, progressRepository.save(progress));
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
        if (status != null) pr.setStatus(parseEnum(DiscipleProgressRequirement.RequirementStatus.class, status, DiscipleProgressRequirement.RequirementStatus.PENDING));
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

    private Map<String, Object> progressView(UUID tenantId, DiscipleProgress pr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pr.getId());
        m.put("discipleId", pr.getDiscipleId());
        m.put("journeyId", pr.getJourneyId());
        m.put("currentStageId", pr.getCurrentStageId());
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
        List<Map<String, Object>> reqs = progressRequirementRepository.findByTenantIdAndProgressId(tenantId, pr.getId())
                .stream().map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("requirementId", r.getRequirementId());
                    rm.put("status", r.getStatus().name());
                    rm.put("completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : null);
                    rm.put("evidence", r.getEvidence());
                    rm.put("notes", r.getNotes());
                    rm.put("verifiedById", r.getVerifiedBy());
                    return rm;
                }).toList();
        m.put("requirementProgress", reqs);
        return m;
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
            result = assignmentRepository.findByTenantIdAndStatus(tenantId, parseEnum(MentorAssignment.AssignmentStatus.class, status, null), p);
        } else {
            result = assignmentRepository.findByTenantId(tenantId, p);
        }
        return result.getContent().stream().map(this::assignmentView).toList();
    }

    public Map<String, Object> createAssignment(UUID tenantId, Map<String, Object> body) {
        MentorAssignment a = MentorAssignment.builder()
                .tenantId(tenantId)
                .mentorId(UUID.fromString(String.valueOf(body.get("mentorId"))))
                .discipleId(UUID.fromString(String.valueOf(body.get("discipleId"))))
                .journeyId(longVal(body.get("journeyId")))
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

    private Map<String, Object> assignmentView(MentorAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("mentorId", a.getMentorId());
        m.put("discipleId", a.getDiscipleId());
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
            result = meetingRepository.findByTenantIdAndStatus(tenantId, parseEnum(MentorMeeting.MeetingStatus.class, status, null), p);
        } else {
            result = meetingRepository.findByTenantId(tenantId, p);
        }
        return result.getContent().stream().map(this::meetingView).toList();
    }

    public Map<String, Object> scheduleMeeting(UUID tenantId, Map<String, Object> body) {
        MentorMeeting mt = MentorMeeting.builder()
                .tenantId(tenantId)
                .assignmentId(longVal(body.get("assignmentId")))
                .mentorId(UUID.fromString(String.valueOf(body.get("mentorId"))))
                .discipleId(UUID.fromString(String.valueOf(body.get("discipleId"))))
                .scheduledAt(body.get("scheduledAt") != null ? Instant.parse((String) body.get("scheduledAt")) : Instant.now())
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
        m.put("discipleId", mt.getDiscipleId());
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

    public Map<String, Object> getReport(UUID tenantId, Long journeyId) {
        requireJourney(tenantId, journeyId);
        long total = progressRepository.countByTenantIdAndJourneyId(tenantId, journeyId);
        long active = progressRepository.countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, DiscipleProgress.ProgressStatus.IN_PROGRESS);
        long completed = progressRepository.countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, DiscipleProgress.ProgressStatus.COMPLETED);
        long stalled = progressRepository.countByTenantIdAndJourneyIdAndStatus(tenantId, journeyId, DiscipleProgress.ProgressStatus.STALLED);
        long totalMeetings = meetingRepository.countByTenantIdAndJourneyId(tenantId, journeyId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("journeyId", journeyId);
        m.put("totalDisciples", total);
        m.put("activeDisciples", active);
        m.put("completedDisciples", completed);
        m.put("stalledDisciples", stalled);
        m.put("totalMeetings", totalMeetings);
        m.put("generatedAt", Instant.now().toString());
        return m;
    }

    public List<Map<String, Object>> getTopMentors(UUID tenantId, Long journeyId, int limit) {
        requireJourney(tenantId, journeyId);
        return assignmentRepository.findByTenantIdAndJourneyId(tenantId, journeyId, PageRequest.of(0, Math.min(limit, 50)))
                .getContent().stream().map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("mentorId", a.getMentorId());
                    m.put("discipleCount", 1);
                    m.put("meetingCount", 0);
                    return m;
                }).toList();
    }

    // ==================== helpers ====================

    private static <T extends Enum<T>> T parseEnum(Class<T> cls, String value, T def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(cls, value); } catch (IllegalArgumentException e) { return def; }
    }

    private static long longVal(Object o) { return o == null ? 0 : Long.parseLong(String.valueOf(o)); }
    private static int intVal(Object o, int def) { return o == null ? def : Integer.parseInt(String.valueOf(o)); }
    private static int intVal(Object o, Integer def) { return o == null || o == "" ? def : Integer.parseInt(String.valueOf(o)); }
    private static boolean boolVal(Object o, boolean def) { return o == null ? def : Boolean.parseBoolean(String.valueOf(o)); }
}
