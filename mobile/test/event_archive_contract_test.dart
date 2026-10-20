// Contrat du parcours « Archives » (V272) avec le backend réel.
//
// Fige trois choses vérifiées par lecture du backend (ChurchEventController,
// ChurchEventArchive, ChurchEventArchiveSummary, PageResponse) :
//  1. les CHEMINS : les archives n'existent que sous /church-events (la table
//     `event` est commune aux deux API, les ids sont interchangeables) ;
//  2. les FORMES : page normalisée {content,page,size,totalElements,totalPages},
//     résumés SANS snapshotJson, facets {eventYear,months[],archiveCount},
//     détail avec snapshotJson Map et attendanceCount (colonne V272) ;
//  3. le vocabulaire de statut : `ARCHIVED` (clôture serveur) doit se décoder
//     en EventStatus.archived — pas retomber sur « inconnu ».
//
// Convention du dépôt : un faux `ApiService` injecté, aucune dépendance ajoutée.

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/events/models/event_archive_model.dart';
import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:flutter_test/flutter_test.dart';

const _uuid = '11111111-1111-1111-1111-111111111111';
const _archive = '3f2504e0-4f89-11d3-9a0c-0305e82c3301';
const _space = 'aaaaaaaa-0000-0000-0000-000000000001';

class _FakeApi extends ApiService {
  _FakeApi(this.responses) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> responses;
  final List<String> calls = [];
  final List<dynamic> bodies = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    calls.add(
        'GET $path${queryParameters != null ? ' $queryParameters' : ''}');
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: responses[path],
    );
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    calls.add('POST $path');
    bodies.add(data);
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: responses[path],
    );
  }
}

/// Faux qui refuse toute écriture : prouve qu'aucun succès n'est fabriqué.
class _ThrowingApi extends ApiService {
  _ThrowingApi() : super(baseUrl: 'http://fake');

  @override
  Future<Response> post(String path, {dynamic data}) async => throw DioException(
      requestOptions: RequestOptions(path: path),
      type: DioExceptionType.badResponse);
}

/// Payload EXACT d'un `ChurchEventArchiveSummary` sérialisé par Jackson.
Map<String, dynamic> backendSummaryJson({
  String id = _archive,
  int version = 1,
  int attendanceCount = 7,
}) =>
    {
      'id': id,
      'eventId': _uuid,
      'eventTitle': 'Culte de célébration',
      'eventStartAt': '2023-03-04T09:00:00Z',
      'eventYear': 2023,
      'eventMonth': 3,
      'spaceId': _space,
      'version': version,
      'attendanceCount': attendanceCount,
      'archivedAt': '2023-03-05T18:30:00Z',
      'archivedBy': '22222222-2222-2222-2222-222222222222',
    };

