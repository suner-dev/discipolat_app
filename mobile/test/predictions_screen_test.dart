import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/predictions/predictions_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/predictions') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'predictionType': 'CROISSANCE_MEMBRES',
                  'currentValue': 156,
                  'predictedValue': 198,
                  'growthRate': 27.0,
                  'confidence': 'HIGH',
                  'narrative': 'Tendance soutenue par les conversions récentes.',
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('PredictionsScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PredictionsScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('🔮 Prédictions IA'), findsOneWidget);
    });

    testWidgets('shows prediction from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PredictionsScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('CROISSANCE_MEMBRES'), findsOneWidget);
      expect(find.textContaining('Actuel 156 → Prévu 198 (+27.0%)'), findsOneWidget);
      expect(find.textContaining('Tendance soutenue'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PredictionsScreen(apiService: _FakeApiService(empty: true))));
      await tester.pumpAndSettle();
      expect(find.text('Aucune prédiction générée.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PredictionsScreen(apiService: _FakeApiService(fail: true))));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les prédictions.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
