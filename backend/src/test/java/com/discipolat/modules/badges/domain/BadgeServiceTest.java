package com.discipolat.modules.badges.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.evangelism.domain.EvangelismStageHistoryRepository;
import com.discipolat.modules.interactions.domain.InteractionRepository;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.visits.domain.VisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock private BadgeRepository badgeRepository;
    @Mock private UserBadgeRepository userBadgeRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private InteractionRepository interactionRepository;
    @Mock private EvangelismStageHistoryRepository stageHistoryRepository;
    @Mock private MemberPresenceRepository memberPresenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private BadgeService badgeService;

    private Badge badge(Badge.Critere critere, int seuil, Badge.Niveau niveau) {
        return Badge.builder()
                .id(UUID.randomUUID())
                .code(critere.name() + "_" + seuil)
                .nom("Badge test")
                .niveau(niveau)
                .critere(critere)
                .seuil(seuil)
                .actif(true)
                .build();
    }

    @Test
    void evaluate_shouldEarnBadgeWhenThresholdReached() {
        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        Badge visiteBadge = badge(Badge.Critere.VISITES, 1, Badge.Niveau.BRONZE);
        Badge inaccessibleBadge = badge(Badge.Critere.INTERACTIONS, 50, Badge.Niveau.OR);

        when(badgeRepository.findByActifTrueOrderBySeuilAsc())
                .thenReturn(List.of(visiteBadge, inaccessibleBadge));
        when(visitRepository.countByVisiteurIdAndStatut(any(), any())).thenReturn(3L);
        when(interactionRepository.countByAuteurId(any())).thenReturn(2L);
        when(stageHistoryRepository.countByEtapeAndCreePar(any(), any())).thenReturn(0L);
        lenient().when(memberPresenceRepository.findByUserIdOrderBySemaineDesc(any()))
                .thenReturn(List.of());
        lenient().when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(userBadgeRepository.findByUserIdAndBadgeId(any(), any()))
                .thenReturn(Optional.empty());
        when(userBadgeRepository.save(any(UserBadge.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<Badge> earned = badgeService.evaluate();

        assertEquals(1, earned.size());
        assertEquals(Badge.Critere.VISITES, earned.get(0).getCritere());
    }

    @Test
    void evaluate_shouldNotDuplicateAlreadyEarnedBadge() {
        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        Badge badge = badge(Badge.Critere.VISITES, 1, Badge.Niveau.BRONZE);
        when(badgeRepository.findByActifTrueOrderBySeuilAsc()).thenReturn(List.of(badge));
        when(visitRepository.countByVisiteurIdAndStatut(any(), any())).thenReturn(5L);
        when(interactionRepository.countByAuteurId(any())).thenReturn(0L);
        when(stageHistoryRepository.countByEtapeAndCreePar(any(), any())).thenReturn(0L);
        lenient().when(memberPresenceRepository.findByUserIdOrderBySemaineDesc(any()))
                .thenReturn(List.of());
        lenient().when(userRepository.findById(any())).thenReturn(Optional.of(new User()));

        // Badge déjà gagné → aucun nouveau badge
        when(userBadgeRepository.findByUserIdAndBadgeId(any(), any()))
                .thenReturn(Optional.of(new UserBadge()));

        List<Badge> earned = badgeService.evaluate();

        assertTrue(earned.isEmpty());
    }

    @Test
    void leaderboard_shouldSortByBadgeCountDescending() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        User ua = new User();
        ua.setId(a);
        ua.setFirstName("A");
        ua.setLastName("Alpha");
        User ub = new User();
        ub.setId(b);
        ub.setFirstName("B");
        ub.setLastName("Beta");

        when(userBadgeRepository.findAll()).thenReturn(List.of(
                UserBadge.builder().userId(a).badgeId(UUID.randomUUID()).build(),
                UserBadge.builder().userId(a).badgeId(UUID.randomUUID()).build(),
                UserBadge.builder().userId(b).badgeId(UUID.randomUUID()).build()
        ));
        lenient().when(userRepository.findById(a)).thenReturn(Optional.of(ua));
        lenient().when(userRepository.findById(b)).thenReturn(Optional.of(ub));

        var rows = badgeService.leaderboard();

        assertEquals(2, rows.size());
        assertEquals(2L, rows.get(0).get("badges"));
        assertEquals(1L, rows.get(1).get("badges"));
        assertEquals("A Alpha", rows.get(0).get("nom"));
    }
}
