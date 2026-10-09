// LOT 1 §GLISE-D'ABORD (T1.6, mobile) — widget test du `ChurchPicker`.
//
// Verrouille le CONTRAT §6 du plan côté mobile :
//   1. deux chemins uniques, `/public/churches/suggest` et
//      `/public/churches/exists` — jamais un troisième, jamais un chemin
//      d'annuaire plus large ;
//   2. champs consommés limités à la liste blanche (`name`, `slug`, `city`,
//      `country`, `total`, `items`, `found`) — une réponse du serveur qui
//      fuiterait un `tenantId` ou un `email` doit rester INVISIBLE ;
//   3. debounce 300 ms : aucune requête avant l'écoulement du délai ;
//   4. `minLength = 2` : en deçà, pas de requête (le serveur répond 400,
//      inutile de consommer le quota `PerIpRateLimiter`) ;
//   5. « non listée » et « inexistante » répondent PAREIL (R3, 404
//      indistinguable) ;
//   6. les CTA « non trouvée » ne naviguent PAS depuis le picker : la
//      navigation reste l'affaire de l'hôte, pour ne pas dupliquer GoRouter
//      (A1/A3, Annexe C #1).
//
// Convention du dépôt : un faux `ApiService` injecté, aucune dépendance
// nouvelle (cf. `currency_catalog_test.dart`, `accept_invitation_screen_test.dart`).

import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/login/church_picker.dart';

/// Faux ApiService : journalise chaque `GET` et renvoie le payload fourni.
/// Le `Object? error` permet de simuler un échec Dio (quota, réseau).
class _FakeApi extends ApiService {
  _FakeApi() : super(baseUrl: 'http://fake');

  final List<String> calls = [];
  final List<Map<String, dynamic>?> params = [];
  Object? nextError;

  /// Body renvoyé par `/suggest` (liste) et `/exists` (booléen).
  Map<String, dynamic> suggestBody = {
    'total': 0,
    'items': <Map<String, dynamic>>[],
  };
  Map<String, dynamic> existsBody = {'found': false};

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    calls.add(path);
    this.params.add(params ?? queryParameters);
    if (nextError != null) {
      final err = nextError!;
      nextError = null;
      throw err;
    }
    final body = path.endsWith('/suggest') ? suggestBody : existsBody;
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: body,
    );
  }
}

Widget _host(Widget child) => MaterialApp(
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );

/// Fait avancer le faux timer du debounce (le widget utilise un vrai
/// `Timer(Duration(300ms))`, donc `pumpAndSettle` suffit en général).
Future<void> _flush(WidgetTester tester) async {
  await tester.pump(const Duration(milliseconds: 400));
  await tester.pumpAndSettle();
}

