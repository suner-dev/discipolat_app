import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/health/models/health_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Fiche lot de pharmacie — vue aplatie V240 (`itemName`, `quantite`,
/// `seuilAlerte`, `isExpired` calculé serveur, `status` wire avec accents).
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
    // Couleurs dérivées de l'état SERVEUR (isExpired / status / seuil),
    // jamais d'une date recalculée côté client.
    final Color color;
    if (stock.isExpired || stock.status == StockStatus.expire) {
      color = Colors.red;
    } else if (stock.isLowStock ||
        stock.status == StockStatus.stockFaible ||
        stock.status == StockStatus.epuise) {
      color = Colors.orange;
    } else if (stock.status == StockStatus.expirant) {
      color = Colors.amber;
    } else {
      color = Colors.green;
    }

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
                    child: const Icon(Icons.medication_rounded,
                        color: Colors.red, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          stock.itemName ?? 'Article inconnu',
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          stock.lotNumber != null
                              ? 'Lot : ${stock.lotNumber}'
                              : 'Lot non renseigné',
                          style: TextStyle(
                              fontSize: 12,
                              color: AppColors.surface.withOpacity(0.7)),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: color.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          color == Colors.green
                              ? Icons.check_circle_rounded
                              : Icons.warning_amber_rounded,
                          size: 12,
                          color: color,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          stock.status.displayName.toUpperCase(),
                          style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                              color: color),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _infoColumn(
                    'Quantité',
                    '${stock.quantite ?? 0}',
                    stock.isLowStock ? Colors.orange : Colors.green,
                    Icons.inventory_2_rounded,
                  ),
                  const SizedBox(width: 12),
                  _infoColumn(
                    'Seuil alerte',
                    '${stock.seuilAlerte ?? 0}',
                    Colors.blue,
                    Icons.warning_rounded,
                  ),
                  const SizedBox(width: 12),
                  _infoColumn(
                    'Expiration',
                    stock.dateExpiration != null
                        ? _prettyDate(stock.dateExpiration!)
                        : '—',
                    stock.isExpired ? Colors.red : Colors.teal,
                    Icons.event_rounded,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// `yyyy-MM-dd` (LocalDate serveur) → `dd/MM/yyyy`, tolérant au parsing.
  static String _prettyDate(String iso) {
    final parsed = DateTime.tryParse(iso);
    if (parsed == null) return iso;
    final m = parsed.month.toString().padLeft(2, '0');
    final d = parsed.day.toString().padLeft(2, '0');
    return '$d/$m/${parsed.year}';
  }

  Widget _infoColumn(String label, String value, Color color, IconData icon) {
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
                Flexible(
                  child: Text(
                    label,
                    style: TextStyle(
                        fontSize: 10, fontWeight: FontWeight.w500, color: color),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: TextStyle(
                  fontSize: 14, fontWeight: FontWeight.bold, color: color),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}
