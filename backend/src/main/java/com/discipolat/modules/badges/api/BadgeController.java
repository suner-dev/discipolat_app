package com.discipolat.modules.badges.api;

import com.discipolat.modules.badges.domain.Badge;
import com.discipolat.modules.badges.domain.BadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/badges")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /** Profil gamification de l'utilisateur connecté. */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> myBadges() {
        return ResponseEntity.ok(badgeService.myBadges());
    }

    /** Profil gamification d'un utilisateur (fiche d'un faiseur, etc.). */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> userBadges(@PathVariable UUID userId) {
        return ResponseEntity.ok(badgeService.userBadges(userId));
    }

    /** Classement global par nombre de badges. */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> leaderboard() {
        return ResponseEntity.ok(badgeService.leaderboard());
    }

    /** Déclenche la vérification des badges (appelé après une action notable). */
    @PostMapping("/evaluate")
    public ResponseEntity<List<Badge>> evaluate() {
        return ResponseEntity.ok(badgeService.evaluate());
    }
}
