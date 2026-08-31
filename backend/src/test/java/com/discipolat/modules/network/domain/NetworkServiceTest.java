package com.discipolat.modules.network.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NetworkService — Réseau inter-églises")
class NetworkServiceTest {

    @Mock private NetworkResourceRepository resourceRepository;
    @Mock private NetworkEventRepository eventRepository;
    @Mock private NetworkEventParticipantRepository participantRepository;
    @Mock private NetworkDirectoryRepository directoryRepository;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private NetworkService service;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        SecurityTestHelper.loginAs(userId, "PASTEUR");
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.logout();
        TenantContext.clear();
    }

    // ======================== RESSOURCES ========================

    @Test
    @DisplayName("listSharedResources — retourne les ressources publiques")
    void listSharedResources() {
        NetworkResource r = new NetworkResource();
        r.setId(UUID.randomUUID());
        r.setTitle("Guide de discipolat");
        r.setSharedWithPublic(true);
        when(resourceRepository.findBySharedWithPublicTrueAndIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(r));

        List<NetworkResource> result = service.listSharedResources();

        assertEquals(1, result.size());
        assertEquals("Guide de discipolat", result.get(0).getTitle());
    }

    @Test
    @DisplayName("createResource — crée une ressource avec tenant et user courants")
    void createResource() {
        NetworkResource r = new NetworkResource();
        r.setTitle("Nouveau template");
        r.setCategory("TEMPLATE");
        when(resourceRepository.save(any(NetworkResource.class))).thenAnswer(inv -> {
            NetworkResource saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        NetworkResource result = service.createResource(r);

        assertEquals(tenantId, result.getTenantId());
        assertEquals(userId, result.getSharedByUserId());
        assertTrue(result.getSharedWithPublic());
        assertEquals(0, result.getDownloads());
        assertTrue(result.getIsActive());
        verify(resourceRepository).save(any());
    }

    @Test
    @DisplayName("incrementDownloads — incrémente le compteur")
    void incrementDownloads() {
        NetworkResource r = new NetworkResource();
        r.setId(UUID.randomUUID());
        r.setTenantId(tenantId);
        r.setSharedWithPublic(true);
        r.setDownloads(5);
        when(resourceRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NetworkResource result = service.incrementDownloads(r.getId());

        assertEquals(6, result.getDownloads());
    }

    @Test
    @DisplayName("deactivateResource — vérifie le tenant avant suppression")
    void deactivateResource_tenantGuard() {
        NetworkResource r = new NetworkResource();
        r.setId(UUID.randomUUID());
        r.setTenantId(UUID.randomUUID()); // Autre tenant
        when(resourceRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThrows(SecurityException.class, () -> service.deactivateResource(r.getId()));
    }

    // ======================== ÉVÉNEMENTS ========================

    @Test
    @DisplayName("createEvent — crée un événement avec tenant et user courants")
    void createEvent() {
        NetworkEvent e = new NetworkEvent();
        e.setTitle("Conférence nationale");
        e.setStartsAt(LocalDateTime.now().plusDays(30));
        when(eventRepository.save(any(NetworkEvent.class))).thenAnswer(inv -> {
            NetworkEvent saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        NetworkEvent result = service.createEvent(e);

        assertEquals(tenantId, result.getTenantId());
        assertEquals(userId, result.getCreatedByUserId());
        assertTrue(result.getSharedWithPublic());
        assertEquals(0, result.getCurrentParticipants());
    }

    @Test
    @DisplayName("joinEvent — incrémente les participants et marque joinedByMe")
    void joinEvent() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSharedWithPublic(true);
        e.setCurrentParticipants(10);
        e.setMaxParticipants(50);
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.existsByEventIdAndUserId(e.getId(), userId)).thenReturn(false);

        NetworkEvent result = service.joinEvent(e.getId());

        assertEquals(11, result.getCurrentParticipants());
        assertTrue(result.getJoinedByMe());
        verify(participantRepository).save(any(NetworkEventParticipant.class));
    }

    @Test
    @DisplayName("joinEvent — idempotent : déjà inscrit ne re-décrémente pas le compteur")
    void joinEvent_alreadyJoined() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSharedWithPublic(true);
        e.setCurrentParticipants(11);
        e.setMaxParticipants(50);
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        when(participantRepository.existsByEventIdAndUserId(e.getId(), userId)).thenReturn(true);

        NetworkEvent result = service.joinEvent(e.getId());

        assertEquals(11, result.getCurrentParticipants());
        assertTrue(result.getJoinedByMe());
        verify(participantRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("joinEvent — rejette si complet")
    void joinEvent_full() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSharedWithPublic(true);
        e.setCurrentParticipants(50);
        e.setMaxParticipants(50);
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));

        assertThrows(IllegalStateException.class, () -> service.joinEvent(e.getId()));
    }

    @Test
    @DisplayName("leaveEvent — décrémente les participants (RSVP réel)")
    void leaveEvent() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSharedWithPublic(true);
        e.setCurrentParticipants(10);
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.existsByEventIdAndUserId(e.getId(), userId)).thenReturn(true);

        NetworkEvent result = service.leaveEvent(e.getId());

        assertEquals(9, result.getCurrentParticipants());
        assertFalse(result.getJoinedByMe());
        verify(participantRepository).deleteByEventIdAndUserId(e.getId(), userId);
    }

    @Test
    @DisplayName("leaveEvent — idempotent : non inscrit ne décrémente pas le compteur")
    void leaveEvent_notJoined() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSharedWithPublic(true);
        e.setCurrentParticipants(7);
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        when(participantRepository.existsByEventIdAndUserId(e.getId(), userId)).thenReturn(false);

        NetworkEvent result = service.leaveEvent(e.getId());

        assertEquals(7, result.getCurrentParticipants());
        assertFalse(result.getJoinedByMe());
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("leaveEvent — ne descend pas sous 0")
    void leaveEvent_minZero() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setSharedWithPublic(true);
        e.setCurrentParticipants(0);
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepository.existsByEventIdAndUserId(e.getId(), userId)).thenReturn(true);

        NetworkEvent result = service.leaveEvent(e.getId());

        assertEquals(0, result.getCurrentParticipants());
    }

    @Test
    @DisplayName("listUpcomingEvents — marque joinedByMe pour l'utilisateur courant")
    void listUpcomingEvents_marksJoinedByMe() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setSharedWithPublic(true);
        NetworkEventParticipant p = new NetworkEventParticipant();
        p.setEventId(e.getId());
        p.setUserId(userId);
        when(eventRepository.findBySharedWithPublicTrueAndIsActiveTrueAndStartsAtAfterOrderByStartsAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(e));
        when(participantRepository.findByEventIdInAndUserId(List.of(e.getId()), userId))
                .thenReturn(List.of(p));

        List<NetworkEvent> result = service.listUpcomingEvents();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getJoinedByMe());
    }

    @Test
    @DisplayName("deactivateEvent — vérifie le tenant")
    void deactivateEvent_tenantGuard() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(UUID.randomUUID());
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));

        assertThrows(SecurityException.class, () -> service.deactivateEvent(e.getId()));
    }

    // ======================== ANTI-IDOR (données privées inter-tenant) ========================

    @Test
    @DisplayName("incrementDownloads — refuse une ressource PRIVÉE d'une autre église")
    void incrementDownloads_privateCrossTenant() {
        NetworkResource r = new NetworkResource();
        r.setId(UUID.randomUUID());
        r.setTenantId(UUID.randomUUID()); // Autre église
        r.setSharedWithPublic(false);     // Non partagée
        when(resourceRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThrows(SecurityException.class, () -> service.incrementDownloads(r.getId()));
    }

    @Test
    @DisplayName("joinEvent — refuse un événement PRIVÉ d'une autre église")
    void joinEvent_privateCrossTenant() {
        NetworkEvent e = new NetworkEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(UUID.randomUUID()); // Autre église
        e.setSharedWithPublic(false);     // Non partagé
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));

        assertThrows(SecurityException.class, () -> service.joinEvent(e.getId()));
    }

    // ======================== RÉPERTOIRE ========================

    @Test
    @DisplayName("toggleListing — active le listing")
    void toggleListing_on() {
        NetworkDirectory d = new NetworkDirectory();
        d.setTenantId(tenantId);
        d.setChurchName("Église de la Grâce");
        d.setIsListed(false);
        when(directoryRepository.findByTenantId(tenantId)).thenReturn(Optional.of(d));
        when(directoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NetworkDirectory result = service.toggleListing(true);

        assertTrue(result.getIsListed());
        assertNotNull(result.getListedAt());
    }

    @Test
    @DisplayName("toggleListing — désactive le listing")
    void toggleListing_off() {
        NetworkDirectory d = new NetworkDirectory();
        d.setTenantId(tenantId);
        d.setIsListed(true);
        when(directoryRepository.findByTenantId(tenantId)).thenReturn(Optional.of(d));
        when(directoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NetworkDirectory result = service.toggleListing(false);

        assertFalse(result.getIsListed());
    }

    @Test
    @DisplayName("toggleListing — refuse le listing sans nom d'église")
    void toggleListing_requiresChurchName() {
        NetworkDirectory d = new NetworkDirectory();
        d.setTenantId(tenantId);
        d.setChurchName(null); // Pas de nom
        when(directoryRepository.findByTenantId(tenantId)).thenReturn(Optional.of(d));

        assertThrows(IllegalStateException.class, () -> service.toggleListing(true));
    }

    @Test
    @DisplayName("getOrCreateMyDirectoryEntry — crée si inexistant")
    void getOrCreateMyDirectoryEntry_create() {
        when(directoryRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(directoryRepository.save(any(NetworkDirectory.class))).thenAnswer(inv -> {
            NetworkDirectory d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        NetworkDirectory result = service.getOrCreateMyDirectoryEntry();

        assertEquals(tenantId, result.getTenantId());
        assertFalse(result.getIsListed());
    }

    @Test
    @DisplayName("updateMyDirectoryEntry — met à jour les champs fournis")
    void updateMyDirectoryEntry() {
        NetworkDirectory existing = new NetworkDirectory();
        existing.setTenantId(tenantId);
        existing.setChurchName("Ancien nom");
        when(directoryRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(directoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NetworkDirectory updates = new NetworkDirectory();
        updates.setChurchName("Nouveau nom");
        updates.setCity("Abidjan");

        NetworkDirectory result = service.updateMyDirectoryEntry(updates);

        assertEquals("Nouveau nom", result.getChurchName());
        assertEquals("Abidjan", result.getCity());
    }

    // ======================== STATISTIQUES ========================

    @Test
    @DisplayName("getNetworkStats — retourne un résumé complet")
    void getNetworkStats() {
        when(resourceRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(5L);
        when(eventRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(3L);
        when(directoryRepository.countByIsListedTrue()).thenReturn(12L);

        Map<String, Object> stats = service.getNetworkStats();

        assertEquals(5L, stats.get("totalSharedResources"));
        assertEquals(3L, stats.get("totalPublicEvents"));
        assertEquals(12L, stats.get("totalListedChurches"));
        assertEquals(5L, stats.get("myResources"));
        assertEquals(3L, stats.get("myEvents"));
    }
}
