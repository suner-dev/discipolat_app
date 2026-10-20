// Tests des écrans de gestion des dress codes (lecture + édition).
// Vérifie : rendu, chargement, validation, gestion des règles, et soumission
// (POST /dress-codes, PUT /dress-codes/{id}) avec vérification des payloads.
//
// AUCUN appel réseau : l'API est injectée via le paramètre `apiService`.
import 'dart:async';

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/dress_code/dress_code_editor_screen.dart';
import 'package:discipolat_mobile/presentation/screens/dress_code/dress_code_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Faux `ApiService` conforme à la convention du dépôt (`Response` dio).
class _FakeDressCodeApi extends ApiService {
  _FakeDressCodeApi({
    this.list,
    this.detail,
    this.failGet = false,
    this.failSave = false,
    this.pendingGet,
  }) : super(baseUrl: 'http://fake');

  /// Réponse de `GET /dress-codes`.
  final List<Map<String, dynamic>>? list;

  /// Réponse de `GET /dress-codes/{id}`.
  final Map<String, dynamic>? detail;

  /// Provoque une erreur réseau sur les GET.
  final bool failGet;

  /// Provoque une erreur réseau sur POST/PUT.
  final bool failSave;

  /// Si non nul, le GET reste en vol jusqu'à la complétion du completer :
  /// permet d'observer l'état « chargement ».
  final Completer<void>? pendingGet;

  final List<String> getPaths = [];
  final List<String> postPaths = [];
  final List<String> putPaths = [];
  final List<Map<String, dynamic>?> bodies = [];

  @override
  Future<Response> get(
    String path, {
    Map<String, dynamic>? params,
    Map<String, dynamic>? queryParameters,
  }) async {
    getPaths.add(path);
    if (pendingGet != null) {
      await pendingGet!.future;
    }
    if (failGet) {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        type: DioExceptionType.connectionError,
      );
    }
    if (path == '/dress-codes') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: list ?? [],
      );
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: detail ?? {},
    );
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    bodies.add(data is Map<String, dynamic> ? data : null);
    if (failSave) {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        type: DioExceptionType.connectionError,
      );
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 201,
      data: {'id': 'created-1'},
    );
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    bodies.add(data is Map<String, dynamic> ? data : null);
    if (failSave) {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        type: DioExceptionType.connectionError,
      );
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: {'id': 'updated-1'},
    );
  }
}

Map<String, dynamic> _dressCodeJson({
  String id = 'dc-1',
  String title = 'Tenue du dimanche',
  String serviceName = 'Accueil',
  String status = 'PUBLISHED',
  List<Map<String, dynamic>> rules = const [],
}) =>
    {
      'id': id,
      'title': title,
      'serviceName': serviceName,
      'status': status,
      'rules': rules,
    };

/// Fait défiler le formulaire jusqu'à ce que le widget cible soit visible,
/// puis le tape. Le formulaire de l'éditeur est un `SingleChildScrollView` :
/// sans ce scroll, les widgets du bas sont hors écran et le tap est ignoré.
Future<void> _scrollAndTap(
  WidgetTester tester,
  Finder finder, {
  Finder? scrollable,
}) async {
  final target = scrollable ?? find.byType(Scrollable).first;
  await tester.scrollUntilVisible(
    finder,
    120,
    scrollable: target,
  );
  await tester.pumpAndSettle();
  await tester.tap(finder);
  await tester.pumpAndSettle();
}

