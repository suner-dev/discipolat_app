import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/l10n/app_localizations.dart';
import 'package:discipolat_mobile/presentation/widgets/parcours_card.dart';

/// « Mon parcours pastoral » (mobile) — récit chronologique en lecture seule.
///
/// Couvre : la traduction des CODES techniques renvoyés par le serveur
/// (ADHESION | TRANSFERT | CAPACITE | RATTACHEMENT), le tri déjà opéré
/// côté serveur (aucune re-tri côté widget), les signaux faibles du `resume`,
/// le repli SILCIEUX quand il n'y a rien à raconter (jamais un bloc vide
/// trompeur), l'erreur explicite avec réessai, et le parsing dégradé.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  String? lastPath;
  bool fail = false;
  int failStatus = 0;
  Map<String, dynamic> payload = const {};

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    lastPath = path;
    if (fail &&
        (path == '/hierarchy/me/parcours' ||
            path.startsWith('/hierarchy/users/') && path.endsWith('/parcours'))) {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        response: failStatus == 0
            ? null
            : Response(
                requestOptions: RequestOptions(path: path),
                statusCode: failStatus,
              ),
        error: 'network down',
      );
    }
    return Response(requestOptions: RequestOptions(path: path), data: payload);
  }
}

Widget wrap(Widget child) => MaterialApp(
      locale: const Locale('fr'),
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );

