package com.discipolat.modules.bibleReading.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BibleReadingService {

    private final BibleReadingPlanRepository planRepo;
    private final BibleReadingEntryRepository entryRepo;

    public BibleReadingService(BibleReadingPlanRepository planRepo, BibleReadingEntryRepository entryRepo) {
        this.planRepo = planRepo;
        this.entryRepo = entryRepo;
    }

    // ─── Plans ───

    public List<BibleReadingPlan> listPlansByUser(UUID userId) {
        return planRepo.findByCreateurIdOrderByCreeLeDesc(userId);
    }

    public List<BibleReadingPlan> listSharedPlans() {
        return planRepo.findByTenantIdAndPartageFamilleTrueOrderByCreeLeDesc(TenantContext.getCurrentTenantId());
    }

    public BibleReadingPlan getPlan(UUID id) {
        return planRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("BibleReadingPlan", id));
    }

    public BibleReadingPlan createPlan(BibleReadingPlan plan) {
        plan.setTenantId(TenantContext.getCurrentTenantId());
        plan.setCreeLe(LocalDateTime.now());
        return planRepo.save(plan);
    }

    public BibleReadingPlan updatePlan(UUID id, BibleReadingPlan updates) {
        BibleReadingPlan p = getPlan(id);
        if (updates.getTitre() != null) p.setTitre(updates.getTitre());
        if (updates.getDescription() != null) p.setDescription(updates.getDescription());
        if (updates.getStatut() != null) p.setStatut(updates.getStatut());
        p.setPartageFamille(updates.isPartageFamille());
        if (updates.getJoursTotal() > 0) p.setJoursTotal(updates.getJoursTotal());
        p.setModifieLe(LocalDateTime.now());
        return planRepo.save(p);
    }

    public void deletePlan(UUID id) { planRepo.deleteById(id); }

    // ─── Entries ───

    public List<BibleReadingEntry> listEntries(UUID planId, UUID userId) {
        return entryRepo.findByPlanIdAndUtilisateurIdOrderByDateLectureDesc(planId, userId);
    }

    public List<BibleReadingEntry> listByUser(UUID userId) {
        return entryRepo.findByUtilisateurIdOrderByDateLectureDesc(userId);
    }

    public List<BibleReadingEntry> listToday(UUID userId) {
        return entryRepo.findByUtilisateurIdAndDateLectureOrderByDateLectureDesc(userId, LocalDate.now());
    }

    public BibleReadingEntry getEntry(UUID id) {
        return entryRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("BibleReadingEntry", id));
    }

    public BibleReadingEntry createEntry(BibleReadingEntry entry) {
        entry.setTenantId(TenantContext.getCurrentTenantId());
        entry.setCreeLe(LocalDateTime.now());
        return entryRepo.save(entry);
    }

    public BibleReadingEntry markAsRead(UUID id) {
        BibleReadingEntry e = getEntry(id);
        e.setLu(true);
        e.setDateLecture(LocalDate.now());
        // Update plan progress
        BibleReadingPlan plan = getPlan(e.getPlanId());
        long completed = entryRepo.countByPlanIdAndUtilisateurIdAndLuTrue(e.getPlanId(), e.getUtilisateurId());
        plan.setJoursCompletes((int) completed);
        planRepo.save(plan);
        return entryRepo.save(e);
    }

    public BibleReadingEntry addNote(UUID id, String note) {
        BibleReadingEntry e = getEntry(id);
        e.setNote(note);
        return entryRepo.save(e);
    }

    public void deleteEntry(UUID id) { entryRepo.deleteById(id); }

    // ─── Stats ───

    public Map<String, Object> getStats(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        List<BibleReadingEntry> entries = entryRepo.findByUtilisateurIdOrderByDateLectureDesc(userId);
        long totalLu = entries.stream().filter(BibleReadingEntry::isLu).count();
        stats.put("totalEntries", entries.size());
        stats.put("totalRead", totalLu);
        stats.put("streak", calculateStreak(entries));
        stats.put("plansActifs", planRepo.findByCreateurIdAndStatutOrderByCreeLeDesc(userId, BibleReadingPlan.Statut.ACTIF).size());
        return stats;
    }

    public List<Map<String, Object>> getFamilyProgress(UUID userId) {
        // Return progress for shared plans' participants
        List<BibleReadingPlan> sharedPlans = listSharedPlans();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BibleReadingPlan plan : sharedPlans) {
            List<BibleReadingEntry> planEntries = entryRepo.findByPlanIdAndUtilisateurIdOrderByDateLectureDesc(plan.getId(), userId);
            long read = planEntries.stream().filter(BibleReadingEntry::isLu).count();
            Map<String, Object> progress = new HashMap<>();
            progress.put("userId", userId);
            progress.put("planId", plan.getId());
            progress.put("planTitre", plan.getTitre());
            progress.put("joursCompletes", read);
            progress.put("joursTotal", plan.getJoursTotal());
            progress.put("pourcentage", plan.getJoursTotal() > 0 ? (int)(read * 100.0 / plan.getJoursTotal()) : 0);
            result.add(progress);
        }
        return result;
    }

    private int calculateStreak(List<BibleReadingEntry> entries) {
        Set<LocalDate> dates = entries.stream()
                .filter(BibleReadingEntry::isLu)
                .map(BibleReadingEntry::getDateLecture)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate d = LocalDate.now();
        while (dates.contains(d)) { streak++; d = d.minusDays(1); }
        return streak;
    }
}
