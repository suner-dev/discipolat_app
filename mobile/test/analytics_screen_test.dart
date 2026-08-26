import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/engagement/analytics_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/engagement-analytics') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {'metricCategory': 'attendance', 'metricValue': 75.4, 'metricName': 'Taux de présence', 'changePercentage': 12.5},
                {'metricCategory': 'participation', 'metricValue': 40.0, 'metricName': 'Participation événements', 'changePercentage': -3.2},
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('EngagementAnalyticsScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, EngagementAnalyticsScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('📈 Engagement'), findsOneWidget);
    });

    testWidgets('shows metrics from API', (tester) async {
      await pumpLocalized(tester, EngagementAnalyticsScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('ATTENDANCE'), findsOneWidget);
      expect(find.text('Taux de présence'), findsOneWidget);
      expect(find.text('+12.5%'), findsOneWidget);
      expect(find.text('-3.2%'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, EngagementAnalyticsScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucune métrique enregistrée.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, EngagementAnalyticsScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les métriques.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
