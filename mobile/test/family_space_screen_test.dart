import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/realtime_bus_service.dart';
import 'package:discipolat_mobile/presentation/screens/family_space/family_space_screen.dart';

import 'helpers/pump_localized.dart';

/// §G4.1/G4.2 — Fake « Family OS » : mêmes routes et mêmes formes que le
/// backend réel (/families/{id}/os/*), y compris la recherche scopée d'âmes.
class _FakeApiService extends ApiService {
  _FakeApiService({this.noFamily = false}) : super(baseUrl: 'http://fake');

  /// Empty-state : aucune famille rattachée (ni comme chef, ni comme faiseur).
  final bool noFamily;

  /// §G5.8 — true après un FamilyMemberAdded distant : le 3e membre apparaît.
  bool memberAdded = false;

  final List<String> getPaths = [];
  final List<Map<String, dynamic>?> getParams = [];
  final List<String> postedPaths = [];
  final List<String> putPaths = [];
  final List<dynamic> putBodies = [];

  static const dash = {
    'familyId': 'f1',
    'upcomingVisits': [
      {'id': 'v1', 'soulId': 's1', 'soulName': 'Marie K.', 'visitDate': '2026-09-22', 'visitType': 'VISITE', 'status': 'PLANNED', 'nextActionDate': null},
    ],
    'followUps': [
      {'id': 'v2', 'soulId': 's2', 'soulName': 'Jean P.', 'visitDate': '2026-09-10', 'visitType': 'SUIVI', 'status': 'PLANNED', 'nextActionDate': '2026-09-15'},
    ],
    'recentReceptions': [
      {'id': 'r1', 'soulId': 's1', 'receptionDate': '2026-09-05', 'receptionType': 'ACCUEIL'},
    ],
    'upcomingMeetings': [],
    'overdueFollowUps': 1,
  };

  static const visits = [
    {'id': 'v1', 'familyId': 'f1', 'soulId': 's1', 'visitDate': '2026-09-22', 'visitType': 'VISITE', 'subject': 'Premier contact', 'status': 'PLANNED'},
    {'id': 'v2', 'familyId': 'f1', 'soulId': 's2', 'visitDate': '2026-09-10', 'visitType': 'SUIVI', 'subject': null, 'status': 'COMPLETED'},
  ];

  static const candidates = [
    {'soulId': 's7', 'userId': 'u7', 'prenom': 'Esther', 'nom': 'M.'},
  ];

  Response _ok(String path, dynamic data) => Response(
      requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

  List<dynamic> get members => [
        {'id': 's1', 'prenom': 'Marie', 'nom': 'K.', 'typeDisciple': 'NOUVEAU_CONVERTI'},
        {'id': 's2', 'prenom': 'Jean', 'nom': 'P.', 'typeDisciple': 'NOUVEL_ARRIVANT'},
        if (memberAdded)
          {'id': 's7', 'prenom': 'Esther', 'nom': 'M.', 'typeDisciple': 'NOUVEL_ARRIVANT'},
      ];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    getParams.add(params);
    if (path == '/families/f1') return _ok(path, {'id': 'f1', 'nom': 'Famille Bétel'});
    if (noFamily && (path == '/families' || path == '/souls')) {
      return _ok(path, {'content': []});
    }
    if (path == '/families/f1/os/dashboard') return _ok(path, dash);
    if (path == '/families/f1/os/visits') return _ok(path, visits);
    if (path == '/families/f1/os/members') return _ok(path, members);
    if (path == '/families/f1/os/activities') {
      return _ok(path, {
        'content': [
          {'id': 'a1', 'activityType': 'VISIT', 'title': 'Visite: Marie K.', 'activityDate': '2026-09-22', 'status': 'PLANNED'},
        ],
        'page': 0, 'size': 20, 'totalElements': 7, 'totalPages': 1,
      });
    }
    if (path == '/families/f1/os/search-souls') return _ok(path, candidates);
    if (path == '/users') {
      return _ok(path, {
        'content': [
          {'id': 'u9', 'firstName': 'Pierre', 'lastName': 'F.'},
        ],
      });
    }
    return _ok(path, {});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    putBodies.add(data);
    return _ok(path, {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postedPaths.add(path);
    return _ok(path, {});
  }
}

Future<void> pumpFamily(WidgetTester tester, _FakeApiService api,
    {Stream<RealtimeEvent>? realtimeEvents}) async {
  await pumpLocalized(tester,
      FamilySpaceScreen(apiService: api, realtimeEvents: realtimeEvents));
  await tester.pumpAndSettle();
}

void main() {
  setUp(() {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'u1',
      'email': 'chef@x.cd',
      'role': 'CHEF_DE_FAMILLE',
      'familleGereeId': 'f1',
      'estChefDeFamille': true,
    });
  });
  tearDown(() => AuthState().logout());

