import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import '../../data/local/database.dart';
import '../../data/local/offline_sync_manager.dart';
import '../../l10n/app_localizations.dart';

/// Persistent banner shown when the device is offline.
/// Displays pending items count and a sync button when back online.
class OfflineBanner extends ConsumerWidget {
  const OfflineBanner({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final connectivity = ref.watch(connectivityProvider);
    final syncManager = ref.watch(offlineSyncManagerProvider);

    return connectivity.when(
      data: (results) {
        final isOnline = results.any((r) =>
            r == ConnectivityResult.wifi ||
            r == ConnectivityResult.mobile ||
            r == ConnectivityResult.ethernet);

        if (isOnline && syncManager.pendingCount == 0) {
          return const SizedBox.shrink();
        }

        return Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          color: isOnline ? Colors.orange.shade700 : Colors.red.shade700,
          child: Row(
            children: [
              Icon(
                isOnline ? Icons.cloud_upload : Icons.cloud_off,
                color: Colors.white,
                size: 16,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  isOnline
                      ? AppLocalizations.of(context).translate('syncingCount').replaceAll('{count}', '${syncManager.pendingCount}')
                      : AppLocalizations.of(context).translate('offlineCount').replaceAll('{count}', '${syncManager.pendingCount}'),
                  style: const TextStyle(color: Colors.white, fontSize: 12),
                ),
              ),
              if (isOnline && syncManager.pendingCount > 0 && !syncManager.isSyncing)
                TextButton(
                  onPressed: () => syncManager.syncPendingItems(),
                  child: Text(
                    AppLocalizations.of(context).syncNow,
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                  ),
                ),
              if (syncManager.isSyncing)
                const SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                ),
            ],
          ),
        );
      },
      loading: () => const SizedBox.shrink(),
      error: (_, __) => const SizedBox.shrink(),
    );
  }
}
