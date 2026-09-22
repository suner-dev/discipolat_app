import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class EventCard extends StatelessWidget {
  final Event event;
  final VoidCallback onTap;
  final VoidCallback? onRegister;
  final VoidCallback? onCheckIn;
  final bool showTime;

  const EventCard({
    super.key,
    required this.event,
    required this.onTap,
    this.onRegister,
    this.onCheckIn,
    this.showTime = false,
  });

  @override
  Widget build(BuildContext context) {
    final isLive = event.status == EventStatus.live;
    final isUpcoming = event.status == EventStatus.published;
    final isPast = event.status == EventStatus.completed || event.status == EventStatus.cancelled;
    final typeColor = event.type.getColorHex().toColor();

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
            // Image/Thumbnail with status badge
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                  child: AspectRatio(
                    aspectRatio: 16 / 9,
                    child: event.thumbnailUrl != null
                        ? Image.network(
                            event.thumbnailUrl!,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => _buildPlaceholder(typeColor),
                          )
                        : _buildPlaceholder(typeColor),
                  ),
                ),
                Positioned(
                  top: 12,
                  left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: _getStatusColor(event.status),
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
                          event.status.displayName,
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
                Positioned(
                  top: 12,
                  right: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: typeColor,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      event.type.displayName,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
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
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          event.title,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      if (event.maxAttendees != null && event.maxAttendees! > 0)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: typeColor.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '${event.currentAttendees}/${event.maxAttendees}',
                            style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: typeColor),
                          ),
                        ),
                    ],
                  ),
                  if (event.description != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      event.description!,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: AppColors.surface.withOpacity(0.7),
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                  const SizedBox(height: 12),
                  // Date/Location
                  Row(
                    children: [
                      Icon(Icons.schedule_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                      const SizedBox(width: 4),
                      Text(
                        showTime
                            ? '${DateFormat('dd/MM/yyyy HH:mm').format(event.startAt.toLocal())} - ${DateFormat('HH:mm').format(event.endAt.toLocal())}'
                            : DateFormat('dd/MM/yyyy HH:mm').format(event.startAt.toLocal()),
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppColors.surface.withOpacity(0.7),
                        ),
                      ),
                      if (event.location != null) ...[
                        const SizedBox(width: 16),
                        Icon(Icons.location_on_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            event.location!,
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: AppColors.surface.withOpacity(0.7),
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ],
                  ),
                  // Tags
                  if (event.tags != null && event.tags!.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 4,
                      runSpacing: 4,
                      children: event.tags!.take(3).map((tag) {
                        return Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: typeColor.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '#$tag',
                            style: TextStyle(fontSize: 10, color: typeColor),
                          ),
                        );
                      }).toList(),
                    ),
                  ],
                  // Actions
                  if (onRegister != null || onCheckIn != null) ...[
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        if (onRegister != null)
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: onRegister,
                              icon: const Icon(Icons.how_to_reg_rounded, size: 18),
                              label: const Text('S\'inscrire'),
                              style: FilledButton.styleFrom(
                                backgroundColor: typeColor,
                                foregroundColor: Colors.white,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                        if (onRegister != null && onCheckIn != null) const SizedBox(width: 8),
                        if (onCheckIn != null)
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: onCheckIn,
                              icon: const Icon(Icons.check_circle_rounded, size: 18),
                              label: const Text('Check-in'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: Colors.green,
                                side: const BorderSide(color: Colors.green),
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

  Widget _buildPlaceholder(Color typeColor) {
    return Container(
      color: typeColor.withOpacity(0.1),
      child: Center(
        child: Icon(
          Icons.event_rounded,
          size: 48,
          color: typeColor.withOpacity(0.5),
        ),
      ),
    );
  }

  Color _getStatusColor(EventStatus status) {
    switch (status) {
      case EventStatus.draft:
        return Colors.grey;
      case EventStatus.published:
        return Colors.blue;
      case EventStatus.live:
        return Colors.red;
      case EventStatus.completed:
        return Colors.green;
      case EventStatus.cancelled:
        return Colors.grey;
    }
  }
}