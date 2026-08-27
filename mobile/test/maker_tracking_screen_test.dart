import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/maker_tracking/maker_tracking_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.contains('/maker-tracking/resume')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'formations': 5, 'competences': 8, 'ames': 12, 'points': 1250});
    }
    if (path == '/api/v1/maker-tracking') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'content': []});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('MakerTrackingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: MakerTrackingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Mon Parcours de Faiseur'), findsOneWidget);
    });
    testWidgets('shows points', (tester) async {
      await tester.pumpWidget(MaterialApp(home: MakerTrackingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.textContaining('1250'), findsOneWidget);
    });
  });
}
