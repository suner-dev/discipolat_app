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

    test('distinguishes explicit unlimited, null and zero limits', () {
      final usage = QuotaUsage.fromJson({
        'users': {'used': 7, 'limit': null},
        'storage': {'usedMb': 3, 'limitMb': 0, 'percent': 0},
        'courses': {'used': 8, 'limit': null, 'unlimited': true},
        'messages': {'used': 2, 'limit': 'unlimited'},
      });

      expect(usage.metric('users')?.isUnlimited, isFalse);
      expect(usage.metric('users')?.usagePercent, isNull);
      expect(usage.metric('storage')?.isUnlimited, isFalse);
      expect(usage.metric('storage')?.usagePercent, isNull);
      expect(usage.metric('courses')?.isUnlimited, isTrue);
      expect(usage.metric('messages')?.isUnlimited, isTrue);
      expect(usage.metric('courses')?.hasData, isTrue);
    });

    test('parses the persisted usage snapshot and converts bytes to MB', () {
      final usage = QuotaUsage.fromJson({
        'snapshot': {
          'users': {
            'used': 7,
            'limit': 20,
            'utilizationPercent': 35,
            'unit': 'USERS',
            'serverEnforced': true,
          },
          'storageBytes': {
            'used': 2 * 1024 * 1024,
            'limit': 4 * 1024 * 1024,
            'utilizationPercent': 50,
            'unit': 'BYTES',
          },
          'aiCredits': {
            'used': 11,
            'limit': null,
            'utilizationPercent': null,
            'unit': 'AI_CREDITS',
          },
        },
        'churches': {'used': 2, 'limit': 5, 'percent': 40},
      });

      expect(usage.metric('users')?.used, 7);
      expect(usage.metric('users')?.limit, 20);
      expect(usage.metric('users')?.usagePercent, 35);
      expect(usage.metric('storage')?.used, 2);
      expect(usage.metric('storage')?.limit, 4);
      expect(usage.metric('aiRequests')?.used, 11);
      expect(usage.metric('aiRequests')?.isUnlimited, isFalse);
      expect(usage.metric('churches')?.limit, 5);
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
