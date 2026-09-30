package com.discipolat.modules.events.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationListener;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.files.domain.EntityAttachmentRepository;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
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
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

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
    @Mock
    private com.discipolat.modules.audit.domain.AuditService auditService;
    @Mock
    private EntityPropagationPublisher propagationPublisher;
    @Mock
    private EntityPropagationListener propagationListener;

    private EventService eventService;
    private EntityAttachmentService attachmentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID familleId = UUID.randomUUID();
    private final UUID autreFamilleId = UUID.randomUUID();

    @Mock private com.discipolat.modules.tenants.domain.QuotaService quotaService;

    private static final UUID TEST_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Event evenementFamille;
    private Event evenementEglise;

    @AfterEach
    void tearDown() {
        com.discipolat.common.multitenancy.TenantContext.clear();
    }

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        // Le controle de quota (constat M3) resout le tenant via l'evenement puis
        // via le contexte de requete : les evenements de ce test ne portent pas
        // `tenantId` (auto-rempli a la persistance en conditions reelles).
        com.discipolat.common.multitenancy.TenantContext.setTenantId(TEST_TENANT_ID);
        attachmentService = new EntityAttachmentService(attachmentRepository, fileEntityRepository, securityUtils);
        eventService = new EventService(eventRepository, registrationRepository, templateRepository,
                userRepository, notificationService, securityUtils, workspaceScope, attachmentService, auditService,
                quotaService, propagationPublisher, propagationListener);

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
        when(eventRepository.findByStatutAndDeletedAtIsNull("PLANIFIE", PageRequest.of(0, 20)))
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
        verify(eventRepository, never()).findByFamilleIdAndDeletedAtIsNull(any(UUID.class), any(Pageable.class));
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
        SecurityTestHelper.loginAs(userId);
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

    // ======================== ÉVÉNEMENTS DE DÉPARTEMENT ========================

    private final UUID departmentId = UUID.randomUUID();
    private final UUID autreDepartmentId = UUID.randomUUID();

    @Test
    void findById_evenementDeSonDepartement_autorise() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessDepartment(departmentId)).thenReturn(true);
        Event evenementDept = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(userId)
                .departmentId(departmentId)
                .titre("Convention du département")
                .dateDebut(LocalDateTime.now().plusDays(5))
                .build();
        when(eventRepository.findById(evenementDept.getId())).thenReturn(Optional.of(evenementDept));

        assertEquals(evenementDept.getId(), eventService.findById(evenementDept.getId()).getId());
    }

    @Test
    void findById_evenementDunAutreDepartement_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessDepartment(autreDepartmentId)).thenReturn(false);
        Event evenementAutreDept = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(UUID.randomUUID())
                .departmentId(autreDepartmentId)
                .titre("Événement d'un autre département")
                .dateDebut(LocalDateTime.now().plusDays(2))
                .build();
        when(eventRepository.findById(evenementAutreDept.getId())).thenReturn(Optional.of(evenementAutreDept));

        assertThrows(AccessDeniedException.class, () -> eventService.findById(evenementAutreDept.getId()));
    }

    @Test
    void findByDepartmentId_departementHorsEspace_pageVide() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessDepartment(autreDepartmentId)).thenReturn(false);

        Page<Event> result = eventService.findByDepartmentId(autreDepartmentId, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
        verify(eventRepository, never()).findByDepartmentIdAndDeletedAtIsNull(any(UUID.class), any(Pageable.class));
    }

    @Test
    void findByDepartmentId_departementGere_retourneLesEvenements() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessDepartment(departmentId)).thenReturn(true);
        Event e1 = Event.builder().id(UUID.randomUUID()).organisateurId(userId)
                .departmentId(departmentId).titre("Répétition")
                .dateDebut(LocalDateTime.now().plusDays(1)).build();
        when(eventRepository.findByDepartmentIdAndDeletedAtIsNull(departmentId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(e1)));

        Page<Event> result = eventService.findByDepartmentId(departmentId, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(e1.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findAll_filtreLesEvenementsDesAutresDepartements() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        Event evenementAutreDept = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(UUID.randomUUID())
                .departmentId(autreDepartmentId)
                .titre("Événement d'un autre département")
                .dateDebut(LocalDateTime.now().plusDays(2))
                .build();
        when(eventRepository.findByStatutAndDeletedAtIsNull("PLANIFIE", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(evenementEglise, evenementAutreDept)));
        when(workspaceScope.accessibleFamilyIds()).thenReturn(java.util.Set.of());
        when(workspaceScope.accessibleDepartmentIds()).thenReturn(java.util.Set.of(departmentId));

        Page<Event> result = eventService.findAll(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(evenementEglise.getId(), result.getContent().get(0).getId());
    }

    @Test
    void create_departementHorsEspace_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessDepartment(autreDepartmentId)).thenReturn(false);

        Event nouveau = Event.builder()
                .departmentId(autreDepartmentId)
                .titre("Événement interdit")
                .dateDebut(LocalDateTime.now().plusDays(2))
                .build();

        assertThrows(AccessDeniedException.class, () -> eventService.create(nouveau, null));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_departementGere_autorise() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessDepartment(departmentId)).thenReturn(true);
        SecurityTestHelper.loginAs(userId);
        User orgUser = User.builder().id(userId).firstName("Test").lastName("User")
                .role(com.discipolat.common.domain.UserRole.MEMBRE)
                .roles(java.util.Set.of(com.discipolat.common.domain.UserRole.MEMBRE))
                .statut(com.discipolat.modules.users.domain.UserStatus.ACTIVE).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(orgUser));
        when(userRepository.findByRole(com.discipolat.common.domain.UserRole.PASTEUR)).thenReturn(List.of());
        Event nouveau = Event.builder()
                .departmentId(departmentId)
                .titre("Convention 2026")
                .dateDebut(LocalDateTime.now().plusDays(10))
                .build();
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event saved = eventService.create(nouveau, null);

        assertEquals("PLANIFIE", saved.getStatut());
        verify(eventRepository).save(nouveau);
    }

    // ==================================================================
    // Options d'evenement (V200) — elles doivent etre APPLIQUEES, pas decoratives
    // ==================================================================

    @Test
    void register_refuse_quandLesInscriptionsNeSontPasOuvertes() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        Event ouvert = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(UUID.randomUUID())
                .titre("Evenement libre")
                .dateDebut(LocalDateTime.now().plusDays(1))
                .requiresRegistration(Boolean.FALSE)
                .build();
        when(eventRepository.findById(ouvert.getId())).thenReturn(Optional.of(ouvert));

        // Fail-closed : `null` est traite comme « non ouvert ».
        IllegalStateException refus = assertThrows(IllegalStateException.class,
                () -> eventService.register(ouvert.getId()));
        assertTrue(refus.getMessage().contains("inscriptions"));
    }

    @Test
    void register_accepte_quandLesInscriptionsSontOuvertes() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        // Pas de `requiresRegistration` explicite : on verifie que le DEFAUT
        // preserve le comportement d'avant V200 (inscription ouverte).
        Event ouvert = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(UUID.randomUUID())
                .titre("Conference")
                .dateDebut(LocalDateTime.now().plusDays(1))
                .build();
        when(eventRepository.findById(ouvert.getId())).thenReturn(Optional.of(ouvert));
        when(registrationRepository.findByEventIdAndUtilisateurId(
                any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
        when(registrationRepository.save(any(EventRegistration.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EventRegistration registration = eventService.register(ouvert.getId());

        assertEquals(ouvert.getId(), registration.getEventId());
    }

    @Test
    void markAttendance_refuse_quandLePointageEstDesactive() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        Event sansPointage = Event.builder()
                .id(UUID.randomUUID())
                .organisateurId(UUID.randomUUID())
                .titre("Evenement sans pointage")
                .dateDebut(LocalDateTime.now())
                .checkinEnabled(Boolean.FALSE)
                .build();
        when(eventRepository.findById(sansPointage.getId())).thenReturn(Optional.of(sansPointage));

        assertThrows(IllegalStateException.class,
                () -> eventService.markAttendance(sansPointage.getId(), userId, true));
    }

    @Test
    void update_appliqueLesOptionsSansEcraserLesAbsentes() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        Event existant = Event.builder()
                .id(UUID.randomUUID())
                // L'organisateur doit etre l'utilisateur connecte : c'est la
                // regle `canManageEvent` qui autorise la modification.
                .organisateurId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .titre("Evenement")
                .dateDebut(LocalDateTime.now().plusDays(1))
                .visibility("PUBLIC")
                .requiresRegistration(Boolean.TRUE)
                .checkinEnabled(Boolean.FALSE)
                .tags(new String[]{"priere"})
                .build();
        when(eventRepository.findById(existant.getId())).thenReturn(Optional.of(existant));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        // Patch partiel : seuls `imageUrl` et `isPublic` sont fournis.
        // On passe explicitement `null` sur les absents, comme le fait
        // l'EventController en traduisant `UpdateEventRequest` : un composant
        // absent y vaut `null`. (Construire le patch par le builder SANS
        // rappeler les setters poserait les @Builder.Default, ce qui
        // n'est pas un patch mais un remplacement.)
        Event patch = Event.builder()
                .id(existant.getId())
                .titre(null)
                .description(null)
                .lieu(null)
                .dateDebut(null)
                .dateFin(null)
                .limitePlaces(null)
                .typeEvenement(null)
                .statut(null)
                .compteRendu(null)
                .imageUrl("https://files.example/couverture.jpg")
                .tags(null)
                .visibility("CHURCH")
                .requiresRegistration(null)
                .checkinEnabled(null)
                .streamId(null)
                .build();

        Event saved = eventService.update(patch.getId(), patch, null);

        assertEquals("https://files.example/couverture.jpg", saved.getImageUrl());
        assertEquals(Boolean.FALSE, saved.getPublicEvent());
        // Non fournis -> conserves
        assertEquals(Boolean.TRUE, saved.getRequiresRegistration());
        assertEquals(Boolean.FALSE, saved.getCheckinEnabled());
        assertArrayEquals(new String[]{"priere"}, saved.getTags());
    }

    @Test
    void create_conserveLesOptionsRecues() {
        Event nouveau = Event.builder()
                .organisateurId(userId)
                .titre("Evenement avec options")
                .dateDebut(LocalDateTime.now().plusDays(2))
                .imageUrl("https://files.example/v.png")
                .tags(new String[]{"retraite", "jeunesse"})
                .visibility("PUBLIC")
                .requiresRegistration(Boolean.TRUE)
                .checkinEnabled(Boolean.TRUE)
                .build();
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event saved = eventService.create(nouveau, null);

        assertEquals(Boolean.TRUE, saved.getPublicEvent());
        assertEquals(Boolean.TRUE, saved.getRequiresRegistration());
        assertEquals(Boolean.TRUE, saved.getCheckinEnabled());
        assertArrayEquals(new String[]{"retraite", "jeunesse"}, saved.getTags());
    }
}
