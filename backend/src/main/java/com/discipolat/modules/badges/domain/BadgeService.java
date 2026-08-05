package com.discipolat.modules.badges.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import com.discipolat.modules.evangelism.domain.EvangelismStageHistoryRepository;
import com.discipolat.modules.interactions.domain.InteractionRepository;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.visits.domain.Visit;
import com.discipolat.modules.visits.domain.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Badges & gamification : chaque utilisateur gagne des badges automatiquement
 * en fonction de ses actions réelles (visites réalisées, interactions de suivi,
 * âmes baptisées dans le pipeline, fidélité). Le classement reflète le nombre
 * de badges obtenus.
 */
@Service
@Transactional
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final VisitRepository visitRepository;
    private final InteractionRepository interactionRepository;
    private final EvangelismStageHistoryRepository stageHistoryRepository;
    private final MemberPresenceRepository memberPresenceRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public BadgeService(BadgeRepository badgeRepository,
                        UserBadgeRepository userBadgeRepository,
                        VisitRepository visitRepository,
                        InteractionRepository interactionRepository,
                        EvangelismStageHistoryRepository stageHistoryRepository,
                        MemberPresenceRepository memberPresenceRepository,
                        UserRepository userRepository,
                        SecurityUtils securityUtils) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.visitRepository = visitRepository;
        this.interactionRepository = interactionRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.memberPresenceRepository = memberPresenceRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /** Progression de l'utilisateur connecté sur chaque critère + badges gagnés. */
    @Transactional(readOnly = true)
    public Map<String, Object> myBadges() {
        UUID userId = securityUtils.getCurrentUserId();
        return buildProfile(userId);
    }

    /** Progression d'un utilisateur donné (fiche d'un faiseur/responsable). */
    @Transactional(readOnly = true)
    public Map<String, Object> userBadges(UUID userId) {
        return buildProfile(userId);
    }

    /**
     * Classement global par nombre de badges (top 20).
     * Agrégation en mémoire pour éviter N+1 (une seule requête sur user_badges).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> leaderboard() {
        Map<UUID, Long> badgeCounts = userBadgeRepository.findAll().stream()
                .collect(Collectors.groupingBy(UserBadge::getUserId, Collectors.counting()));

        return badgeCounts.entrySet().stream()
                .map(e -> userRepository.findById(e.getKey())
                        .map(u -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("userId", u.getId());
                            row.put("nom", u.getFirstName() + " " + u.getLastName());
                            row.put("badges", e.getValue());
                            return row;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("badges")).reversed())
                .limit(20)
                .toList();
    }

    /** Vérifie tous les badges non encore gagnés de l'utilisateur connecté (appel après actions). */
    public List<Badge> evaluate() {
        UUID userId = securityUtils.getCurrentUserId();
        Map<Badge.Critere, Double> scores = measure(userId);
        List<Badge> newlyEarned = new ArrayList<>();
        for (Badge badge : badgeRepository.findByActifTrueOrderBySeuilAsc()) {
            double score = scores.getOrDefault(badge.getCritere(), 0.0);
            if (score >= badge.getSeuil()
                    && userBadgeRepository.findByUserIdAndBadgeId(userId, badge.getId()).isEmpty()) {
                userBadgeRepository.save(UserBadge.builder()
                        .userId(userId)
                        .badgeId(badge.getId())
                        .build());
                newlyEarned.add(badge);
            }
        }
        return newlyEarned;
    }

    private Map<String, Object> buildProfile(UUID userId) {
        Map<Badge.Critere, Double> scores = measure(userId);

        List<Badge> allBadges = badgeRepository.findByActifTrueOrderBySeuilAsc();
        Set<UUID> earnedIds = new HashSet<>(
                userBadgeRepository.findByUserId(userId).stream().map(UserBadge::getBadgeId).toList());

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", userId);
        profile.put("totalBadges", earnedIds.size());
        profile.put("scores", scores);

        List<Map<String, Object>> badgeList = new ArrayList<>();
        for (Badge b : allBadges) {
            double score = scores.getOrDefault(b.getCritere(), 0.0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("code", b.getCode());
            row.put("nom", b.getNom());
            row.put("description", b.getDescription());
            row.put("icone", b.getIcone());
            row.put("niveau", b.getNiveau());
            row.put("critere", b.getCritere());
            row.put("seuil", b.getSeuil());
            row.put("score", score);
            row.put("gagne", earnedIds.contains(b.getId()));
            row.put("progression", b.getSeuil() > 0
                    ? Math.min(100.0, Math.round((score / b.getSeuil()) * 1000.0) / 10.0) : 0.0);
            badgeList.add(row);
        }
        profile.put("badges", badgeList);
        return profile;
    }

    /** Mesure les scores de l'utilisateur sur chacun des 5 critères. */
    private Map<Badge.Critere, Double> measure(UUID userId) {
        Map<Badge.Critere, Double> scores = new EnumMap<>(Badge.Critere.class);
        scores.put(Badge.Critere.VISITES,
                (double) visitRepository.countByVisiteurIdAndStatut(userId, Visit.StatutVisite.REALISEE));
        scores.put(Badge.Critere.INTERACTIONS,
                (double) interactionRepository.countByAuteurId(userId));
        scores.put(Badge.Critere.EVANGELISATION,
                (double) stageHistoryRepository.countByEtapeAndCreePar(EvangelismEtape.BAPTEME, userId));

        // Présence : nombre de semaines distinctes avec au moins une présence confirmée
        double weeks = memberPresenceRepository.findByUserIdOrderBySemaineDesc(userId).stream()
                .filter(p -> p.getPresences() != null
                        && p.getPresences().values().stream().anyMatch(Boolean::booleanValue))
                .map(MemberPresence::getSemaine)
                .distinct()
                .count();
        scores.put(Badge.Critere.PRESENCE, weeks);

        // Fidélité : années depuis la création du compte
        double years = userRepository.findById(userId)
                .map(u -> u.getCreatedAt() != null
                        ? ChronoUnit.YEARS.between(u.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(), LocalDate.now())
                        : 0L)
                .orElse(0L);
        scores.put(Badge.Critere.FIDELITE, years);
        return scores;
    }
}
