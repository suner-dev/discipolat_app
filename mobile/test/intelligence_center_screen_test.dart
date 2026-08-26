import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/intelligence/intelligence_center_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/intelligence') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'name': 'Taux de présence',
                  'description': 'Présence moyenne aux cultes',
                  'trend': 'UP',
                  'currentValue': 78.5,
                  'unit': '%',
                  'isAlert': true,
                },
                {
                  'name': 'Nouveaux disciples',
                  'description': 'Intégrations du mois',
                  'trend': 'STABLE',
                  'currentValue': 12.0,
                  'unit': '',
                  'isAlert': false,
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('IntelligenceCenterScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, IntelligenceCenterScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text("🏛️ Centre d'intelligence"), findsOneWidget);
    });

    testWidgets('shows KPIs and alert banner from API', (tester) async {
      await pumpLocalized(tester, IntelligenceCenterScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('1 alerte(s) active(s)'), findsOneWidget);
      expect(find.text('Taux de présence'), findsOneWidget);
      expect(find.text('Présence moyenne aux cultes'), findsOneWidget);
      expect(find.text('78.5%'), findsOneWidget);
      expect(find.text('Nouveaux disciples'), findsOneWidget);
      expect(find.text('12.0'), findsOneWidget);
    });

    testWidgets('shows no alert banner without alerts', (tester) async {
      // Liste vide → pas de bannière, écran reste rendu.
      await pumpLocalized(tester, IntelligenceCenterScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.textContaining('alerte(s)'), findsNothing);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, IntelligenceCenterScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les KPIs. Le centre doit être initialisé côté admin.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
