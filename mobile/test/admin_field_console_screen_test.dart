import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/realtime_bus_service.dart';
import 'package:discipolat_mobile/presentation/screens/admin/admin_field_console_screen.dart';

import 'helpers/pump_localized.dart';

/// §G5.5 (§59) — Fake de la console admin terrain : APIs RÉELLES simulées
/// (mêmes routes et mêmes formes de réponses que le backend).
class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false})
      : super(baseUrl: 'http://fake');
  final bool fail;

  /// §G5.8 — mutable : la file peut être vidée par un autre appareil.
  bool empty;

  /// §G5.8 — Bob pointé présent ailleurs → la feuille doit se rafraîchir.
  bool bobPresent = false;

  final List<String> postedPaths = [];
  final List<Map<String, dynamic>> postedBodies = [];
  final List<String> putPaths = [];
  final List<Map<String, dynamic>> putBodies = [];
  final List<String> getPaths = [];

  Response _ok(String path, dynamic data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/workflow-engine/tasks/pending') {
      return _ok(path, empty
          ? <dynamic>[]
          : [
              {
                'taskId': 't1',
                'instanceId': 'i1',
                'workflowName': 'Validation membre',
                'entityType': 'MEMBER',
                'entityId': 'e1',
                'stepName': 'Approbation admin',
                'stepType': 'APPROVAL',
                'assigneeRole': 'ADMIN',
                'dueAt': null,
                'createdAt': '2026-09-20T10:00:00Z',
              },
            ]);
    }
    if (path == '/departments') {
      return _ok(path, [
        {'id': 'd1', 'nom': 'Louange'},
      ]);
    }
    if (path == '/events/department/d1') {
      // Page<Event> côté backend → { content: [...] }
      return _ok(path, {
        'content': [
          {'id': 'e1', 'titre': 'Veillée de prière'},
        ],
      });
    }
    if (path == '/departments/d1/events/e1/attendance') {
      return _ok(path, {
        'eventTitre': 'Veillée de prière',
        'membres': [
          {'soulId': 's1', 'nom': 'Alice', 'present': true},
          {'soulId': 's2', 'nom': 'Bob', 'present': bobPresent ? true : null},
        ],
        'total': 2,
        'presents': bobPresent ? 2 : 1,
      });
    }
    return _ok(path, []);
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postedPaths.add(path);
    postedBodies.add(Map<String, dynamic>.from((data ?? {}) as Map));
    return _ok(path, {'ok': true});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    putBodies.add(Map<String, dynamic>.from((data ?? {}) as Map));
    return _ok(path, {'ok': true});
  }
}

