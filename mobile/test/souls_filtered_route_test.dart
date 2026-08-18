import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/souls/souls_list_screen.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// ApiService factice : enregistre les chemins ET les params appelés.
class _RecordingApiService extends ApiService {
  _RecordingApiService() : super(baseUrl: 'http://fake');

  final List<String> requestedPaths = [];
  final List<Map<String, dynamic>> requestedParams = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    requestedPaths.add(path);
    requestedParams.add(params ?? const {});
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: <String, dynamic>{
        'content': <dynamic>[],
        'totalElements': 0,
        'totalPages': 1,
        'number': 0,
        'size': 50,
        'first': true,
        'last': true,
      },
    );
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Widget wrap(ApiService api, {String? statut, String? type}) {
    return MaterialApp(
      theme: GlassTheme.darkTheme,
      home: SoulsListScreen(
        statutFilter: statut,
        typeFilter: type,
        apiService: api,
      ),
    );
  }

  testWidgets('KPI « Décrochés » → /souls?statut=DECROCHE : l’appel API porte le filtre et la liste l’affiche', (tester) async {
    final api = _RecordingApiService();
    await tester.pumpWidget(wrap(api, statut: 'DECROCHE'));
    await tester.pumpAndSettle();

    expect(api.requestedPaths, contains('/souls'));
    expect(api.requestedParams.any((p) => p['statut'] == 'DECROCHE'), isTrue);
    // Le filtre actif est visible avec un bouton pour l'effacer.
    expect(find.textContaining('Statut : DECROCHE'), findsOneWidget);
    expect(find.text('Effacer'), findsOneWidget);
  });

  testWidgets('KPI « Convertis » → /souls?typeDisciple=NOUVEAU_CONVERTI : le filtre type est transmis', (tester) async {
    final api = _RecordingApiService();
    await tester.pumpWidget(wrap(api, type: 'NOUVEAU_CONVERTI'));
    await tester.pumpAndSettle();

    expect(api.requestedParams.any((p) => p['typeDisciple'] == 'NOUVEAU_CONVERTI'), isTrue);
    expect(find.textContaining('Type : NOUVEAU_CONVERTI'), findsOneWidget);
  });

  testWidgets('sans filtre : aucun paramètre envoyé et pas de bandeau filtre', (tester) async {
    final api = _RecordingApiService();
    await tester.pumpWidget(wrap(api));
    await tester.pumpAndSettle();

    expect(api.requestedParams.any((p) => p.containsKey('statut') || p.containsKey('typeDisciple')), isFalse);
    expect(find.text('Effacer'), findsNothing);
  });

  testWidgets('« Effacer » retire le filtre et recharge sans paramètre', (tester) async {
    final api = _RecordingApiService();
    await tester.pumpWidget(wrap(api, statut: 'EN_VEILLE'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Effacer'));
    await tester.pumpAndSettle();

    // Le DERNIER appel ne porte plus le filtre (rechargement après effacement).
    expect(api.requestedParams.last.containsKey('statut'), isFalse);
    expect(find.text('Effacer'), findsNothing);
  });
}
