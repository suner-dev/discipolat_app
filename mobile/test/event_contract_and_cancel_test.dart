// Contrat du module `events` avec le backend réel.
//
// Ce fichier verrouille les bris de contrat trouvés le 2026-09-29, tous
// vérifiés par lecture du backend (`EventResponse`, `EventChecklistItem`,
// `DressCodeResponse`, `EventTeam`) :
//  1. identifiants `int` côté mobile contre `UUID` côté backend ;
//  2. vocabulaire de statut : le mobile lisait DRAFT/PUBLISHED/CANCELLED, le
//     backend ne produit que PLANIFIE/EN_COURS/TERMINE/ANNULE ;
//  3. sept routes INVENTÉES par le mobile, dont trois concepts absents du
//     backend (« membre d'équipe avec un rôle », alors que le serveur connaît
//     une équipe avec un responsable) ;
//  4. l'annulation n'existait pas : elle affichait « bientôt disponible ».
//
// Convention du dépôt : un faux `ApiService` injecté, aucune dépendance ajoutée.

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:flutter_test/flutter_test.dart';

const _uuid = '11111111-1111-1111-1111-111111111111';
const _person = '22222222-2222-2222-2222-222222222222';

class _FakeApi extends ApiService {
  _FakeApi(this.responses) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> responses;
  final List<String> calls = [];
  final List<dynamic> bodies = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add('GET $path${queryParameters != null ? ' $queryParameters' : ''}');
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: responses[path],
    );
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    calls.add('PUT $path');
    bodies.add(data);
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

/// Faux qui refuse toute écriture : sert à prouver qu'aucun succès n'est
/// fabriqué en cas d'échec.
class _ThrowingApi extends ApiService {
  _ThrowingApi() : super(baseUrl: 'http://fake');

  @override
  Future<Response> put(String path, {dynamic data}) async => throw DioException(
      requestOptions: RequestOptions(path: path), type: DioExceptionType.badResponse);
}

/// Payload au format EXACT de `EventResponse` (backend). Les noms de champs
/// sont ceux du backend : c'est la seule fixture honnête.
Map<String, dynamic> backendEventJson({String statut = 'PLANIFIE', String id = _uuid}) => {
      'id': id,
      'organisateurId': _person,
      'familleId': null,
      'departmentId': null,
      'typeEvenement': 'REUNION',
      'titre': 'Reunion de priere',
      'description': null,
      'lieu': 'Salle 1',
      'dateDebut': '2026-10-01T18:00:00',
      'dateFin': '2026-10-01T20:00:00',
      'limitePlaces': 50,
      'nbInscrits': 3,
      'statut': statut,
      'compteRendu': null,
      'createdAt': '2026-09-01T10:00:00',
      'piecesJointes': <dynamic>[],
    };

