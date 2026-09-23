import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/discipleship/models/discipleship_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;

class ProgressCard extends StatelessWidget {
  final DiscipleProgress progress;
  final VoidCallback onTap;

  const ProgressCard({
    super.key,
    required this.progress,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _getStatusColor(progress.status);

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
                    radius: 24,
                    backgroundColor: Colors.purple.withOpacity(0.2),
                    child: Text(
                      progress.discipleName.isNotEmpty ? progress.discipleName[0] : '?',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.purple),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          progress.discipleName,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        Text(
                          progress.journeyName,
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(progress.status).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      progress.status.name,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: _getStatusColor(progress.status)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              // Progress bars
              Column(
                children: [
                  _buildProgressRow(context, 'Étapes', progress.completedStages, progress.totalStages, Colors.purple),
                  const SizedBox(height: 8),
                  _buildProgressRow(context, 'Exigences', progress.completedRequirements, progress.totalRequirements, Colors.blue),
                ],
              ),
              const SizedBox(height: 12),
              // Status and dates
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(progress.status).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      progress.status.name,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: _getStatusColor(progress.status)),
                    ),
                  ),
                  const Spacer(),
                  if (progress.nextMilestoneDate != null && progress.nextMilestoneName != null)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.purple.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.flag_rounded, size: 12, color: Colors.purple),
                          const SizedBox(width: 4),
                          Text(
                            '${progress.nextMilestoneName}: ${DateFormat('dd/MM/yyyy').format(progress.nextMilestoneDate!.toLocal())}',
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

  Widget _buildProgressRow(BuildContext context, String label, int completed, int total, Color color) {
    final progress = total > 0 ? completed / total : 0.0;
    final percentage = (progress * 100).toStringAsFixed(1);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(label, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7))),
            Text(
              '$completed / $total ($percentage%)',
              style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: color),
            ),
          ],
        ),
        const SizedBox(height: 4),
        LinearProgressIndicator(
          value: progress.clamp(0.0, 1.0),
          backgroundColor: AppColors.surfaceDark,
          valueColor: AlwaysStoppedAnimation<Color>(color),
          minHeight: 6,
          borderRadius: BorderRadius.circular(3),
        ),
      ],
    );
  }

  Color _getStatusColor(ProgressStatus status) {
    switch (status) {
      case ProgressStatus.notStarted:
        return Colors.grey;
      case ProgressStatus.inProgress:
        return Colors.blue;
      case ProgressStatus.stalled:
        return Colors.orange;
      case ProgressStatus.completed:
        return Colors.green;
      case ProgressStatus.abandoned:
        return Colors.red;
    }
  }
}