package com.discipolat.modules.evangelism;

import com.discipolat.modules.evangelism.domain.ConversionScoringService;
import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import com.discipolat.modules.evangelism.domain.EvangelismStageHistoryRepository;
import com.discipolat.modules.evangelism.domain.EvangelismTrack;
import com.discipolat.modules.evangelism.domain.EvangelismTrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionScoringServiceTest {

    @Mock private EvangelismTrackRepository trackRepository;
    @Mock private EvangelismStageHistoryRepository historyRepository;

    private ConversionScoringService service;
    private final UUID soulId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ConversionScoringService(trackRepository, historyRepository);
    }

    @Test
    void scoreEleve_pourProspectAvanceEtRecent() {
        EvangelismTrack track = EvangelismTrack.builder()
                .soulId(soulId).etape(EvangelismEtape.BAPTEME)
                .dateEtape(LocalDate.now().minusDays(3)).build();
        when(trackRepository.findBySoulId(soulId)).thenReturn(Optional.of(track));
        when(historyRepository.countByTrackId(any())).thenReturn(6L);

        Map<String, Object> score = service.scoreFor(soulId);
        assertThat((long) score.get("score")).isGreaterThanOrEqualTo(80L);
        assertThat(score.get("label")).isEqualTo("Conversion imminente");
        assertThat(score.get("stagnant")).isEqualTo(false);
    }

    @Test
    void stagnation_penaliseEtRecommandeVisite() {
        EvangelismTrack track = EvangelismTrack.builder()
                .soulId(soulId).etape(EvangelismEtape.PREMIER_CONTACT)
                .dateEtape(LocalDate.now().minusDays(40)).build();
        when(trackRepository.findBySoulId(soulId)).thenReturn(Optional.of(track));
        when(historyRepository.countByTrackId(any())).thenReturn(1L);

        Map<String, Object> score = service.scoreFor(soulId);
        assertThat(score.get("stagnant")).isEqualTo(true);
        assertThat((String) score.get("recommendation")).contains("visite personnelle");
        // base 15 + stages 2 - pénalité (40-21) = -4 → borné à 2
        assertThat((long) score.get("score")).isEqualTo(2L);
    }

    @Test
    void scoreZero_sansPipeline() {
        when(trackRepository.findBySoulId(soulId)).thenReturn(Optional.empty());
        Map<String, Object> score = service.scoreFor(soulId);
        assertThat(score.get("score")).isEqualTo(0L);
        assertThat(score.get("label")).isEqualTo("Aucun pipeline");
    }

    @Test
    void scoreAll_trieDuPlusPrometteurAuPlusRisque() {
        UUID soulA = UUID.randomUUID(), soulB = UUID.randomUUID();
        EvangelismTrack avance = EvangelismTrack.builder()
                .soulId(soulA).etape(EvangelismEtape.SUIVI)
                .dateEtape(LocalDate.now()).build();
        EvangelismTrack froid = EvangelismTrack.builder()
                .soulId(soulB).etape(EvangelismEtape.NOUVELLE_AME)
                .dateEtape(LocalDate.now().minusDays(60)).build();
        when(trackRepository.findAll()).thenReturn(List.of(froid, avance));
        when(historyRepository.countByTrackId(any())).thenReturn(0L);

        List<Map<String, Object>> scores = service.scoreAll(List.of(froid, avance));
        assertThat(scores).hasSize(2);
        assertThat(scores.get(0).get("soulId")).isEqualTo(soulA);
    }
}
