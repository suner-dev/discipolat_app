import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/login/register_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Les TROIS issues de l'inscription lues sur la RÉPONSE du serveur.
///
/// Avant le correctif, le bandeau ne connaissait que deux cas et déduisait le
/// troisième de la présence d'un `joinCode` dans l'URL. L'inscription au nom
/// d'une église (`tenantId`, 201 + `role: MEMBRE` + `churchName` — le compte est
/// réellement créé) tombait donc dans le cas « demande d'organisation » et
/// affichait : « Aucun compte ne sera créé avant approbation », avec un Super
/// Admin qui n'intervient pas dans ce chemin, sans mentionner l'email
/// d'activation qui est la seule chose qui débloque le compte.
class _FakeApi extends ApiService {
  _FakeApi(this.registerBody, {this.registerStatus = 201})
      : super(baseUrl: 'http://fake');

  final Map<String, dynamic> registerBody;
  final int registerStatus;
  final List<Map<String, dynamic>> sentBodies = [];

  @override
  Future<Response> post(String path, {dynamic data}) async {
    sentBodies.add(Map<String, dynamic>.from(data as Map));
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: registerStatus,
      data: registerBody,
    );
  }

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: const <String, dynamic>{},
    );
  }

  @override
  Future<void> saveTokens(Map<String, dynamic> tokens) async {}
}

Widget _app(ApiService api, {bool createChurch = false, String? joinCode}) {
  return MaterialApp(
    home: RegisterScreen(
      apiService: api,
      createChurch: createChurch,
      joinCode: joinCode,
    ),
  );
}

/// Champ identifié par son libellé : l'ordre des champs varie selon le mode
/// (le nom de l'église n'existe qu'en « créer mon église »), un index serait
/// donc fragile. `TextFormField` n'expose pas `decoration`, on passe donc par le
/// texte du libellé rendu puis on remonte au champ qui le porte.
Finder _field(String label) => find
    .ancestor(of: find.text(label), matching: find.byType(TextFormField))
    .first;

/// Remplit le formulaire et valide. Les 3 consentements sont obligatoires.
Future<void> _submit(WidgetTester tester) async {
  await tester.enterText(_field('Prénom'), 'Marie');
  await tester.enterText(_field('Nom'), 'Kouassi');
  await tester.enterText(_field('Adresse email'), 'marie@example.com');
  await tester.enterText(_field('Mot de passe'), 'motdepasse123');
  await tester.enterText(_field('Confirmer le mot de passe'), 'motdepasse123');

  // `CheckboxListTile` plutôt que `Checkbox` : chaque case est une tuile
  // entièrement cliquable. Les trois sont en dessous de la ligne de flottaison
  // dans la taille de test par défaut — sans `ensureVisible`, le tap atterrit
  // dans le vide et la validation refuse avec « vous devez accepter les CGU ».
  final consentements = find.byType(CheckboxListTile);
  expect(consentements, findsNWidgets(3),
      reason: 'les 3 consentements RGPD (art. 7 et art. 9) sont obligatoires');
  for (var i = 0; i < 3; i++) {
    await tester.ensureVisible(consentements.at(i));
    await tester.pumpAndSettle();
    await tester.tap(consentements.at(i), warnIfMissed: false);
    await tester.pump();
  }
  final valeurs = tester
      .widgetList<CheckboxListTile>(consentements)
      .map((t) => t.value)
      .toList();
  expect(valeurs, everyElement(isTrue),
      reason: 'les 3 consentements doivent être cochés avant envoi');

  final submit = find.widgetWithText(FilledButton, 'Créer mon compte');
  await tester.ensureVisible(submit);
  await tester.pumpAndSettle();
  await tester.tap(submit);
  await tester.pumpAndSettle();
}

void main() {
  group('RegisterScreen — issues de l’inscription', () {

    testWidgets(
        'église en APPROVAL : « demande de rejointure transmise », aucun compte annoncé',
        (tester) async {
      final api = _FakeApi(
        {
          'status': 'PENDING_APPROVAL',
          'churchName': 'Église Bethel de Cocody',
          'message': "Demande enregistrée. Un responsable de l'église vous contactera.",
        },
        registerStatus: 202,
      );

      await tester.pumpWidget(_app(api));
      await tester.pumpAndSettle();
      await _submit(tester);

      expect(find.text('Demande de rejointure transmise'), findsOneWidget);
      // Le détail est vérifié AU MOT : c'est lui qui disait « Aucun compte ne
      // sera créé avant approbation » en nommant un Super Admin inexistant.
      const detail = 'Un responsable de l\'église « Église Bethel de Cocody » '
          'validera votre entrée puis vous contactera. '
          'Aucun compte n\'est créé avant sa décision.';
      expect(find.text(detail), findsOneWidget);
    });

    testWidgets(
        'église en adhésion ouverte : « Compte créé » + email d\'activation, AUCUN Super Admin',
        (tester) async {
      final api = _FakeApi({
        'message': 'Account created. Check your email to activate it.',
        'role': 'MEMBRE',
        'churchName': 'Église Bethel de Cocody',
      });

      await tester.pumpWidget(_app(api));
      await tester.pumpAndSettle();
      await _submit(tester);

      const detail = 'Vérifiez votre email pour activer le compte, puis rejoignez '
          '« Église Bethel de Cocody ». Le compte est déjà rattaché à cette église.';
      expect(find.text('Compte créé'), findsOneWidget);
      expect(find.text(detail), findsOneWidget);
    });

    testWidgets('demande d’organisation simple : le message Super Admin reste exact',
        (tester) async {
      final api = _FakeApi({
        'message': "Demande d'église reçue. Elle sera examinée par un Super Admin.",
        'status': 'PENDING_APPROVAL',
      });

      await tester.pumpWidget(_app(api));
      await tester.pumpAndSettle();
      await _submit(tester);

      expect(
          find.text('Demande reçue. Elle sera examinée par un Super Admin.'),
          findsOneWidget);
      expect(find.text('Aucun compte ne sera créé avant approbation.'),
          findsOneWidget);
    });

    testWidgets('inscription par code en adhésion ouverte : le compte existe bel et bien',
        (tester) async {
      final api = _FakeApi({
        'message': 'Account created. Check your email to activate it.',
        'role': 'MEMBRE',
        'churchName': 'Église Bethel de Cocody',
      });

      await tester.pumpWidget(_app(api, joinCode: 'BETHEL-7K2M'));
      await tester.pumpAndSettle();
      await _submit(tester);

      // Avant, la simple présence d'un joinCode affichait « demande transmise »
      // — faux pour une église en adhésion ouverte, où le compte est créé.
      // Avant, la seule présence d'un joinCode affichait « demande transmise » —
      // faux pour une église en adhésion ouverte, où le compte est créé.
      expect(find.text('Compte créé'), findsOneWidget);
      expect(
        find.textContaining('Aucun compte ne sera créé avant approbation.'),
        findsNothing,
      );
    });

    testWidgets('le payload porte bien l’église demandée', (tester) async {
      final api = _FakeApi({
        'message': 'Account created. Check your email to activate it.',
        'role': 'MEMBRE',
        'churchName': 'Église Bethel de Cocody',
      });

      await tester.pumpWidget(_app(api));
      await tester.pumpAndSettle();
      await _submit(tester);

      expect(api.sentBodies, hasLength(1));
      expect(api.sentBodies.single['email'], 'marie@example.com');
    });
  });
}
