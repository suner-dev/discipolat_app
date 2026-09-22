import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:video_player/video_player.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';
import 'package:discipolat_mobile/features/streaming/services/streaming_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/streaming/widgets/stream_chat_overlay.dart';

class StreamDetailScreen extends ConsumerStatefulWidget {
  final int streamId;

  const StreamDetailScreen({super.key, required this.streamId});

  @override
  ConsumerState<StreamDetailScreen> createState() => _StreamDetailScreenState();
}

class _StreamDetailScreenState extends ConsumerState<StreamDetailScreen> {
  VideoPlayerController? _videoController;
  bool _showChat = false;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _loadStream();
  }

  Future<void> _loadStream() async {
    try {
      final stream = await ref.read(streamingServiceProvider).getStream(widget.streamId);
      
      if (stream.streamUrl != null && stream.streamUrl!.isNotEmpty) {
        _videoController = VideoPlayerController.networkUrl(Uri.parse(stream.streamUrl!))
          ..initialize().then((_) {
            if (mounted) {
              setState(() {
                _isInitialized = true;
              });
              if (stream.status == StreamStatus.live) {
                _videoController!.play();
              }
            }
          }).catchError((error) {
            print('Video init error: $error');
          });
      }
      
      // Increment viewer count
      ref.read(streamingServiceProvider).incrementViewer(widget.streamId);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e')),
        );
      }
    }
  }

  @override
  void dispose() {
    _videoController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final streamAsync = ref.watch(_streamProvider(widget.streamId));

    return Scaffold(
      backgroundColor: Colors.black,
      body: streamAsync.when(
        data: (stream) => _buildContent(stream),
        loading: () => const Center(child: CircularProgressIndicator(color: AppColors.primary)),
        error: (error, _) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline_rounded, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              Text('Erreur de chargement', style: Theme.of(context).textTheme.headlineSmall?.copyWith(color: Colors.white)),
              const SizedBox(height: 8),
              Text(error.toString(), style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.surface.withOpacity(0.7))),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildContent(StreamModel stream) {
    final isLive = stream.status == StreamStatus.live;

    return Stack(
      children: [
        // Video player
        Positioned.fill(
          child: _isInitialized && _videoController != null
              ? AspectRatio(
                  aspectRatio: _videoController!.value.aspectRatio,
                  child: VideoPlayer(_videoController!),
                )
              : Container(
                  color: Colors.black,
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        CircularProgressIndicator(color: AppColors.primary),
                        const SizedBox(height: 16),
                        Text(
                          isLive ? 'Connexion au direct...' : 'Chargement du replay...',
                          style: TextStyle(color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                ),
        ),
        // Top bar
        Positioned(
          top: 0,
          left: 0,
          right: 0,
          child: SafeArea(
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Colors.black.withOpacity(0.7), Colors.transparent],
                ),
              ),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
                    onPressed: () => context.pop(),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          stream.title,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (stream.scheduledAt != null)
                          Text(
                            isLive ? 'En direct depuis ${DateFormat('HH:mm').format(stream.startedAt!.toLocal())}' :
                                'Planifié le ${DateFormat('dd/MM/yyyy HH:mm').format(stream.scheduledAt.toLocal())}',
                            style: TextStyle(color: Colors.white70, fontSize: 12),
                          ),
                      ],
                    ),
                  ),
                  // Viewer count
                  if (isLive)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: Colors.red,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.remove_red_eye, color: Colors.white, size: 14),
                          const SizedBox(width: 4),
                          Consumer(
                            builder: (context, ref, _) {
                              final viewersAsync = ref.watch(_viewerCountProvider(stream.id));
                              return viewersAsync.when(
                                data: (count) => Text(
                                  count.toString(),
                                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                                ),
                                loading: () => const Text('--', style: TextStyle(color: Colors.white)),
                                error: (_, __) => const Text('--', style: TextStyle(color: Colors.white)),
                              );
                            },
                          ),
                        ],
                      ),
                    ),
                  // Chat toggle
                  IconButton(
                    icon: Icon(
                      _showChat ? Icons.chat_bubble_rounded : Icons.chat_bubble_outline_rounded,
                      color: _showChat ? AppColors.primary : Colors.white,
                    ),
                    onPressed: () => setState(() => _showChat = !_showChat),
                  ),
                  // More actions
                  PopupMenuButton<String>(
                    icon: const Icon(Icons.more_vert_rounded, color: Colors.white),
                    onSelected: (value) => _handleAction(value, stream),
                    itemBuilder: (context) => [
                      if (isLive)
                        const PopupMenuItem(value: 'end', child: Row(children: [Icon(Icons.stop, color: Colors.red), SizedBox(width: 8), Text('Arrêter le stream')])),
                      if (!isLive && stream.status == StreamStatus.scheduled)
                        const PopupMenuItem(value: 'go_live', child: Row(children: [Icon(Icons.play_arrow, color: Colors.green), SizedBox(width: 8), Text('Lancer en direct')])),
                      const PopupMenuItem(value: 'share', child: Row(children: [Icon(Icons.share), SizedBox(width: 8), Text('Partager')])),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
        // Chat overlay
        if (_showChat)
          Positioned(
            top: 0,
            right: 0,
            bottom: 0,
            width: MediaQuery.of(context).size.width * 0.85,
            child: StreamChatOverlay(
              streamId: widget.streamId,
              onClose: () => setState(() => _showChat = false),
            ),
          ),
        // Controls overlay for video
        if (_isInitialized && _videoController != null)
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: SafeArea(
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [Colors.black.withOpacity(0.8), Colors.transparent],
                  ),
                ),
                child: VideoProgressIndicator(
                  _videoController!,
                  allowScrubbing: true,
                  colors: VideoProgressColors(
                    playedColor: AppColors.primary,
                    bufferedColor: AppColors.primary.withOpacity(0.3),
                    backgroundColor: Colors.white24,
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }

  void _handleAction(String action, StreamModel stream) {
    switch (action) {
      case 'end':
        _endStream(stream.id);
        break;
      case 'go_live':
        _goLive(stream.id);
        break;
      case 'share':
        _shareStream(stream);
        break;
    }
  }

  Future<void> _goLive(int id) async {
    try {
      await ref.read(streamingServiceProvider).goLive(id);
      ref.invalidate(_streamProvider(id));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Stream lancé en direct ✓')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  Future<void> _endStream(int id) async {
    try {
      await ref.read(streamingServiceProvider).endStream(id);
      ref.invalidate(_streamProvider(id));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Stream terminé ✓')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  void _shareStream(StreamModel stream) {
    // TODO: Implement share
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Partage bientôt disponible')),
    );
  }
}

// Providers
final _streamProvider = FutureProvider.family<StreamModel, int>((ref, id) async {
  final service = ref.watch(streamingServiceProvider);
  return service.getStream(id);
});

final _viewerCountProvider = FutureProvider.family<StreamViewerCount, int>((ref, streamId) async {
  final service = ref.watch(streamingServiceProvider);
  return service.getViewerCount(streamId);
});
