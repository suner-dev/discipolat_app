import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/engagement/analytics_screen.dart';

void main() {
  group('EngagementAnalyticsScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EngagementAnalyticsScreen()));
      expect(find.text('📊 Analytics d\'Engagement'), findsOneWidget);
    });

    testWidgets('shows period selector', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EngagementAnalyticsScreen()));
      expect(find.text('7j'), findsOneWidget);
      expect(find.text('30j'), findsOneWidget);
    });

    testWidgets('shows stat cards', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EngagementAnalyticsScreen()));
      expect(find.text('12.4K'), findsOneWidget);
      expect(find.text('892'), findsOneWidget);
    });

    testWidgets('shows top pages', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EngagementAnalyticsScreen()));
      expect(find.text('Pages les plus visitées'), findsOneWidget);
      expect(find.text('/dashboard'), findsOneWidget);
    });

    testWidgets('shows funnel', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EngagementAnalyticsScreen()));
      expect(find.text('Funnel inscription → engagement'), findsOneWidget);
    });
  });
}
