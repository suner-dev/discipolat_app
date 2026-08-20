import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/presentation/screens/admin/benchmark_screen.dart';
import 'helpers/fake_api_service.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  testWidgets('BenchmarkScreen renders title', (tester) async {
    await tester.pumpWidget(wrap(BenchmarkScreen(apiService: FakeApiService())));
    expect(find.text('🏆 Benchmark'), findsOneWidget);
  });

  testWidgets('BenchmarkScreen loads data and shows percentile', (tester) async {
    final fakeApi = FakeApiService();
    await tester.pumpWidget(wrap(BenchmarkScreen(apiService: fakeApi)));
    await tester.pumpAndSettle(const Duration(seconds: 3));

    expect(find.text('Votre position'), findsOneWidget);
    expect(fakeApi.getPaths, isNotEmpty);
  });

  testWidgets('BenchmarkScreen has refresh button', (tester) async {
    await tester.pumpWidget(wrap(BenchmarkScreen(apiService: FakeApiService())));
    expect(find.byIcon(Icons.refresh), findsOneWidget);
  });
}