void main() {
  group('DressCodeScreen — lecture seule', () {
    testWidgets('affiche "Aucun dress code" quand la liste est vide',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: DressCodeScreen(apiService: _FakeDressCodeApi())),
      );
      await tester.pumpAndSettle();

      expect(find.text('Aucun dress code'), findsOneWidget);
    });

    testWidgets('affiche le chargement en cours avant la réponse',
        (tester) async {
      final gate = Completer<void>();
      final api = _FakeDressCodeApi(pendingGet: gate);

      await tester.pumpWidget(
        MaterialApp(home: DressCodeScreen(apiService: api)),
      );
      // La requête est en vol : l'écran affiche l'indicateur de chargement.
      await tester.pump();

      expect(find.byType(CircularProgressIndicator), findsOneWidget);

      gate.complete();
      await tester.pumpAndSettle();
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });

    testWidgets('affiche le titre, le service, le statut et les règles',
        (tester) async {
      final api = _FakeDressCodeApi(
        list: [
          _dressCodeJson(
            rules: [
              {'groupName': 'Hommes'},
              {'groupName': 'Femmes'},
            ],
          )
        ],
      );

      await tester.pumpWidget(
        MaterialApp(home: DressCodeScreen(apiService: api)),
      );
      await tester.pumpAndSettle();

      expect(find.text('Tenue du dimanche'), findsOneWidget);
      expect(find.text('Accueil • PUBLISHED'), findsOneWidget);
      expect(find.text('Hommes'), findsOneWidget);
      expect(find.text('Femmes'), findsOneWidget);
      expect(api.getPaths, contains('/dress-codes'));
    });

    testWidgets(
        'affiche un message d\'erreur et « Réessayer » si le GET échoue',
        (tester) async {
      final api = _FakeDressCodeApi(failGet: true);

      await tester.pumpWidget(
        MaterialApp(home: DressCodeScreen(apiService: api)),
      );
      await tester.pumpAndSettle();

      expect(find.text('Impossible de charger les dress codes'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });

  group('DressCodeEditorScreen — rendu et chargement', () {
    testWidgets('affiche "Créer une tenue" quand aucun id n\'est fourni',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: DressCodeEditorScreen(apiService: _FakeDressCodeApi())),
      );

      expect(find.text('Créer une tenue'), findsOneWidget);
      expect(find.text('Modifier la tenue'), findsNothing);
    });

    testWidgets('affiche "Modifier la tenue" quand un id est fourni',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: DressCodeEditorScreen(
            id: 'dc-42',
            apiService: _FakeDressCodeApi(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Modifier la tenue'), findsOneWidget);
      expect(find.text('Créer une tenue'), findsNothing);
    });

    testWidgets('charge le dress code et ses règles via GET /dress-codes/{id}',
        (tester) async {
      final api = _FakeDressCodeApi(
        detail: _dressCodeJson(
          id: 'dc-42',
          title: 'Tenue de Noël',
          serviceName: 'Messe',
          status: 'DRAFT',
          rules: [
            {'groupName': 'Protocole', 'description': 'Costume sombre'},
          ],
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: DressCodeEditorScreen(id: 'dc-42', apiService: api),
        ),
      );
      await tester.pumpAndSettle();

      expect(api.getPaths, contains('/dress-codes/dc-42'));
      expect(find.text('Tenue de Noël'), findsOneWidget);
      expect(find.text('Protocole'), findsOneWidget);
      expect(find.text('Costume sombre'), findsOneWidget);
    });

    testWidgets('affiche une erreur de chargement si le GET échoue',
        (tester) async {
      final api = _FakeDressCodeApi(failGet: true);

      await tester.pumpWidget(
        MaterialApp(
          home: DressCodeEditorScreen(id: 'dc-42', apiService: api),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Impossible de charger le dress code'), findsOneWidget);
    });
  });

  group('DressCodeEditorScreen — règles', () {
    testWidgets('« Ajouter une règle » ajoute un formulaire de règle',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: DressCodeEditorScreen(apiService: _FakeDressCodeApi())),
      );
      await tester.pumpAndSettle();

      expect(find.text('Groupe'), findsNothing);

      await _scrollAndTap(tester, find.text('Ajouter une règle'));

      expect(find.text('Groupe'), findsOneWidget);
      expect(find.text('Description'), findsOneWidget);
    });

    testWidgets('l\'icône de suppression retire la règle correspondante',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: DressCodeEditorScreen(apiService: _FakeDressCodeApi())),
      );
      await tester.pumpAndSettle();

      await _scrollAndTap(tester, find.text('Ajouter une règle'));
      await _scrollAndTap(tester, find.text('Ajouter une règle'));

      expect(find.text('Groupe'), findsNWidgets(2));

      await tester.tap(find.byIcon(Icons.remove_circle_outline).first);
      await tester.pumpAndSettle();

      expect(find.text('Groupe'), findsOneWidget);
    });
  });

  group('DressCodeEditorScreen — validation', () {
    testWidgets(
        'refuse d\'enregistrer sans titre et affiche l\'erreur de champ',
        (tester) async {
      final api = _FakeDressCodeApi();

      await tester.pumpWidget(
        MaterialApp(home: DressCodeEditorScreen(apiService: api)),
      );
      await tester.pumpAndSettle();

      await _scrollAndTap(tester, find.text('Sauvegarder'));

      expect(find.text('Champ requis'), findsWidgets);
      // Aucun appel réseau : la validation a bloqué la soumission.
      expect(api.postPaths, isEmpty);
    });
  });

  group('DressCodeEditorScreen — soumission', () {
    testWidgets('crée un dress code via POST /dress-codes avec le bon payload',
        (tester) async {
      final api = _FakeDressCodeApi();

      await tester.pumpWidget(
        MaterialApp(home: DressCodeEditorScreen(apiService: api)),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.widgetWithText(TextFormField, 'Titre *'),
        'Tenue de Noël',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Service (événement/espace)'),
        'Messe',
      );
      await tester.pumpAndSettle();

      await _scrollAndTap(tester, find.text('Ajouter une règle'));
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Groupe'),
        'Protocole',
      );
      await tester.pumpAndSettle();

      await _scrollAndTap(tester, find.text('Sauvegarder'));

      expect(api.postPaths, ['/dress-codes']);
      expect(api.putPaths, isEmpty);

      final body = api.bodies.single!;
      expect(body['title'], 'Tenue de Noël');
      expect(body['serviceName'], 'Messe');
      expect(body['status'], 'DRAFT');
      expect((body['rules'] as List).length, 1);
      expect((body['rules'] as List).first['groupName'], 'Protocole');

      expect(find.text('Dress code créé'), findsOneWidget);
    });

    testWidgets('met à jour un dress code via PUT /dress-codes/{id}',
        (tester) async {
      final api = _FakeDressCodeApi(
        detail: _dressCodeJson(id: 'dc-42', title: 'Ancienne tenue'),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: DressCodeEditorScreen(id: 'dc-42', apiService: api),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.widgetWithText(TextFormField, 'Titre *'),
        'Nouvelle tenue',
      );
      await tester.pumpAndSettle();

      await _scrollAndTap(tester, find.text('Sauvegarder'));

      expect(api.putPaths, ['/dress-codes/dc-42']);
      expect(api.postPaths, isEmpty);
      expect(api.bodies.single!['title'], 'Nouvelle tenue');
      expect(find.text('Dress code mis à jour'), findsOneWidget);
    });

    testWidgets('affiche un message d\'erreur si la sauvegarde échoue',
        (tester) async {
      final api = _FakeDressCodeApi(failSave: true);

      await tester.pumpWidget(
        MaterialApp(home: DressCodeEditorScreen(apiService: api)),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.widgetWithText(TextFormField, 'Titre *'),
        'Tenue de Noël',
      );
      await tester.pumpAndSettle();

      await _scrollAndTap(tester, find.text('Sauvegarder'));

      expect(find.text('Erreur lors de l\'enregistrement'), findsOneWidget);
    });
  });
}
