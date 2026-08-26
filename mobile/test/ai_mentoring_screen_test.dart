import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/mentoring/ai_mentoring_screen.dart';

import 'helpers/pump_localized.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/mentoring/all') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {'titre': 'Accompagner Jean sur sa assiduité', 'analyse': '3 absences consécutives détectées.', 'priorité': 'HAUTE'},
                {'titre': 'Encourager Marie', 'analyse': 'Progression régulière.', 'priorite': 'BASSE'},
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('AiMentoringScreen', () {
    testWidgets('renders app bar', (tester) async {
      await pumpLocalized(tester, AiMentoringScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('🧠 Mentorat IA'), findsOneWidget);
    });

    testWidgets('shows AI suggestions from API', (tester) async {
      await pumpLocalized(tester, AiMentoringScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Accompagner Jean sur sa assiduité'), findsOneWidget);
      expect(find.text('3 absences consécutives détectées.'), findsOneWidget);
      expect(find.text('Encourager Marie'), findsOneWidget);
      expect(find.text('Haute'), findsOneWidget);
      expect(find.text('Basse'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await pumpLocalized(tester, AiMentoringScreen(apiService: _FakeApiService(empty: true)));
      await tester.pumpAndSettle();
      expect(find.text('Aucune suggestion. Générez-en depuis le web.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await pumpLocalized(tester, AiMentoringScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les suggestions.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