  group('FamilySpaceScreen — OS famille réel (G4.1/G4.2)', () {
    testWidgets('rend le vrai dashboard /families/f1/os/dashboard',
        (tester) async {
      final api = _FakeApiService();
      await pumpFamily(tester, api);

      expect(find.text('Famille Bétel'), findsOneWidget);
      expect(find.text('Suivis en retard'), findsOneWidget);
      expect(find.text('Marie K.'), findsWidgets);
      expect(find.text('Jean P.'), findsOneWidget);
      // Journal scopé famille (fuite corrigée §G4.1)
      expect(api.getPaths, contains('/families/f1/os/activities'));
      expect(api.getPaths, contains('/families/f1/os/members'));
    });

    testWidgets('termine une visite planifiée via PUT réel', (tester) async {
      final api = _FakeApiService();
      await pumpFamily(tester, api);

      await tester.tap(find.text('Visites'));
      await tester.pumpAndSettle();
      expect(find.text('Premier contact'), findsOneWidget);

      await tester.tap(find.byKey(const Key('familyOsVisitComplete-v1')));
      await tester.pumpAndSettle();

      expect(api.putPaths, contains('/families/f1/os/visits/v1'));
      expect(api.putBodies.last, {'status': 'COMPLETED'});
      expect(find.text('Visite mise à jour'), findsOneWidget);
    });

    testWidgets('ajout d\u2019âme : recherche scopée CAMPUS + POST réel',
        (tester) async {
      final api = _FakeApiService();
      await pumpFamily(tester, api);

      await tester.tap(find.byKey(const Key('familyOsAddFab')));
      await tester.pumpAndSettle();

      await tester.enterText(
          find.byKey(const Key('familyOsSoulSearch')), 'Esther');
      await tester.tap(find.byIcon(Icons.search));
      await tester.pumpAndSettle();

      // minimisation §G4.2 : scope CAMPUS par défaut
      final searchCall = api.getPaths.indexOf('/families/f1/os/search-souls');
      expect(searchCall, greaterThanOrEqualTo(0));
      expect(api.getParams[searchCall]?['scope'], 'CAMPUS');

      await tester.tap(find.byKey(const Key('familyOsCandidate-s7')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('familyOsAddConfirm')));
      await tester.pumpAndSettle();

      expect(api.postedPaths, contains('/families/f1/os/members?soulId=s7'));
      expect(find.text('Âme ajoutée à la famille'), findsOneWidget);
    });

    testWidgets('état vide quand aucune famille n\u2019est rattachée',
        (tester) async {
      final api = _FakeApiService(noFamily: true);
      AuthState().setAuthenticated(true, userData: {
        'userId': 'u1',
        'role': 'FAISEUR',
      });
      await pumpFamily(tester, api);

      expect(find.textContaining('Aucune famille ne vous est associée'),
          findsOneWidget);
      // Résolution réelle : chef de famille puis âmes suivies comme faiseur.
      expect(api.getPaths, contains('/families'));
      expect(api.getPaths, contains('/souls'));
    });

    testWidgets('FamilyMemberAdded distant recharge la liste (< 5 s, G5.8)',
        (tester) async {
      final bus = StreamController<RealtimeEvent>();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await pumpFamily(tester, api, realtimeEvents: bus.stream);

      // Onglet Membres : la liste des âmes rattachées est visible.
      await tester.tap(find.text('Membres'));
      await tester.pumpAndSettle();
      expect(find.text('Esther M.'), findsNothing);
      final membersCalls =
          api.getPaths.where((p) => p == '/families/f1/os/members').length;

      api.memberAdded = true;
      bus.add(RealtimeEvent(
          eventId: 1, eventType: 'FamilyMemberAdded', aggregateId: 'f1'));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(
          api.getPaths
              .where((p) => p == '/families/f1/os/members')
              .length,
          greaterThan(membersCalls));
      expect(find.text('Esther M.'), findsOneWidget);
    });
  });
}
