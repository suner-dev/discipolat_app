import 'package:flutter/material.dart';

import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/tasks/models/task_model.dart';

class TaskFilterChips extends StatelessWidget {
  final TaskStatus? selectedStatus;
  final TaskPriority? selectedPriority;
  final TaskType? selectedType;
  final ValueChanged<TaskStatus?> onStatusChanged;
  final ValueChanged<TaskPriority?> onPriorityChanged;
  final ValueChanged<TaskType?> onTypeChanged;

  const TaskFilterChips({
    super.key,
    required this.selectedStatus,
    required this.selectedPriority,
    required this.selectedType,
    required this.onStatusChanged,
    required this.onPriorityChanged,
    required this.onTypeChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 100,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        children: [
          // Status filter
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                FilterChip(
                  label: const Text('Tous'),
                  selected: selectedStatus == null,
                  onSelected: (_) => onStatusChanged(null),
                  selectedColor: AppColors.primary.withOpacity(0.2),
                  checkmarkColor: AppColors.primary,
                  labelStyle: TextStyle(
                    color: selectedStatus == null ? AppColors.primary : AppColors.surface.withOpacity(0.7),
                    fontWeight: selectedStatus == null ? FontWeight.w600 : FontWeight.normal,
                  ),
                  side: BorderSide(
                    color: selectedStatus == null ? AppColors.primary : AppColors.surface.withOpacity(0.3),
                  ),
                  backgroundColor: AppColors.cardDark,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                ),
                const SizedBox(width: 8),
                ...TaskStatus.values.map((status) {
                  final color = status.getColor();
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(status.displayName),
                      selected: selectedStatus == status,
                      onSelected: (_) => onStatusChanged(status),
                      selectedColor: color.withOpacity(0.2),
                      checkmarkColor: color,
                      labelStyle: TextStyle(
                        color: selectedStatus == status ? color : AppColors.surface.withOpacity(0.7),
                        fontWeight: selectedStatus == status ? FontWeight.w600 : FontWeight.normal,
                      ),
                      side: BorderSide(
                        color: selectedStatus == status ? color : AppColors.surface.withOpacity(0.3),
                      ),
                      backgroundColor: AppColors.cardDark,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                    ),
                  );
                }),
              ],
            ),
          ),
          const SizedBox(height: 8),
          // Priority filter
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                FilterChip(
                  label: const Text('Toutes'),
                  selected: selectedPriority == null,
                  onSelected: (_) => onPriorityChanged(null),
                  selectedColor: AppColors.primary.withOpacity(0.2),
                  checkmarkColor: AppColors.primary,
                  labelStyle: TextStyle(
                    color: selectedPriority == null ? AppColors.primary : AppColors.surface.withOpacity(0.7),
                    fontWeight: selectedPriority == null ? FontWeight.w600 : FontWeight.normal,
                  ),
                  side: BorderSide(
                    color: selectedPriority == null ? AppColors.primary : AppColors.surface.withOpacity(0.3),
                  ),
                  backgroundColor: AppColors.cardDark,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                ),
                const SizedBox(width: 8),
                ...TaskPriority.values.map((priority) {
                  final color = priority.getColor();
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(width: 10, height: 10, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
                          const SizedBox(width: 6),
                          Text(priority.displayName),
                        ],
                      ),
                      selected: selectedPriority == priority,
                      onSelected: (_) => onPriorityChanged(priority),
                      selectedColor: color.withOpacity(0.2),
                      checkmarkColor: color,
                      labelStyle: TextStyle(
                        color: selectedPriority == priority ? color : AppColors.surface.withOpacity(0.7),
                        fontWeight: selectedPriority == priority ? FontWeight.w600 : FontWeight.normal,
                      ),
                      side: BorderSide(
                        color: selectedPriority == priority ? color : AppColors.surface.withOpacity(0.3),
                      ),
                      backgroundColor: AppColors.cardDark,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                    ),
                  );
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
