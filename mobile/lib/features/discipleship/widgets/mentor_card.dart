import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/discipleship/models/discipleship_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;

class MentorCard extends StatelessWidget {
  final MentorAssignment assignment;
  final VoidCallback onTap;

  const MentorCard({
    super.key,
    required this.assignment,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isActive = assignment.status == AssignmentStatus.active;
    final statusColor = isActive ? Colors.green : (assignment.status == AssignmentStatus.pending ? Colors.orange : Colors.grey);

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
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: Colors.purple.withOpacity(0.2),
                    child: const Icon(Icons.person_rounded, color: Colors.purple, size: 28),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '${assignment.mentorName} → ${assignment.discipleName}',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Parcours: ${assignment.journeyId}',
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: (assignment.status == AssignmentStatus.active ? Colors.green : (assignment.status == AssignmentStatus.pending ? Colors.orange : Colors.grey)).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      assignment.status.name,
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: assignment.status == AssignmentStatus.active ? Colors.green : (assignment.status == AssignmentStatus.pending ? Colors.orange : Colors.grey),
                      ),
                    ),
                  ),
                ],
              ),
              if (assignment.notes != null && assignment.notes!.isNotEmpty) ...[
                const SizedBox(height: 12),
                Text(
                  assignment.notes!,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(Icons.calendar_today_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                  const SizedBox(width: 4),
                  Text(
                    'Assigné le ${DateFormat('dd/MM/yyyy').format(assignment.assignedAt.toLocal())}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  ),
                  if (assignment.nextMeetingAt != null) ...[
                    const SizedBox(width: 16),
                    Icon(Icons.calendar_today_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Prochaine: ${DateFormat('dd/MM/yyyy').format(assignment.nextMeetingAt!.toLocal())}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ],
              ),
              if (assignment.lastMeetingAt != null) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.history_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Dernière réunion: ${DateFormat('dd/MM/yyyy').format(assignment.lastMeetingAt!.toLocal())}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: isActive ? Colors.green.withOpacity(0.2) : Colors.grey.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      assignment.status.name,
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: isActive ? Colors.green : (assignment.status == AssignmentStatus.pending ? Colors.orange : Colors.grey),
                      ),
                    ),
                  ),
                  const Spacer(),
                  if (assignment.nextMeetingAt != null)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.purple.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.calendar_today_rounded, size: 12, color: Colors.purple),
                          const SizedBox(width: 4),
                          Text(
                            'Prochaine: ${DateFormat('dd/MM/yyyy').format(assignment.nextMeetingAt!.toLocal())}',
                            style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: Colors.purple),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}