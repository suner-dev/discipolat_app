import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/admin_requests/admin_requests_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/admin-requests') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {'typeDemande': 'Certificat de membre', 'motif': 'Pour dossier bancaire', 'statut': 'SOUMISE'},
                {'typeDemande': 'Transfert département', 'motif': '', 'statut': 'APPROUVÉE'},
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('AdminRequestsScreen', () {
    testWidgets('renders app bar with title', (tester) async {
      await pumpLocalized(tester, AdminRequestsScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('📋 Demandes admin'), findsOneWidget);
    });

    testWidgets('shows requests from API', (tester) async {
      await pumpLocalized(tester, AdminRequestsScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Certificat de membre'), findsOneWidget);
      expect(find.text('Pour dossier bancaire'), findsOneWidget);
      expect(find.text('SOUMISE'), findsOneWidget);
      expect(find.text('Transfert département'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, AdminRequestsScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucune demande.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, AdminRequestsScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les demandes.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
