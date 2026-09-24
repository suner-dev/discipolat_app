import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/usage_analytics_service.dart';
import 'package:discipolat_mobile/models/usage_analytics_summary.dart';
import 'package:discipolat_mobile/presentation/screens/usage_analytics/usage_analytics_screen.dart';

class _ControlledUsageAnalyticsService extends UsageAnalyticsService {
  final Map<int, Completer<UsageAnalyticsSummary>> _requests = {};
  final List<int> requestedDays = [];

  @override
  Future<bool?> fetchAnalyticsEnabled() async => true;

  @override
  Future<UsageAnalyticsSummary> fetchSummary({required int days}) {
    requestedDays.add(days);
    final completer = Completer<UsageAnalyticsSummary>();
    _requests[days] = completer;
    return completer.future;
  }

  void complete(int days, UsageAnalyticsSummary summary) {
    _requests[days]!.complete(summary);
  }
}

UsageAnalyticsSummary _summary(int days) {
  return UsageAnalyticsSummary(
    totalEvents: days,
    pageViews: days * 1000,
    activeUsers: days + 100000,
    averageDurationSeconds: days,
    topPages: const {},
    byDevice: const {},
  );
}

void main() {
  testWidgets('an older period response cannot replace a newer one', (
    tester,
  ) async {
    final service = _ControlledUsageAnalyticsService();
    await tester.pumpWidget(
      MaterialApp(home: UsageAnalyticsScreen(service: service)),
    );
    await tester.pump();

    expect(service.requestedDays, [7]);

    await tester.tap(find.byType(PopupMenuButton<String>));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));
    await tester.tap(find.text('30 jours'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));

    expect(service.requestedDays, [7, 30]);

    service.complete(30, _summary(30));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('30'), findsOneWidget);
    expect(find.text('30000'), findsOneWidget);
    expect(find.text('100030'), findsOneWidget);

    service.complete(7, _summary(7));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('30'), findsOneWidget);
    expect(find.text('30000'), findsOneWidget);
    expect(find.text('100030'), findsOneWidget);
    expect(find.text('7'), findsNothing);
    expect(find.text('7000'), findsNothing);
    expect(find.text('100007'), findsNothing);
  });
}
