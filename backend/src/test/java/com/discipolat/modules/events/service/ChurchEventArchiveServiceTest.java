package com.discipolat.modules.events.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.service.AuditEventService;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.dresscode.domain.DressCode;
import com.discipolat.modules.dresscode.domain.DressCodeRule;
import com.discipolat.modules.dresscode.domain.DressCodeRepository;
import com.discipolat.modules.dresscode.domain.DressCodeRuleRepository;
import com.discipolat.modules.events.api.ChurchEventArchiveFacet;
import com.discipolat.modules.events.domain.ChurchEventArchive;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventAsset;
import com.discipolat.modules.events.domain.EventAttendance;
import com.discipolat.modules.events.domain.EventDocument;
import com.discipolat.modules.events.domain.EventExpense;
import com.discipolat.modules.events.domain.EventSchedule;
import com.discipolat.modules.events.domain.EventSpace;
import com.discipolat.modules.events.domain.EventTask;
import com.discipolat.modules.events.domain.EventTeam;
import com.discipolat.modules.events.repository.ChurchEventArchiveRepository;
import com.discipolat.modules.events.repository.ChurchEventRepository;
import com.discipolat.modules.events.repository.EventAssetRepository;
import com.discipolat.modules.events.repository.EventAttendanceRepository;
import com.discipolat.modules.events.repository.EventDocumentRepository;
import com.discipolat.modules.events.repository.EventExpenseRepository;
import com.discipolat.modules.events.repository.EventScheduleRepository;
import com.discipolat.modules.events.repository.EventSpaceRepository;
import com.discipolat.modules.events.repository.EventTaskRepository;
import com.discipolat.modules.events.repository.EventTeamRepository;
import com.discipolat.modules.events.repository.LocationRepository;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V272 — tests du parcours « Archives » côté service (section C du contrat
 * {@code TODO_ARCHIVES_V272.md}) : clôture versionnée (v1/v2), snapshot complet,
 * colonne dénormalisée {@code attendanceCount}, audit + outbox, isolation
 * inter-tenant, filtres legacy {@code eventId}/espace (repli snapshot),
 * recherche paginée, facettes, refus du statut ARCHIVED par PUT, filtre
 * {@code status} enfin appliqué.
 *
 * <p>Le verrou pessimiste ({@code findForUpdate}) est vérifié ici au niveau
 * appel ; sa sérialisation réelle est éprouvée par
 * {@code ChurchEventArchiveHttpIntegrationTest} sur la chaîne HTTP complète.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChurchEventArchiveServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SPACE_ID = UUID.randomUUID();
    private static final UUID OTHER_SPACE_ID = UUID.randomUUID();
    private static final UUID ARCHIVE_ID = UUID.randomUUID();

    @InjectMocks private ChurchEventService service;

    @Mock private ChurchEventRepository churchEventRepository;
    @Mock private EventSpaceRepository eventSpaceRepository;
    @Mock private EventTeamRepository eventTeamRepository;
    @Mock private EventTaskRepository eventTaskRepository;
    @Mock private EventAssetRepository eventAssetRepository;
    @Mock private EventExpenseRepository eventExpenseRepository;
    @Mock private EventAttendanceRepository eventAttendanceRepository;
    @Mock private EventDocumentRepository eventDocumentRepository;
    @Mock private EventScheduleRepository eventScheduleRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private PersonRepository personRepository;
    @Mock private AuditEventService auditEventService;
    @Mock private OutboxPublisher outboxPublisher;
    @Mock private ChurchEventArchiveRepository archiveRepository;
    @Mock private DressCodeRepository dressCodeRepository;
    @Mock private DressCodeRuleRepository dressCodeRuleRepository;

    // ========== helpers ==========

    private static Event plannedEvent() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT);
        event.setTitre("Séminaire du 4 mars");
        event.setDescription("Retraite trimestrielle");
        event.setTypeEvenement("REUNION");
        event.setStatut(Event.STATUT_PLANIFIE);
        event.setDateDebut(LocalDateTime.of(2023, 3, 4, 9, 0));
        event.setDateFin(LocalDateTime.of(2023, 3, 4, 17, 0));
        return event;
    }

    /** Bouchonne toutes les lectures d'un événement archivable « riche ». */
    private void stubFullEvent(Event event) {
        lenient().when(churchEventRepository.findForUpdate(EVENT_ID, TENANT))
                .thenReturn(Optional.of(event));
        lenient().when(eventSpaceRepository.findByChurchEventId(EVENT_ID)).thenReturn(List.of(
                EventSpace.builder().tenantId(TENANT).churchEventId(EVENT_ID)
                        .spaceId(SPACE_ID).role("MAIN").build()));
        lenient().when(eventTeamRepository.findByChurchEventId(EVENT_ID)).thenReturn(List.of(
                EventTeam.builder().name("Louange").description("Équipe louange").build()));
        lenient().when(eventTaskRepository.findByTenantIdAndChurchEventIdOrderByDueAtAsc(TENANT, EVENT_ID))
                .thenReturn(List.of(EventTask.builder().title("Sonoriser").status("DONE").build()));
        lenient().when(eventScheduleRepository.findByTenantIdAndChurchEventIdOrderByStartAtAsc(TENANT, EVENT_ID))
                .thenReturn(List.of(EventSchedule.builder().title("Ouverture").build()));
        lenient().when(eventAssetRepository.findByChurchEventId(EVENT_ID)).thenReturn(List.of(
                EventAsset.builder().assetId(UUID.randomUUID()).quantity(2).build()));
        lenient().when(eventExpenseRepository.findByChurchEventId(EVENT_ID)).thenReturn(List.of());
        lenient().when(eventDocumentRepository.findByChurchEventId(EVENT_ID)).thenReturn(List.of());
        lenient().when(eventAttendanceRepository.findByChurchEventId(EVENT_ID))
                .thenReturn(List.of(mock(EventAttendance.class), mock(EventAttendance.class)));
        lenient().when(spaceRepository.findById(SPACE_ID))
                .thenReturn(Optional.of(Space.builder().name("Sanctuaire").build()));

        DressCode dressCode = mock(DressCode.class);
        lenient().when(dressCode.getId()).thenReturn(UUID.randomUUID());
        lenient().when(dressCode.getTitle()).thenReturn("Tenue choeur");
        lenient().when(dressCode.getServiceName()).thenReturn("Culte");
        lenient().when(dressCode.getSpaceId()).thenReturn(SPACE_ID);
        lenient().when(dressCode.getStatus()).thenReturn("ARCHIVED");
        lenient().when(dressCodeRepository.findByTenantIdAndEventId(TENANT, EVENT_ID))
                .thenReturn(List.of(dressCode));
        DressCodeRule rule = mock(DressCodeRule.class);
        lenient().when(rule.getGroupName()).thenReturn("Choeur");
        lenient().when(rule.getDescription()).thenReturn("Robe blanche");
        lenient().when(dressCodeRuleRepository.findByDressCodeId(any())).thenReturn(List.of(rule));
    }

    /** {@code save} d'archive : rend la ligne avec son id (le service l'audit). */
    private void stubArchiveSave() {
        lenient().when(archiveRepository.save(any(ChurchEventArchive.class)))
                .thenAnswer(invocation -> {
                    ChurchEventArchive candidate = invocation.getArgument(0);
                    candidate.setId(ARCHIVE_ID);
                    return candidate;
                });
    }

    // ========== clôture : version, snapshot, attendanceCount, audit/outbox ==========

    @Test
    @DisplayName("Première clôture : v1, snapshot intégral, attendanceCount figé, statut ARCHIVED, audit + outbox")
    void archiveEventFirstClosureCreatesVersionOneWithFullSnapshot() {
        Event event = plannedEvent();
        stubFullEvent(event);
        stubArchiveSave();
        when(archiveRepository.findByEventIdOrderByVersionDesc(EVENT_ID)).thenReturn(List.of());

        ChurchEventArchive saved = service.archiveEvent(TENANT, ACTOR, EVENT_ID);

        ArgumentCaptor<ChurchEventArchive> captor = ArgumentCaptor.forClass(ChurchEventArchive.class);
        verify(archiveRepository).save(captor.capture());
        ChurchEventArchive written = captor.getValue();

        assertThat(written.getVersion()).isEqualTo(1);
        assertThat(written.getAttendanceCount()).isEqualTo(2);
        assertThat(written.getTenantId()).isEqualTo(TENANT);
        assertThat(written.getEventId()).isEqualTo(EVENT_ID);
        assertThat(written.getEventTitle()).isEqualTo("Séminaire du 4 mars");
        assertThat(written.getEventYear()).isEqualTo(2023);
        assertThat(written.getEventMonth()).isEqualTo(3);
        assertThat(written.getSpaceId()).isEqualTo(SPACE_ID);
        assertThat(written.getArchivedBy()).isEqualTo(ACTOR);

        Map<String, Object> snapshot = written.getSnapshotJson();
        assertThat(snapshot)
                .containsEntry("title", "Séminaire du 4 mars")
                .containsEntry("type", "REUNION")
                .containsEntry("attendanceCount", 2)
                .containsEntry("expenses", 0)
                .containsEntry("documents", 0);
        assertThat((List<?>) snapshot.get("teams")).hasSize(1);
        assertThat((List<?>) snapshot.get("tasks")).hasSize(1);
        assertThat((List<?>) snapshot.get("schedule")).hasSize(1);
        assertThat((List<?>) snapshot.get("assets")).hasSize(1);
        List<Map<String, Object>> spaces = (List<Map<String, Object>>) snapshot.get("spaces");
        assertThat(spaces.get(0)).contains(entry("spaceName", "Sanctuaire"), entry("role", "MAIN"));
        List<Map<String, Object>> dressCodes = (List<Map<String, Object>>) snapshot.get("dressCodes");
        assertThat(dressCodes.get(0)).contains(entry("title", "Tenue choeur"));
        assertThat((List<?>) dressCodes.get(0).get("rules")).hasSize(1);

        // Le verrou pessimiste est bien le chemin de lecture (V272).
        verify(churchEventRepository).findForUpdate(EVENT_ID, TENANT);
        // L'événement passe au statut de clôture, posé par le service seul.
        assertThat(event.getStatut()).isEqualTo(Event.STATUT_ARCHIVE);
        verify(churchEventRepository).save(event);

        verify(auditEventService).log(eq(TENANT), eq(ACTOR), eq(null), eq("EVENT_ARCHIVED"),
                eq("CHURCH_EVENT"), eq(EVENT_ID), any(Map.class), any(Map.class), eq(null), eq(null));
        verify(outboxPublisher).publish(eq(TENANT), eq("CHURCH_EVENT"), eq(EVENT_ID),
                eq("EventArchived"), any(Map.class));
        assertThat(saved.getId()).isEqualTo(ARCHIVE_ID);
    }

    @Test
    @DisplayName("Re-clôture : version incrémentée (max+1), l'archive v1 n'est jamais écrasée")
    void archiveEventReclosureIncrementsVersionWithoutOverwriting() {
        Event event = plannedEvent();
        stubFullEvent(event);
        stubArchiveSave();
        ChurchEventArchive v1 = ChurchEventArchive.builder()
                .tenantId(TENANT).eventId(EVENT_ID).eventTitle("Séminaire du 4 mars")
                .eventStartAt(java.time.OffsetDateTime.parse("2023-03-04T09:00:00Z"))
                .eventYear(2023).eventMonth(3).version(1).attendanceCount(2)
                .snapshotJson(new LinkedHashMap<>())
                .build();
        v1.setId(UUID.randomUUID());
        when(archiveRepository.findByEventIdOrderByVersionDesc(EVENT_ID)).thenReturn(List.of(v1));

        service.archiveEvent(TENANT, ACTOR, EVENT_ID);

        ArgumentCaptor<ChurchEventArchive> captor = ArgumentCaptor.forClass(ChurchEventArchive.class);
        verify(archiveRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(2);
        // Immunité : aucune écriture ne touche la ligne v1 (pas de UPDATE).
        assertThat(captor.getValue().getId()).isNotEqualTo(v1.getId());
    }

    @Test
    @DisplayName("Clôture d'un événement absent ou d'un autre tenant : EntityNotFound, rien n'est écrit")
    void archiveEventOnOtherTenantOrMissingEventRefusesWithoutWriting() {
        when(churchEventRepository.findForUpdate(EVENT_ID, OTHER_TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiveEvent(OTHER_TENANT, ACTOR, EVENT_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(archiveRepository, never()).save(any(ChurchEventArchive.class));
        verify(churchEventRepository, never()).save(any(Event.class));
    }

    // ========== consultation : legacy getArchives + search + facets + détail ==========

    private static ChurchEventArchive archive(UUID id, UUID eventId, UUID spaceId, Integer version,
                                              Map<String, Object> snapshot) {
        ChurchEventArchive built = ChurchEventArchive.builder()
                .tenantId(TENANT).eventId(eventId).eventTitle("E" + version)
                .eventStartAt(java.time.OffsetDateTime.parse("2023-03-04T09:00:00Z"))
                .eventYear(2023).eventMonth(3).spaceId(spaceId).version(version)
                .attendanceCount(7).snapshotJson(new LinkedHashMap<>(snapshot))
                .build();
        built.setId(id);
        return built;
    }

    @Test
    @DisplayName("getArchives : le filtre eventId (V272) restreint la liste aux versions de l'é événement")
    void getArchivesFiltersByVersionedEvent() {
        UUID otherEvent = UUID.randomUUID();
        when(archiveRepository.findByTenantIdAndEventYearAndEventMonthOrderByEventStartAtDesc(TENANT, 2023, 3))
                .thenReturn(List.of(
                        archive(UUID.randomUUID(), EVENT_ID, null, 2, Map.of()),
                        archive(UUID.randomUUID(), EVENT_ID, null, 1, Map.of()),
                        archive(UUID.randomUUID(), otherEvent, null, 1, Map.of())));

        List<ChurchEventArchive> result = service.getArchives(TENANT, 2023, 3, null, EVENT_ID);

        assertThat(result).hasSize(2)
                .allSatisfy(a -> assertThat(a.getEventId()).isEqualTo(EVENT_ID));
        verify(archiveRepository)
                .findByTenantIdAndEventYearAndEventMonthOrderByEventStartAtDesc(TENANT, 2023, 3);
    }

    @Test
    @DisplayName("getArchives : filtre espace avec repli sur les espaces secondaires du snapshot")
    void getArchivesSpaceFilterFallsBackOnSnapshot() {
        ChurchEventArchive primary = archive(UUID.randomUUID(), EVENT_ID, SPACE_ID, 1, Map.of());
        ChurchEventArchive secondaryOnly = archive(UUID.randomUUID(), EVENT_ID, null, 1, Map.of(
                "spaces", List.of(Map.of("spaceId", OTHER_SPACE_ID.toString(), "spaceName", "Annexe"))));
        ChurchEventArchive foreign = archive(UUID.randomUUID(), EVENT_ID, UUID.randomUUID(), 1, Map.of(
                "spaces", List.of(Map.of("spaceId", UUID.randomUUID().toString()))));
        when(archiveRepository.findByTenantIdOrderByEventStartAtDesc(TENANT))
                .thenReturn(List.of(primary, secondaryOnly, foreign));

        List<ChurchEventArchive> result = service.getArchives(TENANT, null, null, OTHER_SPACE_ID);

        assertThat(result).containsExactly(secondaryOnly);

        List<ChurchEventArchive> byPrimary = service.getArchives(TENANT, null, null, SPACE_ID);
        assertThat(byPrimary).containsExactly(primary);
    }

    @Test
    @DisplayName("searchArchives : borne paginée déléguée au dépôt avec la portée tenant")
    void searchArchivesDelegatesScopedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(archiveRepository.searchSummaries(TENANT, 2023, 3, SPACE_ID, EVENT_ID, pageable))
                .thenReturn(Page.empty(pageable));

        assertThat(service.searchArchives(TENANT, 2023, 3, SPACE_ID, EVENT_ID, pageable)).isEmpty();
        verify(archiveRepository).searchSummaries(TENANT, 2023, 3, SPACE_ID, EVENT_ID, pageable);
    }

    @Test
    @DisplayName("getArchiveFacets : années triées DESC, mois ASC, comptes agrégés par année")
    void getArchiveFacetsGroupsMonthsByYear() {
        when(archiveRepository.findYearMonthFacets(TENANT)).thenReturn(List.of(
                new Object[]{2024, 3, 1L},
                new Object[]{2024, 1, 2L},
                new Object[]{2023, 12, 5L}));

        List<ChurchEventArchiveFacet> facets = service.getArchiveFacets(TENANT);

        assertThat(facets).hasSize(2);
        assertThat(facets.get(0).eventYear()).isEqualTo(2024);
        assertThat(facets.get(0).months()).containsExactly(1, 3);
        assertThat(facets.get(0).archiveCount()).isEqualTo(3L);
        assertThat(facets.get(1).eventYear()).isEqualTo(2023);
        assertThat(facets.get(1).months()).containsExactly(12);
        assertThat(facets.get(1).archiveCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getArchive : détail d'une autre tenant = EntityNotFound (anti-IDOR)")
    void getArchiveRejectsCrossTenantRead() {
        ChurchEventArchive archive = archive(ARCHIVE_ID, EVENT_ID, SPACE_ID, 1, Map.of());
        when(archiveRepository.findById(ARCHIVE_ID)).thenReturn(Optional.of(archive));

        assertThat(service.getArchive(TENANT, ARCHIVE_ID).getId()).isEqualTo(ARCHIVE_ID);
        assertThatThrownBy(() -> service.getArchive(OTHER_TENANT, ARCHIVE_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ========== gardes d'écriture (V272) ==========

    @Test
    @DisplayName("updateChurchEvent : le statut ARCHIVED est refusé — seul POST /archive l'écrit")
    void updateChurchEventRefusesArchivedStatut() {
        Event event = plannedEvent();
        when(churchEventRepository.findByIdAndTenantIdAndDeletedAtIsNull(EVENT_ID, TENANT))
                .thenReturn(Optional.of(event));
        Event patch = new Event();
        patch.setStatut(Event.STATUT_ARCHIVE);

        assertThatThrownBy(() -> service.updateChurchEvent(TENANT, ACTOR, EVENT_ID, patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/archive");
        verify(churchEventRepository, never()).save(any(Event.class));
        assertThat(event.getStatut()).isEqualTo(Event.STATUT_PLANIFIE);
    }

    @Test
    @DisplayName("updateChurchEvent : les autres statuts restent autorisés (comportement historique)")
    void updateChurchEventStillAcceptsOtherStatuses() {
        Event event = plannedEvent();
        when(churchEventRepository.findByIdAndTenantIdAndDeletedAtIsNull(EVENT_ID, TENANT))
                .thenReturn(Optional.of(event));
        Event patch = new Event();
        patch.setStatut(Event.STATUT_TERMINE);
        when(churchEventRepository.save(event)).thenReturn(event);

        service.updateChurchEvent(TENANT, ACTOR, EVENT_ID, patch);

        assertThat(event.getStatut()).isEqualTo(Event.STATUT_TERMINE);
    }

    @Test
    @DisplayName("getChurchEvents : filtre status appliqué (trim) ; absent/vide = liste historique")
    void getChurchEventsAppliesStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(churchEventRepository.findByTenantIdAndDeletedAtIsNull(TENANT, pageable))
                .thenReturn(Page.empty(pageable));
        when(churchEventRepository.findByTenantIdAndStatutAndDeletedAtIsNull(TENANT, "PLANIFIE", pageable))
                .thenReturn(Page.empty(pageable));

        service.getChurchEvents(TENANT, null, pageable);
        service.getChurchEvents(TENANT, "   ", pageable);
        service.getChurchEvents(TENANT, " PLANIFIE ", pageable);

        verify(churchEventRepository, org.mockito.Mockito.times(2))
                .findByTenantIdAndDeletedAtIsNull(TENANT, pageable);
        verify(churchEventRepository).findByTenantIdAndStatutAndDeletedAtIsNull(TENANT, "PLANIFIE", pageable);
    }
}