void main() {
  group('Modèles : tolérance et typage UUID (jamais de cast int)', () {
    test('résumé : ids chaînes, dates parsées, attendanceCount lu', () {
      final s = EventArchiveSummary.fromJson(backendSummaryJson());
      expect(s.id, _archive);
      expect(s.id, isA<String>());
      expect(s.eventId, _uuid);
      expect(s.spaceId, _space);
      expect(s.version, 1);
      expect(s.attendanceCount, 7);
      expect(s.eventStartAt!.year, 2023);
      expect(s.archivedAt!.month, 3);
    });

    test('résumé presque vide : null partout, aucune exception', () {
      final s = EventArchiveSummary.fromJson(const {'id': _archive});
      expect(s.id, _archive);
      expect(s.eventTitle, '');
      expect(s.eventYear, isNull);
      expect(s.attendanceCount, isNull);
      expect(s.archivedAt, isNull);
    });

    test('page : la forme PageResponse du serveur se lit intégralement', () {
      final p = EventArchivePage.fromJson({
        'content': [
          backendSummaryJson(),
          backendSummaryJson(id: 'b2', version: 2, attendanceCount: 12),
        ],
        'page': 0,
        'size': 12,
        'totalElements': 34,
        'totalPages': 3,
      });
      expect(p.content.length, 2);
      expect(p.content[1].version, 2);
      expect(p.totalElements, 34);
      expect(p.totalPages, 3);
      expect(p.hasPrevious, isFalse);
      expect(p.hasNext, isTrue);
    });

    test('page : content absent ⇒ liste vide, jamais une exception', () {
      final p = EventArchivePage.fromJson(const {'page': 0});
      expect(p.content, isEmpty);
      expect(p.hasNext, isFalse);
    });

    test('facets : months tolérant (valeurs hors 1..12 écartées)', () {
      final f = EventArchiveFacet.fromJson(const {
        'eventYear': 2023,
        'months': [3, 5, 13, 'x'],
        'archiveCount': 2,
      });
      expect(f.eventYear, 2023);
      expect(f.months, [3, 5]);
      expect(f.archiveCount, 2);
    });

    test('détail : snapshotJson est la Map serveur, absente ⇒ {}', () {
      final a = EventArchive.fromJson({
        ...backendSummaryJson(),
        'tenantId': _space,
        'snapshotJson': {
          'titre': 'Culte de célébration',
          'equipes': [1, 2, 3],
        },
      });
      expect(a.snapshotJson['titre'], 'Culte de célébration');
      expect((a.snapshotJson['equipes'] as List).length, 3);
      final bare = EventArchive.fromJson(const {'id': _archive});
      expect(bare.snapshotJson, isEmpty);
    });
  });

  group('Service : chemins et paramètres réels du contrat V272', () {
    test('getArchives paginate sur /church-events/archives/search (pas /events)',
        () async {
      final api = _FakeApi({
        '/church-events/archives/search': {
          'content': [backendSummaryJson()],
          'page': 1,
          'size': 12,
          'totalElements': 13,
          'totalPages': 2,
        },
      });
      final page = await EventsService(api)
          .getArchives(page: 1, year: 2023, month: 3, eventId: _uuid);
      expect(
        api.calls.single,
        'GET /church-events/archives/search '
        '{page: 1, size: 12, year: 2023, month: 3, eventId: $_uuid}',
      );
      expect(page.content.single.id, _archive);
      expect(page.hasNext, isFalse);
    });

    test('getArchives sans filtre n\'envoie que page/size (compat legacy)',
        () async {
      final api = _FakeApi({
        '/church-events/archives/search': {
          'content': <dynamic>[],
          'page': 0,
          'size': 12,
          'totalElements': 0,
          'totalPages': 0,
        },
      });
      await EventsService(api).getArchives();
      expect(api.calls.single,
          'GET /church-events/archives/search {page: 0, size: 12}');
    });

    test('getArchive : GET /church-events/archives/{id}, snapshot inclus',
        () async {
      final api = _FakeApi({
        '/church-events/archives/$_archive': {
          ...backendSummaryJson(version: 2, attendanceCount: 9),
          'snapshotJson': {'dressCodes': []},
        },
      });
      final a = await EventsService(api).getArchive(_archive);
      expect(api.calls.single, 'GET /church-events/archives/$_archive');
      expect(a.version, 2);
      expect(a.attendanceCount, 9);
      expect(a.snapshotJson.containsKey('dressCodes'), isTrue);
    });

    test('getArchiveFacets : liste serveur → List typée, jamais de crash',
        () async {
      final api = _FakeApi({
        '/church-events/archives/facets': [
          {'eventYear': 2023, 'months': [3], 'archiveCount': 1},
          {'eventYear': 2024, 'months': [1, 2], 'archiveCount': 2},
        ],
      });
      final facets = await EventsService(api).getArchiveFacets();
      expect(api.calls.single, 'GET /church-events/archives/facets');
      expect(facets.length, 2);
      expect(facets[1].months, [1, 2]);
    });

    test('archiveEvent : POST /church-events/{eventId}/archive, renvoie la vN',
        () async {
      final api = _FakeApi({
        '/church-events/$_uuid/archive': {
          ...backendSummaryJson(version: 2, attendanceCount: 9),
          'snapshotJson': {'statut': 'ARCHIVED'},
        },
      });
      final a = await EventsService(api).archiveEvent(_uuid);
      expect(api.calls.single, 'POST /church-events/$_uuid/archive');
      expect(a.version, 2);
    });

    test('archiveEvent : un échec serveur n\'est JAMAIS un succès fabriqué',
        () async {
      await expectLater(
        () => EventsService(_ThrowingApi()).archiveEvent(_uuid),
        throwsA(isA<Exception>()),
      );
    });
  });

  group('Statut ARCHIVED (V272) : décodable côté mobile', () {
    Map<String, dynamic> backendEventJson({String statut = 'PLANIFIE'}) => {
          'id': _uuid,
          'organisateurId': '22222222-2222-2222-2222-222222222222',
          'familleId': null,
          'departmentId': null,
          'typeEvenement': 'CULTE',
          'titre': 'Culte de célébration',
          'description': null,
          'lieu': 'Salle 1',
          'dateDebut': '2023-03-04T09:00:00',
          'dateFin': null,
          'limitePlaces': 50,
          'nbInscrits': 3,
          'statut': statut,
          'compteRendu': null,
          'createdAt': '2023-02-01T10:00:00',
          'piecesJointes': <dynamic>[],
        };

    test('un événement clôturé (ARCHIVED) se décode archived, pas unknown',
        () async {
      final api =
          _FakeApi({'/events/$_uuid': backendEventJson(statut: 'ARCHIVED')});
      final event = await EventsService(api).getEvent(_uuid);
      expect(event.statut, EventStatus.archived);
      expect(event.statut.displayName, 'Archivé');
      expect(EventStatusWire.wire(EventStatus.archived), 'ARCHIVED');
    });

    test('aller-retour wire incohérent toléré : valeur inventée ⇒ unknown',
        () {
      expect(EventStatusWire.decode('ARCHIVÉE'), EventStatus.unknown);
      expect(EventStatusWire.decode('archived'), EventStatus.archived);
    });
  });
}
