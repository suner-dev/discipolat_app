import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/discipleship/models/discipleship_model.dart'
    show MentorMeeting, MeetingStatus;
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;

class MeetingCard extends StatelessWidget {
  final MentorMeeting meeting;
  final VoidCallback onTap;

  const MeetingCard({
    super.key,
    required this.meeting,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isUpcoming = meeting.actualAt == null && meeting.scheduledAt.isAfter(DateTime.now());
    final isPast = meeting.actualAt != null || meeting.scheduledAt.isBefore(DateTime.now());
    final isToday = _isToday(meeting.scheduledAt);
    final statusColor = _getStatusColor(meeting.status);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.purple.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.meeting_room_rounded, color: Colors.purple, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Text(
                              'Réunion de mentoring',
                              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                            ),
                            if (meeting.isGroup == true) ...[
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: Colors.purple.withOpacity(0.2),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: const Text(
                                  'Groupe',
                                  style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.purple),
                                ),
                              ),
                            ],
                          ),
                        Text(
                          'Mentor: ${meeting.mentorName} • Disciple: ${meeting.discipleName}',
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(meeting.status).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      meeting.status.name,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: _getStatusColor(meeting.status)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (meeting.notes != null && meeting.notes!.isNotEmpty) ...[
                Text(
                  meeting.notes!,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
              ],
              Row(
                children: [
                  Icon(Icons.schedule_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                  const SizedBox(width: 4),
                  Text(
                    isToday
                        ? 'Aujourd\'hui à ${DateFormat('HH:mm').format(meeting.scheduledAt.toLocal())}'
                        : '${DateFormat('EEEE dd MMMM', 'fr_FR').format(meeting.scheduledAt.toLocal())} à ${DateFormat('HH:mm').format(meeting.scheduledAt.toLocal())}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: AppColors.surface.withOpacity(0.7),
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                    ),
                  ),
                ],
              ),
              if (meeting.location != null && meeting.location!.isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.location_on_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      meeting.location!,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ),
              ],
              if (meeting.durationMinutes != null) ...[
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.timer_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Durée: ${meeting.durationMinutes} min',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ),
              ],
              if (meeting.actualAt != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.green.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.check_circle_rounded, size: 18, color: Colors.green),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Terminée le ${DateFormat('dd/MM/yyyy à HH:mm').format(meeting.actualAt!.toLocal())}',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.green),
                            ),
                            if (meeting.actionItems != null && meeting.actionItems!.isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Text(
                                'Actions: ${meeting.actionItems}',
                                style: TextStyle(fontSize: 10, color: AppColors.surface.withOpacity(0.7)),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                            if (meeting.nextSteps != null && meeting.nextSteps!.isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Text(
                                'Prochaines étapes: ${meeting.nextSteps}',
                                style: TextStyle(fontSize: 10, color: AppColors.surface.withOpacity(0.7)),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  bool _isToday(DateTime dateTime) {
    final now = DateTime.now();
    return dateTime.year == now.year &&
        dateTime.month == now.month &&
        dateTime.day == now.day;
  }

  Color _getStatusColor(MeetingStatus status) {
    switch (status) {
      case MeetingStatus.scheduled:
        return Colors.blue;
      case MeetingStatus.completed:
        return Colors.green;
      case MeetingStatus.cancelled:
        return Colors.red;
      case MeetingStatus.rescheduled:
        return Colors.orange;
    }
  }
}