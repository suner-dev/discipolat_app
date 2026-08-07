package com.discipolat.modules.ai.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.interactions.domain.InteractionRepository;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SpiritualScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock private SoulRepository soulRepository;
    @Mock private MakerReportRepository makerReportRepository;
    @Mock private InteractionRepository interactionRepository;
    @Mock private PrayerRepository prayerRepository;
    @Mock private SpiritualScoreService spiritualScoreService;
    @Mock private com.discipolat.modules.souls.domain.SoulService soulService;

    @InjectMocks private AiAssistantService aiAssistantService;

    private UUID soulId;
    private Soul activeSoul;
    private Soul decrocheSoul;

    @BeforeEach
    void setUp() {
        soulId = UUID.randomUUID();
        activeSoul = Soul.builder()
                .id(soulId)
                .nom("Mukendi")
                .prenom("Jean")
                .statut(StatutAme.ACTIF)
                .dateIntegration(LocalDate.now().minusMonths(6))
                .dateDernierContact(LocalDateTime.now().minusDays(2))
                .build();
        decrocheSoul = Soul.builder()
                .id(soulId)
                .nom("Kabongo")
                .prenom("Paul")
                .statut(StatutAme.DECROCHE)
                .dateIntegration(LocalDate.now().minusYears(1))
                .dateDernierContact(LocalDateTime.now().minusDays(40))
                .build();

        Map<String, Object> score = Map.of(
                "global", 78, "sante", 80, "fidelite", 75,
                "engagement", 70, "participation", 85,
                "label", "Progression solide", "semaine", LocalDate.now().toString());

        lenient().when(spiritualScoreService.computeScore(soulId)).thenReturn(score);
        lenient().when(makerReportRepository.findByAmeIdAndSemaine(any(), any())).thenReturn(List.of());
        lenient().when(interactionRepository.countBySoulId(soulId)).thenReturn(3L);
        lenient().when(prayerRepository.findByAmeIdAndDeletedFalse(soulId)).thenReturn(List.of());
    }

    @Test
    void analyze_returnsCompleteAnalysis_forActiveSoul() {
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(activeSoul));

        Map<String, Object> result = aiAssistantService.analyze(soulId);

        assertEquals(soulId, result.get("soulId"));
        assertEquals("Jean Mukendi", result.get("nom"));
        assertNotNull(result.get("score"));
        assertNotNull(result.get("signaux"));
        assertNotNull(result.get("suggestions"));
        assertNotNull(result.get("encouragement"));
        assertNotNull(result.get("resume"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> signaux = (List<Map<String, Object>>) result.get("signaux");
        // Aucun signal attendu : statut ACTIF + contact récent + pas d'absences
        assertTrue(signaux.isEmpty(), "Une âme active avec contact récent ne devrait avoir aucun signal");
    }

    @Test
    void analyze_detectsDecrochage_asCriticalSignal() {
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(decrocheSoul));

        Map<String, Object> result = aiAssistantService.analyze(soulId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> signaux = (List<Map<String, Object>>) result.get("signaux");

        boolean hasCritical = signaux.stream()
                .anyMatch(s -> "CRITIQUE".equals(s.get("severite")) && "Décrochage".equals(s.get("type")));
        assertTrue(hasCritical, "Une âme décrochée doit produire un signal CRITIQUE 'Décrochage'");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) result.get("suggestions");
        assertFalse(suggestions.isEmpty(), "Un signal critique doit déclencher des suggestions d'action");
    }

    @Test
    void encouragement_returnsTextAndSource() {
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(activeSoul));

        Map<String, String> result = aiAssistantService.encouragement(soulId);

        assertNotNull(result.get("texte"));
        assertFalse(result.get("texte").isBlank());
        assertNotNull(result.get("source"));
    }

    @Test
    void analyze_throwsWhenSoulNotFound() {
        when(soulRepository.findById(soulId)).thenReturn(Optional.empty());

        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> aiAssistantService.analyze(soulId));
    }
}
