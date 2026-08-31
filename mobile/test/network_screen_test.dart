import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/network/network_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.data = const {}, this.fail = false})
      : super(baseUrl: 'http://fake');

  final Map<String, dynamic> data;
  final bool fail;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw Exception('network down');
    if (path == '/api/v1/network/resources') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data['resources'] ??
            [
              {'id': 'r1', 'title': 'Guide de discipolat', 'category': 'BEST_PRACTICE', 'downloads': 42},
            ],
      );
    }
    if (path == '/api/v1/network/events') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data['events'] ??
            [
              {
                'id': 'e1',
                'title': 'Conférence inter-églises',
                'city': 'Douala',
                'country': 'Cameroun',
                'isVirtual': false,
                'currentParticipants': 12,
                'maxParticipants': 100,
                'startsAt': '2026-09-15T10:00:00',
                'joinedByMe': false,
              },
            ],
      );
    }
    if (path == '/api/v1/network/directory') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data['directory'] ??
            [
              {'id': 'd1', 'churchName': 'Église Bethel', 'city': 'Kinshasa', 'country': 'RDC', 'memberCount': 350},
            ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    if (path == '/api/v1/network/events/e1/join') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: {
          'id': 'e1',
          'title': 'Conférence inter-églises',
          'currentParticipants': 13,
          'maxParticipants': 100,
          'joinedByMe': true,
        },
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }
}

Future<void> _pump(WidgetTester tester, _FakeApiService api) async {
  await tester.pumpWidget(MaterialApp(home: NetworkScreen(apiService: api)));
  await tester.pumpAndSettle();
}

void main() {
  group('NetworkScreen', () {
    testWidgets('renders app bar with i18n title and 3 tabs', (tester) async {
      await _pump(tester, _FakeApiService());
      expect(find.text('Réseau inter-églises'), findsOneWidget);
      expect(find.text('Ressources'), findsOneWidget);
      expect(find.text('Événements'), findsOneWidget);
      expect(find.text('Annuaire'), findsOneWidget);
    });

    testWidgets('shows shared resources from API', (tester) async {
      await _pump(tester, _FakeApiService());
      expect(find.text('Guide de discipolat'), findsOneWidget);
      expect(find.text('BEST_PRACTICE • 42 téléchargements'), findsOneWidget);
    });

    testWidgets('shows upcoming events and joins via API', (tester) async {
      await _pump(tester, _FakeApiService());
      await tester.tap(find.text('Événements'));
      await tester.pumpAndSettle();
      expect(find.text('Conférence inter-églises'), findsOneWidget);
      expect(find.text('Participer'), findsOneWidget);

      await tester.tap(find.byIcon(Icons.group_add));
      await tester.pumpAndSettle();
      expect(find.text('Quitter'), findsOneWidget);
    });

    testWidgets('shows directory churches from API', (tester) async {
      await _pump(tester, _FakeApiService());
      await tester.tap(find.text('Annuaire'));
      await tester.pumpAndSettle();
      expect(find.text('Église Bethel'), findsOneWidget);
      expect(find.text('Kinshasa, RDC • 350 membres'), findsOneWidget);
    });

    testWidgets('shows empty state when API returns no data', (tester) async {
      await _pump(tester, _FakeApiService(data: {
        'resources': <dynamic>[],
        'events': <dynamic>[],
        'directory': <dynamic>[],
      }));
      expect(find.text('Aucune ressource partagée'), findsOneWidget);
    });

    testWidgets('shows error state with retry when API fails', (tester) async {
      await _pump(tester, _FakeApiService(fail: true));
      expect(find.text('Erreur lors du chargement du réseau'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