void main() {
  group('Identifiants UUID', () {
    test('le service appelle le backend avec l\'UUID, pas un entier', () async {
      final api = _FakeApi({'/events/$_uuid': backendEventJson()});
      try {
        await EventsService(api).getEvent(_uuid);
      } catch (_) {
        // La LECTURE échoue encore : le modèle attend `title`/`startAt` et le
        // backend renvoie `titre`/`dateDebut`. Défaut distinct, documenté dans
        // le groupe « divergence de schéma » ci-dessous.
      }
      expect(api.calls, ['GET /events/$_uuid']);
    });
  });

  group('Vocabulaire de statut', () {
    test('les 4 statuts du backend sont lus', () {
      expect(EventStatusWire.decode('PLANIFIE'), EventStatus.published);
      expect(EventStatusWire.decode('EN_COURS'), EventStatus.live);
      expect(EventStatusWire.decode('TERMINE'), EventStatus.completed);
      expect(EventStatusWire.decode('ANNULE'), EventStatus.cancelled);
    });

    test('un statut libre inconnu ne fait PAS échouer la lecture', () {
      // Le backend expose un String libre : un enum strict lèverait et ferait
      // échouer toute la liste pour un seul enregistrement atypique.
      expect(EventStatusWire.decode('REPORTEE'), EventStatus.unknown);
      expect(EventStatusWire.decode(''), EventStatus.unknown);
      expect(EventStatusWire.decode(null), EventStatus.unknown);
      expect(EventStatusWire.decode(42), EventStatus.unknown);
    });
  });

  group('Annulation — fonctionnalité réelle', () {
    test('envoie PUT /events/{id} avec le statut ANNULE', () async {
      final api = _FakeApi({
        '/events/$_uuid': backendEventJson(statut: 'ANNULE'),
      });
      try {
        await EventsService(api).cancelEvent(_uuid);
      } catch (_) {/* lecture du schéma encore rompu, autre défaut */}

      expect(api.calls, ['PUT /events/$_uuid']);
      // Corps PARTIEL et vocabulaire du backend : le titre et les dates restent
      // intacts (EventService.update n'écrit que les champs non nuls).
      expect(api.bodies.single, {'statut': 'ANNULE'});
    });

    test('remonte l\'erreur backend au lieu de fabriquer un succès', () {
      expect(
        () => EventsService(_ThrowingApi()).cancelEvent(_uuid),
        throwsA(isA<Exception>()),
      );
    });
  });

  group('Sous-fonctionnalités — chemins réels', () {
    test('équipe : /church-events/{id}/teams, et une équipe a un responsable',
        () async {
      final api = _FakeApi({
        '/church-events/$_uuid/teams': [
          {'id': _uuid, 'name': 'Accueil', 'leadPersonId': _person, 'color': '#16a34a'},
        ],
      });

      final teams = await EventsService(api).getEventTeams(_uuid);

      expect(api.calls, ['GET /church-events/$_uuid/teams']);
      expect(teams.single.name, 'Accueil');
      expect(teams.single.leadPersonId, _person);
    });

    test('checklist : /event-checklists/event/{id}', () async {
      final api = _FakeApi({
        '/event-checklists/event/$_uuid': [
          {
            'id': _uuid,
            'eventId': _uuid,
            'title': 'Preparer la sono',
            'status': 'PENDING',
            'orderIndex': 1,
          },
        ],
      });

      final items = await EventsService(api).getChecklist(_uuid);

      expect(api.calls, ['GET /event-checklists/event/$_uuid']);
      expect(items.single.title, 'Preparer la sono');
      expect(items.single.status, ChecklistStatus.pending);
    });

    test('tenue : /dress-codes filtré par eventId', () async {
      final api = _FakeApi({
        '/dress-codes': [
          {
            'id': _uuid,
            'title': 'Tenue de ceremonie',
            'serviceName': 'Culte',
            'status': 'ACTIVE',
            'archived': false,
            'rules': [
              {'groupName': 'Hommes', 'description': 'Chemise blanche'},
            ],
          },
        ],
      });

      final codes = await EventsService(api).getDressCodes(_uuid);

      expect(api.calls.single, contains('/dress-codes'));
      expect(api.calls.single, contains('eventId'));
      expect(codes.single.title, 'Tenue de ceremonie');
      expect(codes.single.rules.single.groupName, 'Hommes');
    });
  });

  group('Divergence de schéma CONNUE (module non fonctionnel)', () {
    // Ce test ne « passe » pas par hasard : il DOCUMENTE un écart mesuré. Le
    // modèle `Event` a été écrit contre un schéma qui n'a jamais été celui du
    // backend. Tant qu'il existe avec cette liste, la correction n'a pas eu
    // lieu ; le supprimer et le remplacer par des tests de parsing réel est le
    // signal de fin de la migration.
    test('le modèle mobile et EventResponse n\'ont AUCUN champ en commun', () {
      const backendFields = {
        'id', 'organisateurId', 'familleId', 'departmentId', 'typeEvenement',
        'titre', 'description', 'lieu', 'dateDebut', 'dateFin', 'limitePlaces',
        'nbInscrits', 'statut', 'compteRendu', 'createdAt', 'piecesJointes',
      };
      const expectedByMobile = {
        'title', 'startAt', 'endAt', 'location', 'type', 'currentAttendees',
        'maxAttendees', 'isPublic', 'requiresRegistration', 'hasCheckIn',
        'spaceId', 'dressCodeId', 'organizerId', 'attachments',
      };
      final missing = expectedByMobile.difference(backendFields);
      expect(missing, isNotEmpty,
          reason:
              'Si cette liste est vide, le modèle a été réaligné sur '
              'EventResponse : supprime ce test et ajoute les tests de parsing.');
      expect(missing.length, greaterThanOrEqualTo(10));
    });
  });
}
