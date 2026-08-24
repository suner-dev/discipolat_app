package com.discipolat.modules.health;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.health.domain.SpiritualHealthService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpiritualHealthServiceTest {

    @Mock private SoulRepository soulRepository;

    private SpiritualHealthService service;
    private final UUID familleId = UUID.randomUUID();

    private Soul soul(StatutAme statut, LocalDateTime dernierContact, String etat) {
        return Soul.builder()
                .id(UUID.randomUUID())
                .nom("Kabongo").prenom("Jean")
                .statut(statut)
                .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI)
                .dateDernierContact(dernierContact)
                .etatSpirituel(etat)
                .niveauCroissance(3)
                .familleId(familleId)
                .build();
    }

    @BeforeEach
    void setUp() {
        service = new SpiritualHealthService(soulRepository, null, null);
    }

    @Test
    void risqueCritique_pourAmeDecrochee() {
        assertThat(service.riskScore(soul(StatutAme.DECROCHE, LocalDateTime.now(), "OK"))).isEqualTo(100);
    }

    @Test
    void risqueEleve_sansContactDepuis30Jours() {
        int risk = service.riskScore(soul(StatutAme.ACTIF, LocalDateTime.now().minusDays(30), "OK"));
        assertThat(risk).isEqualTo(35); // statut ACTIF 0 + 30j sans contact
    }

    @Test
    void risqueFaible_contactRecentEtActif() {
        int risk = service.riskScore(soul(StatutAme.ACTIF, LocalDateTime.now().minusDays(1), "STABLE"));
        assertThat(risk).isLessThanOrEqualTo(10);
    }

    @Test
    void observatoire_agregFamillesEtDistribution() {
        when(soulRepository.findByDeletedFalse()).thenReturn(List.of(
                soul(StatutAme.DECROCHE, LocalDateTime.now(), "OK"),
                soul(StatutAme.EN_VEILLE, LocalDateTime.now().minusDays(40), "EN_DIFFICULTE"),
                soul(StatutAme.ACTIF, LocalDateTime.now().minusDays(1), "STABLE")));

        Map<String, Object> obs = service.observatory();
        assertThat(obs.get("totalSouls")).isEqualTo(3);
        assertThat((int) obs.get("criticalCount")).isGreaterThanOrEqualTo(1);
        assertThat(obs.get("predictionHorizon")).isEqualTo("2-3 semaines");

        List<Map<String, Object>> atRisk = (List<Map<String, Object>>) obs.get("soulsAtRisk");
        // Les deux premières âmes ont un risque ≥ 50
        assertThat(atRisk.size()).isGreaterThanOrEqualTo(2);
        List<Map<String, Object>> familles = (List<Map<String, Object>>) obs.get("familiesAtRisk");
        assertThat(familles.size()).isGreaterThanOrEqualTo(1);
    }
}
