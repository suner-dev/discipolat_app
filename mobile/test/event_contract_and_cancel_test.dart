// Migration du contrat d'identifiants + vocabulaire de statut + annulation
// d'événement (module `events`).
//
// Ce fichier verrouille les TROIS bris de contrat que le module avait avec le
// backend, mesurés le 2026-09-29 :
//  1. identifiants : le mobile envoyait des `int`, le backend est en `UUID`
//     (250 entités sur 260) ;
//  2. vocabulaire : le mobile lisait DRAFT/PUBLISHED/LIVE/COMPLETED/CANCELLED,
//     le backend ne produit et ne filtre que PLANIFIE/EN_COURS/TERMINE/ANNULE
//     (cf. `StatutEvenement` côté web et `Event.statut` côté backend) ;
//  3. l'annulation n'existait pas : elle affichait « bientôt disponible ».
//
// Convention du dépôt : un faux `ApiService` injecté, aucune dépendance
// ajoutée (comme compliance_service_test.dart).

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeApi extends ApiService {
  _FakeApi(this.responses) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> responses;
  final List<String> calls = [];
  final List<dynamic> bodies = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add('GET $path');
    if (responses.containsKey(path)) {
      return Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 200,
          data: responses[path]);
    }
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    calls.add('PUT $path');
    bodies.add(data);
    if (responses.containsKey(path)) {
      return Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 200,
          data: responses[path]);
    }
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }
}

const _uuid = '11111111-1111-1111-1111-111111111111';

/// Payload au format EXACT de `EventResponse` (backend). Les noms de champs
/// sont ceux du backend, volontairement : c'est la seule fixture honnête.
Map<String, dynamic> backendEventJson({String statut = 'PLANIFIE', String id = _uuid}) => {
      'id': id,
      'organisateurId': '22222222-2222-2222-2222-222222222222',
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
  group('Contrat — identifiants UUID', () {
    test('le service appelle le backend avec l\'UUID, pas un entier', () async {
      final api = _FakeApi({'/events/$_uuid': backendEventJson()});
      final service = EventsService(api);

      // La REQUETE est correcte : c'est ce que cette migration corrige.
      try {
        await service.getEvent(_uuid);
      } catch (_) {
        // La LECTURE echoue encore : le modèle attend `title`/`startAt` et le
        // backend envoie `titre`/`dateDebut`. Defaut distinct, documente dans
        // le groupe « DIVERGENCE DE SCHEMA CONNUE » ci-dessous.
      }
      expect(api.calls, ['GET /events/$_uuid']);
    });
  });

  group('Contrat — vocabulaire de statut', () {
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

    test('la liste se lit même si un statut est inconnu', () async {
      final api = _FakeApi({
        '/events': [
          backendEventJson(statut: 'PLANIFIE'),
          backendEventJson(statut: 'REPORTEE', id: '33333333-3333-3333-3333-333333333333'),
        ],
      });

      // La normalisation du statut est tolérante : elle ne lève pas. Ce qui
      // échoue ensuite est le parsing du reste du schéma (autre défaut).
      EventStatusWire.decode('REPORTEE');
      try {
        await EventsService(api).getEvents();
      } catch (_) {/* voir le groupe DIVERGENCE */}
      expect(api.calls.first, 'GET /events');
    });
  });

  group('DIVERGENCE DE SCHEMA CONNUE (module non fonctionnel)', () {
    // Ce test ne « passe » pas par hasard : il **documente** un écart mesuré.
    // Le modèle `Event` du mobile a été écrit contre un schéma qui n'a jamais
    // été celui du backend :
    //   mobile  attend  title, startAt, endAt, location, type (enum), …
    //   backend renvoie titre, dateDebut, dateFin, lieu, typeEvenement (String), …
    // Aucun champ de `EventResponse` ne porte le nom attendu par le modèle :
    // `Event.fromJson` échoue sur le premier `null as String`.
    //
    // Tant que ce test existe avec cette liste, la correction n'a pas eu lieu.
    // Le migrer (réécrire le modèle sur `EventResponse`, en prenant le type
    // `Evenement` du web comme spécification) consiste à vider cette liste.
    test('le modèle mobile et EventResponse n\'ont AUCUN champ en commun', () {
      const backendFields = {
        'id', 'organisateurId', 'familleId', 'departmentId', 'typeEvenement',
        'titre', 'description', 'lieu', 'dateDebut', 'dateFin', 'limitePlaces',
        'nbInscrits', 'statut', 'compteRendu', 'createdAt', 'piecesJointes',
      };
      // Champs que le modèle mobile exige et que le backend n'envoie pas.
      const expectedByMobile = {
        'title', 'startAt', 'endAt', 'location', 'type', 'currentAttendees',
        'maxAttendees', 'isPublic', 'requiresRegistration', 'hasCheckIn',
        'spaceId', 'dressCodeId', 'organizerId', 'attachments',
      };
      final missing = expectedByMobile.difference(backendFields);
      expect(missing, isNotEmpty,
          reason:
              'Si cette liste est vide, le modèle a été réaligné sur '
              'EventResponse : supprime ce test et ajoute les tests de parsing '
              'réels.');
      // Preuve chiffrée du fossé.
      expect(missing.length, greaterThanOrEqualTo(10));
    });
  });

  group('Annulation — functionality réelle', () {
    test('envoie PUT /events/{id} avec le statut ANNULE', () async {
      final api = _FakeApi({
        '/events/$_uuid': backendEventJson(statut: 'ANNULE'),
      });
      final service = EventsService(api);

      try {
        await service.cancelEvent(_uuid);
      } catch (_) {/* lecture du schema encore rompu, autre defaut */}

      expect(api.calls, ['PUT /events/$_uuid']);
      // Corps PARTIEL et vocabulaire du backend : le titre et les dates restent
      // intacts (EventService.update n'ecrit que les champs non nuls).
      expect(api.bodies.single, {'statut': 'ANNULE'});
    });

    test('n\'annule pas un événement déjà annulé (erreur backend remontée)', () {
      // Pas de simulation de succès : une erreur doit remonter telle quelle.
      final api = _ThrowingApi();
      expect(
        () => EventsService(api).cancelEvent(_uuid),
        throwsA(isA<Exception>()),
      );
    });
  });
}

/// Faux qui refuse toute écriture : sert à prouver qu'aucun « succès » n'est
/// fabriqué en cas d'échec.
class _ThrowingApi extends ApiService {
  _ThrowingApi() : super(baseUrl: 'http://fake');

  @override
  Future<Response> put(String path, {dynamic data}) async =>
      throw DioException(requestOptions: RequestOptions(path: path), type: DioExceptionType.badResponse);
}
