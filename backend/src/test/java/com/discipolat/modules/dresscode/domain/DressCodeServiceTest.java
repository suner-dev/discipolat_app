package com.discipolat.modules.dresscode.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du service métier DressCode (§G3.4).
 *
 * Couvre : listage filtré, lecture cloisonnée par tenant, création (avec
 * règles), mise à jour, archivage, suppression, lecture des règles et
 * comptage. La publication (outbox + notifications) n'est déclenchée que pour
 * un dress code PUBLISHED.
 */
@ExtendWith(MockitoExtension.class)
class DressCodeServiceTest {

    @Mock
    private DressCodeRepository dressCodeRepository;

    @Mock
    private DressCodeRuleRepository ruleRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    @Mock
    private SpaceMembershipRepository spaceMembershipRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private DressCodeService service;

    private UUID tenantId;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        service = new DressCodeService(
                dressCodeRepository, ruleRepository, outboxPublisher,
                spaceMembershipRepository, personRepository, userRepository,
                notificationService);
        tenantId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        com.discipolat.common.multitenancy.TenantContext.setTenantId(tenantId);
        SecurityTestHelper.loginAs(creatorId);
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.logout();
        com.discipolat.common.multitenancy.TenantContext.clear();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private DressCode dressCode(String title, String status) {
        DressCode dc = new DressCode();
        dc.setId(UUID.randomUUID());
        dc.setTenantId(tenantId);
        dc.setTitle(title);
        dc.setServiceName("Accueil");
        dc.setStatus(status);
        return dc;
    }

