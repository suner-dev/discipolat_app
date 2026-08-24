package com.discipolat.modules.quest.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.quest.domain.QuestService;
import com.discipolat.modules.quest.domain.XpLedger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quest")
public class QuestController {

    private final QuestService service;
    private final SecurityUtils securityUtils;

    public QuestController(QuestService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> myProfile() {
        return ResponseEntity.ok(service.myProfile());
    }

    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> profileFor(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.profileFor(userId));
    }

    @GetMapping("/quests")
    public ResponseEntity<List<Map<String, Object>>> weeklyQuests() {
        return ResponseEntity.ok(service.weeklyQuests(securityUtils.getCurrentUserId()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> leaderboard() {
        return ResponseEntity.ok(service.leaderboard());
    }

    /** P9 — Classement agrégé par famille ou département (?by=FAMILLE|DEPARTMENT). */
    @GetMapping("/leaderboard/groups")
    public ResponseEntity<List<Map<String, Object>>> groupLeaderboard(
            @RequestParam(defaultValue = "FAMILLE") String by) {
        return ResponseEntity.ok(service.groupLeaderboard(by));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }

    /** Attribution manuelle d'XP (bonus événement live, semaine évangélisation…). */
    @PostMapping("/award")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<XpLedger> award(@RequestBody Map<String, Object> body) {
        UUID userId = UUID.fromString(body.get("userId").toString());
        XpLedger.QuestAction action = XpLedger.QuestAction.valueOf(body.get("action").toString());
        Integer points = body.containsKey("points") && body.get("points") != null
                ? Integer.valueOf(body.get("points").toString()) : null;
        String description = body.containsKey("description") ? body.get("description").toString() : null;
        return ResponseEntity.ok(service.award(userId, action, points, description));
    }

    // ======================== P9 — DÉFIS HEBDO AUTO + BADGES CONTEXTUALISÉS ========================

    /** Défis hebdomadaires générés automatiquement selon le profil. */
    @GetMapping("/weekly-challenges")
    public ResponseEntity<Map<String, Object>> weeklyChallenges() {
        return ResponseEntity.ok(service.generateWeeklyChallenges());
    }

    /** Badges contextualisés par rôle et actions. */
    @GetMapping("/contextual-badges")
    public ResponseEntity<List<Map<String, Object>>> contextualBadges() {
        return ResponseEntity.ok(service.getContextualBadges());
    }
}
