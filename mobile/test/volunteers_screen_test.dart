import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/volunteers/volunteers_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/volunteers') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'membreId': 'mem12345-abcd',
                  'heuresMois': 12,
                  'nbEvenements': 3,
                  'disponibilite': 'WEEK-ENDS',
                  'statut': 'ACTIF',
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('VolunteersScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, VolunteersScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('🤝 Bénévoles'), findsOneWidget);
    });

    testWidgets('shows volunteer from API with readable status', (tester) async {
      await pumpLocalized(tester, VolunteersScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Bénévole #mem12345'), findsOneWidget);
      expect(find.textContaining('12 h/mois'), findsOneWidget);
      expect(find.textContaining('WEEK-ENDS'), findsOneWidget);
      expect(find.text('Actif'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, VolunteersScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucun bénévole enregistré.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, VolunteersScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les bénévoles.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
