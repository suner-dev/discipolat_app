// G1 (mobile) — Le formulaire d'identité consomme le catalogue ISO-4217 du
// backend et ne bloque jamais l'utilisateur.
//
// Convention du dépôt : un faux `ApiService` injecté (comme
// compliance_service_test.dart), aucune dépendance ajoutée.

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/currency_catalog_service.dart';
import 'package:discipolat_mobile/presentation/screens/tenant/onboarding_step_forms.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

const _catalog = {
  'standard': 'ISO-4217',
  'count': 2,
  'currencies': [
    {'code': 'XAF', 'name': 'Franc CFA (BEAC)', 'symbol': 'FCFA', 'decimals': 0},
    {'code': 'EUR', 'name': 'Euro', 'symbol': '€', 'decimals': 2},
  ],
};

/// Faux qui respecte le contrat du service : 200 + payload, ou exception.
class _FakeApi extends ApiService {
  _FakeApi(this.response) : super(baseUrl: 'http://fake');

  final Object response;
  final List<String> calls = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add('GET $path');
    if (response is Exception) throw response as Exception;
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: response,
    );
  }
}

/// Le formulaire est monté dans un `ListView` par l'écran du wizard
/// (tenant_onboarding_screen.dart:232). On reproduit ce montage réel : isoler
/// le formulaire dans une page fixe ferait déborder la colonne alors que
/// l'utilisateur, lui, fait défiler.
Widget _host(_FakeApi api, void Function(Map<String, dynamic>) onSubmit) => MaterialApp(
      home: Scaffold(
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            ChurchIdentityForm(enabled: true, onSubmit: onSubmit, apiService: api),
          ],
        ),
      ),
    );

void main() {
  group('CurrencyCatalogService', () {
    test('lit GET /platform/currencies et décode strictement', () async {
      final api = _FakeApi(_catalog);

      final catalog = await CurrencyCatalogService(api).fetch();

      expect(api.calls, ['GET /platform/currencies']);
      expect(catalog.fromServer, isTrue);
      expect(catalog.currencies.map((c) => c.code), containsAll(['XAF', 'EUR']));
      // Le libellé porte le symbole : l'utilisateur reconnaît sa devise.
      expect(
        catalog.currencies.firstWhere((c) => c.code == 'XAF').label,
        contains('FCFA'),
      );
    });

    test('repli honnête si le serveur échoue', () async {
      final catalog = await CurrencyCatalogService(_FakeApi(Exception('500'))).fetch();

      expect(catalog.fromServer, isFalse);
      expect(catalog.currencies, isNotEmpty);
    });

    test('repli si la réponse est inexploitable (forme inattendue)', () async {
      // Le backend a changé de forme : ni planter, ni afficher une liste vide.
      final catalog = await CurrencyCatalogService(
        _FakeApi({'standard': 'ISO-4217', 'currencies': 'pas une liste'}),
      ).fetch();

      expect(catalog.fromServer, isFalse);
      expect(catalog.currencies, isNotEmpty);
    });

    test('une entrée invalide est rejetée sans faire tomber le catalogue', () async {
      final catalog = await CurrencyCatalogService(_FakeApi({
        'standard': 'ISO-4217',
        'currencies': [
          {'code': 'XAF', 'name': 'Franc CFA', 'symbol': 'FCFA', 'decimals': 0},
          {'code': 'ZZ', 'name': '', 'symbol': '', 'decimals': 9},
        ],
      })).fetch();

      expect(catalog.currencies.length, 1);
      expect(catalog.currencies.single.code, 'XAF');
    });
  });

  group('ChurchIdentityForm — champ devise', () {
    testWidgets('charge le catalogue et l\'annonce quand il est servi', (tester) async {
      await tester.pumpWidget(_host(_FakeApi(_catalog), (_) {}));
      await tester.pump();
      await tester.pump();

      expect(find.textContaining('Liste officielle des devises'), findsOneWidget);
      expect(find.textContaining('indisponible'), findsNothing);
    });

    testWidgets('avertit quand le catalogue est indisponible, sans bloquer la saisie',
        (tester) async {
      await tester.pumpWidget(_host(_FakeApi(Exception('500')), (_) {}));
      await tester.pump();
      await tester.pump();

      expect(find.textContaining('indisponible'), findsOneWidget);
      // Repli, pas blocage : le champ devise reste saisissable.
      final field = tester.widget<TextField>(
        find.descendant(
          of: find.byType(Autocomplete<CurrencyOption>),
          matching: find.byType(TextField),
        ),
      );
      expect(field.enabled, isTrue);
    });

    testWidgets('soumet la devise saisie, au format attendu par le backend',
        (tester) async {
      Map<String, dynamic>? submitted;
      await tester.pumpWidget(_host(_FakeApi(_catalog), (data) => submitted = data));
      await tester.pump();
      await tester.pump();

      await tester.enterText(
        find.widgetWithText(TextField, "Nom de l'église *"),
        'Église de Douala',
      );
      await tester.enterText(
        find.widgetWithText(TextField, 'Devise (ISO-4217)'),
        'XAF',
      );
      await tester.tap(find.text('Enregistrer'));
      await tester.pump();

      expect(submitted, isNotNull);
      expect(submitted!['churchName'], 'Église de Douala');
      expect(submitted!['currency'], 'XAF');
    });
  });
}
