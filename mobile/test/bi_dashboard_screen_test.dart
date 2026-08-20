import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/presentation/screens/dashboard/bi_dashboard_screen.dart';
import 'helpers/fake_api_service.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  testWidgets('BiDashboardScreen renders title', (tester) async {
    await tester.pumpWidget(wrap(BiDashboardScreen(apiService: FakeApiService())));
    expect(find.text('📊 Business Intelligence'), findsOneWidget);
  });

  testWidgets('BiDashboardScreen shows loading then loads KPIs', (tester) async {
    final fakeApi = FakeApiService();
    await tester.pumpWidget(wrap(BiDashboardScreen(apiService: fakeApi)));
    await tester.pumpAndSettle(const Duration(seconds: 3));

    expect(find.text('Total membres'), findsOneWidget);
    expect(find.text('Actifs'), findsOneWidget);
    expect(fakeApi.getPaths, isNotEmpty);
  });

  testWidgets('BiDashboardScreen has refresh button', (tester) async {
    await tester.pumpWidget(wrap(BiDashboardScreen(apiService: FakeApiService())));
    expect(find.byIcon(Icons.refresh), findsOneWidget);
  });

  testWidgets('BiDashboardScreen has period selector', (tester) async {
    await tester.pumpWidget(wrap(BiDashboardScreen(apiService: FakeApiService())));
    expect(find.byType(PopupMenuButton<String>), findsOneWidget);
  });

  testWidgets('BiDashboardScreen shows growth card', (tester) async {
    await tester.pumpWidget(wrap(BiDashboardScreen(apiService: FakeApiService())));
    await tester.pumpAndSettle(const Duration(seconds: 3));
    expect(find.text('Croissance'), findsOneWidget);
  });
}
