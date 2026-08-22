import 'package:camera/camera.dart' as cam;
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/ar_onboarding/ar_onboarding_screen.dart';
import 'package:discipolat_mobile/presentation/screens/face_checkin/face_checkin_screen.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Routeur minimal : /ar héberge l'écran, /login sert d'arrivée après la visite.
Widget _wrapRouter(Widget child) => MaterialApp.router(
      routerConfig: GoRouter(initialLocation: '/ar', routes: [
        GoRoute(path: '/ar', builder: (_, __) => child),
        GoRoute(
            path: '/login',
            builder: (_, __) =>
                const Scaffold(body: Center(child: Text('LOGIN')))),
      ]),
    );

Widget _wrap(Widget child) => MaterialApp(home: child);

/// ApiService factice — aucune vraie requête.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/face/templates') {
      return Response<dynamic>(requestOptions: RequestOptions(path: path), data: []);
    }
    return Response<dynamic>(requestOptions: RequestOptions(path: path), data: {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    return Response<dynamic>(requestOptions: RequestOptions(path: path), data: {
      'matched': true,
      'confidence': 0.92,
      'displayName': 'Jean',
      'message': 'ok',
    });
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('ArOnboardingScreen', () {
    testWidgets('rend le parcours en mode dégradé (sans caméra) et navigue entre les étapes',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      await tester.pumpWidget(_wrapRouter(ArOnboardingScreen(
        camerasProvider: () async => <cam.CameraDescription>[],
      )));
      // L'animation de pulsation tourne en boucle : on pompe par pas,
      // jamais avec pumpAndSettle qui ne se terminerait jamais.
      await tester.pump(const Duration(milliseconds: 400));

      // Étape 1 visible malgré l'absence de caméra
      expect(find.text('Bienvenue dans Discipolat'), findsOneWidget);
      expect(find.text('Suivant'), findsOneWidget);

      // Étape 2
      await tester.tap(find.text('Suivant'));
      await tester.pump(const Duration(milliseconds: 400));
      expect(find.text('Suivi de croissance'), findsOneWidget);

      // Passer → marque l'onboarding terminé puis redirige vers /login
      await tester.tap(find.text('Passer'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getBool('onboarding_complete'), isTrue);
    });
  });

  group('FaceCheckinScreen', () {
    testWidgets("affiche la consigne d'identification et les boutons de capture",
        (tester) async {
      await tester.pumpWidget(_wrap(FaceCheckinScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.photo_camera), findsOneWidget);
      expect(find.byIcon(Icons.photo_library), findsOneWidget);
      expect(find.text('Identifier'), findsOneWidget);

      // Sans photo capturée, le bouton est désactivé.
      // FilledButton.icon crée un sous-type privé : bySubtype obligatoire.
      final buttonFinder = find.ancestor(
        of: find.text('Identifier'),
        matching: find.bySubtype<FilledButton>(),
      );
      expect(buttonFinder, findsOneWidget);
      final button = tester.widget<FilledButton>(buttonFinder);
      expect(button.onPressed, isNull);
    });

    testWidgets('mode enrôlement : champ nom requis', (tester) async {
      await tester.pumpWidget(_wrap(FaceCheckinScreen(
        enrollMode: true,
        apiService: _FakeApiService(),
      )));
      await tester.pumpAndSettle();

      expect(find.text('Enrôlement facial'), findsOneWidget);
      expect(find.text('Enrôler ce visage'), findsOneWidget);
      expect(find.byType(TextField), findsOneWidget);
    });
  });
}
