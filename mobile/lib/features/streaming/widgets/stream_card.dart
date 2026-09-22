import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class StreamCard extends StatelessWidget {
  final StreamModel stream;
  final VoidCallback onTap;
  final VoidCallback? onGoLive;
  final VoidCallback? onEnd;

  const StreamCard({
    super.key,
    required this.stream,
    required this.onTap,
    this.onGoLive,
    this.onEnd,
  });

  @override
  Widget build(BuildContext context) {
    final isLive = stream.status == StreamStatus.live;
    final isScheduled = stream.status == StreamStatus.scheduled;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      elevation: 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Thumbnail with status badge
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                  child: AspectRatio(
                    aspectRatio: 16 / 9,
                    child: stream.thumbnailUrl != null
                        ? Image.network(
                            stream.thumbnailUrl!,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => _buildPlaceholder(),
                          )
                        : _buildPlaceholder(),
                  ),
                ),
                Positioned(
                  top: 12,
                  right: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: isLive ? Colors.red : isScheduled ? AppColors.primary : AppColors.surface.withOpacity(0.5),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (isLive) ...[
                          Container(
                            width: 6,
                            height: 6,
                            decoration: const BoxDecoration(
                              color: Colors.white,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 4),
                        ],
                        Text(
                          isLive ? 'EN DIRECT' : stream.status.displayName,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                if (stream.viewerCount > 0 && isLive)
                  Positioned(
                    bottom: 12,
                    left: 12,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.black.withOpacity(0.7),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.remove_red_eye, color: Colors.white, size: 12),
                          const SizedBox(width: 4),
                          Text(
                            stream.viewerCount.toString(),
                            style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w500),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
            // Content
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    stream.title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (stream.description != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      stream.description!,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: AppColors.surface.withOpacity(0.7),
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                  const SizedBox(height: 12),
                  // Meta info
                  Row(
                    children: [
                      if (stream.scheduledAt != null) ...[
                        Icon(Icons.schedule_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                        const SizedBox(width: 4),
                        Text(
                          DateFormat('dd/MM/yyyy HH:mm').format(stream.scheduledAt.toLocal()),
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppColors.surface.withOpacity(0.7),
                          ),
                        ),
                      ],
                      if (stream.scheduledAt != null && stream.startedAt != null) const SizedBox(width: 12),
                      if (stream.startedAt != null) ...[
                        Icon(Icons.play_circle_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                        const SizedBox(width: 4),
                        Text(
                          'Commencé ${DateFormat('HH:mm').format(stream.startedAt!.toLocal())}',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppColors.surface.withOpacity(0.7),
                          ),
                        ),
                      ],
                    ],
                  ),
                  // Actions
                  if (onGoLive != null || onEnd != null) ...[
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        if (onGoLive != null)
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: onGoLive,
                              icon: const Icon(Icons.play_arrow_rounded, size: 18),
                              label: const Text('Lancer'),
                              style: FilledButton.styleFrom(
                                backgroundColor: AppColors.primary,
                                foregroundColor: Colors.white,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                        if (onGoLive != null && onEnd != null) const SizedBox(width: 8),
                        if (onEnd != null)
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: onEnd,
                              icon: const Icon(Icons.stop_rounded, size: 18),
                              label: const Text('Arrêter'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: Colors.red,
                                side: const BorderSide(color: Colors.red),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                        if (onGoLive == null && onEnd == null)
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: () {},
                              icon: const Icon(Icons.play_circle_rounded, size: 18),
                              label: const Text('Replay'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AppColors.surface.withOpacity(0.7),
                                side: BorderSide(color: AppColors.surface.withOpacity(0.3)),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPlaceholder() {
    return Container(
      color: AppColors.primary.withOpacity(0.1),
      child: Center(
        child: Icon(
          Icons.videocam_rounded,
          size: 48,
          color: AppColors.primary.withOpacity(0.5),
        ),
      ),
    );
  }
}
