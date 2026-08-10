package com.discipolat.modules.events.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.files.domain.EntityAttachmentRepository;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Isolation des espaces métiers : les événements liés à une famille ne sont
 * visibles / modifiables que dans l'espace du rôle actif. Les événements d'église
 * (sans famille) restent visibles par tous ; seuls l'organisateur, la famille
 * gérée ou les super-utilisateurs peuvent les modifier.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventRegistrationRepository registrationRepository;
    @Mock
    private WeeklyProgramTemplateRepository templateRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private WorkspaceScopeService workspaceScope;
    @Mock
    private EntityAttachmentRepository attachmentRepository;
    @Mock
    private FileEntityRepository fileEntityRepository;

    private EventService eventService;
    private EntityAttachmentService attachmentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID familleId = UUID.randomUUID();
    private final UUID autreFamilleId = UUID.randomUUID();

    private Event evenementFamille;
    private Event evenementEglise;

    @BeforeEach
    void setUp() {
        attachmentService = new EntityAttachmentService(attachmentRepository, fileEntityRepository, securityUtils);
        eventService = new EventService(eventRepository, registrationRepository, templateRepository,
                userRepository, notificationService, securityUtils, workspaceScope, attachmentService);

        evenementFamille = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(userId)
                .familleId(familleId)
                .titre("Retraite de la famille")
                .dateDebut(LocalDateTime.now().plusDays(3))
                .build();

        evenementEglise = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(UUID.randomUUID())
                .familleId(null)
                .titre("Culte général")
                .dateDebut(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void findById_evenementEglise_visibleParTous() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(eventRepository.findById(evenementEglise.getId())).thenReturn(Optional.of(evenementEglise));

        assertEquals(evenementEglise.getId(), eventService.findById(evenementEglise.getId()).getId());
    }

    @Test
    void findById_familleHorsEspace_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(familleId)).thenReturn(false);
        when(eventRepository.findById(evenementFamille.getId())).thenReturn(Optional.of(evenementFamille));

        assertThrows(AccessDeniedException.class, () -> eventService.findById(evenementFamille.getId()));
    }

    @Test
    void findById_familleVisible_autorise() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(familleId)).thenReturn(true);
        when(eventRepository.findById(evenementFamille.getId())).thenReturn(Optional.of(evenementFamille));

        assertEquals(evenementFamille.getId(), eventService.findById(evenementFamille.getId()).getId());
    }

    @Test
    void findAll_faiseurActif_filtreLesFamillesHorsEspace() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(eventRepository.findByStatutAndDeletedFalse("PLANIFIE", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(evenementEglise, evenementFamille)));
        // Le faiseur voit l'événement d'église mais pas celui d'une famille hors espace
        when(workspaceScope.accessibleFamilyIds()).thenReturn(java.util.Set.of());

        Page<Event> result = eventService.findAll(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(evenementEglise.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findByFamilleId_familleHorsEspace_pageVide() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(autreFamilleId)).thenReturn(false);

        Page<Event> result = eventService.findByFamilleId(autreFamilleId, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
        verify(eventRepository, never()).findByFamilleIdAndDeletedFalse(any(UUID.class), any(Pageable.class));
    }

    @Test
    void update_evenementNonGerable_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(familleId)).thenReturn(false);
        when(eventRepository.findById(evenementFamille.getId())).thenReturn(Optional.of(evenementFamille));

        Event updated = Event.builder().titre("Nouveau titre").build();
        assertThrows(AccessDeniedException.class, () -> eventService.update(evenementFamille.getId(), updated, null));
    }

    @Test
    void delete_evenementOrganiseParSoi_autorise() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        // L'organisateur gère l'événement : visibilité famille OK (sa famille) + organisateur == user
        when(workspaceScope.canAccessFamily(familleId)).thenReturn(true);
        when(eventRepository.findById(evenementFamille.getId())).thenReturn(Optional.of(evenementFamille));

        eventService.delete(evenementFamille.getId());

        assertTrue(evenementFamille.isDeleted());
        verify(eventRepository).save(evenementFamille);
    }

    @Test
    void create_familleHorsEspace_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(autreFamilleId)).thenReturn(false);

        Event nouveau = Event.builder()
                .familleId(autreFamilleId)
                .titre("Événement interdit")
                .dateDebut(LocalDateTime.now().plusDays(2))
                .build();

        assertThrows(AccessDeniedException.class, () -> eventService.create(nouveau, null));
        verify(eventRepository, never()).save(any(Event.class));
    }
}
