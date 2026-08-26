import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/church_comparison/church_comparison_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/church-comparisons') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'nomEglise': 'Église Espoir',
                  'effectif': 120,
                  'tauxPresence': 78.0,
                  'tauxConversion': 12.5,
                  'tauxRetention': 85.0,
                  'scoreSpirituelMoyen': 7.0,
                },
                {'nomEglise': 'Église Paix', 'effectif': 95, 'tauxPresence': 70.0, 'tauxConversion': 9.0, 'tauxRetention': 80.0, 'scoreSpirituelMoyen': 6.0},
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('ChurchComparisonScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, ChurchComparisonScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('⚖️ Comparaison'), findsOneWidget);
    });

    testWidgets('shows churches and metrics from API', (tester) async {
      await pumpLocalized(tester, ChurchComparisonScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Église Espoir'), findsOneWidget);
      expect(find.text('Église Paix'), findsOneWidget);
      expect(find.textContaining('Effectif: 120'), findsOneWidget);
      expect(find.textContaining('Présence: 78%'), findsOneWidget);
      expect(find.textContaining('Conversion: 12.5%'), findsOneWidget);
      expect(find.textContaining('Rétention: 85%'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, ChurchComparisonScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucune comparaison enregistrée.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, ChurchComparisonScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les comparaisons.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
