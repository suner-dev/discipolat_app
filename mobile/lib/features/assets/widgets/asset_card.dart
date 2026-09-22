import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/assets/models/asset_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class AssetCard extends StatelessWidget {
  final Asset asset;
  final VoidCallback onTap;
  final VoidCallback? onCheckout;
  final VoidCallback? onMaintenance;
  final VoidCallback? onTransfer;

  const AssetCard({
    super.key,
    required this.asset,
    required this.onTap,
    this.onCheckout,
    this.onMaintenance,
    this.onTransfer,
  });

  @override
  Widget build(BuildContext context) {
    final typeColor = asset.type.getColorHex().toColor();
    final statusColor = asset.status.getColor();
    final needsMaintenance = asset.needsMaintenance;
    final underWarranty = asset.isUnderWarranty;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Image/Thumbnail
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
                  child: AspectRatio(
                    aspectRatio: 16 / 9,
                    child: asset.photoUrl != null
                        ? Image.network(
                            asset.photoUrl!,
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
                      color: typeColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          width: 8,
                          height: 8,
                          decoration: BoxDecoration(
                            color: asset.type.getColorHex().toColor(),
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 4),
                        Text(
                          asset.type.displayName,
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
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      asset.status.displayName,
                      style: TextStyle(
                        color: statusColor,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
                if (needsMaintenance)
                  Positioned(
                    bottom: 12,
                    left: 12,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.orange.withOpacity(0.8),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.build_rounded, color: Colors.white, size: 12),
                          const SizedBox(width: 4),
                          Text(
                            'Maintenance requise',
                            style: TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                  ),
                if (underWarranty)
                  Positioned(
                    bottom: 12,
                    right: 12,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.green.withOpacity(0.8),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.verified_rounded, color: Colors.white, size: 12),
                          const SizedBox(width: 4),
                          Text(
                            'Sous garantie',
                            style: TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                          ),
                        ],
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
                          asset.name,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      if (asset.serialNumber != null)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: typeColor.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            'SN: ${asset.serialNumber}',
                            style: TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: typeColor),
                          ),
                        ),
                    ],
                  ),
                  if (asset.description != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      asset.description!,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: AppColors.surface.withOpacity(0.7),
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                  const SizedBox(height: 12),
                  // Meta info
                  Row(
                    children: [
                      if (asset.location != null) ...[
                        Icon(Icons.location_on_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            asset.location!,
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: AppColors.surface.withOpacity(0.7),
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    if (asset.location != null && asset.departmentName != null) const SizedBox(width: 12),
                    if (asset.departmentName != null) ...[
                      Icon(Icons.business_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          asset.departmentName!,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppColors.surface.withOpacity(0.7),
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                    if (asset.assignedToName != null) ...[
                      const SizedBox(width: 12),
                      Icon(Icons.person_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          'À: ${asset.assignedToName}',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppColors.surface.withOpacity(0.7),
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ],
                  const SizedBox(height: 12),
                  // Warranty/Maintenance badges
                  Row(
                    children: [
                      if (asset.isUnderWarranty)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.green.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.verified_rounded, size: 12, color: Colors.green),
                              const SizedBox(width: 4),
                              Text(
                                'Garantie jusqu\'au ${DateFormat('dd/MM/yyyy').format(asset.warrantyExpiry!.toLocal())}',
                                style: TextStyle(fontSize: 10, color: Colors.green),
                              ),
                            ],
                          ),
                        ),
                      if (asset.needsMaintenance) ...[
                        if (asset.isUnderWarranty) const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.orange.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.build_rounded, size: 12, color: Colors.orange),
                              const SizedBox(width: 4),
                              Text(
                                'Maintenance le ${DateFormat('dd/MM/yyyy').format(asset.nextMaintenanceDate!.toLocal())}',
                                style: TextStyle(fontSize: 10, color: Colors.orange),
                              ),
                            ],
                          ),
                        ),
                    ],
                  ],
                  // Actions
                  if (onCheckout != null || onMaintenance != null || onTransfer != null) ...[
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        if (onCheckout != null)
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: onCheckout,
                              icon: const Icon(Icons.checkout_rounded, size: 18),
                              label: const Text('Emprunter'),
                              style: FilledButton.styleFrom(
                                backgroundColor: Colors.blue,
                                foregroundColor: Colors.white,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                        if (onCheckout != null && (onMaintenance != null || onTransfer != null)) const SizedBox(width: 8),
                        if (onMaintenance != null)
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: onMaintenance,
                              icon: const Icon(Icons.build_rounded, size: 18),
                              label: const Text('Maintenance'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: Colors.orange,
                                side: const BorderSide(color: Colors.orange),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                        if (onMaintenance != null && onTransfer != null) const SizedBox(width: 8),
                        if (onTransfer != null)
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: onTransfer,
                              icon: const Icon(Icons.swap_horiz_rounded, size: 18),
                              label: const Text('Transférer'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: Colors.purple,
                                side: const BorderSide(color: Colors.purple),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPlaceholder(Color typeColor) {
    return Container(
      color: typeColor.withOpacity(0.1),
      child: Center(
        child: Icon(
          AssetType.values.firstWhere((e) => e == asset.type).getIcon(),
          size: 48,
          color: typeColor.withOpacity(0.5),
        ),
      ),
    );
  }
}