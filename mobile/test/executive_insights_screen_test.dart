import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/dashboard/executive_insights_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/executive-insights') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'title': 'Baisse de présence des jeunes',
                  'description': '-12% chez les 18-25 ans ce mois-ci.',
                  'severity': 'WARNING',
                  'metricValue': '-12%',
                  'recommendedAction': 'Organiser un événement jeunes.',
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('ExecutiveInsightsScreen (branché API)', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, ExecutiveInsightsScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Insights Exécutifs IA'), findsOneWidget);
    });

    testWidgets('shows insights from API', (tester) async {
      await pumpLocalized(tester, ExecutiveInsightsScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Baisse de présence des jeunes'), findsOneWidget);
      expect(find.textContaining('-12% chez les 18-25 ans'), findsOneWidget);
      expect(find.textContaining('Organiser un événement jeunes.'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, ExecutiveInsightsScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucun insight actif.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, ExecutiveInsightsScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les insights.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
