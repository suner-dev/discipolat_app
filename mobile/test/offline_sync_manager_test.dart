import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/local/offline_sync_manager.dart';

void main() {
  group('OfflineSyncResult', () {
    test('default values are zero', () {
      final result = OfflineSyncResult();
      expect(result.synced, 0);
      expect(result.failed, 0);
      expect(result.isSyncing, false);
      expect(result.hasErrors, false);
    });

    test('hasErrors returns true when failed > 0', () {
      final result = OfflineSyncResult(failed: 1);
      expect(result.hasErrors, true);
    });

    test('hasErrors returns false when failed is 0', () {
      final result = OfflineSyncResult(synced: 5, failed: 0);
      expect(result.hasErrors, false);
    });

    test('isSyncing reflects state', () {
      final syncing = OfflineSyncResult(isSyncing: true);
      final notSyncing = OfflineSyncResult(isSyncing: false);
      expect(syncing.isSyncing, true);
      expect(notSyncing.isSyncing, false);
    });
  });

  group('OfflineSyncManager (unit-level checks without DB)', () {
    test('OfflineSyncResult constructor accepts all params', () {
      final result = OfflineSyncResult(
        synced: 10,
        failed: 2,
        isSyncing: false,
      );
      expect(result.synced, 10);
      expect(result.failed, 2);
      expect(result.isSyncing, false);
      expect(result.hasErrors, true);
    });

    test('OfflineSyncResult with only synced', () {
      final result = OfflineSyncResult(synced: 5);
      expect(result.synced, 5);
      expect(result.failed, 0);
      expect(result.hasErrors, false);
    });

    test('OfflineSyncResult with only failed', () {
      final result = OfflineSyncResult(failed: 3);
      expect(result.synced, 0);
      expect(result.failed, 3);
      expect(result.hasErrors, true);
    });
  });
}
