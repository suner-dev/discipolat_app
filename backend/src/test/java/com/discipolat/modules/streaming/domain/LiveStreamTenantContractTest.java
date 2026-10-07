package com.discipolat.modules.streaming.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Contrat multi-tenant V240 du module streaming : le tenant vient
 * exclusivement du contexte authentifié (TenantContext / JWT), jamais du
 * client. Verrouille trois invariants de sécurité :
 * <ol>
 *   <li>create : tenantId/créateur forcés serveur, id client ignoré
 *       (l'ancien @RequestBody LiveStream laissait la main au client) ;</li>
 *   <li>toute opération par id (go-live/end/viewer/get/chat.send) passe
 *       par findByIdAndTenantId — un id hors tenant est un 404, et
 *       aucune écriture n'a lieu ;</li>
 *   <li>lecture/count du chat scopés tenant (countByStreamIdAndTenantId,
 *       jamais countByStreamId brut).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class LiveStreamTenantContractTest {

    @Mock private LiveStreamRepository liveStreamRepository;
    @Mock private StreamChatMessageRepository chatRepository;

    @InjectMocks private LiveStreamService liveStreamService;
    @InjectMocks private StreamChatMessageService chatService;

    private final UUID tenant = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenant);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private LiveStream stream(Long id, UUID tenantId) {
        LiveStream s = new LiveStream();
        s.setId(id);
        s.setTenantId(tenantId);
        s.setTitle("Culte");
        s.setStatus(LiveStream.StreamStatus.SCHEDULED);
        return s;
    }

    // ========== create : forcage serveur ==========

    @Test
    void create_forcesTenantAndCreator_andDropsClientId_evenWhenBodyForgesThem() {
        LiveStream body = stream(999L, UUID.randomUUID()); // client forge id + tenant
        body.setCreatedBy(UUID.randomUUID());
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<LiveStream> captor = ArgumentCaptor.forClass(LiveStream.class);
        LiveStream saved = liveStreamService.create(body, actor);

        verify(liveStreamRepository).save(captor.capture());
        assertNull(captor.getValue().getId(), "l'id fourni par le client doit être ignoré (BIGSERIAL serveur)");
        assertEquals(tenant, saved.getTenantId(), "le tenant vient du contexte, jamais du corps");
        assertEquals(actor, saved.getCreatedBy());
        assertEquals(LiveStream.StreamStatus.SCHEDULED, saved.getStatus());
        assertEquals(0, saved.getViewerCount());
    }

    @Test
    void create_sansContexteTenant_refuseEcriture() {
        TenantContext.clear();
        assertThrows(IllegalStateException.class, () -> liveStreamService.create(stream(null, null), actor));
        verify(liveStreamRepository, never()).save(any());
    }

    // ========== opérations par id : anti-IDOR ==========

    @Test
    void goLive_crossTenant_introuvableEtAucuneEcriture() {
        Long foreignId = 42L;
        when(liveStreamRepository.findByIdAndTenantId(foreignId, tenant)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> liveStreamService.goLive(tenant, foreignId));
        verify(liveStreamRepository, never()).save(any());
    }

    @Test
    void endStream_crossTenant_introuvableEtAucuneEcriture() {
        when(liveStreamRepository.findByIdAndTenantId(42L, tenant)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> liveStreamService.endStream(tenant, 42L));
        verify(liveStreamRepository, never()).save(any());
    }

    @Test
    void incrementViewers_crossTenant_introuvableEtAucuneEcriture() {
        when(liveStreamRepository.findByIdAndTenantId(42L, tenant)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> liveStreamService.incrementViewers(tenant, 42L));
        verify(liveStreamRepository, never()).save(any());
    }

    @Test
    void goLive_memeTenant_passeParRequeteScopee() {
        LiveStream own = stream(7L, tenant);
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(own));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveStream result = liveStreamService.goLive(tenant, 7L);

        assertEquals(LiveStream.StreamStatus.LIVE, result.getStatus());
        assertNotNull(result.getStartedAt());
        verify(liveStreamRepository).findByIdAndTenantId(7L, tenant);
        verify(liveStreamRepository, never()).findById(any());
    }

    @Test
    void get_retourneVueScopee_404Sinon() {
        LiveStream own = stream(7L, tenant);
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(own));
        assertEquals(own, liveStreamService.get(tenant, 7L));

        when(liveStreamRepository.findByIdAndTenantId(8L, tenant)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> liveStreamService.get(tenant, 8L));
    }

    // ========== update/delete (V240) : scopés, champs inertes ==========

    @Test
    void update_crossTenant_introuvableEtAucuneEcriture() {
        when(liveStreamRepository.findByIdAndTenantId(42L, tenant)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> liveStreamService.update(tenant, 42L, stream(42L, UUID.randomUUID())));
        verify(liveStreamRepository, never()).save(any());
    }

    @Test
    void update_nappliqueQueLesChampsEditables_idTenantCreateurInertés() {
        LiveStream own = stream(7L, tenant);
        own.setCreatedBy(actor);
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(own));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(inv -> inv.getArgument(0));

        LiveStream forged = stream(999L, UUID.randomUUID()); // corps client malveillant
        forged.setCreatedBy(UUID.randomUUID());
        forged.setTitle("Nouveau titre");

        LiveStream result = liveStreamService.update(tenant, 7L, forged);

        assertEquals(7L, result.getId());
        assertEquals(tenant, result.getTenantId());
        assertEquals(actor, result.getCreatedBy());
        assertEquals("Nouveau titre", result.getTitle());
    }

    @Test
    void update_titreVide_rejet400() {
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(stream(7L, tenant)));
        LiveStream blank = stream(7L, tenant);
        blank.setTitle("  ");

        assertThrows(IllegalArgumentException.class, () -> liveStreamService.update(tenant, 7L, blank));
        verify(liveStreamRepository, never()).save(any());
    }

    @Test
    void delete_crossTenant_introuvableEtAucuneSuppression() {
        when(liveStreamRepository.findByIdAndTenantId(42L, tenant)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> liveStreamService.delete(tenant, 42L));
        verify(liveStreamRepository, never()).delete(any());
    }

    @Test
    void delete_refuseStreamEnDirect() {
        LiveStream live = stream(7L, tenant);
        live.setStatus(LiveStream.StreamStatus.LIVE);
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(live));

        assertThrows(IllegalArgumentException.class, () -> liveStreamService.delete(tenant, 7L));
        verify(liveStreamRepository, never()).delete(any());
    }

    @Test
    void delete_streamTermine_supprimeViaRequeteScopee() {
        LiveStream ended = stream(7L, tenant);
        ended.setStatus(LiveStream.StreamStatus.ENDED);
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(ended));

        liveStreamService.delete(tenant, 7L);

        verify(liveStreamRepository).delete(ended);
    }

    // ========== listes : uniquement par tenant scopé ==========

    @Test
    void listByTenant_et_listLive_utilisentRequetesScopees() {
        when(liveStreamRepository.findByTenantIdOrderByScheduledAtDesc(tenant)).thenReturn(List.of());
        when(liveStreamRepository.findByTenantIdAndStatus(tenant, LiveStream.StreamStatus.LIVE)).thenReturn(List.of());

        assertEquals(List.of(), liveStreamService.listByTenant(tenant));
        assertEquals(List.of(), liveStreamService.listLive(tenant));
        verify(liveStreamRepository).findByTenantIdOrderByScheduledAtDesc(tenant);
        verify(liveStreamRepository).findByTenantIdAndStatus(tenant, LiveStream.StreamStatus.LIVE);
        verify(liveStreamRepository, never()).findAll();
    }

    // ========== chat : lecture/count/send scopés ==========

    @Test
    void chatSend_refuseStreamHorsTenant_aucuneInsertion() {
        when(liveStreamRepository.findByIdAndTenantId(42L, tenant)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatService.send(42L, actor, "Paul", "Bienvenue", null));
        verify(chatRepository, never()).save(any());
    }

    @Test
    void chatSend_surStreamDuTenant_forceTenantContexte() {
        when(liveStreamRepository.findByIdAndTenantId(7L, tenant)).thenReturn(Optional.of(stream(7L, tenant)));
        when(chatRepository.save(any(StreamChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        StreamChatMessage msg = chatService.send(7L, actor, "Paul", "Bienvenue", null);

        assertEquals(tenant, msg.getTenantId(), "le tenant du message vient du contexte, jamais du client");
        assertEquals(7L, msg.getStreamId());
        assertEquals("TEXT", msg.getMessageType());
    }

    @Test
    void chatCount_utiliseRequeteScopeeTenant() {
        when(chatRepository.countByStreamIdAndTenantId(7L, tenant)).thenReturn(3L);

        assertEquals(3L, chatService.countByStream(7L));
        verify(chatRepository).countByStreamIdAndTenantId(7L, tenant);
    }

    @Test
    void chatList_sansContexteTenant_refuseLecture() {
        TenantContext.clear();
        assertThrows(IllegalStateException.class, () -> chatService.listByStream(7L));
        verifyNoInteractions(chatRepository);
    }
}
