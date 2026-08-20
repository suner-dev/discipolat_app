import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/presentation/screens/alerts/smart_alerts_screen.dart';
import 'helpers/fake_api_service.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  testWidgets('SmartAlertsScreen renders title', (tester) async {
    await tester.pumpWidget(wrap(SmartAlertsScreen(apiService: FakeApiService())));
    expect(find.text('🔍 Alertes Intelligentes'), findsOneWidget);
  });

  testWidgets('SmartAlertsScreen shows loading then loads data', (tester) async {
    final fakeApi = FakeApiService();
    await tester.pumpWidget(wrap(SmartAlertsScreen(apiService: fakeApi)));
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    await tester.pumpAndSettle(const Duration(seconds: 3));

    // After loading, should show summary cards
    expect(find.text('🔍 Alertes Intelligentes'), findsOneWidget);
    expect(fakeApi.getPaths, isNotEmpty);
  });

  testWidgets('SmartAlertsScreen has refresh button', (tester) async {
    await tester.pumpWidget(wrap(SmartAlertsScreen(apiService: FakeApiService())));
    expect(find.byIcon(Icons.refresh), findsOneWidget);
  });

  testWidgets('SmartAlertsScreen shows scan button', (tester) async {
    await tester.pumpWidget(wrap(SmartAlertsScreen(apiService: FakeApiService())));
    await tester.pumpAndSettle(const Duration(seconds: 3));
    expect(find.byIcon(Icons.radar), findsOneWidget);
  });
}
