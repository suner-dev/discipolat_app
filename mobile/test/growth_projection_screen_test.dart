import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/growth_projection/growth_projection_screen.dart';

/// ApiService factice : renvoie des réponses JSON selon le chemin demandé.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/growth-projections/prophecy') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: {
          'croissanceAnnuellePct': 15,
          'effectifProjete12Mois': 198,
          'besoinsLeaders': 4,
          'message': 'Tendance positive',
        },
      );
    }
    if (path == '/growth-projections') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: [
          {
            'nom': 'Projection Église 2026',
            'effectifActuel': 156,
            'effectifProjete': 198,
            'tauxCroissanceAnnuel': 15,
            'typeProjection': 'EGLISE',
          },
        ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }
}

void main() {
  group('GrowthProjectionScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(
        home: GrowthProjectionScreen(apiService: _FakeApiService()),
      ));
      expect(find.text('📊 Projection de Croissance'), findsOneWidget);
    });

    testWidgets('shows real prophecy data from API', (tester) async {
      await tester.pumpWidget(MaterialApp(
        home: GrowthProjectionScreen(apiService: _FakeApiService()),
      ));
      await tester.pumpAndSettle();
      expect(find.text('Prophétie de croissance (analyse réelle)'), findsOneWidget);
      expect(find.text('Croissance annuelle projetée : 15 %'), findsOneWidget);
      expect(find.text('Effectif dans 12 mois : 198'), findsOneWidget);
      expect(find.text('Nouveaux leaders nécessaires : 4'), findsOneWidget);
    });

    testWidgets('shows simulator section', (tester) async {
      await tester.pumpWidget(MaterialApp(
        home: GrowthProjectionScreen(apiService: _FakeApiService()),
      ));
      await tester.pumpAndSettle();
      expect(find.text('Simulateur'), findsOneWidget);
      expect(find.text('Taux de croissance annuel (%)'), findsOneWidget);
      expect(find.text('Horizon (mois)'), findsOneWidget);
      expect(find.text('Simuler'), findsOneWidget);
    });

    testWidgets('shows saved projections from API', (tester) async {
      await tester.pumpWidget(MaterialApp(
        home: GrowthProjectionScreen(apiService: _FakeApiService()),
      ));
      await tester.pumpAndSettle();
      await tester.dragUntilVisible(
        find.text('Projections enregistrées'),
        find.byType(ListView),
        const Offset(0, -200),
      );
      expect(find.text('Projections enregistrées'), findsOneWidget);
      expect(find.textContaining('156 → 198 membres'), findsOneWidget);
    });
  });
}
