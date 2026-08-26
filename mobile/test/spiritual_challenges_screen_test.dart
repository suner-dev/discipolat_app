import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/spiritual_challenges/spiritual_challenges_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/spiritual-challenges') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'titre': 'Jeûne de 7 jours',
                  'description': 'Jeûne partiel avec prière matinale.',
                  'objectifJours': 7,
                  'joursComplétés': 2,
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('SpiritualChallengesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, SpiritualChallengesScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('🔥 Défis spirituels'), findsOneWidget);
    });

    testWidgets('shows challenge with progress from API', (tester) async {
      await pumpLocalized(tester, SpiritualChallengesScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Jeûne de 7 jours'), findsOneWidget);
      expect(find.text('Jeûne partiel avec prière matinale.'), findsOneWidget);
      expect(find.text('2/7'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, SpiritualChallengesScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucun défi en cours.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, SpiritualChallengesScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les défis spirituels.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
