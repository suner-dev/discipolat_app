import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/succession/succession_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/succession') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'rôleCible': 'Responsable louange',
                  'candidatId': 'cand12345-abcd',
                  'planFormation': 'Mentorat 6 mois + direction de 3 cultes.',
                  'readiness': 'PRÊT',
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('SuccessionScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, SuccessionScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('👑 Succession'), findsOneWidget);
    });

    testWidgets('shows succession plan from API with readable readiness', (tester) async {
      await pumpLocalized(tester, SuccessionScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Responsable louange'), findsOneWidget);
      expect(find.text('Candidat #cand1234'), findsOneWidget);
      expect(find.textContaining('Mentorat 6 mois'), findsOneWidget);
      expect(find.text('Prêt'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, SuccessionScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucun plan de succession.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, SuccessionScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les plans de succession.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