void main() {
  late _FakeApiService api;

  setUp(() {
    api = _FakeApiService();
  });

  group('MyParcoursCard', () {
    testWidgets('lit /hierarchy/me/parcours et traduit les natures d\'événement',
        (WidgetTester tester) async {
      api.payload = {
        'userId': 'u-awa',
        'nomComplet': 'Awa Diallo',
        'depuis': '2020-01-01T00:00:00Z',
        'totalEvenements': 3,
        'evenements': [
          {
            'horodatage': '2020-01-01T00:00:00Z',
            'type': 'ADHESION',
            'action': 'DEBUT',
            'libelle': 'Adhésion · Église'
          },
          {
            'horodatage': '2021-06-01T00:00:00Z',
            'type': 'CAPACITE',
            'action': 'DEBUT',
            'libelle': 'Capacité · Berger · Campus Nord'
          },
          {
            'horodatage': '2023-01-01T00:00:00Z',
            'type': 'RATTACHEMENT',
            'action': 'FIN',
            'libelle': 'Rattachement retiré · Mon pasteur · Jean Maka'
          },
        ],
        'resume': {
          'parType': {'ADHESION': 1, 'CAPACITE': 1, 'RATTACHEMENT': 1},
          'transferts': 0,
          'rattachementsRompus': 1
        },
      };

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api)));
      await tester.pumpAndSettle();

      expect(api.lastPath, '/hierarchy/me/parcours');
      expect(find.text('Mon parcours pastoral'), findsOneWidget);
      // Codes techniques JAMAIS affichés tels quel : le membre lit sa langue.
      expect(find.textContaining('ADHESION'), findsNothing);
      expect(find.text('Adhésion · Début · 01/01/2020'), findsOneWidget);
      expect(find.text('Capacité · Début · 01/06/2021'), findsOneWidget);
      expect(find.text('Rattachement · Fin · 01/01/2023'), findsOneWidget);
      expect(find.text('Membre depuis le 01/01/2020'), findsOneWidget);
      // Signal faible : seule une rupture ici — le compteur vient du serveur.
      expect(find.text('1 rattachement(s) rompu(s)'), findsOneWidget);
      expect(find.textContaining('transfert(s)'), findsNothing);
    });

    testWidgets('aucun récit : rien ne s\'affiche (état normal, pas une erreur)',
        (WidgetTester tester) async {
      api.payload = {'evenements': <Map<String, dynamic>>[], 'resume': {}};

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api)));
      await tester.pumpAndSettle();

      expect(find.text('Mon parcours pastoral'), findsNothing);
      expect(find.text('Parcours indisponible.'), findsNothing);
    });

    testWidgets('brique en panne : message explicite + réessai, jamais un vide silencieux',
        (WidgetTester tester) async {
      api.fail = true;

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api)));
      await tester.pumpAndSettle();

      expect(find.text('Parcours indisponible.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);

      // Le réessai repasse bien par le service (et réussit une fois la panne passée).
      api.fail = false;
      api.payload = {
        'evenements': [
          {'horodatage': '2024-02-01T00:00:00Z', 'type': 'TRANSFERT', 'action': 'FIN', 'libelle': 'Transfert'}
        ],
        'resume': {'transferts': 1, 'rattachementsRompus': 0},
      };
      await tester.tap(find.text('Réessayer'));
      await tester.pumpAndSettle();

      expect(find.text('Parcours indisponible.'), findsNothing);
      expect(find.text('Transfert · Fin · 01/02/2024'), findsOneWidget);
      expect(find.text('1 transfert(s)'), findsOneWidget);
    });

    testWidgets('événement sans date : le récit reste lisible (tri nulls-last serveur)',
        (WidgetTester tester) async {
      api.payload = {
        'evenements': [
          {'horodatage': null, 'type': 'CAPACITE', 'action': 'DEBUT', 'libelle': 'Capacité'}
        ],
        'resume': {},
      };

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api)));
      await tester.pumpAndSettle();

      expect(find.text('Capacité · Début'), findsOneWidget);
      expect(find.text('Membre depuis le'), findsNothing);
    });

    testWidgets("d'un tiers : endpoint scopé /hierarchy/users/{id}/parcours",
        (WidgetTester tester) async {
      api.payload = {
        'evenements': [
          {'horodatage': '2020-01-01T00:00:00Z', 'type': 'ADHESION', 'action': 'DEBUT', 'libelle': 'Adhésion'}
        ],
        'resume': {},
      };

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api, userId: 'u-tiers')));
      await tester.pumpAndSettle();

      expect(api.lastPath, '/hierarchy/users/u-tiers/parcours');
      expect(find.text('Adhésion · Début · 01/01/2020'), findsOneWidget);
    });

    testWidgets('garde refusée (403) sur un tiers : repli silencieux, pas un message de panne',
        (WidgetTester tester) async {
      api.fail = true;
      api.failStatus = 403;

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api, userId: 'u-tiers')));
      await tester.pumpAndSettle();

      expect(find.text('Parcours indisponible.'), findsNothing);
      expect(find.text('Mon parcours pastoral'), findsNothing);
    });

    testWidgets('panne réseau (sans statut) sur un tiers : erreur explicite + réessai',
        (WidgetTester tester) async {
      api.fail = true;

      await tester.pumpWidget(wrap(MyParcoursCard(apiService: api, userId: 'u-tiers')));
      await tester.pumpAndSettle();

      expect(find.text('Parcours indisponible.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });

  group('ParcoursParsing', () {
    test('réponse dégradée → liste vide, jamais d\'exception', () {
      expect(ParcoursParsing.eventsOf(const {}), isEmpty);
      expect(ParcoursParsing.eventsOf({'evenements': 'oops'}), isEmpty);
      expect(
          ParcoursParsing.eventsOf({
            'evenements': [
              {'type': 'ADHESION'},
              'ligne_non_objet',
            ]
          }).length,
          1);
      expect(ParcoursParsing.resumeOf({'resume': 42}), isEmpty);
      expect(ParcoursParsing.intOf({'transferts': 3.0}, 'transferts'), 3);
      expect(ParcoursParsing.intOf({'transferts': null}, 'transferts'), 0);
    });

    test('ParcoursEvent parse l\'ISO et distingue DEBUT/FIN', () {
      final e = ParcoursEvent.fromJson({
        'horodatage': '2021-06-01T00:00:00Z',
        'type': 'CAPACITE',
        'action': 'FIN',
        'libelle': 'Fin de capacité · Berger',
      });
      expect(e.date, DateTime.parse('2021-06-01T00:00:00Z'));
      expect(e.isFin, isTrue);
      expect(ParcoursEvent.fromJson(const {}).date, isNull);
      expect(ParcoursEvent.fromJson(const {}).isFin, isFalse);
    });
  });

  group('parité i18n du parcours', () {
    const keys = [
      'parcoursTitle',
      'parcoursSince',
      'parcoursTypeAdhesion',
      'parcoursTypeTransfert',
      'parcoursTypeCapacite',
      'parcoursTypeRattachement',
      'parcoursActionDebut',
      'parcoursActionFin',
      'parcoursTransferts',
      'parcoursRuptures',
      'parcoursUnavailable',
    ];

    test('les 6 locales portent les mêmes clés de parcours', () {
      for (final locale in [
        const Locale('fr'),
        const Locale('en'),
        const Locale('pt'),
        const Locale('es'),
        const Locale('sw'),
        const Locale('ar'),
      ]) {
        final l10n = AppLocalizations(locale);
        for (final key in keys) {
          final value = l10n.translate(key);
          expect(value, isNot(key), reason: '$locale / $key intraduit');
          expect(value, isNotEmpty);
        }
      }
    });

    test('les placeholders sont respectés dans toutes les locales', () {
      final en = AppLocalizations(const Locale('en'));
      expect(en.parcoursSince('01/01/2020'), 'Member since 01/01/2020');
      expect(en.parcoursTransferts(2), '2 transfer(s)');
      expect(en.parcoursRuptures(1), '1 broken connection(s)');
      final sw = AppLocalizations(const Locale('sw'));
      expect(sw.parcoursTransferts(2), contains('2'));
      expect(sw.parcoursRuptures(1), contains('1'));
    });
  });
}
