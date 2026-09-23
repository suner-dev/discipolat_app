import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/finances/models/finance_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

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
    final typeColor = _getTypeColor(transaction.type);
    final isIncoming = _isIncoming(transaction.type);
    final amount = NumberFormat('#,##0.00', 'fr_FR').format(transaction.amount.abs());

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
                child: Icon(_getTypeIcon(transaction.type), color: typeColor, size: 22),
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
                            transaction.reference,
                            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '${isIncoming ? '+' : '-'}$amount ${transaction.currency}',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: typeColor),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      transaction.description ?? transaction.category.displayName,
                      style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 4,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        _buildStatusBadge(transaction.status),
                        _buildInfo(
                          Icons.calendar_today_rounded,
                          DateFormat('dd/MM/yyyy').format(transaction.date),
                          AppColors.surface.withOpacity(0.7),
                        ),
                        if (transaction.accountName != null)
                          _buildInfo(Icons.account_balance_rounded, transaction.accountName!, Colors.blue),
                        if (transaction.isReconciled)
                          _buildInfo(Icons.verified_rounded, 'Réconcilié', Colors.green),
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

  Widget _buildStatusBadge(TransactionStatus status) {
    return _buildInfo(_getStatusIcon(status), _getStatusLabel(status), _getStatusColor(status));
  }

  static bool _isIncoming(TransactionType type) {
    switch (type) {
      case TransactionType.income:
      case TransactionType.donation:
      case TransactionType.tontinePayout:
        return true;
      case TransactionType.expense:
      case TransactionType.transfer:
      case TransactionType.tontineContribution:
        return false;
    }
  }

  static IconData _getTypeIcon(TransactionType type) {
    switch (type) {
      case TransactionType.income:
        return Icons.trending_up_rounded;
      case TransactionType.expense:
        return Icons.trending_down_rounded;
      case TransactionType.transfer:
        return Icons.swap_horiz_rounded;
      case TransactionType.donation:
        return Icons.volunteer_activism_rounded;
      case TransactionType.tontineContribution:
        return Icons.savings_rounded;
      case TransactionType.tontinePayout:
        return Icons.redeem_rounded;
    }
  }

  static Color _getTypeColor(TransactionType type) {
    switch (type) {
      case TransactionType.income:
      case TransactionType.donation:
        return Colors.green;
      case TransactionType.expense:
        return Colors.red;
      case TransactionType.transfer:
        return Colors.blue;
      case TransactionType.tontineContribution:
        return Colors.purple;
      case TransactionType.tontinePayout:
        return Colors.teal;
    }
  }

  static IconData _getStatusIcon(TransactionStatus status) {
    switch (status) {
      case TransactionStatus.pending:
        return Icons.schedule_rounded;
      case TransactionStatus.completed:
        return Icons.check_circle_rounded;
      case TransactionStatus.failed:
        return Icons.error_rounded;
      case TransactionStatus.cancelled:
        return Icons.cancel_rounded;
      case TransactionStatus.refunded:
        return Icons.replay_rounded;
    }
  }

  static String _getStatusLabel(TransactionStatus status) {
    switch (status) {
      case TransactionStatus.pending:
        return 'En attente';
      case TransactionStatus.completed:
        return 'Complétée';
      case TransactionStatus.failed:
        return 'Échouée';
      case TransactionStatus.cancelled:
        return 'Annulée';
      case TransactionStatus.refunded:
        return 'Remboursée';
    }
  }

  static Color _getStatusColor(TransactionStatus status) {
    switch (status) {
      case TransactionStatus.pending:
        return Colors.orange;
      case TransactionStatus.completed:
        return Colors.green;
      case TransactionStatus.failed:
        return Colors.red;
      case TransactionStatus.cancelled:
        return Colors.grey;
      case TransactionStatus.refunded:
        return Colors.blue;
    }
  }
}
