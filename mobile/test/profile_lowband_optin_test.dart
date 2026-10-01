import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/data/local/locale_provider.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/l10n/app_localizations.dart';
import 'package:discipolat_mobile/presentation/screens/profile/profile_screen.dart';

/// §G5.9 — ApiService factice : profil avec/sans téléphone et opt-in,
/// et capture du PUT /users/me { whatsappOptIn }.
class _FakeApiService extends ApiService {
  _FakeApiService({this.phone = '+24100000000', this.optIn = false})
      : super(baseUrl: 'http://fake');

  final String? phone;
  bool optIn;

  String? putPath;
  dynamic putBody;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    if (path == '/users/me') {
      return Response(
        statusCode: 200,
        data: {
          'firstName': 'Anne',
          'lastName': 'Nzé',
          'email': 'anne@discipolat.com',
          'role': 'MEMBRE',
          'phone': phone,
          'whatsappOptIn': optIn,
          'createdAt': '2026-01-01T00:00:00Z',
        },
        requestOptions: RequestOptions(path: path),
      );
    }
    if (path == '/dashboard/my-metrics') {
      return Response(
        statusCode: 200,
        data: {'scoreSpirituel': 42},
        requestOptions: RequestOptions(path: path),
      );
    }
    return Response(
      statusCode: 404,
      data: {},
      requestOptions: RequestOptions(path: path),
    );
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPath = path;
    putBody = data;
    if (data is Map && data.containsKey('whatsappOptIn')) {
      optIn = data['whatsappOptIn'] == true;
    }
    return Response(
      statusCode: 200,
      data: {'whatsappOptIn': optIn},
      requestOptions: RequestOptions(path: path),
    );
  }
}

Widget _wrap(Widget child) {
  final router = GoRouter(
    routes: [
      GoRoute(path: '/', builder: (_, __) => child),
      GoRoute(path: '/login', builder: (_, __) => const SizedBox.shrink()),
      GoRoute(path: '/souls', builder: (_, __) => const SizedBox.shrink()),
      GoRoute(path: '/reports/maker', builder: (_, __) => const SizedBox.shrink()),
      GoRoute(path: '/profile', builder: (_, __) => const SizedBox.shrink()),
      GoRoute(path: '/security-settings', builder: (_, __) => const SizedBox.shrink()),
    ],
  );
  return MaterialApp.router(
    routerConfig: router,
    locale: const Locale('fr'),
    localizationsDelegates: const [
      AppLocalizations.delegate,
      GlobalMaterialLocalizations.delegate,
      GlobalWidgetsLocalizations.delegate,
      GlobalCupertinoLocalizations.delegate,
    ],
    supportedLocales: kSupportedLocales,
  );
}

void main() {
  testWidgets('§G5.9 — la carte « Portail basse connexion » affiche le switch',
      (tester) async {
    await tester.pumpWidget(_wrap(ProfileScreen(apiService: _FakeApiService())));
    await tester.pumpAndSettle();

    expect(find.text('Portail basse connexion'), findsOneWidget);
    final switchFinder = find.byType(Switch);
    expect(switchFinder, findsOneWidget);
    expect(tester.widget<Switch>(switchFinder).value, isFalse);
    expect(tester.widget<Switch>(switchFinder).onChanged, isNotNull);
  });

  testWidgets('§G5.9 — basculer le switch envoie PUT /users/me whatsappOptIn',
      (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(ProfileScreen(apiService: api)));
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.byType(Switch));
    await tester.tap(find.byType(Switch));
    await tester.pump();
    await tester.pumpAndSettle();

    expect(api.putPath, '/users/me');
    expect(api.putBody, {'whatsappOptIn': true});
    expect(tester.widget<Switch>(find.byType(Switch)).value, isTrue);
  });

  testWidgets('§G5.9 — opt-in déjà actif : switch allumé', (tester) async {
    await tester.pumpWidget(
        _wrap(ProfileScreen(apiService: _FakeApiService(optIn: true))));
    await tester.pumpAndSettle();

    expect(tester.widget<Switch>(find.byType(Switch)).value, isTrue);
  });

  testWidgets('§G5.9 — sans téléphone, le switch est désactivé + invite',
      (tester) async {
    await tester.pumpWidget(
        _wrap(ProfileScreen(apiService: _FakeApiService(phone: null))));
    await tester.pumpAndSettle();

    expect(tester.widget<Switch>(find.byType(Switch)).onChanged, isNull);
    expect(find.text("Ajoutez d'abord un numéro de téléphone"), findsOneWidget);
  });
}
