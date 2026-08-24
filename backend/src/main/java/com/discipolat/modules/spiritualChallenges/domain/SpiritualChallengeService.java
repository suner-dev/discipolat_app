package com.discipolat.modules.spiritualChallenges.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P1 #51 — Défis spirituels avec auto-génération, streaks, gamification.
 */
@Service
@Transactional
public class SpiritualChallengeService {

    private final SpiritualChallengeRepository repository;

    public SpiritualChallengeService(SpiritualChallengeRepository repository) {
        this.repository = repository;
    }

    public Page<SpiritualChallenge> list(Pageable pageable) {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    public SpiritualChallenge getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualChallenge", id));
    }

    public SpiritualChallenge create(String titre, String description, String type,
                                      UUID assignéÀ, int objectifJours, LocalDateTime deadline, UUID userId) {
        SpiritualChallenge challenge = new SpiritualChallenge();
        challenge.setTenantId(TenantContext.getCurrentTenantId());
        challenge.setTitre(titre);
        challenge.setDescription(description);
        challenge.setType(SpiritualChallenge.Type.valueOf(type != null ? type : "AUTRE"));
        challenge.setCreatedBy(userId);
        challenge.setAssignéÀ(assignéÀ);
        challenge.setObjectifJours(objectifJours > 0 ? objectifJours : 7);
        challenge.setDeadline(deadline);
        return repository.save(challenge);
    }

    public SpiritualChallenge progress(UUID id) {
        SpiritualChallenge challenge = getById(id);
        challenge.setJoursComplétés(challenge.getJoursComplétés() + 1);
        if (challenge.getJoursComplétés() >= challenge.getObjectifJours()) {
            challenge.setStatut(SpiritualChallenge.Statut.TERMINÉ);
            challenge.setCompletedAt(LocalDateTime.now());
        }
        return repository.save(challenge);
    }

    public SpiritualChallenge updateStatut(UUID id, String statut) {
        SpiritualChallenge challenge = getById(id);
        challenge.setStatut(SpiritualChallenge.Statut.valueOf(statut));
        return repository.save(challenge);
    }

    /**
     * General stats for all challenges (no member filter).
     */
    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var all = repository.findByTenantIdOrderByCreatedAtDesc(tenantId,
                PageRequest.of(0, 1000));
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.getNumberOfElements());
        stats.put("enCours", all.stream().filter(c -> c.getStatut() == SpiritualChallenge.Statut.EN_COURS).count());
        stats.put("terminés", all.stream().filter(c -> c.getStatut() == SpiritualChallenge.Statut.TERMINÉ).count());
        stats.put("abandonnés", all.stream().filter(c -> c.getStatut() == SpiritualChallenge.Statut.ABANDONNÉ).count());
        return stats;
    }

    /**
     * Auto-generate weekly challenges based on member profile and gaps.
     */
    public List<SpiritualChallenge> autoGenerateWeekly(UUID tenantId) {
        List<SpiritualChallenge> generated = new ArrayList<>();

        String[][] templates = {
                {"Lecture quotidienne", "Lire au moins 10 versets par jour pendant 7 jours", "LECTURE", "7"},
                {"Prière matinale", "Prier 15 minutes chaque matin pendant 5 jours", "PRIÈRE", "5"},
                {"Service communautaire", "Réaliser 1 acte de service pendant la semaine", "SERVICE", "1"},
                {"Témoignage", "Partager un témoignage avec quelqu'un", "ÉVANGÉLISATION", "1"},
                {"Étude biblique", "Étudier un chapitre de la Bible et écrire des notes", "LECTURE", "1"},
                {"Jeûne", "Jeûner un repas et prier pour l'église", "JEÛNE", "1"},
        };

        for (String[] t : templates) {
            SpiritualChallenge challenge = new SpiritualChallenge();
            challenge.setTenantId(tenantId);
            challenge.setTitre(t[0]);
            challenge.setDescription(t[1]);
            challenge.setType(SpiritualChallenge.Type.valueOf(t[2]));
            challenge.setObjectifJours(Integer.parseInt(t[3]));
            challenge.setStatut(SpiritualChallenge.Statut.EN_COURS);
            challenge.setCreatedBy(UUID.randomUUID());
            generated.add(repository.save(challenge));
        }
        return generated;
    }

    /**
     * Calculate streak for a member (consecutive days with challenge progress).
     */
    public int calculateStreak(UUID memberId) {
        List<SpiritualChallenge> completed = repository.findByTenantIdOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), PageRequest.of(0, 1000))
                .stream()
                .filter(c -> c.getStatut() == SpiritualChallenge.Statut.TERMINÉ)
                .filter(c -> memberId.equals(c.getAssignéÀ()))
                .sorted(Comparator.comparing(SpiritualChallenge::getCompletedAt).reversed())
                .toList();

        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (SpiritualChallenge c : completed) {
            if (c.getCompletedAt() != null && c.getCompletedAt().toLocalDate().equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Get gamification stats for a member.
     */
    public Map<String, Object> getGamificationStats(UUID memberId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var memberChallenges = repository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 1000))
                .stream()
                .filter(c -> memberId.equals(c.getAssignéÀ()))
                .toList();

        long completed = memberChallenges.stream()
                .filter(c -> c.getStatut() == SpiritualChallenge.Statut.TERMINÉ).count();
        int totalPoints = (int) (completed * 50);
        int streak = calculateStreak(memberId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("completed", completed);
        stats.put("totalPoints", totalPoints);
        stats.put("streak", streak);
        stats.put("level", totalPoints / 200 + 1);
        stats.put("badges", calculateBadges(completed, streak));
        return stats;
    }

    private List<String> calculateBadges(long completed, int streak) {
        List<String> badges = new ArrayList<>();
        if (completed >= 1) badges.add("Premier Défi");
        if (completed >= 5) badges.add("Défiur Régulier");
        if (completed >= 10) badges.add("Guerrier Spirituel");
        if (completed >= 25) badges.add("Champion de la Foi");
        if (streak >= 3) badges.add("3 Jours Consécutifs");
        if (streak >= 7) badges.add("Semaine Parfaite");
        if (streak >= 30) badges.add("Légende du Jeûne");
        return badges;
    }
}