void main() {
  group('LOT 1 §GLISE-D\u2019ABORD (T1.6, mobile) — ChurchPicker', () {
    testWidgets('au montage : idle, aucune requête réseau', (tester) async {
      final api = _FakeApi();
      await tester.pumpWidget(_host(ChurchPicker(apiService: api)));
      expect(find.text('Votre \u00e9glise'), findsOneWidget);
      expect(find.text('Commencez \u00e0 taper le nom\u2026'), findsOneWidget);
      expect(api.calls, isEmpty,
          reason: 'aucune requête ne part avant que l\u2019utilisateur tape');
    });

    testWidgets('saisie courte (< 2 caractères) : toujours aucune requête',
        (tester) async {
      final api = _FakeApi();
      await tester.pumpWidget(_host(ChurchPicker(apiService: api)));
      await tester.enterText(find.byType(TextField), 'B');
      await _flush(tester);
      expect(api.calls, isEmpty);
    });

    testWidgets('debounce : une seule suggestion pour plusieurs frappes',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {
          'total': 1,
          'items': [
            {'name': 'Bethel', 'slug': 'bethel', 'city': 'Douala', 'country': 'CM'},
          ],
        };
      await tester.pumpWidget(_host(ChurchPicker(apiService: api)));
      // Trois frappes rapprochées : le timer recharge à chaque frappe, une
      // seule requête part à la fin.
      await tester.enterText(find.byType(TextField), 'Be');
      await tester.pump(const Duration(milliseconds: 50));
      await tester.enterText(find.byType(TextField), 'Bet');
      await tester.pump(const Duration(milliseconds: 50));
      await tester.enterText(find.byType(TextField), 'Beth');
      await _flush(tester);
      expect(api.calls, ['/public/churches/suggest']);
      expect(api.params.single, {'q': 'Beth'});
    });

    testWidgets('chemin et nom de champ figés (contrat §6, R2/R3)',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {
          'total': 2,
          'items': [
            {
              'name': 'Bethel',
              'slug': 'bethel',
              'city': 'Douala',
              'country': 'CM',
              // Ces deux clés NE DOIVENT PAS être lues par le widget.
              'tenantId': 'should-be-invisible',
              'email': 'pasteur@should-be-invisible.example',
            },
            {
              'name': 'Grace',
              'slug': 'grace',
              'city': 'Kinshasa',
              'country': 'CD',
            },
          ],
        };
      await tester.pumpWidget(_host(ChurchPicker(apiService: api)));
      await tester.enterText(find.byType(TextField), 'eglise');
      await _flush(tester);
      expect(api.calls, contains('/public/churches/suggest'));
      // Le texte des suggestions ne contient JAMAIS une des clés fuitées.
      expect(find.textContaining('should-be-invisible'), findsNothing);
      expect(find.textContaining('@'), findsNothing);
      // Les deux noms + localités sont visibles.
      expect(find.text('Bethel'), findsOneWidget);
      expect(find.text('Douala, CM'), findsOneWidget);
      expect(find.text('Grace'), findsOneWidget);
      expect(find.text('Kinshasa, CD'), findsOneWidget);
    });

    testWidgets('tap sur une suggestion : onSelect(name, slug) émis, aucune navigation interne',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {
          'total': 1,
          'items': [
            {'name': 'Bethel', 'slug': 'bethel', 'city': 'Douala', 'country': 'CM'},
          ],
        };
      String? pickedName;
      String? pickedSlug;
      await tester.pumpWidget(_host(ChurchPicker(
        apiService: api,
        onSelect: (n, s) {
          pickedName = n;
          pickedSlug = s;
        },
      )));
      await tester.enterText(find.byType(TextField), 'Beth');
      await _flush(tester);
      await tester.tap(find.text('Bethel'));
      await tester.pumpAndSettle();
      expect(pickedName, 'Bethel');
      expect(pickedSlug, 'bethel');
      // Le picker ne navigue pas : c'est à l'hôte (register_screen.dart)
      // d'écrire l'état. Aucun `context.go(` / `context.push(` ici.
      expect(find.textContaining('Rejoindre avec un code'), findsNothing);
    });

    testWidgets('suggest vide → notFound + CTA (aucune navigation émise)',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {'total': 0, 'items': <Map<String, dynamic>>[]};
      String? notFoundName;
      await tester.pumpWidget(_host(ChurchPicker(
        apiService: api,
        onNotFound: (typed) => notFoundName = typed,
      )));
      await tester.enterText(find.byType(TextField), 'zzzz');
      await _flush(tester);
      expect(find.text('Aucune \u00e9glise publique ne correspond.'),
          findsOneWidget);
      expect(find.bySemanticsIdentifier('churchPicker.cta.join'), findsOneWidget);
      expect(find.bySemanticsIdentifier('churchPicker.cta.accept-invitation'),
          findsOneWidget);
      expect(find.bySemanticsIdentifier('churchPicker.cta.create-church'),
          findsOneWidget);
      // onNotFound n'est appelé QUE sur Entrée (confirmExact), pas sur
      // la suggestion vide : vérifier que ce n'est PAS déclenché ici.
      expect(notFoundName, isNull);
    });

    testWidgets('Entrée avec /exists found=true : onSelect',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {'total': 0, 'items': <Map<String, dynamic>>[]}
        ..existsBody = {'found': true, 'name': 'Bethel', 'slug': 'bethel'};
      String? name;
      String? slug;
      await tester.pumpWidget(_host(ChurchPicker(
        apiService: api,
        onSelect: (n, s) {
          name = n;
          slug = s;
        },
      )));
      await tester.enterText(find.byType(TextField), 'Bethel');
      await _flush(tester);
      // Simule la touche Entrée sur le TextField.
      await tester.tap(find.byType(TextField));
      await tester.testTextInput.receiveAction(TextInputAction.search);
      await tester.pumpAndSettle();
      expect(api.calls, contains('/public/churches/exists'));
      expect(name, 'Bethel');
      expect(slug, 'bethel');
    });

    testWidgets('Entrée avec /exists found=false : onNotFound, réponse indistinguable',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {'total': 0, 'items': <Map<String, dynamic>>[]}
        ..existsBody = {'found': false};
      String? notFoundName;
      await tester.pumpWidget(_host(ChurchPicker(
        apiService: api,
        onNotFound: (typed) => notFoundName = typed,
      )));
      await tester.enterText(find.byType(TextField), 'fantome');
      await _flush(tester);
      await tester.tap(find.byType(TextField));
      await tester.testTextInput.receiveAction(TextInputAction.search);
      await tester.pumpAndSettle();
      expect(notFoundName, 'fantome');
      // R3 : le corps `{found:false}` est le MÊME pour « non listée » et
      // « inexistante ». Le picker n'a AUCUN moyen de distinguer.
      expect(api.calls, contains('/public/churches/exists'));
    });

    testWidgets('erreur réseau (DioException) : état error, non bloquant',
        (tester) async {
      final api = _FakeApi();
      await tester.pumpWidget(_host(ChurchPicker(apiService: api)));
      // Programmation d'une erreur sur la PROCHAINE requête.
      api.nextError = DioException(
        requestOptions: RequestOptions(path: '/public/churches/suggest'),
        type: DioExceptionType.connectionTimeout,
        message: 'timeout',
      );
      await tester.enterText(find.byType(TextField), 'Bethel');
      await _flush(tester);
      expect(
        find.textContaining('Indisponible pour le moment'),
        findsOneWidget,
      );
      expect(find.textContaining('timeout'), findsOneWidget);
      expect(find.widgetWithText(TextButton, 'R\u00e9essayer'), findsOneWidget);
      // IMPORTANT : le formulaire hôte reste utilisable, le picker ne
      // bloque RIEN (D5, non-bloquant). On vérifie qu'aucune modal n'est
      // ouverte en interceptant tout le pipeline.
      expect(find.byType(SnackBar), findsNothing);
      expect(find.byType(Dialog), findsNothing);
    });

    testWidgets('Effacer (X) : réinitialise l\u2019état sans rappeler le serveur',
        (tester) async {
      final api = _FakeApi()
        ..suggestBody = {
          'total': 1,
          'items': [
            {'name': 'Bethel', 'slug': 'bethel'},
          ],
        };
      await tester.pumpWidget(_host(ChurchPicker(apiService: api)));
      await tester.enterText(find.byType(TextField), 'Beth');
      await _flush(tester);
      expect(api.calls.length, 1);
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();
      expect(find.text('Beth'), findsNothing); // input vidé
      expect(api.calls.length, 1, reason: 'clear ne déclenche AUCUNE requête');
    });
  });
}
