import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/finances/models/finance_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Carte de transaction — champs réellement exposés par la vue serveur
/// `FinanceService.toMap` (type, categorie, montant, devise, dateTransaction).
class TransactionCard extends StatelessWidget {
  final Transaction transaction;
  final VoidCallback onTap;

  const TransactionCard({
    super.key,
    required this.transaction,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final type = transaction.type;
    final typeColor = _getTypeColor(type);
    final isIncoming = type == TransactionType.recette;
    final amount = NumberFormat('#,##0.00', 'fr_FR').format(transaction.montant.abs());
    final titre = transaction.description.isNotEmpty
        ? transaction.description
        : (transaction.categorie ?? 'Transaction');
    final date = transaction.date;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: typeColor.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(_getTypeIcon(type), color: typeColor, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            transaction.categorie ?? titre,
                            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '${isIncoming ? '+' : '-'}$amount ${transaction.devise ?? transaction.symboleOuDevise}',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: typeColor),
                        ),
                      ],
                    ),
                    if (transaction.description.isNotEmpty && transaction.categorie != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        transaction.description,
                        style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 4,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        _buildInfo(
                          Icons.calendar_today_rounded,
                          date != null ? DateFormat('dd/MM/yyyy').format(date) : 'Sans date',
                          AppColors.surface.withOpacity(0.7),
                        ),
                        if (type != null)
                          _buildInfo(_getTypeIcon(type), type.label, typeColor),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(Icons.chevron_right_rounded, color: AppColors.surface.withOpacity(0.4)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfo(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
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

  static IconData _getTypeIcon(TransactionType? type) {
    switch (type) {
      case TransactionType.recette:
        return Icons.trending_up_rounded;
      case TransactionType.depense:
        return Icons.trending_down_rounded;
      case null:
        return Icons.help_outline_rounded;
    }
  }

  static Color _getTypeColor(TransactionType? type) {
    switch (type) {
      case TransactionType.recette:
        return Colors.green;
      case TransactionType.depense:
        return Colors.red;
      case null:
        return Colors.grey;
    }
  }
}
