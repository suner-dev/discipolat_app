import 'package:flutter/material.dart';
import '../models/asset_model.dart';

class AssetFilterChips extends StatelessWidget {
  final AssetType? selectedType;
  final AssetStatus? selectedStatus;
  final ValueChanged<AssetType?> onTypeChanged;
  final ValueChanged<AssetStatus?> onStatusChanged;

  const AssetFilterChips({
    super.key,
    this.selectedType,
    this.selectedStatus,
    required this.onTypeChanged,
    required this.onStatusChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Filtrer par type',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: AssetType.values.map((type) {
                final isSelected = selectedType == type;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(type.displayName),
                    selected: isSelected,
                    onSelected: (_) => onTypeChanged(isSelected ? null : type),
                    selectedColor: Colors.blue.withOpacity(0.2),
                    checkmarkColor: Colors.blue,
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Filtrer par statut',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: AssetStatus.values.map((status) {
                final isSelected = selectedStatus == status;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(status.displayName),
                    selected: isSelected,
                    onSelected: (_) => onStatusChanged(isSelected ? null : status),
                    selectedColor: Colors.green.withOpacity(0.2),
                    checkmarkColor: Colors.green,
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }
}
