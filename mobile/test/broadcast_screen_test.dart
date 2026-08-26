import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/broadcast/broadcast_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/announcements') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: [
          {'titre': 'Rappel: Culte spécial', 'target': 'Tous les membres', 'readRate': 0.92, 'createdAt': '2025-08-24T10:00:00'},
          {'titre': 'Réunion technique', 'target': 'Dép. Technique', 'readRate': 0.85, 'createdAt': '2025-08-22T14:00:00'},
        ],
      );
    }
    if (path == '/api/v1/announcements/stats') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: {'totalSent': 12, 'readRate': 89},
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }
}

void main() {
  group('BroadcastScreen', () {
    testWidgets('renders app bar with i18n title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BroadcastScreen(apiService: _FakeApiService())));
      expect(find.text('Diffusion / Broadcast'), findsOneWidget);
    });

    testWidgets('shows stats from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BroadcastScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('12'), findsOneWidget);
      expect(find.text('89%'), findsOneWidget);
    });

    testWidgets('shows recent broadcasts from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BroadcastScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Rappel: Culte spécial'), findsOneWidget);
      expect(find.text('Réunion technique'), findsOneWidget);
    });

    testWidgets('shows targeting options', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BroadcastScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Tous les membres'), findsOneWidget);
      expect(find.text('Par département'), findsOneWidget);
    });

    testWidgets('shows FAB', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BroadcastScreen(apiService: _FakeApiService())));
      expect(find.text('Nouvelle diffusion'), findsOneWidget);
    });
  });
}
