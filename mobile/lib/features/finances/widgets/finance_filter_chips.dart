import 'package:flutter/material.dart';

import 'package:discipolat_mobile/features/finances/models/finance_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Barre de filtres horizontale pour la liste des transactions.
///
/// Deux lignes de puces : type (RECETTE/DEPENSE — seuls types serveur) et
/// catégorie (texte libre, valeurs proposées depuis les transactions déjà
/// chargées ; le serveur filtre sur `categorie`).
class FinanceFilterChips extends StatelessWidget {
  final TransactionType? selectedType;
  final String? selectedCategory;
  final List<String> availableCategories;

  final ValueChanged<TransactionType?> onTypeChanged;
  final ValueChanged<String?> onCategoryChanged;

  const FinanceFilterChips({
    super.key,
    required this.selectedType,
    required this.selectedCategory,
    required this.availableCategories,
    required this.onTypeChanged,
    required this.onCategoryChanged,
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
            labelOf: (t) => t.label,
            colorOf: _typeColor,
            onChanged: onTypeChanged,
          ),
          const SizedBox(height: 8),
          _buildRow<String>(
            values: availableCategories,
            selected: selectedCategory,
            allLabel: 'Toutes catégories',
            labelOf: (c) => c,
            colorOf: (_) => AppColors.primary,
            onChanged: onCategoryChanged,
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

  static Color _typeColor(TransactionType type) {
    switch (type) {
      case TransactionType.recette:
        return Colors.green;
      case TransactionType.depense:
        return Colors.red;
    }
  }
}
