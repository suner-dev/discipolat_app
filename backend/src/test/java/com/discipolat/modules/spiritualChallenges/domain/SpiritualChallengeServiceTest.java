package com.discipolat.modules.spiritualChallenges.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * G5.5 — Streaks & gamification exposés par GET /spiritual-challenges/my/stats.
 * Le streak compte les JOURS CONSÉCUTIFS (aujourd'hui en arrière) où un défi a
 * été terminé par le membre ; un trou arrête le compte. Les points, le niveau
 * et les badges dérivent des défis TERMINÉS du membre uniquement.
 */
@ExtendWith(MockitoExtension.class)
class SpiritualChallengeServiceTest {

    @Mock private SpiritualChallengeRepository repository;

    private SpiritualChallengeService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SpiritualChallengeService(repository);
        TenantContext.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private SpiritualChallenge challenge(UUID assignee, SpiritualChallenge.Statut statut, LocalDate completedOn) {
        SpiritualChallenge c = new SpiritualChallenge();
        c.setTenantId(TENANT);
        c.setTitre("Défi");
        c.setStatut(statut);
        c.setAssignéÀ(assignee);
        if (completedOn != null) c.setCompletedAt(completedOn.atTime(10, 0));
        return c;
    }

    private void stubRepository(SpiritualChallenge... challenges) {
        when(repository.findByTenantIdOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(challenges)));
    }

    @Test
    void streakCompteLesJoursConsecutifsDePuisAujourdHui() {
        LocalDate today = LocalDate.now();
        stubRepository(
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(1)),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(2))
        );
        assertEquals(3, service.calculateStreak(MEMBER));
    }

    @Test
    void streakSouffreAprunTrouDansLaSerie() {
        LocalDate today = LocalDate.now();
        stubRepository(
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today),
                // trou : hier absent
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(2))
        );
        assertEquals(1, service.calculateStreak(MEMBER));
    }

    @Test
    void streakIgnoreLesDefisDesAutresMembres() {
        LocalDate today = LocalDate.now();
        stubRepository(
                challenge(OTHER, SpiritualChallenge.Statut.TERMINÉ, today),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today)
        );
        assertEquals(1, service.calculateStreak(MEMBER));
    }

    @Test
    void statsDeGamificationDeriventDesDefisTerminesDuMembre() {
        LocalDate today = LocalDate.now();
        stubRepository(
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(1)),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(2)),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(3)),
                challenge(MEMBER, SpiritualChallenge.Statut.TERMINÉ, today.minusDays(4)),
                challenge(MEMBER, SpiritualChallenge.Statut.EN_COURS, null),
                challenge(OTHER, SpiritualChallenge.Statut.TERMINÉ, today)
        );

        Map<String, Object> stats = service.getGamificationStats(MEMBER);

        assertEquals(5L, stats.get("completed"));
        assertEquals(250, stats.get("totalPoints"));      // 5 × 50
        assertEquals(2, stats.get("level"));              // 250/200 + 1
        assertEquals(5, stats.get("streak"));
        @SuppressWarnings("unchecked")
        List<String> badges = (List<String>) stats.get("badges");
        assertTrue(badges.contains("Premier Défi"));
        assertTrue(badges.contains("Défiur Régulier"));
        // streak 5 < 7 : pas "Semaine Parfaite"
        assertFalse(badges.contains("Semaine Parfaite"));
    }

    @Test
    void statsVidesSansDefiTermine() {
        stubRepository();
        Map<String, Object> stats = service.getGamificationStats(MEMBER);
        assertEquals(0L, stats.get("completed"));
        assertEquals(0, stats.get("totalPoints"));
        assertEquals(1, stats.get("level"));
        assertEquals(0, stats.get("streak"));
        assertTrue(((List<?>) stats.get("badges")).isEmpty());
    }
}
