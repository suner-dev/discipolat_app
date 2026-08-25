import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/kpi_narrative/kpi_narrative_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/kpi-narrative') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'typeKPI': 'PRESENCE',
                  'tendance': 'HAUSSE',
                  'variationPct': 6,
                  'narration': 'La présence progresse grâce aux nouveaux groupes.',
                  'recommandations': 'Ouvrir un deuxième créneau de culte.',
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('KpiNarrativeScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: KpiNarrativeScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('📖 Narration des KPIs'), findsOneWidget);
    });

    testWidgets('shows KPI narrative from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: KpiNarrativeScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('PRESENCE'), findsOneWidget);
      expect(find.text('6%'), findsOneWidget);
      expect(find.text('La présence progresse grâce aux nouveaux groupes.'), findsOneWidget);
      expect(find.text('💡 Ouvrir un deuxième créneau de culte.'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await tester.pumpWidget(MaterialApp(home: KpiNarrativeScreen(apiService: _FakeApiService(empty: true))));
      await tester.pumpAndSettle();
      expect(find.text('Aucune narration générée. Utilisez « Générer » côté web.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await tester.pumpWidget(MaterialApp(home: KpiNarrativeScreen(apiService: _FakeApiService(fail: true))));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les narrations KPI.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
