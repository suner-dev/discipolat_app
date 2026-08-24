package com.discipolat.modules.families.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.NiveauRisque;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class FamilyRiskServiceTest {

    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private FamilyRiskHistoryRepository riskHistoryRepository;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private MemberPresenceRepository presenceRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private com.discipolat.modules.souls.domain.WorkspaceScopeService workspaceScope;

    private FamilyRiskService riskService;
    private UUID familyId;
    private Family family;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        lenient().when(securityUtils.isSuperUser()).thenReturn(true);
        riskService = new FamilyRiskService(
                familyRepository, riskHistoryRepository, soulRepository,
                presenceRepository, alertRepository, securityUtils, workspaceScope);
        familyId = UUID.randomUUID();
        family = Family.builder()
                .id(familyId)
                .nom("Famille Test")
                .niveauRisque(NiveauRisque.NORMAL)
                .build();
    }

    private Soul soul(StatutAme statut, LocalDate dateIntegration) {
        return Soul.builder()
                .id(UUID.randomUUID())
                .nom("Nom")
                .prenom("Prenom")
                .statut(statut)
                .dateIntegration(dateIntegration)
                .faiseurId(UUID.randomUUID())
                .build();
    }

    @Test
    void getRiskAssessment_WithEmptyFamily_ShouldReturnScoreZero() {
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(soulRepository.findAllByFamilleId(familyId)).thenReturn(List.of());
        when(alertRepository.findByFamilleId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        var result = riskService.getRiskAssessment(familyId);

        assertEquals(0, result.get("scoreRisque"));
        assertEquals(NiveauRisque.NORMAL, result.get("niveauSuggere"));
        assertEquals(NiveauRisque.NORMAL, result.get("niveauActuel"));
    }

    @Test
    void getRiskAssessment_WithManyLostSouls_ShouldSuggestRisk() {
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        // 3 âmes perdues + 1 en veille
        when(soulRepository.findAllByFamilleId(familyId)).thenReturn(List.of(
                soul(StatutAme.DECROCHE, LocalDate.now().minusMonths(6)),
                soul(StatutAme.DECROCHE, LocalDate.now().minusMonths(6)),
                soul(StatutAme.DECROCHE, LocalDate.now().minusMonths(6)),
                soul(StatutAme.EN_VEILLE, LocalDate.now().minusMonths(6)),
                soul(StatutAme.ACTIF, LocalDate.now().minusMonths(3))
        ));
        when(presenceRepository.findBySoulIdInOrderBySemaineDesc(anyList())).thenReturn(List.of());
        when(alertRepository.findByFamilleId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        var result = riskService.getRiskAssessment(familyId);

        int score = (Integer) result.get("scoreRisque");
        assertTrue(score >= 30, "Score should be high with 3 lost souls, got " + score);
        assertEquals(3L, result.get("amesPerdues"));
        assertNotEquals(NiveauRisque.NORMAL, result.get("niveauSuggere"));
    }

    @Test
    void getRiskAssessment_WithLowPresence_ShouldAddPoints() {
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(soulRepository.findAllByFamilleId(familyId)).thenReturn(List.of(
                soul(StatutAme.ACTIF, LocalDate.now().minusMonths(2))
        ));
        UUID soulId = soulRepository.findAllByFamilleId(familyId).get(0).getId();
        MemberPresence presence = MemberPresence.builder()
                .userId(UUID.randomUUID())
                .soulId(soulId)
                .semaine(LocalDate.now())
                .presences(Map.of("Dimanche", false, "Étude", false, "Culte", true))
                .build();
        when(presenceRepository.findBySoulIdInOrderBySemaineDesc(anyList()))
                .thenReturn(List.of(presence));
        when(alertRepository.findByFamilleId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        var result = riskService.getRiskAssessment(familyId);

        assertEquals(33.3, ((Number) result.get("tauxPresence")).doubleValue(), 0.1);
        assertTrue((Integer) result.get("scoreRisque") >= 18, "Low presence should add points");
    }

    @Test
    void setNiveauRisque_ShouldPersistAndHistory() {
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.save(any())).thenReturn(family);
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());

        var result = riskService.setNiveauRisque(familyId, NiveauRisque.A_RISQUE, "Test");

        assertEquals(NiveauRisque.A_RISQUE, result.getNiveauRisque());
        verify(riskHistoryRepository).save(any(FamilyRiskHistory.class));
    }

    @Test
    void setNiveauRisque_SameLevel_ShouldNotCreateHistory() {
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        riskService.setNiveauRisque(familyId, NiveauRisque.NORMAL, "Pas de changement");

        verify(riskHistoryRepository, never()).save(any(FamilyRiskHistory.class));
    }

    @Test
    void getRiskAssessment_UnknownFamily_ShouldThrow() {
        when(familyRepository.findById(familyId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> riskService.getRiskAssessment(familyId));
    }
}