void main() {
  group('AdminFieldConsoleScreen — approbations', () {
    testWidgets('renders title and tabs', (tester) async {
      await pumpLocalized(tester, AdminFieldConsoleScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Console admin terrain'), findsOneWidget);
      expect(find.text('Approbations'), findsOneWidget);
      expect(find.text('Présence'), findsOneWidget);
    });

    testWidgets('renders enriched pending tasks from API', (tester) async {
      await pumpLocalized(tester, AdminFieldConsoleScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      // workflowName · stepName proviennent de GET /tasks/pending enrichi
      expect(find.text('Validation membre · Approbation admin'), findsOneWidget);
      expect(find.byIcon(Icons.check_circle), findsOneWidget);
      expect(find.byIcon(Icons.cancel), findsOneWidget);
    });

    testWidgets('approve calls POST /tasks/{id}/approve with comment', (tester) async {
      final api = _FakeApiService();
      await pumpLocalized(tester, AdminFieldConsoleScreen(apiService: api));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.check_circle));
      await tester.pumpAndSettle();
      expect(find.text('Approuver'), findsOneWidget);

      await tester.enterText(find.byType(TextField), 'Dossier vérifié');
      await tester.tap(find.text('Confirmer'));
      await tester.pumpAndSettle();

      expect(api.postedPaths, ['/workflow-engine/tasks/t1/approve']);
      expect(api.postedBodies.single['comment'], 'Dossier vérifié');
      expect(find.text('Décision enregistrée'), findsOneWidget);
    });

    testWidgets('reject calls POST /tasks/{id}/reject', (tester) async {
      final api = _FakeApiService();
      await pumpLocalized(tester, AdminFieldConsoleScreen(apiService: api));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.cancel));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Confirmer'));
      await tester.pumpAndSettle();

      expect(api.postedPaths, ['/workflow-engine/tasks/t1/reject']);
    });

    testWidgets('empty state when no pending task', (tester) async {
      await pumpLocalized(
          tester, AdminFieldConsoleScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucune approbation en attente.'), findsOneWidget);
    });

    testWidgets('error state with retry', (tester) async {
      await pumpLocalized(
          tester, AdminFieldConsoleScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Erreur de chargement'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });

  group('AdminFieldConsoleScreen — présence', () {
    Future<_FakeApiService> openAttendance(WidgetTester tester,
        {_FakeApiService? api}) async {
      final service = api ?? _FakeApiService();
      await pumpLocalized(tester, AdminFieldConsoleScreen(apiService: service));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Présence'));
      await tester.pumpAndSettle();
      return service;
    }

    testWidgets('department dropdown lists real departments', (tester) async {
      await openAttendance(tester);
      await tester.tap(find.byType(DropdownButtonFormField<String>).first);
      await tester.pumpAndSettle();
      expect(find.text('Louange'), findsOneWidget);
      await tester.tap(find.text('Louange'));
      await tester.pumpAndSettle();
      // Le département sélectionné charge les événements rattachés.
      expect(find.text('Choisir un événement'), findsOneWidget);
    });

    testWidgets('selecting event loads attendance sheet', (tester) async {
      await openAttendance(tester);
      await tester.tap(find.byType(DropdownButtonFormField<String>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Louange'));
      await tester.pumpAndSettle();

      await tester.tap(find.byType(DropdownButtonFormField<String>).last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Veillée de prière').last);
      await tester.pumpAndSettle();

      expect(find.text('Veillée de prière'), findsWidgets);
      expect(find.text('✓ 1 / 2'), findsOneWidget);
      expect(find.text('Alice'), findsOneWidget);
      expect(find.text('Bob'), findsOneWidget);
      expect(find.text('Tout marquer présent'), findsOneWidget);
    });

    testWidgets('tapping a member sends PUT attendance', (tester) async {
      final api = await openAttendance(tester);
      await tester.tap(find.byType(DropdownButtonFormField<String>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Louange'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(DropdownButtonFormField<String>).last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Veillée de prière').last);
      await tester.pumpAndSettle();

      // Bob (present=null) → bascule vers présent.
      await tester.tap(find.text('Bob'));
      await tester.pumpAndSettle();

      expect(api.putPaths, ['/departments/d1/events/e1/attendance']);
      expect(api.putBodies.single['soulId'], 's2');
      expect(api.putBodies.single['present'], true);
    });

    testWidgets('mark all sends POST mark-all', (tester) async {
      final api = await openAttendance(tester);
      await tester.tap(find.byType(DropdownButtonFormField<String>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Louange'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(DropdownButtonFormField<String>).last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Veillée de prière').last);
      await tester.pumpAndSettle();

      await tester.tap(find.text('Tout marquer présent'));
      await tester.pumpAndSettle();

      expect(api.postedPaths,
          ['/departments/d1/events/e1/attendance/mark-all?present=true']);
    });
  });

  group('AdminFieldConsoleScreen — synchro temps réel (G5.8)', () {
    int pendingCalls(_FakeApiService api) => api.getPaths
        .where((p) => p == '/workflow-engine/tasks/pending')
        .length;

    testWidgets('TaskCompleted distant recharge la file d\u2019approbation',
        (tester) async {
      final bus = StreamController<RealtimeEvent>.broadcast();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await pumpLocalized(tester,
          AdminFieldConsoleScreen(apiService: api, realtimeEvents: bus.stream));
      await tester.pumpAndSettle();
      expect(find.text('Validation membre · Approbation admin'), findsOneWidget);

      // Un admin web traite la tâche → outbox → firehose du tenant.
      api.empty = true;
      bus.add(RealtimeEvent(eventId: 1, eventType: 'TaskCompleted'));
      await tester.pumpAndSettle();

      expect(find.text('Aucune approbation en attente.'), findsOneWidget);
    });

    testWidgets('événement hors périmètre : aucune recharge', (tester) async {
      final bus = StreamController<RealtimeEvent>.broadcast();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await pumpLocalized(tester,
          AdminFieldConsoleScreen(apiService: api, realtimeEvents: bus.stream));
      await tester.pumpAndSettle();
      final before = pendingCalls(api);

      bus.add(RealtimeEvent(eventId: 2, eventType: 'DressCodePublished'));
      await tester.pumpAndSettle();

      expect(pendingCalls(api), before);
      expect(find.text('Validation membre · Approbation admin'), findsOneWidget);
    });

    testWidgets('AttendanceRecorded distant rafraîchit la feuille ouverte',
        (tester) async {
      final bus = StreamController<RealtimeEvent>.broadcast();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await pumpLocalized(tester,
          AdminFieldConsoleScreen(apiService: api, realtimeEvents: bus.stream));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Présence'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(DropdownButtonFormField<String>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Louange'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(DropdownButtonFormField<String>).last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Veillée de prière').last);
      await tester.pumpAndSettle();
      expect(find.text('✓ 1 / 2'), findsOneWidget);

      // Un autre pointeur a validé Bob sur le web → la feuille ouverte se met à jour.
      api.bobPresent = true;
      bus.add(RealtimeEvent(eventId: 3, eventType: 'AttendanceRecorded'));
      await tester.pumpAndSettle();

      expect(find.text('✓ 2 / 2'), findsOneWidget);
    });

    testWidgets('trou de delta (fullRefresh) → recharge complète de la file',
        (tester) async {
      final bus = StreamController<RealtimeEvent>.broadcast();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await pumpLocalized(tester,
          AdminFieldConsoleScreen(apiService: api, realtimeEvents: bus.stream));
      await tester.pumpAndSettle();
      final before = pendingCalls(api);

      bus.add(RealtimeEvent(eventId: 42, eventType: 'X', fullRefresh: true));
      await tester.pumpAndSettle();

      expect(pendingCalls(api), greaterThan(before));
    });
  });
}
