import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';
import 'package:discipolat_mobile/features/streaming/services/streaming_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/streaming/widgets/stream_card.dart';
import 'package:discipolat_mobile/features/streaming/widgets/stream_filter_chips.dart';

class StreamsScreen extends ConsumerStatefulWidget {
  const StreamsScreen({super.key});

  @override
  ConsumerState<StreamsScreen> createState() => _StreamsScreenState();
}

class _StreamsScreenState extends ConsumerState<StreamsScreen> with SingleTickerProviderStateMixin {
  String _filter = 'all';
  late final StreamService _service;

  @override
  void initState() {
    super.initState();
    _service = ref.read(streamingServiceProvider);
  }

  @override
  Widget build(BuildContext context) {
    final streamsAsync = ref.watch(_streamsProvider(_filter));

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Streaming & Diffusion'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => context.push('/streaming/create'),
            tooltip: 'Nouveau stream',
          ),
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () => ref.invalidate(_streamsProvider(_filter)),
            tooltip: 'Actualiser',
          ),
        ],
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [AppColors.cardDark, AppColors.surfaceDark],
          ),
        ),
        child: Column(
          children: [
            // Stats header
            _buildStatsHeader(),
            // Filter chips
            StreamFilterChips(
              selectedFilter: _filter,
              onFilterChanged: (filter) => setState(() => _filter = filter),
            ),
            // Streams list
            Expanded(
              child: streamsAsync.when(
                data: (streams) {
                  if (streams.isEmpty) {
                    return _buildEmptyState();
                  }
                  return RefreshIndicator(
                    onRefresh: () => ref.refresh(_streamsProvider(_filter).future),
                    child: ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: streams.length,
                      itemBuilder: (context, index) {
                        final stream = streams[index];
                        return StreamCard(
                          stream: stream,
                          onTap: () => context.push('/streaming/${stream.id}'),
                          onGoLive: stream.status == StreamStatus.scheduled
                              ? () => _handleGoLive(stream.id)
                              : null,
                          onEnd: stream.status == StreamStatus.live
                              ? () => _handleEndStream(stream.id)
                              : null,
                        );
                      },
                    ),
                  );
                },
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (error, _) => _buildErrorState(error.toString()),
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/streaming/create'),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nouveau stream'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  Widget _buildStatsHeader() {
    return Consumer(
      builder: (context, ref, _) {
        final allStreamsAsync = ref.watch(_streamsProvider('all'));
        final liveStreamsAsync = ref.watch(_liveStreamsProvider);

        return allStreamsAsync.when(
          data: (allStreams) {
            return liveStreamsAsync.when(
              data: (liveStreams) {
                final liveCount = liveStreams.length;
                final currentViewers = liveStreams.fold(0, (sum, s) => sum + s.viewerCount);
                final totalViews = allStreams.fold(0, (sum, s) => sum + s.totalViews);

                return Container(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      _buildStatCard('En direct', liveCount.toString(), Icons.radio, Colors.red),
                      const SizedBox(width: 12),
                      _buildStatCard('Spectateurs', currentViewers.toString(), Icons.people, AppColors.primary),
                      const SizedBox(width: 12),
                      _buildStatCard('Vues totales', totalViews.toString(), Icons.visibility, Colors.green),
                    ],
                  ),
                );
              },
              loading: () => const SizedBox(height: 100, child: Center(child: CircularProgressIndicator())),
              error: (_, __) => const SizedBox.shrink(),
            );
          },
          loading: () => const SizedBox(height: 100, child: Center(child: CircularProgressIndicator())),
          error: (_, __) => const SizedBox.shrink(),
        );
      },
    );
  }

  Widget _buildStatCard(String label, String value, IconData icon, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.cardDark,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 8),
            Text(
              value,
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            Text(
              label,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: AppColors.surface.withOpacity(0.7),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.videocam_off_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(
            'Aucun stream',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
              color: AppColors.surface.withOpacity(0.7),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Planifiez votre premier stream pour diffuser en direct',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: AppColors.surface.withOpacity(0.7),
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: () => context.push('/streaming/create'),
            icon: const Icon(Icons.add_rounded),
            label: const Text('Créer un stream'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState(String error) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline_rounded, size: 64, color: Colors.red),
          const SizedBox(height: 16),
          Text('Erreur de chargement', style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          Text(error, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.surface.withOpacity(0.7))),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: () => ref.invalidate(_streamsProvider(_filter)),
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Réessayer'),
          ),
        ],
      ),
    );
  }

  Future<void> _handleGoLive(int id) async {
    try {
      await _service.goLive(id);
      if (mounted) {
        ref.invalidate(_streamsProvider(_filter));
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Stream lancé en direct ✓')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e')),
        );
      }
    }
  }

  Future<void> _handleEndStream(int id) async {
    try {
      await _service.endStream(id);
      if (mounted) {
        ref.invalidate(_streamsProvider(_filter));
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Stream terminé ✓')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e')),
        );
      }
    }
  }
}

// Providers
final _streamsProvider = FutureProvider.family<List<StreamModel>, String>((ref, filter) async {
  final service = ref.watch(streamingServiceProvider);
  return service.getStreams(status: filter == 'all' ? null : filter.toUpperCase());
});

final _liveStreamsProvider = FutureProvider<List<StreamModel>>((ref) async {
  final service = ref.watch(streamingServiceProvider);
  return service.getLiveStreams();
});
