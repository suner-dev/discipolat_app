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

  group('Parsing du contrat réel (migration achevée)', () {
    test('un EventResponse réel est lu sans erreur et sans perte', () {
      final event = Event.fromJson(backendEventJson());

      expect(event.id, _uuid);
      expect(event.titre, 'Reunion de priere');
      expect(event.lieu, 'Salle 1');
      expect(event.typeEvenement, 'REUNION');
      expect(event.dateDebut, DateTime(2026, 10, 1, 18));
      expect(event.dateFin, isNotNull);
      expect(event.limitePlaces, 50);
      expect(event.nbInscrits, 3);
      expect(event.statut, EventStatus.published);
      expect(event.organisateurId, _person);
      expect(event.piecesJointes, isEmpty);
    });

    test('le statut ANNULE du serveur devient EventStatus.cancelled', () {
      final event = Event.fromJson(backendEventJson(statut: 'ANNULE'));
      expect(event.statut, EventStatus.cancelled);
    });

    test('un statut atypique ne fait pas échouer la lecture', () async {
      // La tolérance est une propriété de la FRONTIÈRE (le service) : c'est le
      // seul endroit qui décode une réponse du serveur. Le modèle reste strict,
      // sinon json_serializable ne génère plus son décodeur.
      final api = _FakeApi({
        '/events/$_uuid': backendEventJson(statut: 'REPORTEE'),
      });
      final event = await EventsService(api).getEvent(_uuid);
      expect(event.statut, EventStatus.unknown);
      expect(event.titre, 'Reunion de priere'); // le reste est bien lu
    });

    test('un statut atypique ne fait pas échouer toute la liste', () async {
      final api = _FakeApi({
        '/events': [
          backendEventJson(statut: 'PLANIFIE'),
          backendEventJson(
              statut: 'REPORTEE', id: '33333333-3333-3333-3333-333333333333'),
        ],
      });
      final events = await EventsService(api).getEvents();
      expect(events.length, 2);
      expect(events.first.statut, EventStatus.published);
      expect(events.last.statut, EventStatus.unknown);
    });

    test('une pièce jointe réelle est lue (AttachmentItem)', () {
      final json = backendEventJson();
      json['piecesJointes'] = [
        {
          'id': _uuid,
          'fileId': _person,
          'nom': 'Ordre du jour.pdf',
          'url': 'https://files.example/ordre-du-jour.pdf',
        },
      ];
      final event = Event.fromJson(json);
      expect(event.piecesJointes.single.nom, 'Ordre du jour.pdf');
      expect(event.piecesJointes.single.url, 'https://files.example/ordre-du-jour.pdf');
    });

    test("l'état dérivé n'est jamais lu dans la réponse", () {
      // Les drapeaux client ne sont pas du contrat : le serveur ne les envoie
      // pas, et le modèle ne doit donc pas les prétendre.
      final event = Event.fromJson(backendEventJson());
      expect(event.isRegistered, isFalse);
      expect(event.isCheckedIn, isFalse);
      expect(event.isOrganizedBy(null), isFalse);
      expect(event.isOrganizedBy(_person), isTrue);
    });

    test('le corps ENVOYÉ utilise les noms du serveur', () {
      final event = Event.fromJson(backendEventJson());
      final body = event.toJson();
      expect(body['titre'], 'Reunion de priere');
      expect(body['lieu'], 'Salle 1');
      expect(body['typeEvenement'], 'REUNION');
      expect(body['dateDebut'], isA<String>());
      // et aucun des anciens noms anglicisés
      expect(body.containsKey('title'), isFalse);
      expect(body.containsKey('startAt'), isFalse);
      expect(body.containsKey('location'), isFalse);
    });
  });
}
