import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/tasks/models/task_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class TaskCard extends StatelessWidget {
  final Task task;
  final VoidCallback onTap;

  const TaskCard({
    super.key,
    required this.task,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final priorityColor = task.priority.getColor();
    final statusColor = task.status.getColor();
    final isOverdue = task.isOverdue;

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
                      color: task.priority.getColor().withOpacity(0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(_getTypeIcon(task.type), color: task.type.getColor(), size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          task.title,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (task.projectName != null) ...[
                          const SizedBox(height: 2),
                          Text(
                            task.projectName!,
                            style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                          ),
                        ],
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      task.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: statusColor),
                    ),
                  ),
                ],
              ),
              if (task.description != null) ...[
                const SizedBox(height: 8),
                Text(
                  task.description!,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  if (task.dueDate != null) ...[
                    _buildInfoChip(
                      isOverdue ? Icons.warning_amber_rounded : Icons.schedule_rounded,
                      isOverdue ? 'En retard: ${DateFormat('dd/MM/yyyy').format(task.dueDate!.toLocal())}' : 'Échéance: ${DateFormat('dd/MM/yyyy').format(task.dueDate!.toLocal())}',
                      isOverdue ? Colors.red : AppColors.primary,
                    ),
                    const SizedBox(width: 8),
                  ],
                  if (task.assignedToName != null) ...[
                    _buildInfoChip(Icons.person_rounded, task.assignedToName!, AppColors.primary),
                    const SizedBox(width: 8),
                  ],
                  if (task.tags != null && task.tags!.isNotEmpty) ...[
                    _buildInfoChip(Icons.tag_rounded, '${task.tags!.length} tags', AppColors.surface.withOpacity(0.7)),
                  ],
                ],
              ),
              if (task.tags != null && task.tags!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Wrap(
                  spacing: 4,
                  runSpacing: 4,
                  children: task.tags!.take(3).map((tag) {
                    return Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppColors.primary.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        '#$tag',
                        style: TextStyle(fontSize: 10, color: AppColors.primary),
                      ),
                    );
                  }).toList(),
                ),
              ],
              if (task.subtasks != null && task.subtasks!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.checklist_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      '${task.subtasks!.where((s) => s.status == TaskStatus.done).length}/${task.subtasks!.length} sous-tâches',
                      style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ),
              ],
              if (task.comments != null && task.comments!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.comment_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      '${task.comments!.length} commentaire(s)',
                      style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 4),
          Text(label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: color)),
        ],
      ),
    );
  }

  IconData _getTypeIcon(TaskType type) {
    switch (type) {
      case TaskType.task:
        return Icons.task_alt_rounded;
      case TaskType.subtask:
        return Icons.subdirectory_arrow_right_rounded;
      case TaskType.epic:
        return Icons.flag_rounded;
      case TaskType.story:
        return Icons.book_rounded;
      case TaskType.bug:
        return Icons.bug_report_rounded;
      case TaskType.feature:
        return Icons.star_rounded;
      case TaskType.chores:
        return Icons.cleaning_services_rounded;
      case TaskType.meeting:
        return Icons.meeting_room_rounded;
      case TaskType.call:
        return Icons.call_rounded;
      case TaskType.review:
        return Icons.rate_review_rounded;
    }
  }
}