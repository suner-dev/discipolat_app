import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/import/SpaceConfigTransferScreen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  static final Map<String, dynamic> exportDoc = {
    'format': 'space_export_v1',
    'version': '1.0',
    'scope': 'SPACE',
    'exportedAt': '2025-01-02T03:04:05Z',
    'spaces': [
      {'id': 's1', 'name': 'Montalembert'},
    ],
    'modules': [
      {'key': 'members', 'enabled': true},
      {'key': 'attendance', 'enabled': false},
    ],
    'statuses': [
      {'label': 'Actif', 'type': 'member'},
    ],
    'customFields': [
      {'name': 'baptism_date', 'type': 'DATE'},
    ],
    'workflows': [
      {'code': 'member_onboarding'},
    ],
  };

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/spaces') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: [
          {'id': 's1', 'name': 'Montalembert'},
          {'id': 's2', 'name': 'Réservation'},
        ],
      );
    }
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }

  @override
  Future<Response> getBytes(String path, {Map<String, dynamic>? params}) async {
    final jsonStr = jsonEncode(exportDoc);
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: Uint8List.fromList(utf8.encode(jsonStr)),
    );
  }
}

/// Scroll a finder into view within the screen's [ListView] then wait.
Future<void> _tapVisible(
  WidgetTester tester,
  Finder target,
) async {
  await tester.ensureVisible(target);
  await tester.pumpAndSettle();
  await tester.tap(target);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('renders in read-only mode with the canonical notice',
      (tester) async {
    await pumpLocalized(
        tester, SpaceConfigTransferScreen(apiService: _FakeApiService()));
    await tester.pumpAndSettle();

    expect(find.text('Format canonique space_export_v1'), findsOneWidget);
    expect(find.textContaining('Lecture seule en v1.0'), findsOneWidget);
  });

  testWidgets('loads a space export and renders the summary', (tester) async {
    await pumpLocalized(
        tester, SpaceConfigTransferScreen(apiService: _FakeApiService()));
    await tester.pumpAndSettle();

    // Open the spaces dropdown and pick "Montalembert"
    await tester.tap(find.byType(DropdownButton<String>));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Montalembert'));

    // Load the configuration
    await _tapVisible(tester, find.text('Charger la configuration'));

    // La synthèse est sous le pli dans la fenêtre de test (800x600) : le
    // ListView la construit paresseusement, il faut donc la faire défiler.
    await tester.scrollUntilVisible(
      find.text('Synthèse du document'),
      200,
      scrollable: find.byType(Scrollable).first,
    );

    expect(find.text('Synthèse du document'), findsOneWidget);
    expect(find.text('space_export_v1'), findsOneWidget);
    expect(find.text('1.0'), findsOneWidget);
    expect(find.text('SPACE'), findsOneWidget);
  });

  testWidgets('analyzes pasted JSON', (tester) async {
    await pumpLocalized(
        tester, SpaceConfigTransferScreen(apiService: _FakeApiService()));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byType(EditableText),
      jsonEncode(_FakeApiService.exportDoc),
    );
    await tester.pumpAndSettle();

    await _tapVisible(tester, find.text('Analyser le JSON'));

    // La synthèse est sous le pli dans la fenêtre de test (800x600) : le
    // ListView la construit paresseusement, il faut donc la faire défiler.
    await tester.scrollUntilVisible(
      find.text('Synthèse du document'),
      200,
      scrollable: find.byType(Scrollable).first,
    );

    expect(find.text('Synthèse du document'), findsOneWidget);
    expect(find.text('Espaces'), findsOneWidget); // 1 space in doc
    expect(find.text('Modules'), findsOneWidget); // 2 modules
  });
}
