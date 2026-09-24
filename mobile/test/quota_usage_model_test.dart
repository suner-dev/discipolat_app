import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/models/quota_usage.dart';

void main() {
  group('QuotaUsage', () {
    test('parses the current resource keyed response', () {
      final usage = QuotaUsage.fromJson({
        'users': {'used': 4, 'limit': 10, 'percent': 40},
        'storage': {'usedMb': 12, 'limitMb': 100, 'percent': 12},
        'aiRequests': {'used': 3, 'limit': 20, 'percent': 15},
      });

      expect(usage.metric('users')?.used, 4);
      expect(usage.metric('users')?.limit, 10);
      expect(usage.metric('users')?.usagePercent, 40);
      expect(usage.metric('storage')?.used, 12);
      expect(usage.metric('aiRequests')?.limit, 20);
    });

    test('parses the legacy flat response without inventing fields', () {
      final usage = QuotaUsage.fromJson({
        'maxUsers': 10,
        'currentUsers': 4,
        'maxStorageMb': 100,
        'currentStorageMb': 12,
      });

      expect(usage.metric('users')?.used, 4);
      expect(usage.metric('users')?.limit, 10);
      expect(usage.metric('storage')?.used, 12);
      expect(usage.metric('storage')?.limit, 100);
      expect(usage.metric('churches'), isNull);
    });

    test('preserves unlimited and missing states', () {
      final usage = QuotaUsage.fromJson({
        'users': {'used': 7, 'limit': null},
        'storage': {'usedMb': 3, 'limitMb': 0},
        'courses': <String, dynamic>{},
      });

      expect(usage.metric('users')?.isUnlimited, isTrue);
      expect(usage.metric('users')?.usagePercent, isNull);
      expect(usage.metric('storage')?.isUnlimited, isTrue);
      expect(usage.metric('courses')?.hasData, isFalse);
      expect(usage.metric('messages'), isNull);
    });

    test('serializes only fields supplied by the response', () {
      final usage = QuotaUsage.fromJson({
        'users': {'used': 4, 'limit': 10},
        'courses': <String, dynamic>{},
      });

      expect(usage.toJson(), {
        'users': {'used': 4, 'limit': 10, 'percent': 40},
        'courses': <String, dynamic>{},
      });
    });
  });
}
