import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class StockCard extends StatelessWidget {
  final PharmacyStock stock;
  final VoidCallback onTap;

  const StockCard({
    super.key,
    required this.stock,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isLowStock = stock.isLowStock;
    final isExpired = stock.isExpired;
    final statusColor = isExpired ? Colors.red : (isLowStock ? Colors.orange : Colors.green);

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
                    backgroundColor: Colors.red.withOpacity(0.2),
                    child: const Icon(Icons.medication_rounded, color: Colors.red, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          stock.medicationName,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Lot: ${stock.batchNumber ?? 'N/A'}',
                          style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          isExpired ? Icons.warning_rounded : (isLowStock ? Icons.warning_amber_rounded : Icons.check_circle_rounded),
                          size: 12,
                          color: statusColor,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          isExpired ? 'EXPIRÉ' : (isLowStock ? 'STOCK BAS' : 'OK'),
                          style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: statusColor),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _buildInfoColumn(
                    'Stock',
                    '${stock.availableQuantity} / ${stock.quantity}',
                    isLowStock ? Colors.orange : Colors.green,
                    Icons.inventory_2_rounded,
                  ),
                  const SizedBox(width: 16),
                  _buildInfoColumn(
                    'Seuil min',
                    stock.minStockLevel.toString(),
                    Colors.blue,
                    Icons.warning_rounded,
                  ),
                  const SizedBox(width: 16),
                  _buildInfoColumn(
                    'Expiration',
                    DateFormat('dd/MM/yyyy').format(stock.expiryDate.toLocal()),
                    stock.isExpired ? Colors.red : Colors.green,
                    Icons.calendar_today_rounded,
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _buildInfoColumn(
                    'Prix achat',
                    '${NumberFormat.currency(locale: 'fr_FR', symbol: '').format(stock.unitCost ?? 0)} ${stock.medicationName}',
                    Colors.purple,
                    Icons.attach_money_rounded,
                  ),
                  const SizedBox(width: 16),
                  _buildInfoColumn(
                    'Prix vente',
                    '${NumberFormat.currency(locale: 'fr_FR', symbol: '').format(stock.sellingPrice ?? 0)} ${stock.medicationName}',
                    Colors.green,
                    Icons.sell_rounded,
                  ),
                ],
              ),
              if (stock.location != null) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(Icons.location_on_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Emplacement: ${stock.location}',
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

  Widget _buildInfoColumn(String label, String value, Color color, IconData icon) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 14, color: color),
                const SizedBox(width: 4),
                Text(label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: color)),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: color),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}