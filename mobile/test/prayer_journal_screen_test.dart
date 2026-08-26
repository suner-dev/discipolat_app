import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/prayers/prayer_journal_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.empty = false}) : super(baseUrl: 'http://fake');
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/prayer-journal/stats') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: {'total': 12, 'enCours': 7, 'exaucees': 5},
      );
    }
    if (path == '/prayer-journal') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {'id': 'p1', 'contenu': 'Guérison de maman', 'categorie': 'PRIERE', 'statut': 'EN_COURS'},
                {'id': 'p2', 'contenu': 'Emploi pour David', 'categorie': 'INTERCESSION', 'statut': 'EXAUC_EE'},
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }

  @override
  Future<Response> patch(String path, {dynamic data}) async {
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'ok': true});
  }
}

void main() {
  group('PrayerJournalScreen (branché API)', () {
    testWidgets('renders app bar with title', (tester) async {
      await pumpLocalized(tester, PrayerJournalScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Journal de Prière'), findsOneWidget);
    });

    testWidgets('shows stats and entries from API', (tester) async {
      await pumpLocalized(tester, PrayerJournalScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('12'), findsOneWidget);
      expect(find.text('Total'), findsOneWidget);
      expect(find.text('En cours'), findsOneWidget);
      expect(find.text('Exaucées'), findsOneWidget);
      expect(find.text('Guérison de maman'), findsOneWidget);
      expect(find.text('Emploi pour David'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, PrayerJournalScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Commencez à écrire vos prières'), findsOneWidget);
    });
  });
}
