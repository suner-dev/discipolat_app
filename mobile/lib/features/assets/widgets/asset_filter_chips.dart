import 'package:flutter/material.dart';

import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/assets/models/asset_model.dart';

class AssetFilterChips extends StatelessWidget {
  final AssetType? selectedType;
  final AssetStatus? selectedStatus;
  final bool showNeedsMaintenance;
  final bool showUnderWarranty;
  final ValueChanged<AssetType?> onTypeChanged;
  final ValueChanged<AssetStatus?> onStatusChanged;
  final ValueChanged<bool> onMaintenanceChanged;
  final ValueChanged<bool> onWarrantyChanged;

  const AssetFilterChips({
    super.key,
    required this.selectedType,
    required this.selectedStatus,
    required this.showNeedsMaintenance,
    required this.showUnderWarranty,
    required this.onTypeChanged,
    required this.onStatusChanged,
    required this.onMaintenanceChanged,
    required this.onWarrantyChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 100,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        children: [
          // Type filter
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                FilterChip(
                  label: const Text('Tous'),
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
                ...AssetType.values.map((type) {
                  final color = type.getColorHex().toColor();
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 10,
                            height: 10,
                            decoration: BoxDecoration(
                              color: color,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 6),
                          Text(type.displayName),
                        ],
                      ),
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
          const SizedBox(height: 8),
          // Status & Special filters
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                FilterChip(
                  label: const Text('Tous statuts'),
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
                ...AssetStatus.values.map((status) {
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
                  ),
                ),
                const SizedBox(width: 8),
                FilterChip(
                  label: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.build_rounded, size: 14, color: Colors.orange),
                      const SizedBox(width: 4),
                      const Text('Maintenance'),
                    ],
                  ),
                  selected: showNeedsMaintenance,
                  onSelected: onMaintenanceChanged,
                  selectedColor: Colors.orange.withOpacity(0.2),
                  checkmarkColor: Colors.orange,
                  labelStyle: TextStyle(
                    color: showNeedsMaintenance ? Colors.orange : AppColors.surface.withOpacity(0.7),
                    fontWeight: showNeedsMaintenance ? FontWeight.w600 : FontWeight.normal,
                  ),
                  side: BorderSide(
                    color: showNeedsMaintenance ? Colors.orange : AppColors.surface.withOpacity(0.3),
                  ),
                  backgroundColor: AppColors.cardDark,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                ),
                const SizedBox(width: 8),
                FilterChip(
                  label: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.verified_rounded, size: 14, color: Colors.green),
                      const SizedBox(width: 4),
                      const Text('Garantie'),
                    ],
                  ),
                  selected: showUnderWarranty,
                  onSelected: onWarrantyChanged,
                  selectedColor: Colors.green.withOpacity(0.2),
                  checkmarkColor: Colors.green,
                  labelStyle: TextStyle(
                    color: showUnderWarranty ? Colors.green : AppColors.surface.withOpacity(0.7),
                    fontWeight: showUnderWarranty ? FontWeight.w600 : FontWeight.normal,
                  ),
                  side: BorderSide(
                    color: showUnderWarranty ? Colors.green : AppColors.surface.withOpacity(0.3),
                  ),
                  backgroundColor: AppColors.cardDark,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}