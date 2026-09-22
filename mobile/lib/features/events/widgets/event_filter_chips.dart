import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class EventFilterChips extends StatelessWidget {
  final EventStatus? selectedStatus;
  final EventType? selectedType;
  final ValueChanged<EventStatus?> onStatusChanged;
  final ValueChanged<EventType?> onTypeChanged;

  const EventFilterChips({
    super.key,
    required this.selectedStatus,
    required this.selectedType,
    required this.onStatusChanged,
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
                ...EventStatus.values.map((status) {
                  final color = _getStatusColor(status);
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
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          // Type filter
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                FilterChip(
                  label: const Text('Tous types'),
                  selected: selectedType == null,
                  onSelected: (_) => onTypeChanged(null),
                  selectedColor: AppColors.primary.withOpacity(0.2),
                  checkmarkColor: AppColors.primary,
                  labelStyle: TextStyle(
                    color: selectedType == null ? AppColors.primary : AppColors.surface.withOpacity(0.7),
                    fontWeight: selectedType == null ? FontWeight.w600 : FontWeight.normal,
                  ),
                  side: BorderSide(
                    color: selectedType == null ? AppColors.primary : AppColors.surface.withOpacity(0.3),
                  ),
                  backgroundColor: AppColors.cardDark,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                ),
                const SizedBox(width: 8),
                ...EventType.values.map((type) {
                  final color = type.getColorHex().toColor();
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(type.displayName),
                      selected: selectedType == type,
                      onSelected: (_) => onTypeChanged(type),
                      selectedColor: color.withOpacity(0.2),
                      checkmarkColor: color,
                      labelStyle: TextStyle(
                        color: selectedType == type ? color : AppColors.surface.withOpacity(0.7),
                        fontWeight: selectedType == type ? FontWeight.w600 : FontWeight.normal,
                      ),
                      side: BorderSide(
                        color: selectedType == type ? color : AppColors.surface.withOpacity(0.3),
                      ),
                      backgroundColor: AppColors.cardDark,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
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