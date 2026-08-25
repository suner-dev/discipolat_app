package com.discipolat.modules.parallelfollowups.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.RaisonSuiviParallele;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.parallelfollowups.api.CreateParallelFollowupRequest;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class ParallelFollowupServiceTest {

    @Mock
    private ParallelFollowupRepository repository;
    @Mock
    private WorkspaceScopeService workspaceScopeService;
    @Mock
    private SecurityUtils securityUtils;

    private ParallelFollowupService service;
    private UUID currentUserId;
    private UUID ameId;
    private UUID autreAmeId;
    private ParallelFollowup followup;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new ParallelFollowupService(repository, workspaceScopeService, securityUtils);
        currentUserId = UUID.randomUUID();
        ameId = UUID.randomUUID();
        autreAmeId = UUID.randomUUID();
        followup = ParallelFollowup.builder()
                .id(UUID.randomUUID())
                .ameId(ameId)
                .initiateurId(currentUserId)
                .raison(RaisonSuiviParallele.VISITE)
                .dateDebut(LocalDate.now())
                .statut(StatutSuiviParallele.EN_COURS)
                .build();
    }

    private CreateParallelFollowupRequest request(UUID initiateurId) {
        return new CreateParallelFollowupRequest(ameId, initiateurId, null,
                RaisonSuiviParallele.VISITE, "detail", LocalDate.now());
    }

    @Test
    void create_ForInaccessibleSoul_ShouldThrowAccessDenied() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessSoul(ameId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.create(request(currentUserId)));

        verify(repository, never()).save(any());
    }

    @Test
    void create_NonSuperUser_ShouldForceInitiateurToCurrentUser() {
        UUID spoofedId = UUID.randomUUID();
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessSoul(ameId)).thenReturn(true);
        SecurityTestHelper.loginAs(currentUserId);
        when(repository.save(any(ParallelFollowup.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.create(request(spoofedId));

        verify(repository).save(argThat(f -> currentUserId.equals(f.getInitiateurId())));
    }

    @Test
    void create_SuperUser_ShouldHonourRequestedInitiateur() {
        UUID initiatorId = UUID.randomUUID();
        when(workspaceScopeService.isSuperUser()).thenReturn(true);
        when(repository.save(any(ParallelFollowup.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.create(request(initiatorId));

        verify(repository).save(argThat(f -> initiatorId.equals(f.getInitiateurId())));
    }

    @Test
    void findById_NotOwn_NotAccessible_ShouldThrowAccessDenied() {
        ParallelFollowup foreign = ParallelFollowup.builder()
                .id(UUID.randomUUID())
                .ameId(autreAmeId)
                .initiateurId(UUID.randomUUID())
                .raison(RaisonSuiviParallele.AUTRE)
                .dateDebut(LocalDate.now())
                .statut(StatutSuiviParallele.EN_COURS)
                .build();
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        SecurityTestHelper.loginAs(currentUserId);
        when(workspaceScopeService.canAccessSoul(autreAmeId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.findById(foreign.getId()));
    }

    @Test
    void findById_Own_ShouldReturn() {
        when(repository.findById(followup.getId())).thenReturn(Optional.of(followup));
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        SecurityTestHelper.loginAs(currentUserId);

        ParallelFollowup result = service.findById(followup.getId());

        assertEquals(followup.getId(), result.getId());
    }

    @Test
    void findById_Unknown_ShouldThrowEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void findAll_NonSuperUser_ShouldFilterOutForeignFollowups() {
        ParallelFollowup foreign = ParallelFollowup.builder()
                .id(UUID.randomUUID())
                .ameId(autreAmeId)
                .initiateurId(UUID.randomUUID())
                .raison(RaisonSuiviParallele.AUTRE)
                .dateDebut(LocalDate.now())
                .statut(StatutSuiviParallele.EN_COURS)
                .build();
        var pageable = PageRequest.of(0, 20);
        when(repository.findByStatut(StatutSuiviParallele.EN_COURS, pageable))
                .thenReturn(new PageImpl<>(List.of(followup, foreign), pageable, 2));
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        SecurityTestHelper.loginAs(currentUserId);
        when(workspaceScopeService.canAccessSoul(autreAmeId)).thenReturn(false);

        var result = service.findActive(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(followup.getId(), result.getContent().get(0).getId());
    }
}