    private DressCodeRule rule(String groupName) {
        DressCodeRule r = new DressCodeRule();
        r.setId(UUID.randomUUID());
        r.setGroupName(groupName);
        r.setDescription("Description " + groupName);
        return r;
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    void findAll_delegueAuRepositoryAvecLesFiltresSpaceEtEvent() {
        DressCode dc = dressCode("Tenue du dimanche", "PUBLISHED");
        when(dressCodeRepository.findFiltered(eq(tenantId), any(), any()))
                .thenReturn(List.of(dc));

        List<DressCode> result = service.findAll(tenantId, null, null);

        assertThat(result).containsExactly(dc);
        verify(dressCodeRepository).findFiltered(eq(tenantId), any(), any());
    }

    @Test
    void findAll_transmetLEspaceEtLEvenement() {
        UUID spaceId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(dressCodeRepository.findFiltered(eq(tenantId), eq(spaceId), eq(eventId)))
                .thenReturn(List.of());

        List<DressCode> result = service.findAll(tenantId, spaceId, eventId);

        assertThat(result).isEmpty();
        verify(dressCodeRepository).findFiltered(tenantId, spaceId, eventId);
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    void findById_retourneLeDressCodeDuTenant() {
        DressCode dc = dressCode("Tenue du dimanche", "PUBLISHED");
        when(dressCodeRepository.findById(dc.getId())).thenReturn(Optional.of(dc));

        DressCode result = service.findById(tenantId, dc.getId());

        assertThat(result).isSameAs(dc);
    }

    @Test
    void findById_lanceEntityNotFoundExceptionSiInexistant() {
        UUID id = UUID.randomUUID();
        when(dressCodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(tenantId, id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("DressCode");
    }

    @Test
    void findById_refuseUnDressCodeDUnAutreTenant() {
        DressCode dc = dressCode("Tenue privée", "PUBLISHED");
        dc.setTenantId(UUID.randomUUID()); // autre tenant
        when(dressCodeRepository.findById(dc.getId())).thenReturn(Optional.of(dc));

        assertThatThrownBy(() -> service.findById(tenantId, dc.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── create ─────────────────────────────────────────────────────────────

    @Test
    void create_affecteTenantEtCreateurEtSauvegarde() {
        DressCode dc = new DressCode();
        dc.setTitle("Tenue du dimanche");
        dc.setServiceName("Accueil");
        dc.setStatus("DRAFT");

        when(dressCodeRepository.save(any(DressCode.class)))
                .thenAnswer(inv -> {
                    DressCode d = inv.getArgument(0);
                    if (d.getId() == null) d.setId(UUID.randomUUID());
                    return d;
                });

        DressCode saved = service.create(tenantId, creatorId, dc, null);

        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getCreatedBy()).isEqualTo(creatorId);
        assertThat(saved.isArchived()).isFalse();
        assertThat(saved.getTitle()).isEqualTo("Tenue du dimanche");
        verify(dressCodeRepository).save(dc);
    }

    @Test
    void create_enregistreLesReglesLieesAuDressCode() {
        DressCode dc = new DressCode();
        dc.setTitle("Tenue de Noël");
        dc.setStatus("DRAFT");

        when(dressCodeRepository.save(any(DressCode.class)))
                .thenAnswer(inv -> {
                    DressCode d = inv.getArgument(0);
                    if (d.getId() == null) d.setId(UUID.randomUUID());
                    return d;
                });

        DressCodeRule r1 = rule("Hommes");
        DressCodeRule r2 = rule("Femmes");

        DressCode saved = service.create(tenantId, creatorId, dc, List.of(r1, r2));

        assertThat(r1.getDressCodeId()).isEqualTo(saved.getId());
        assertThat(r2.getDressCodeId()).isEqualTo(saved.getId());
        verify(ruleRepository).save(r1);
        verify(ruleRepository).save(r2);
    }

    @Test
    void create_nePubliePasSurUnDressCodeDraft() {
        DressCode dc = new DressCode();
        dc.setTitle("Tenue brouillon");
        dc.setStatus("DRAFT");

        when(dressCodeRepository.save(any(DressCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.create(tenantId, creatorId, dc, null);

        verifyNoInteractions(outboxPublisher);
        verifyNoInteractions(notificationService);
    }

    @Test
    void create_publieDansLOutboxUnDressCodePublie() {
        DressCode dc = new DressCode();
        dc.setTitle("Tenue publiée");
        dc.setStatus("PUBLISHED");

        when(dressCodeRepository.save(any(DressCode.class)))
                .thenAnswer(inv -> {
                    DressCode d = inv.getArgument(0);
                    if (d.getId() == null) d.setId(UUID.randomUUID());
                    return d;
                });

        service.create(tenantId, creatorId, dc, null);

        verify(outboxPublisher).publish(eq(tenantId), eq("DRESS_CODE"),
                eq(dc.getId()), eq("DressCodePublished"), any());
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    void update_metAJourLesChampsEtRemplaceLesRegles() {
        DressCode existing = dressCode("Ancienne tenue", "DRAFT");
        when(dressCodeRepository.findById(existing.getId()))
                .thenReturn(Optional.of(existing));
        when(dressCodeRepository.save(any(DressCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DressCode patch = new DressCode();
        patch.setTitle("Nouvelle tenue");
        patch.setServiceName("Messe");
        patch.setStatus("PUBLISHED");

        DressCodeRule newRule = rule("Protocole");

        DressCode result = service.update(tenantId, existing.getId(), patch, List.of(newRule));

        assertThat(result.getTitle()).isEqualTo("Nouvelle tenue");
        assertThat(result.getServiceName()).isEqualTo("Messe");
        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        // Les règles précédentes sont remplacées.
        verify(ruleRepository).deleteByDressCodeId(existing.getId());
        verify(ruleRepository).save(newRule);
        assertThat(newRule.getDressCodeId()).isEqualTo(existing.getId());
    }

    @Test
    void update_lanceEntityNotFoundExceptionSiInexistant() {
        UUID id = UUID.randomUUID();
        when(dressCodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(tenantId, id, new DressCode(), null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── archive / delete ───────────────────────────────────────────────────

    @Test
    void archive_marqueLeDressCodeCommeArchived() {
        DressCode dc = dressCode("Tenue ancienne", "PUBLISHED");
        when(dressCodeRepository.findById(dc.getId())).thenReturn(Optional.of(dc));

        service.archive(tenantId, dc.getId());

        assertThat(dc.isArchived()).isTrue();
        verify(dressCodeRepository).save(dc);
    }

    @Test
    void delete_supprimeLeDressCodeEtSesRegles() {
        DressCode dc = dressCode("Tenue à supprimer", "DRAFT");
        when(dressCodeRepository.findById(dc.getId())).thenReturn(Optional.of(dc));

        service.delete(tenantId, dc.getId());

        verify(ruleRepository).deleteByDressCodeId(dc.getId());
        verify(dressCodeRepository).delete(dc);
    }

    // ── getRules / count ───────────────────────────────────────────────────

    @Test
    void getRules_retourneLesReglesApresValidationDAccess() {
        DressCode dc = dressCode("Tenue du dimanche", "PUBLISHED");
        when(dressCodeRepository.findById(dc.getId())).thenReturn(Optional.of(dc));
        DressCodeRule r = rule("Chœurs");
        when(ruleRepository.findByDressCodeId(dc.getId())).thenReturn(List.of(r));

        List<DressCodeRule> result = service.getRules(tenantId, dc.getId());

        assertThat(result).containsExactly(r);
        verify(dressCodeRepository).findById(dc.getId());
    }

    @Test
    void getRules_refuseUnDressCodeDUnAutreTenant() {
        DressCode dc = dressCode("Tenue privée", "PUBLISHED");
        dc.setTenantId(UUID.randomUUID());
        when(dressCodeRepository.findById(dc.getId())).thenReturn(Optional.of(dc));

        assertThatThrownBy(() -> service.getRules(tenantId, dc.getId()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(ruleRepository, never()).findByDressCodeId(any());
    }

    @Test
    void count_retourneLeNombreDeDressCodesNonArchives() {
        when(dressCodeRepository.countByTenantIdAndArchivedFalse(tenantId)).thenReturn(42L);

        long result = service.count(tenantId);

        assertThat(result).isEqualTo(42L);
    }
}
