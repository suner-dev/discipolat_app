// Tests de l'écran de consentement RGPD — prouvent qu'il consomme les endpoints
// réels du backend (GET/POST /compliance/consents) et n'invente rien.

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/compliance_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/presentation/screens/compliance/consent_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeApi extends ApiService {
  _FakeApi(this.responses) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> responses;
  final List<String> calls = [];
  final List<dynamic> bodies = [];
  bool fail = false;

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add('GET $path');
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: responses[path]);
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    calls.add('POST $path');
    bodies.add(data);
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: responses[path]);
  }
}

Widget _wrap(_FakeApi api) {
  return ProviderScope(
    overrides: [
      apiServiceProvider.overrideWithValue(api),
      complianceServiceProvider.overrideWithValue(ComplianceService(apiService: api)),
    ],
    child: const MaterialApp(home: ConsentScreen()),
  );
}

void main() {
  testWidgets('charge les consentements depuis GET /compliance/consents/mine', (tester) async {
    final api = _FakeApi({
      '/compliance/consents/mine': [
        {'type': 'CGU', 'granted': true},
        {'type': 'MARKETING', 'granted': false},
      ]
    });
    await tester.pumpWidget(_wrap(api));
    await tester.pumpAndSettle();

    expect(api.calls, contains('GET /compliance/consents/mine'));
    expect(find.text('Vos consentements'), findsOneWidget);
    // CGU accordé -> son switch est à "Accordé". Les autres visibles sont
    // "Non accordé" (les items hors viewport ne sont pas construits par la
    // ListView : on ne compte donc que ce qui est réellement rendu).
    expect(find.text('Accordé'), findsWidgets);
    expect(find.text('Non accordé'), findsWidgets);
  });

  testWidgets('propose exactement les 6 consentements du backend', (tester) async {
    final api = _FakeApi({'/compliance/consents/mine': <dynamic>[]});
    await tester.pumpWidget(_wrap(api));
    await tester.pumpAndSettle();

    // ALLOWED_TYPES du ConsentController
    for (final label in [
      "Conditions d'utilisation",
      'Politique de confidentialité',
      'Données sensibles de santé',
      'Notifications WhatsApp',
      'Communications marketing',
      "Photos de l'église",
    ]) {
      // La ListView ne construit que les items visibles : on defile.
      await tester.scrollUntilVisible(find.text(label), 120);
      expect(find.text(label), findsOneWidget, reason: 'manquant : $label');
    }
  });

  testWidgets('un accord envoie POST /compliance/consents avec granted=true', (tester) async {
    final api = _FakeApi({'/compliance/consents/mine': <dynamic>[]});
    await tester.pumpWidget(_wrap(api));
    await tester.pumpAndSettle();

    await tester.tap(find.byType(SwitchListTile).first);
    await tester.pumpAndSettle();

    expect(api.calls, contains('POST /compliance/consents'));
    final body = api.bodies.first as Map<String, dynamic>;
    expect(body['granted'], isTrue);
    expect(ConsentType.tryParse(body['type'] as String), isNotNull);
  });

  testWidgets('un retrait envoie granted=false (RGPD art. 7.3)', (tester) async {
    final api = _FakeApi({
      '/compliance/consents/mine': [
        {'type': 'MARKETING', 'granted': true}
      ]
    });
    await tester.pumpWidget(_wrap(api));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(find.text('Communications marketing'), 120);
    // Recherche par cle stable : independant du scroll et de la hierarchie.
    final marketingTile = find.byKey(const ValueKey('consent-MARKETING'));
    await tester.scrollUntilVisible(marketingTile, 120);
    await tester.pumpAndSettle();
    expect(tester.widget<SwitchListTile>(marketingTile).value, isTrue,
        reason: 'MARKETING etait accorde au depart');

    // On tape le Switch lui-meme : le point central d une SwitchListTile
    // peut tomber hors de la zone cliquable apres un scroll.
    await tester.tap(find.descendant(of: marketingTile, matching: find.byType(Switch)),
        warnIfMissed: false);
    await tester.pumpAndSettle();

    final post = api.bodies.first as Map<String, dynamic>;
    expect(post['granted'], isFalse);
    expect(post['type'], 'MARKETING');
  });

  testWidgets('erreur réseau -> message actionnable, PAS d\'écran vide', (tester) async {
    final api = _FakeApi({})..fail = true;
    await tester.pumpWidget(_wrap(api));
    await tester.pumpAndSettle();

    expect(find.textContaining('Impossible de charger'), findsOneWidget);
    expect(find.text('Réessayer'), findsOneWidget);
  });
}
