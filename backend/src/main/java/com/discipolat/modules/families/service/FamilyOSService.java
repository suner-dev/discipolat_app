package com.discipolat.modules.families.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.*;
import com.discipolat.modules.families.repository.*;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    // ========== DASHBOARD ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getFamilyDashboard(UUID tenantId, UUID familyId) {
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

    public FamilyVisit updateVisit(UUID tenantId, UUID actorId, UUID visitId, FamilyVisit updates) {
        FamilyVisit visit = familyVisitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("FamilyVisit", visitId));
        if (!visit.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cross-tenant");
        }

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
        if (from != null && to != null) {
            return familyVisitRepository.findByFamilyIdAndVisitDateBetweenAndDeletedFalse(familyId, from, to);
        }
        return familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId);
    }

    // ========== RECEPTIONS ==========

    public FamilyReception createReception(UUID tenantId, UUID actorId, FamilyReception reception) {
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
        if (from != null && to != null) {
            return familyReceptionRepository.findByFamilyIdAndReceptionDateBetween(familyId, from, to);
        }
        return familyReceptionRepository.findByFamilyIdAndDeletedFalse(familyId);
    }

    // ========== MEETINGS ==========

    public FamilyMeeting createMeeting(UUID tenantId, UUID actorId, FamilyMeeting meeting) {
        meeting.setTenantId(tenantId);
        meeting.setCreatedBy(actorId);
        return familyMeetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public List<FamilyMeeting> getFamilyMeetings(UUID tenantId, UUID familyId, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return familyMeetingRepository.findByFamilyIdAndMeetingDateBetween(familyId, from, to);
        }
        return familyMeetingRepository.findByFamilyIdAndDeletedFalse(familyId);
    }

    // ========== UNIFIED ACTIVITY LIST ==========

    @Transactional(readOnly = true)
    public Page<FamilyActivity> getFamilyActivities(UUID tenantId, UUID familyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("activityDate").descending());
        return familyActivityRepository.findByTenantIdAndDeletedFalseOrderByActivityDateDesc(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<FamilyActivity> getFamilyActivitiesByDateRange(UUID tenantId, UUID familyId, LocalDate from, LocalDate to) {
        return familyActivityRepository.findByFamilyIdAndActivityDateBetween(familyId, from, to);
    }

    // ========== HELPERS ==========

    private String getSoulName(UUID soulId) {
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

    // ========== G4.2: SEARCH & ADD MEMBERS ==========

    @Transactional(readOnly = true)
    public List<com.discipolat.modules.users.domain.User> searchSoulsForFamily(UUID tenantId, UUID familyId, String search, String scope) {
        // Get already assigned souls
        Set<UUID> assignedSoulIds = getFamilySouls(familyId).stream()
                .map(Soul::getId)
                .collect(Collectors.toSet());

        // Search in tenant scope
        Page<User> candidatesPage = userRepository.findByTenantIdAndDeletedFalse(tenantId, PageRequest.of(0, 100));
        List<com.discipolat.modules.users.domain.User> candidates = candidatesPage.getContent();

        // Filter by scope
        if ("CAMPUS".equals(scope)) {
            // TODO: Filter by campus
        }

        // Filter out already assigned
        return candidates.stream()
                .filter(s -> !assignedSoulIds.contains(s.getId()))
                .filter(s -> search == null || search.isBlank() ||
                        (s.getFirstName() != null && s.getFirstName().toLowerCase().contains(search.toLowerCase())) ||
                        (s.getLastName() != null && s.getLastName().toLowerCase().contains(search.toLowerCase())) ||
                        (s.getEmail() != null && s.getEmail().toLowerCase().contains(search.toLowerCase())))
                .limit(50)
                .toList();
    }

    public Family addSoulToFamily(UUID tenantId, UUID actorId, UUID familyId, UUID soulId, UUID faiseurId) {
        familyRepository.findById(familyId)
                .orElseThrow(() -> new EntityNotFoundException("Family", familyId));

        com.discipolat.modules.souls.domain.Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", soulId));

        // Create FamilyActivity for tracking
        FamilyActivity activity = FamilyActivity.builder()
                .tenantId(tenantId)
                .familyId(familyId)
                .activityType("RECEPTION")
                .referenceId(UUID.randomUUID())
                .title("Ajout membre: " + soul.getPrenom() + " " + soul.getNom())
                .description("Nouveau membre ajouté à la famille")
                .activityDate(LocalDate.now())
                .status("COMPLETED")
                .createdBy(actorId)
                .build();
        familyActivityRepository.save(activity);

        return familyRepository.findById(familyId).get();
    }

    @Transactional(readOnly = true)
    public List<Soul> getFamilySouls(UUID familyId) {
        return soulRepository.findAllByFamilleIdAndDeletedFalse(familyId);
    }
}