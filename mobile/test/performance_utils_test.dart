import 'dart:async';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/utils/performance_utils.dart';

void main() {
  group('Debouncer', () {
    test('debouncer delays execution', () async {
      final debouncer = Debouncer(milliseconds: 100);
      int callCount = 0;

      debouncer.run(() => callCount++);
      debouncer.run(() => callCount++);
      debouncer.run(() => callCount++);

      // Should not have executed yet
      expect(callCount, 0);

      // Wait for debounce to complete
      await Future.delayed(const Duration(milliseconds: 200));

      // Only the last call should have executed
      expect(callCount, 1);

      debouncer.dispose();
    });

    test('debouncer cancels previous timer on new call', () async {
      final debouncer = Debouncer(milliseconds: 150);
      final results = <int>[];

      debouncer.run(() => results.add(1));
      await Future.delayed(const Duration(milliseconds: 50));
      debouncer.run(() => results.add(2));
      await Future.delayed(const Duration(milliseconds: 200));

      expect(results, [2]);

      debouncer.dispose();
    });

    test('debouncer dispose cancels pending timer', () async {
      final debouncer = Debouncer(milliseconds: 100);
      int callCount = 0;

      debouncer.run(() => callCount++);
      debouncer.dispose();

      await Future.delayed(const Duration(milliseconds: 200));
      expect(callCount, 0);
    });

    test('debouncer with zero delay executes immediately', () async {
      final debouncer = Debouncer(milliseconds: 0);
      int callCount = 0;

      debouncer.run(() => callCount++);
      await Future.delayed(const Duration(milliseconds: 50));

      expect(callCount, 1);

      debouncer.dispose();
    });
  });

  group('ApiCache', () {
    test('cache stores and retrieves values', () {
      final cache = ApiCache(ttl: const Duration(minutes: 5));

      cache.put('key1', 'value1');
      expect(cache.get<String>('key1'), 'value1');
    });

    test('cache returns null for missing keys', () {
      final cache = ApiCache();
      expect(cache.get<String>('missing'), isNull);
    });

    test('cache expires entries after TTL', () async {
      final cache = ApiCache(ttl: const Duration(milliseconds: 50));

      cache.put('key1', 'value1');
      expect(cache.get<String>('key1'), 'value1');

      await Future.delayed(const Duration(milliseconds: 100));
      expect(cache.get<String>('key1'), isNull);
    });

    test('cache invalidates by key', () {
      final cache = ApiCache();
      cache.put('key1', 'value1');
      cache.invalidate('key1');
      expect(cache.get<String>('key1'), isNull);
    });

    test('cache invalidatePrefix removes matching entries', () {
      final cache = ApiCache();
      cache.put('/souls/1', 'data1');
      cache.put('/souls/2', 'data2');
      cache.put('/departments/1', 'data3');

      cache.invalidatePrefix('/souls');

      expect(cache.get('/souls/1'), isNull);
      expect(cache.get('/souls/2'), isNull);
      expect(cache.get('/departments/1'), isNotNull);
    });

    test('cache invalidateAll clears everything', () {
      final cache = ApiCache();
      cache.put('a', 1);
      cache.put('b', 2);
      cache.invalidateAll();

      expect(cache.get('a'), isNull);
      expect(cache.get('b'), isNull);
    });

    test('cache stores different types', () {
      final cache = ApiCache();
      cache.put<String>('str', 'hello');
      cache.put<int>('int', 42);
      cache.put<List>('list', [1, 2, 3]);

      expect(cache.get<String>('str'), 'hello');
      expect(cache.get<int>('int'), 42);
      expect(cache.get<List>('list'), [1, 2, 3]);
    });
  });
}
