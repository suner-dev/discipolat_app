import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/local/locale_provider.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/l10n/app_localizations.dart';
import 'package:discipolat_mobile/presentation/screens/profile/my_relations_card.dart';

/// V231 — « Mon encadrement » (mobile).
///
/// Couvre : lecture via les modèles typés, source PAGINÉE de « mes membres »,
/// nom cliquable (ouverture de la fiche), bouton de détachement conditionné par
/// le serveur (`revocable`), et état d'erreur explicite (jamais un vide
/// silencieux qui ferait croire à « aucun encadrant »).
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  String? deletePath;
  String? postPath;
  dynamic postBody;
  bool failRelations = false;
  int membersTotal = 1;
  List<Map<String, dynamic>> membersOnPage0 = [];

  static final _pasteur = {
    'id': 'rel-1',
    'fromUserId': 'u-moi',
    'toUserId': 'u-pasteur',
    'otherUserId': 'u-pasteur',
    'otherNom': 'Jean Maka',
    'relationType': 'PASTEUR',
    'typeLabel': 'Mon pasteur',
    'statut': 'ACTIVE',
    'revocable': true,
  };

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    if (failRelations &&
        (path == '/relations/me' || path == '/relations/me/members')) {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        error: 'network down',
      );
    }
    if (path == '/relations/types') {
      return Response(
        requestOptions: RequestOptions(path: path),
        data: [
          {'code': 'PASTEUR', 'label': 'Mon pasteur'},
          {'code': 'MENTOR', 'label': 'Mon mentor'},
        ],
      );
    }
    if (path == '/relations/me') {
      return Response(
        requestOptions: RequestOptions(path: path),
        data: {'sortantes': [_pasteur], 'entrantes': <Map<String, dynamic>>[]},
      );
    }
    if (path == '/relations/me/members') {
      final page = (params?['page'] ?? queryParameters?['page'] ?? 0) as int;
      return Response(
        requestOptions: RequestOptions(path: path),
        data: {
          'content': page == 0
              ? membersOnPage0
              : <Map<String, dynamic>>[],
          'totalElements': membersTotal,
          'totalPages': (membersTotal / 25).ceil().clamp(1, 999),
          'number': page,
          'size': 25,
        },
      );
    }
    return Response(requestOptions: RequestOptions(path: path), data: []);
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPath = path;
    postBody = data;
    return Response(
      requestOptions: RequestOptions(path: path),
      data: {'id': 'rel-new', 'otherUserId': 'u-x', 'otherNom': 'X',
        'relationType': 'MENTOR', 'typeLabel': 'Mon mentor', 'statut': 'ACTIVE'},
      statusCode: 201,
    );
  }

  @override
  Future<Response> delete(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    deletePath = path;
    return Response(
      requestOptions: RequestOptions(path: path),
      data: {'id': 'rel-1', 'otherUserId': 'u-pasteur', 'otherNom': 'Jean Maka',
        'relationType': 'PASTEUR', 'typeLabel': 'Mon pasteur',
        'statut': 'REVOKED', 'revocable': true},
    );
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

  testWidgets('affiche les encadrants et le total de membres rattachés',
      (WidgetTester tester) async {
    api.membersTotal = 1;
    api.membersOnPage0 = [
      {
        'id': 'rel-2',
        'fromUserId': 'u-awa',
        'toUserId': 'u-pasteur',
        'otherUserId': 'u-awa',
        'otherNom': 'Awa Diallo',
        'relationType': 'MENTOR',
        'typeLabel': 'Mon mentor',
        'statut': 'ACTIVE',
        'revocable': false,
      }
    ];

    await tester.pumpWidget(
        wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    expect(find.text('Jean Maka'), findsOneWidget);
    expect(find.text('Mon pasteur'), findsWidgets);
    expect(find.text('Awa Diallo'), findsOneWidget);
    // Le compteur de « mes membres » vient du serveur paginé.
    expect(find.textContaining('1 membres'), findsOneWidget);
  });

  testWidgets('« Détacher » n’apparaît que si le serveur déclare revocable',
      (WidgetTester tester) async {
    api.membersOnPage0 = [
      {
        'id': 'rel-2',
        'otherUserId': 'u-awa',
        'otherNom': 'Awa Diallo',
        'relationType': 'MENTOR',
        'typeLabel': 'Mon mentor',
        'statut': 'ACTIVE',
        'revocable': false, // l'utilisateur courant ne peut PAS détacher
      }
    ];

    await tester.pumpWidget(wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    // Aucun bouton de détachement pour ce membre…
    expect(find.byIcon(Icons.link_off), findsOneWidget); // seulement celui du déclarant
    // …et le nom reste tapable (ouverture de la fiche).
    expect(find.text('Awa Diallo'), findsOneWidget);
  });

  testWidgets('un encadrant peut détacher quand revocable=true',
      (WidgetTester tester) async {
    api.membersOnPage0 = [
      {
        'id': 'rel-2',
        'otherUserId': 'u-awa',
        'otherNom': 'Awa Diallo',
        'relationType': 'MENTOR',
        'typeLabel': 'Mon mentor',
        'statut': 'ACTIVE',
        'revocable': true,
      }
    ];

    await tester.pumpWidget(wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.link_off), findsNWidgets(2));
  });

  testWidgets('retirer un encadrant appelle DELETE sur la bonne relation',
      (WidgetTester tester) async {
    await tester.pumpWidget(wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    // Le bouton d'action du déclarant (Icône link_off du premier bloc).
    await tester.tap(find.byIcon(Icons.link_off).first);
    await tester.pumpAndSettle();

    expect(api.deletePath, '/relations/me/rel-1');
  });

  testWidgets('panne du serveur : message explicite + Réessayer',
      (WidgetTester tester) async {
    api.failRelations = true;

    await tester.pumpWidget(wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    expect(find.textContaining('Impossible de charger'), findsOneWidget);
    expect(find.text('Réessayer'), findsOneWidget);
    // On n'affiche surtout PAS « aucun encadrant » : ce serait un mensonge.
    expect(find.text('Aucun encadrant déclaré'), findsNothing);
  });

  testWidgets('la liste des membres est paginée côté serveur',
      (WidgetTester tester) async {
    api.membersTotal = 130; // 6 pages de 25

    await tester.pumpWidget(wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    expect(find.text('1 / 6'), findsOneWidget);
    expect(find.textContaining('130'), findsOneWidget);

    await tester.tap(find.text('Suivant'));
    await tester.pumpAndSettle();

    expect(find.text('2 / 6'), findsOneWidget);
  });

  testWidgets('aucune pagination affichée quand tout tient sur une page',
      (WidgetTester tester) async {
    api.membersTotal = 2;

    await tester.pumpWidget(wrap(MyRelationsCard(apiService: api)));
    await tester.pumpAndSettle();

    expect(find.textContaining('/ 1'), findsNothing);
  });
}
