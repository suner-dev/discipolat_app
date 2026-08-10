import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:url_launcher_platform_interface/link.dart' show LinkDelegate;
import 'package:url_launcher_platform_interface/url_launcher_platform_interface.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/members/member_requests_screen.dart';
import 'package:discipolat_mobile/presentation/widgets/attachment_chips.dart';

/// Fake injectable du lanceur de liens : enregistre les URLs lancées et permet
/// de simuler un échec. Étend UrlLauncherPlatform (vérification de token OK).
class _FakeUrlLauncherPlatform extends UrlLauncherPlatform {
  final List<String> launchedUrls = [];
  bool shouldSucceed = true;

  @override
  LinkDelegate? get linkDelegate => null;

  @override
  Future<bool> launchUrl(String url, LaunchOptions options) async {
    launchedUrls.add(url);
    return shouldSucceed;
  }
}

/// ApiService factice : renvoie des demandes (envoyées + reçues) dont certaines
/// portent des pièces jointes.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/members/me/requests') {
      return _json(path, [
        {
          'id': 'req-1',
          'type': 'SUGGESTION',
          'cible': 'PASTEUR',
          'message': 'Ajouter un culte jeunes',
          'statut': 'OUVERT',
          'piecesJointes': [
            {'id': 'att-1', 'fileId': 'f1', 'nom': 'Programme jeunes.pdf', 'url': 'https://drive/1.pdf'},
            {'id': 'att-2', 'fileId': 'f2', 'nom': 'Photo affiche', 'url': 'https://drive/2.png'},
          ],
        },
        {
          'id': 'req-2',
          'type': 'RENDEZ_VOUS',
          'cible': 'RESPONSABLE',
          'message': 'Rendez-vous suivi',
          'statut': 'RESOLU',
          // Sans clé piecesJointes : aucun chip ne doit s'afficher.
        },
      ]);
    }
    if (path == '/members/requests/inbox') {
      return _json(path, [
        {
          'id': 'req-3',
          'type': 'SIGNALEMENT',
          'cible': 'PASTEUR',
          'message': 'Absence répétée',
          'statut': 'OUVERT',
          'auteurNom': 'Marie Dupont',
          'piecesJointes': [
            {'id': 'att-3', 'fileId': 'f3', 'nom': 'Justificatif.pdf', 'url': 'https://drive/3.pdf'},
          ],
        },
      ]);
    }
    throw StateError('Chemin inattendu: $path');
  }

  Response<dynamic> _json(String path, Object data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);
}

void _setRole(String role) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'membre@discipolat.test',
    'roles': [role],
    'activeRole': role,
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late UrlLauncherPlatform originalLauncher;

  setUp(() {
    _setRole('MEMBRE');
    // Remplace le vrai lanceur par le fake avant chaque test (instance statique
    // process-wide : on conserve l'original pour le restaurer au tearDown).
    originalLauncher = UrlLauncherPlatform.instance;
    UrlLauncherPlatform.instance = _FakeUrlLauncherPlatform();
  });

  tearDown(() {
    UrlLauncherPlatform.instance = originalLauncher;
    AuthState().logout();
  });

  Future<_FakeApiService> pumpScreen(WidgetTester tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(MaterialApp(
      home: MemberRequestsScreen(apiService: api),
    ));
    await tester.pumpAndSettle();
    return api;
  }

  testWidgets('affiche les chips des pièces jointes sur « Mes demandes »', (tester) async {
    await pumpScreen(tester);

    expect(find.text('Demandes'), findsOneWidget);
    expect(find.text('Ajouter un culte jeunes'), findsOneWidget);
    // Chips des pièces jointes de la première demande
    expect(find.text('Programme jeunes.pdf'), findsOneWidget);
    expect(find.text('Photo affiche'), findsOneWidget);
  });

  testWidgets("n'affiche aucun chip quand la demande n'a pas de pièces jointes", (tester) async {
    await pumpScreen(tester);

    // La deuxième demande (sans piecesJointes) est visible mais ne porte pas de
    // chips : un seul bloc AttachmentChips est rendu (celui de la première).
    expect(find.text('Rendez-vous suivi'), findsOneWidget);
    expect(find.byType(AttachmentChips), findsOneWidget);
    expect(find.text('Programme jeunes.pdf'), findsOneWidget);
    expect(find.text('Photo affiche'), findsOneWidget);
  });

  testWidgets('le tap sur une chip ouvre le lien dans le navigateur (url_launcher)', (tester) async {
    final launcher = UrlLauncherPlatform.instance as _FakeUrlLauncherPlatform;
    await pumpScreen(tester);

    await tester.tap(find.text('Programme jeunes.pdf'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));

    // Le vrai navigateur est lancé avec l'URL du document — pas de SnackBar.
    expect(launcher.launchedUrls, ['https://drive/1.pdf']);
    expect(find.byType(SnackBar), findsNothing);
  });

  testWidgets("si le lancement échoue, affiche une SnackBar avec l'URL", (tester) async {
    final launcher = UrlLauncherPlatform.instance as _FakeUrlLauncherPlatform;
    launcher.shouldSucceed = false;
    await pumpScreen(tester);

    await tester.tap(find.text('Programme jeunes.pdf'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));

    expect(launcher.launchedUrls, ['https://drive/1.pdf']);
    expect(find.text('Impossible d\'ouvrir le lien: https://drive/1.pdf'), findsOneWidget);
  });

  testWidgets("affiche les chips des demandes reçues dans l'onglet « Reçues »", (tester) async {
    await pumpScreen(tester);

    await tester.tap(find.text('Reçues'));
    await tester.pumpAndSettle();

    expect(find.text('Absence répétée'), findsOneWidget);
    expect(find.text('Justificatif.pdf'), findsOneWidget);
  });
}
