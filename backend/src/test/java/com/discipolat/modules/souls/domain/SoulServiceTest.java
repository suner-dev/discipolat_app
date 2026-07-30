package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.api.CreateSoulRequest;
import com.discipolat.modules.souls.api.UpdateSoulRequest;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoulServiceTest {

    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SoulHistoryRepository soulHistoryRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SoulNoteRepository soulNoteRepository;
    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private MakerReportRepository makerReportRepository;
    @Mock
    private EvaluationService evaluationService;
    @Mock
    private NotificationService notificationService;

    private SoulService soulService;

    private UUID faiseurId;
    private UUID familleId;
    private Soul testSoul;

    @BeforeEach
    void setUp() {
        soulService = new SoulService(soulRepository, soulHistoryRepository,
                soulNoteRepository, securityUtils, userRepository,
                familyRepository, makerReportRepository, evaluationService,
                notificationService);

        faiseurId = UUID.randomUUID();
        familleId = UUID.randomUUID();
        testSoul = Soul.builder()
                .id(UUID.randomUUID())
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@email.com")
                .telephone("0123456789")
                .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI)
                .statut(StatutAme.ACTIF)
                .dateIntegration(LocalDate.now())
                .faiseurId(faiseurId)
                .familleId(familleId)
                .etatSpirituel("NOUVEAU_CONVERTI")
                .niveauCroissance(1)
                .build();
    }

    @Test
    void createSoul_WithValidRequest_ShouldReturnSavedSoul() {
        CreateSoulRequest request = new CreateSoulRequest(
                "Dupont", "Marie", "marie@email.com", "0123456789",
                null, null, null, TypeDisciple.NOUVEAU_CONVERTI,
                LocalDate.now(), null, faiseurId, familleId,
                null, null, null
        );

        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        Soul result = soulService.create(request);

        assertNotNull(result);
        assertEquals("Dupont", result.getNom());
        assertEquals("Marie", result.getPrenom());
        assertEquals(TypeDisciple.NOUVEAU_CONVERTI, result.getTypeDisciple());
        verify(soulRepository).save(any(Soul.class));
        verify(soulHistoryRepository).save(any(SoulHistory.class));
    }

    @Test
    void findById_WithExistingId_ShouldReturnSoul() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));

        Soul result = soulService.findById(testSoul.getId());

        assertNotNull(result);
        assertEquals(testSoul.getId(), result.getId());
        assertEquals("Dupont", result.getNom());
    }

    @Test
    void findById_WithNonExistingId_ShouldThrowEntityNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(soulRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                soulService.findById(nonExistentId)
        );
    }

    @Test
    void updateSoul_ShouldModifyFields() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));
        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        UpdateSoulRequest request = new UpdateSoulRequest(
                "Dupont2", null, null, null, null, null, null,
                TypeDisciple.NOUVEL_ARRIVANT, null, null, StatutAme.EN_VEILLE,
                null, null, null, null, null, "Notes du pasteur"
        );

        Soul result = soulService.update(testSoul.getId(), request);

        assertNotNull(result);
        verify(soulRepository).save(any(Soul.class));
        verify(soulHistoryRepository).save(any(SoulHistory.class));
    }

    @Test
    void deleteSoul_ShouldSetDeletedFlag() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));
        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        soulService.delete(testSoul.getId());

        assertTrue(testSoul.isDeleted());
        verify(soulRepository).save(any(Soul.class));
    }
}
