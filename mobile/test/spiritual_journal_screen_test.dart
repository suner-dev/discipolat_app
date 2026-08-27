import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/spiritual_journal/spiritual_journal_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.contains('/spiritual-journals/by-author')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: [
        {'id': '1', 'titre': 'Matin de prière', 'typeEntree': 'PRIERE', 'dateEntree': '24 août', 'favori': true},
      ]);
    }
    if (path.contains('/spiritual-journals/stats')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'total': 5, 'streak': 5});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('SpiritualJournalScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SpiritualJournalScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('📓 Journal Spirituel'), findsOneWidget);
    });
    testWidgets('shows entries', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SpiritualJournalScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Matin de prière'), findsOneWidget);
    });
  });
}
