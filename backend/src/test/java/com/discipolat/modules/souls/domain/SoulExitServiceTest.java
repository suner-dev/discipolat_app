package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
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
class SoulExitServiceTest {

    @Mock
    private SoulExitRepository soulExitRepository;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SecurityUtils securityUtils;

    private SoulExitService soulExitService;
    private UUID userId;
    private UUID ameId;
    private Soul testSoul;

    @BeforeEach
    void setUp() {
        soulExitService = new SoulExitService(soulExitRepository, soulRepository, securityUtils);
        userId = UUID.randomUUID();
        ameId = UUID.randomUUID();
        testSoul = Soul.builder()
                .id(ameId)
                .nom("Test")
                .prenom("User")
                .statut(StatutAme.ACTIF)
                .build();
    }

    @Test
    void markAsExited_ShouldUpdateSoulStatusAndCreateExitRecord() {
        when(soulRepository.findById(ameId)).thenReturn(Optional.of(testSoul));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(soulExitRepository.save(any(SoulExit.class))).thenAnswer(i -> i.getArgument(0));

        SoulExit result = soulExitService.markAsExited(ameId, "ABANDON", "Ne répond plus", true);

        assertNotNull(result);
        assertEquals("ABANDON", result.getMotif());
        assertEquals(ameId, result.getAmeId());
        assertEquals(userId, result.getFaiseurId());
        verify(soulRepository).save(testSoul);
        assertEquals(StatutAme.DECROCHE, testSoul.getStatut());
    }

    @Test
    void markAsExited_WithNonExistentSoul_ShouldThrowException() {
        when(soulRepository.findById(ameId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () ->
                soulExitService.markAsExited(ameId, "ABANDON", null, true));
    }

    @Test
    void reintegrate_ShouldRestoreSoulStatus() {
        testSoul.setStatut(StatutAme.DECROCHE);
        when(soulRepository.findById(ameId)).thenReturn(Optional.of(testSoul));
        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        Soul result = soulExitService.reintegrate(ameId, StatutAme.EN_INTEGRATION);

        assertEquals(StatutAme.EN_INTEGRATION, result.getStatut());
    }
}
