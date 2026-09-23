import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/finances/models/finance_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Barre de filtres horizontale pour la liste des transactions.
///
/// Trois lignes de puces : type, catégorie et statut.
class FinanceFilterChips extends StatelessWidget {
  final TransactionType? selectedType;
  final TransactionCategory? selectedCategory;
  final TransactionStatus? selectedStatus;

  final ValueChanged<TransactionType?> onTypeChanged;
  final ValueChanged<TransactionCategory?> onCategoryChanged;
  final ValueChanged<TransactionStatus?> onStatusChanged;

  const FinanceFilterChips({
    super.key,
    required this.selectedType,
    required this.selectedCategory,
    required this.selectedStatus,
    required this.onTypeChanged,
    required this.onCategoryChanged,
    required this.onStatusChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildRow<TransactionType>(
            values: TransactionType.values,
            selected: selectedType,
            allLabel: 'Tous types',
            labelOf: _typeLabel,
            colorOf: _typeColor,
            onChanged: onTypeChanged,
          ),
          const SizedBox(height: 8),
          _buildRow<TransactionCategory>(
            values: TransactionCategory.values,
            selected: selectedCategory,
            allLabel: 'Toutes catégories',
            labelOf: (category) => category.displayName,
            colorOf: _categoryColor,
            onChanged: onCategoryChanged,
          ),
          const SizedBox(height: 8),
          _buildRow<TransactionStatus>(
            values: TransactionStatus.values,
            selected: selectedStatus,
            allLabel: 'Tous statuts',
            labelOf: _statusLabel,
            colorOf: _statusColor,
            onChanged: onStatusChanged,
          ),
        ],
      ),
    );
  }

  Widget _buildRow<T>({
    required List<T> values,
    required T? selected,
    required String allLabel,
    required String Function(T) labelOf,
    required Color Function(T) colorOf,
    required ValueChanged<T?> onChanged,
  }) {
    return SizedBox(
      height: 40,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          FilterChip(
            label: Text(allLabel),
            selected: selected == null,
            onSelected: (_) => onChanged(null),
            selectedColor: AppColors.primary.withOpacity(0.2),
            checkmarkColor: AppColors.primary,
            labelStyle: TextStyle(
              color: selected == null ? AppColors.primary : AppColors.surface.withOpacity(0.7),
              fontWeight: selected == null ? FontWeight.w600 : FontWeight.normal,
            ),
            side: BorderSide(
              color: selected == null ? AppColors.primary : AppColors.surface.withOpacity(0.3),
            ),
            backgroundColor: AppColors.cardDark,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          ),
          const SizedBox(width: 8),
          ...values.map((value) {
            final color = colorOf(value);
            final isSelected = selected == value;
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                label: Text(labelOf(value)),
                selected: isSelected,
                onSelected: (_) => onChanged(value),
                selectedColor: color.withOpacity(0.2),
                checkmarkColor: color,
                labelStyle: TextStyle(
                  color: isSelected ? color : AppColors.surface.withOpacity(0.7),
                  fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                ),
                side: BorderSide(
                  color: isSelected ? color : AppColors.surface.withOpacity(0.3),
                ),
                backgroundColor: AppColors.cardDark,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              ),
            );
          }),
        ],
      ),
    );
  }

  static String _typeLabel(TransactionType type) {
    switch (type) {
      case TransactionType.income:
        return 'Revenu';
      case TransactionType.expense:
        return 'Dépense';
      case TransactionType.transfer:
        return 'Transfert';
      case TransactionType.donation:
        return 'Don';
      case TransactionType.tontineContribution:
        return 'Cotisation';
      case TransactionType.tontinePayout:
        return 'Versement';
    }
  }

  static Color _typeColor(TransactionType type) {
    switch (type) {
      case TransactionType.income:
        return Colors.green;
      case TransactionType.expense:
        return Colors.red;
      case TransactionType.transfer:
        return Colors.blue;
      case TransactionType.donation:
        return Colors.amber;
      case TransactionType.tontineContribution:
        return Colors.purple;
      case TransactionType.tontinePayout:
        return Colors.teal;
    }
  }

  static Color _categoryColor(TransactionCategory category) {
    switch (category) {
      case TransactionCategory.donations:
      case TransactionCategory.tithes:
      case TransactionCategory.offerings:
        return Colors.green;
      case TransactionCategory.salaries:
      case TransactionCategory.utilities:
      case TransactionCategory.maintenance:
      case TransactionCategory.supplies:
        return Colors.orange;
      case TransactionCategory.events:
      case TransactionCategory.training:
        return Colors.blue;
      case TransactionCategory.building:
        return Colors.brown;
      case TransactionCategory.missions:
      case TransactionCategory.benevolence:
        return Colors.purple;
      case TransactionCategory.transport:
        return Colors.cyan;
      case TransactionCategory.meals:
        return Colors.pink;
      case TransactionCategory.other:
        return Colors.grey;
    }
  }

  static String _statusLabel(TransactionStatus status) {
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

  static Color _statusColor(TransactionStatus status) {
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
