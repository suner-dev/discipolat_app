import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/realtime_bus_service.dart';
import 'package:discipolat_mobile/presentation/screens/pastoral/pastoral_ministry_screen.dart';

import 'helpers/pump_localized.dart';

/// §G4.3 — Fake « Ministère pastoral » : mêmes routes et formes que le
/// backend réel (lecture seule mobile ; les écritures restent web,
/// @PreAuthorize ADMIN/PASTOR_PRINCIPAL).
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> getPaths = [];
  final List<String> postedPaths = [];

  static const appointments = [
    {'id': 'ap1', 'pastorId': 'p1', 'organizationUnitId': 'c1', 'roleCode': 'PASTEUR', 'title': 'Pasteur de campus', 'startDate': '2024-03-01', 'endDate': null, 'status': 'ACTIVE'},
    {'id': 'ap2', 'pastorId': 'p2', 'organizationUnitId': 'c1', 'roleCode': 'PASTEUR', 'title': null, 'startDate': '2020-01-01', 'endDate': '2024-02-01', 'status': 'ENDED'},
  ];
  static const transfers = [
    {'id': 'tr1', 'pastorId': 'p2', 'fromOrgUnitId': 'c1', 'toOrgUnitId': 'c2', 'transferDate': '2026-09-20', 'reason': 'Réorganisation', 'status': 'PENDING'},
  ];
  static const units = [
    {'id': 'r1', 'type': 'ROOT_CHURCH', 'name': 'Église Centrale'},
    {'id': 'c1', 'type': 'CAMPUS', 'name': 'Campus Nord'},
    {'id': 'c2', 'type': 'CAMPUS', 'name': 'Campus Sud'},
  ];
  static const pastors = [
    {'id': 'p1', 'firstName': 'André', 'lastName': 'Mbala'},
    {'id': 'p2', 'firstName': 'Grégoire', 'lastName': 'K.'},
  ];

  Response _ok(String path, dynamic data) => Response(
      requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    if (path == '/pastorate/appointments') return _ok(path, appointments);
    if (path == '/pastorate/transfers') return _ok(path, transfers);
    if (path == '/org/tree/flat') return _ok(path, units);
    if (path == '/users') return _ok(path, {'content': pastors});
    if (path == '/pastorate/pastors/p1/history') {
      return _ok(path, {'appointments': [appointments.first], 'transfers': []});
    }
    if (path == '/pastorate/pastors/p2/history') {
      return _ok(path, {'appointments': [appointments.last], 'transfers': transfers});
    }
    return _ok(path, {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postedPaths.add(path);
    return _ok(path, {});
  }
}

Future<void> pumpPastoral(WidgetTester tester, _FakeApiService api,
    {Stream<RealtimeEvent>? realtimeEvents}) async {
  await pumpLocalized(tester,
      PastoralMinistryScreen(apiService: api, realtimeEvents: realtimeEvents));
  await tester.pumpAndSettle();
}

void main() {
  group('PastoralMinistryScreen — consultation pastorale (G4.3)', () {
    testWidgets('mandats actifs/passés avec noms résolus (pasteur + unité)',
        (tester) async {
      final api = _FakeApiService();
      await pumpPastoral(tester, api);

      expect(api.getPaths, contains('/pastorate/appointments'));
      expect(find.text('André Mbala'), findsWidgets);
      expect(find.text('Pasteur de campus'), findsOneWidget);
      // Unités résolues via /org/tree/flat, jamais d'UUID brut affiché.
      expect(find.textContaining('Campus Nord'), findsWidgets);
      expect(find.text('Grégoire K.'), findsWidgets); // mandat terminé
    });

    testWidgets('transfert en attente rendu en lecture seule', (tester) async {
      final api = _FakeApiService();
      await pumpPastoral(tester, api);

      await tester.tap(find.text('Transferts'));
      await tester.pumpAndSettle();
      expect(find.text('Campus Nord → Campus Sud'), findsOneWidget);
      expect(find.text('Réorganisation'), findsOneWidget);
      // Parité gardes backend : le mobile n ÉCRIT JAMAIS sur /pastorate.
      expect(api.postedPaths, isEmpty);
      expect(find.byType(FloatingActionButton), findsNothing);
    });

    testWidgets('historique par pasteur via /pastorate/pastors/{id}/history',
        (tester) async {
      final api = _FakeApiService();
      await pumpPastoral(tester, api);

      await tester.tap(find.text('Historique'));
      await tester.pumpAndSettle();
      // Chargé dès l'ouverture : premier pasteur présélectionné (p1).
      expect(api.getPaths, contains('/pastorate/pastors/p1/history'));
      expect(find.text('Pasteur de campus'), findsWidgets);
    });

    testWidgets('PastorAppointed distant recharge les mandats (G5.8)',
        (tester) async {
      final bus = StreamController<RealtimeEvent>();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await pumpPastoral(tester, api, realtimeEvents: bus.stream);

      final before =
          api.getPaths.where((p) => p == '/pastorate/appointments').length;
      bus.add(RealtimeEvent(
          eventId: 9, eventType: 'PastorAppointed', aggregateId: 'ap3'));
      await tester.pump();
      await tester.pumpAndSettle();

      final after =
          api.getPaths.where((p) => p == '/pastorate/appointments').length;
      expect(after, greaterThan(before));
    });
  });
}
