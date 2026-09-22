import 'package:flutter/material.dart';

import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';

class StreamFilterChips extends StatelessWidget {
  final String selectedFilter;
  final ValueChanged<String> onFilterChanged;

  const StreamFilterChips({
    super.key,
    required this.selectedFilter,
    required this.onFilterChanged,
  });

  @override
  Widget build(BuildContext context) {
    final filters = [
      ('all', 'Tous'),
      ('live', '🔴 En direct'),
      ('scheduled', 'Planifiés'),
      ('ended', 'Terminés'),
    ];

    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: filters.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final (value, label) = filters[index];
          final isSelected = selectedFilter == value;

          return FilterChip(
            label: Text(label),
            selected: isSelected,
            onSelected: (_) => onFilterChanged(value),
            selectedColor: AppColors.primary.withOpacity(0.2),
            checkmarkColor: AppColors.primary,
            labelStyle: TextStyle(
              color: isSelected ? AppColors.primary : AppColors.surface.withOpacity(0.7),
              fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
            ),
            side: BorderSide(
              color: isSelected ? AppColors.primary : AppColors.surface.withOpacity(0.3),
            ),
            backgroundColor: AppColors.cardDark,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          );
        },
      ),
    );
  }
}
