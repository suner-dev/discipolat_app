package com.discipolat.modules.quest.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Gamification "Discipolat Quest" — système d'XP, de niveaux et de quêtes.
 *
 * - Chaque action valorisée crédite des points (registre XP)
 * - Les niveaux sont calculés par paliers de 500 XP
 * - Les quêtes hebdomadaires suivent la progression par type d'action
 * - Le classement compare les disciples du même tenant
 */
@Service
@Transactional
public class QuestService {

    @PersistenceContext
    private EntityManager em;

    private static final Logger log = LoggerFactory.getLogger(QuestService.class);

    /** Palier XP par niveau : niveau N atteint à (N-1) * LEVEL_STEP points. */
    static final int LEVEL_STEP = 500;

    private final XpLedgerRepository repository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public QuestService(XpLedgerRepository repository,
                        EntityPropagationPublisher propagationPublisher,
                        SecurityUtils securityUtils) {
        this.repository = repository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /**
     * Crédite des XP pour une action. Utilisé par les autres modules
     * (présence, prière, rapport…) et par l'administration.
     */
    public XpLedger award(UUID userId, XpLedger.QuestAction action, Integer pointsOverride, String description) {
        int points = pointsOverride != null && pointsOverride > 0 ? pointsOverride : action.getDefaultPoints();
        XpLedger entry = XpLedger.builder()
                .tenantId(currentTenantId())
                .userId(userId)
                .action(action)
                .points(points)
                .description(description)
                .build();
        XpLedger saved = repository.save(entry);
        propagationPublisher.publishCreated("QUEST_XP", saved.getId(),
                Map.of("userId", userId.toString(), "action", action.name(), "points", points),
                "XP attribué: +" + points + " (" + action.name() + ")");
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> myProfile() {
        return buildProfile(securityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> profileFor(UUID userId) {
        return buildProfile(userId);
    }

    private Map<String, Object> buildProfile(UUID userId) {
        long totalXp = repository.totalXpForUser(userId);
        int level = levelFor(totalXp);
        long xpInLevel = totalXp - (long) (level - 1) * LEVEL_STEP;

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", userId);
        profile.put("totalXp", totalXp);
        profile.put("level", level);
        profile.put("xpInLevel", xpInLevel);
        profile.put("xpForNextLevel", LEVEL_STEP);
        profile.put("progressPercent", Math.min(100, Math.round(xpInLevel * 100.0 / LEVEL_STEP)));
        profile.put("title", titleFor(level));
        profile.put("history", repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(30)
                .map(e -> Map.of(
                        "action", e.getAction().name(),
                        "points", e.getPoints(),
                        "description", e.getDescription() == null ? "" : e.getDescription(),
                        "date", e.getCreatedAt()))
                .toList());
        return profile;
    }

    /** Quêtes hebdomadaires : objectif par type d'action sur les 7 derniers jours. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> weeklyQuests(UUID userId) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Map<String, Object>> quests = new ArrayList<>();
        for (QuestDefinition def : QuestDefinition.values()) {
            long done = repository.countByUserIdAndActionAndCreatedAtAfter(userId, def.action, weekAgo);
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("code", def.name());
            q.put("label", def.label);
            q.put("target", def.target);
            q.put("done", done);
            q.put("completed", done >= def.target);
            q.put("xpReward", def.xpReward);
            q.put("progressPercent", Math.min(100, Math.round(done * 100.0 / def.target)));
            quests.add(q);
        }
        return quests;
    }

    /** Classement du tenant : top disciples par XP cumulé. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> leaderboard() {
        UUID tenantId = currentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        List<Object[]> rows = repository.sumPointsByUser(tenantId);
        List<Map<String, Object>> board = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            UUID userId = (UUID) row[0];
            long xp = ((Number) row[1]).longValue();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("userId", userId);
            entry.put("totalXp", xp);
            entry.put("level", levelFor(xp));
            entry.put("title", titleFor(levelFor(xp)));
            board.add(entry);
            if (board.size() >= 50) break;
        }
        return board;
    }

    /**
     * P9 — Classement agrégé par famille ou par département.
     * Agrège l'XP des membres (xp_ledger) et classe par total puis moyenne.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> groupLeaderboard(String by) {
        UUID tenantId = currentTenantId();
        if (tenantId == null) return List.of();
        boolean byDepartment = "DEPARTMENT".equalsIgnoreCase(by);
        String sql = byDepartment ? """
                SELECT g.name, count(DISTINCT q.user_id), sum(q.points), avg(q.points)
                FROM xp_ledger q
                JOIN souls s ON s.user_id = q.user_id AND s.deleted = false
                JOIN soul_departments sd ON sd.soul_id = s.id AND sd.actif = true
                JOIN departments g ON g.id = sd.department_id
                WHERE q.tenant_id = :t
                GROUP BY g.name ORDER BY sum(q.points) DESC LIMIT 20
                """ : """
                SELECT g.nom, count(DISTINCT q.user_id), sum(q.points), avg(q.points)
                FROM xp_ledger q
                JOIN souls s ON s.user_id = q.user_id AND s.deleted = false
                JOIN families g ON g.id = s.famille_id AND g.deleted = false
                WHERE q.tenant_id = :t
                GROUP BY g.nom ORDER BY sum(q.points) DESC LIMIT 20
                """;
        List<?> rows = em.createNativeQuery(sql)
                .setParameter("t", tenantId)
                .getResultList();
        List<Map<String, Object>> board = new ArrayList<>();
        int rank = 1;
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("nom", row[0]);
            m.put("type", byDepartment ? "DEPARTEMENT" : "FAMILLE");
            m.put("membres", ((Number) row[1]).longValue());
            m.put("xpTotal", ((Number) row[2]).longValue());
            m.put("xpMoyen", Math.round(((Number) row[3]).doubleValue()));
            board.add(m);
        }
        return board;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        UUID tenantId = currentTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("levelStep", LEVEL_STEP);
        if (tenantId == null) {
            stats.put("participants", 0);
            stats.put("totalXpDistributed", 0);
            return stats;
        }
        List<Object[]> rows = repository.sumPointsByUser(tenantId);
        long totalXp = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        stats.put("participants", rows.size());
        stats.put("totalXpDistributed", totalXp);
        return stats;
    }

    /** Niveau = nombre de paliers complets + 1. */
    static int levelFor(long totalXp) {
        if (totalXp <= 0) return 1;
        return (int) Math.min(99, (totalXp / LEVEL_STEP) + 1);
    }

    /** Titre honorifique associé au niveau. */
    static String titleFor(int level) {
        if (level >= 20) return "Général de l'Armée Spirituelle";
        if (level >= 15) return "Commandant";
        if (level >= 12) return "Capitaine";
        if (level >= 9) return "Sergent";
        if (level >= 6) return "Soldat Confirmé";
        if (level >= 3) return "Recrue Zélée";
        return "Nouveau Soldat";
    }

    private UUID currentTenantId() {
        try {
            return securityUtils.getCurrentTenantId();
        } catch (Exception e) {
            log.debug("Tenant indisponible: {}", e.getMessage());
            return null;
        }
    }

    /** Définitions des quêtes hebdomadaires. */
    private enum QuestDefinition {
        CULTES(XpLedger.QuestAction.PRESENCE_CULTE, "Participer à 2 cultes", 2, 50),
        PRIERES(XpLedger.QuestAction.PRIERE, "Faire 5 prières enregistrées", 5, 40),
        VISITES(XpLedger.QuestAction.VISITE, "Réaliser 2 visites", 2, 60),
        RAPPORT(XpLedger.QuestAction.RAPPORT_HEBDO, "Soumettre le rapport hebdomadaire", 1, 80),
        EVANGELISER(XpLedger.QuestAction.EVANGELISATION_PROSPECT, "Ajouter un nouveau prospect", 1, 100);

        final XpLedger.QuestAction action;
        final String label;
        final int target;
        final int xpReward;

        QuestDefinition(XpLedger.QuestAction action, String label, int target, int xpReward) {
            this.action = action;
            this.label = label;
            this.target = target;
            this.xpReward = xpReward;
        }
    }

    // ======================== P9 — DÉFIS HEBDO AUTO + BADGES CONTEXTUALISÉS ========================

    /**
     * P9 — Génère automatiquement les défis hebdomadaires pour un utilisateur
     * en fonction de son profil et de ses actions récentes.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateWeeklyChallenges() {
        UUID userId = securityUtils.getCurrentUserId();
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> challenges = new ArrayList<>();
        for (QuestDefinition def : QuestDefinition.values()) {
            Map<String, Object> challenge = new LinkedHashMap<>();
            challenge.put("id", def.name());
            challenge.put("label", def.label);
            challenge.put("target", def.target);
            challenge.put("xpReward", def.xpReward);
            challenge.put("action", def.action.name());

            // Progress actuelle
            long currentCount = repository.countByUserIdAndActionAndCreatedAtAfter(
                    userId, def.action,
                    java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay());
            challenge.put("current", currentCount);
            challenge.put("completed", currentCount >= def.target);
            challenge.put("progressPct", Math.min(100, (int)(currentCount * 100 / def.target)));

            challenges.add(challenge);
        }

        result.put("challenges", challenges);
        result.put("weekStart", java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY));
        result.put("weekEnd", java.time.LocalDate.now().with(java.time.DayOfWeek.SUNDAY));
        result.put("totalXpAvailable", challenges.stream().mapToInt(c -> (int) c.get("xpReward")).sum());

        return result;
    }

    /**
     * P9 — Badges contextualisés par profil.
     * Détermine les badges pertinents selon le rôle et les actions de l'utilisateur.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getContextualBadges() {
        UUID userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentUserRole();
        List<Map<String, Object>> badges = new ArrayList<>();

        // Badges communs
        badges.add(badgeInfo("FIRST_VISIT", "Première Visite", "Réaliser votre première visite", 50));
        badges.add(badgeInfo("PRAYER_WARRIOR", "Guerrier de Prière", "Enregistrer 10 prières", 100));
        badges.add(badgeInfo("RAPPORT_MASTER", "Maître Rapport", "Soumettre 4 rapports consécutifs", 150));
        badges.add(badgeInfo("FOLLOWER_FIDEL", "Suivi Fidèle", "Contacter 5 disciples cette semaine", 80));

        // Badges spécifiques au rôle
        if ("FAISEUR".equals(role) || "CHEF_DE_FAMILLE".equals(role)) {
            badges.add(badgeInfo("PASTORAL_SHEPHERD", "Berger Pastoral", "Suivre 10 disciples actifs", 200));
            badges.add(badgeInfo("CONVERSION_LEADER", "Leader de Conversion", "Accompagner 3 conversions", 300));
        }
        if ("ADMIN".equals(role) || "PASTEUR".equals(role)) {
            badges.add(badgeInfo("CHURCH_BUILDER", "Bâtisseur d'Église", "Gérer 50+ membres", 500));
            badges.add(badgeInfo("DATA_ORACLE", "Oracle des Données", "Générer 10 rapports analytiques", 250));
        }
        if ("MEMBRE".equals(role)) {
            badges.add(badgeInfo("COMMUNITY_PILLAR", "Pilier Communautaire", "Participer à 8 événements", 150));
            badges.add(badgeInfo("GROWTH_SEEKER", "Chercheur de Croissance", "Suivre 2 formations", 120));
        }

        return badges;
    }

    private Map<String, Object> badgeInfo(String code, String name, String description, int xpValue) {
        Map<String, Object> badge = new LinkedHashMap<>();
        badge.put("code", code);
        badge.put("name", name);
        badge.put("description", description);
        badge.put("xpValue", xpValue);
        // Check if earned
        UUID userId = null;
        try { userId = securityUtils.getCurrentUserId(); } catch (Exception ignored) {}
        if (userId != null) {
            long count = em.createQuery(
                    "SELECT COUNT(ub) FROM UserBadge ub WHERE ub.userId = :uid AND ub.badge.code = :code", Long.class)
                    .setParameter("uid", userId).setParameter("code", code).getSingleResult();
            badge.put("earned", count > 0);
        } else {
            badge.put("earned", false);
        }
        return badge;
    }
}
