package com.discipolat.modules.quest;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.quest.domain.QuestService;
import com.discipolat.modules.quest.domain.XpLedger;
import com.discipolat.modules.quest.domain.XpLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock private XpLedgerRepository repository;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private QuestService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QuestService(repository, propagationPublisher, securityUtils);
        lenient().when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
        lenient().when(repository.save(any(XpLedger.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void niveau1_quand_xpZero() {
        when(repository.totalXpForUser(userId)).thenReturn(0L);
        Map<String, Object> profile = service.myProfile();
        assertThat(profile.get("level")).isEqualTo(1);
        assertThat(profile.get("totalXp")).isEqualTo(0L);
    }

    @Test
    void niveau3_a_1200xp() {
        when(repository.totalXpForUser(userId)).thenReturn(1200L);
        Map<String, Object> profile = service.myProfile();
        assertThat(profile.get("level")).isEqualTo(3); // 1200 / 500 + 1
        assertThat(profile).containsEntry("title", "Recrue Zélée");
    }

    @Test
    void titreGeneral_niveau20plus() {
        when(repository.totalXpForUser(userId)).thenReturn(10_000L);
        Map<String, Object> profile = service.myProfile();
        assertThat(profile).containsEntry("title", "Général de l'Armée Spirituelle");
    }

    @Test
    void award_utilisePointsParDefaut() {
        XpLedger entry = service.award(userId, XpLedger.QuestAction.VISITE, null, "Visite Jean");
        assertThat(entry.getPoints()).isEqualTo(20);
        verify(propagationPublisher).publishCreated(eq("QUEST_XP"), any(), any(), anyString());
    }

    @Test
    void award_pointsPersonnalises_siOverridePositif() {
        XpLedger entry = service.award(userId, XpLedger.QuestAction.PRIERE, 99, "Bonus live");
        assertThat(entry.getPoints()).isEqualTo(99);
    }

    @Test
    void leaderboard_classeParXpDecroissant() {
        UUID autre = UUID.randomUUID();
        when(repository.sumPointsByUser(tenantId)).thenReturn(List.of(
                new Object[]{autre, 900L},
                new Object[]{userId, 100L}));
        List<Map<String, Object>> board = service.leaderboard();
        assertThat(board).hasSize(2);
        assertThat(board.get(0).get("userId")).isEqualTo(autre);
        assertThat(board.get(0).get("rank")).isEqualTo(1);
        assertThat(board.get(1).get("level")).isEqualTo(1);
    }

    @Test
    void quetesHebdomadaires_contientObjectifsConnus() {
        when(repository.countByUserIdAndActionAndCreatedAtAfter(any(), any(), any())).thenReturn(2L);
        var quests = service.weeklyQuests(userId);
        assertThat(quests).hasSize(5);
        assertThat(quests.stream().map(q -> q.get("code")))
                .containsExactlyInAnyOrder("CULTES", "PRIERES", "VISITES", "RAPPORT", "EVANGELISER");
    }
}
