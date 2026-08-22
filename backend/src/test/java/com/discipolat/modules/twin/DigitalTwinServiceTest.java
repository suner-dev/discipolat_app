package com.discipolat.modules.twin;

import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import com.discipolat.modules.evangelism.domain.EvangelismTrack;
import com.discipolat.modules.evangelism.domain.EvangelismTrackRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.twin.domain.DigitalTwinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalTwinServiceTest {

    @Mock private SoulRepository soulRepository;
    @Mock private EvangelismTrackRepository trackRepository;

    private DigitalTwinService service;
    private final UUID soulId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DigitalTwinService(soulRepository, trackRepository);
        when(soulRepository.countByDeletedFalse()).thenReturn(200L);
        when(soulRepository.countByStatut(com.discipolat.common.enums.StatutAme.ACTIF)).thenReturn(150L);
        when(soulRepository.countByStatut(com.discipolat.common.enums.StatutAme.NOUVEAU_CONVERTI)).thenReturn(20L);
        when(soulRepository.countByStatut(com.discipolat.common.enums.StatutAme.EN_INTEGRATION)).thenReturn(25L);
        when(soulRepository.countByStatut(com.discipolat.common.enums.StatutAme.EN_VEILLE)).thenReturn(4L);
        when(soulRepository.countByStatut(com.discipolat.common.enums.StatutAme.DECROCHE)).thenReturn(1L);
        when(trackRepository.findAll()).thenReturn(List.of(
                EvangelismTrack.builder().soulId(soulId).build()));
        when(trackRepository.findByEtapeOrderByDateEtapeDesc(EvangelismEtape.LEADER)).thenReturn(List.of());
    }

    @Test
    void snapshot_refleteLesStatsReelles() {
        Map<String, Object> snap = service.snapshot();
        assertThat(snap.get("totalSouls")).isEqualTo(200L);
        assertThat(snap.get("pipelinesActifs")).isEqualTo(1L);
    }

    @Test
    void simulate_produitUneProjectionMensuelle() {
        Map<String, Object> result = service.simulate(2.0, 10, 1.5, 12);

        List<Map<String, Object>> projection = (List<Map<String, Object>>) result.get("projection");
        assertThat(projection).hasSize(12);
        assertThat(projection.get(0)).containsKeys("month", "souls", "bestCase", "worstCase");
        // Croissance : dernier mois > baseline 200 avec faiseurs doublés + boost pipeline
        long finalSouls = ((Number) projection.get(11).get("souls")).longValue();
        assertThat(finalSouls).isGreaterThan(200L);
        assertThat(result).containsKeys("currentLeaders", "neededLeaders", "leaderGap");
    }

    @Test
    void simulate_borneLesHypothesesExtravagantes() {
        Map<String, Object> result = service.simulate(99.0, 500, 99.0, 60);
        Map<String, Object> assumptions = (Map<String, Object>) result.get("assumptions");
        assertThat((double) assumptions.get("faiseurMultiplier")).isEqualTo(5.0);
        assertThat((int) assumptions.get("months")).isEqualTo(36);
    }

    @Test
    void leaderGap_calculeSelonLeRatioUnPourHuit() {
        Map<String, Object> result = service.simulate(1.0, 0, 1.0, 6);
        long projected = ((Number) result.get("projectedTotal")).longValue();
        long needed = ((Number) result.get("neededLeaders")).longValue();
        assertThat(needed).isEqualTo(Math.round(projected / 8.0));
    }
}
