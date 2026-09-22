import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';
import 'package:table_calendar/table_calendar.dart';

import 'package:discipolat_mobile/features/assets/models/asset_model.dart';
import 'package:discipolat_mobile/features/assets/services/assets_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart'
    show AppColors;
import 'package:discipolat_mobile/features/assets/widgets/asset_card.dart';
import 'package:discipolat_mobile/features/assets/widgets/asset_filter_chips.dart';

class AssetsScreen extends ConsumerStatefulWidget {
  const AssetsScreen({super.key});

  @override
  ConsumerState<AssetsScreen> createState() => _AssetsScreenState();
}

class _AssetsScreenState extends ConsumerState<AssetsScreen> with SingleTickerProviderStateMixin {
  AssetType? _filterType;
  AssetStatus? _filterStatus;
  bool _showNeedsMaintenance = false;
  bool _showUnderWarranty = false;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final assetsAsync = ref.watch(_assetsProvider(_filterType, _filterStatus, _showNeedsMaintenance, _showUnderWarranty));
    final maintenancesAsync = ref.watch(_maintenancesProvider);
    final checkoutsAsync = ref.watch(_checkoutsProvider);
    final inventoryAsync = ref.watch(_inventoryProvider);

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Actifs & Inventaire'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.surface.withOpacity(0.7),
          tabs: const [
            Tab(text: 'Actifs'),
            Tab(text: 'Maintenance'),
            Tab(text: 'Emprunts'),
            Tab(text: 'Inventaire'),
          ],
        ),
        actions: [
          PopupMenuButton<AssetType?>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() => _filterType = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous types')),
              ...AssetType.values.map((t) => PopupMenuItem(value: t, child: Text(t.displayName))),
            ],
          ),
          PopupMenuButton<AssetStatus?>(
            icon: const Icon(Icons.filter_alt_rounded),
            onSelected: (value) => setState(() => _filterStatus = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous statuts')),
              ...AssetStatus.values.map((s) => PopupMenuItem(value: s, child: Text(s.displayName))),
            ],
          ),
          Row(
            children: [
              FilterChip(
                label: const Text('Maintenance'),
                selected: _showNeedsMaintenance,
                onSelected: (value) => setState(() => _showNeedsMaintenance = value),
                selectedColor: Colors.orange.withOpacity(0.2),
                checkmarkColor: Colors.orange,
              ),
              const SizedBox(width: 8),
              FilterChip(
                label: const Text('Garantie'),
                selected: _showUnderWarranty,
                onSelected: (value) => setState(() => _showUnderWarranty = value),
                selectedColor: Colors.green.withOpacity(0.2),
                checkmarkColor: Colors.green,
              ),
              const SizedBox(width: 8),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.qr_code_scanner_rounded),
            onPressed: () => context.push('/assets/qr-scan'),
            tooltip: 'Scanner QR/Barcode',
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => context.push('/assets/create'),
            tooltip: 'Nouvel actif',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Assets tab
          Column(
            children: [
              AssetFilterChips(
                selectedType: _filterType,
                selectedStatus: _filterStatus,
                showNeedsMaintenance: _showNeedsMaintenance,
                showUnderWarranty: _showUnderWarranty,
                onTypeChanged: (t) => setState(() => _filterType = t),
                onStatusChanged: (s) => setState(() => _filterStatus = s),
                onMaintenanceChanged: (v) => setState(() => _showNeedsMaintenance = v),
                onWarrantyChanged: (v) => setState(() => _showUnderWarranty = v),
              ),
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () => ref.refresh(_assetsProvider(_filterType, _filterStatus, _showNeedsMaintenance, _showUnderWarranty).future),
                  child: assetsAsync.when(
                    data: (assets) {
                      if (assets.isEmpty) {
                        return _buildEmptyState(
                          message: 'Aucun actif',
                          action: () => context.push('/assets/create'),
                        );
                      }
                      return ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: assets.length,
                        itemBuilder: (context, index) {
                          final asset = assets[index];
                          return AssetCard(
                            asset: asset,
                            onTap: () => context.push('/assets/${asset.id}'),
                            onCheckout: asset.status == AssetStatus.available ? () => context.push('/assets/${asset.id}/checkout') : null,
                            onMaintenance: asset.status == AssetStatus.available || asset.status == AssetStatus.inUse
                                ? () => context.push('/assets/${asset.id}/maintenance/create')
                                : null,
                            onTransfer: () => context.push('/assets/${asset.id}/transfer'),
                          );
                        },
                      );
                    },
                    loading: () => const Center(child: CircularProgressIndicator()),
                    error: (error, _) => Center(child: Text('Erreur: $error')),
                  ),
                ),
              ),
            ],
          ),
          // Maintenance tab
          _buildMaintenanceTab(),
          // Checkouts tab
          _buildCheckoutsTab(),
          // Inventory tab
          _buildInventoryTab(),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/assets/create'),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nouvel actif'),
        backgroundColor: AppColors.primary,
      ),
    );
  }

  Widget _buildMaintenanceTab() {
    final maintenancesAsync = ref.watch(_maintenancesProvider);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: () => context.push('/assets/maintenance/create'),
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('Nouvelle maintenance'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.orange),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => ref.refresh(_maintenancesProvider),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Actualiser'),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_maintenancesProvider.future),
            child: maintenancesAsync.when(
              data: (maintenances) {
                if (maintenances.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucune maintenance planifiée',
                    action: () => context.push('/assets/maintenance/create'),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: maintenances.length,
                  itemBuilder: (context, index) {
                    final maintenance = maintenances[index];
                    return _buildMaintenanceCard(maintenance);
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildMaintenanceCard(AssetMaintenance maintenance) {
    final isOverdue = maintenance.status == MaintenanceStatus.overdue;
    final statusColor = maintenance.status.getColor();
    final typeColor = MaintenanceType.values.firstWhere((e) => e.name == maintenance.type.name).getColor();

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: typeColor.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(Icons.build_rounded, color: typeColor, size: 24),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        maintenance.assetName,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      Text(
                        MaintenanceType.values.firstWhere((e) => e.name == maintenance.type.name).displayName,
                        style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: maintenance.status.getColor().withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      maintenance.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: maintenance.status.getColor()),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (maintenance.description != null) ...[
                Text(maintenance.description!, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7))),
                const SizedBox(height: 12),
              ],
              Row(
                children: [
                  Icon(Icons.schedule_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                  const SizedBox(width: 4),
                  Text(
                    'Prévue: ${DateFormat('dd/MM/yyyy').format(maintenance.scheduledDate.toLocal())}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                  ),
                  if (maintenance.nextMaintenanceDate != null) ...[
                    const SizedBox(width: 16),
                    Icon(Icons.calendar_today_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Prochaine: ${DateFormat('dd/MM/yyyy').format(maintenance.nextMaintenanceDate!.toLocal())}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.surface.withOpacity(0.7)),
                    ),
                  ],
                ],
              ),
              if (maintenance.cost != null) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.attach_money_rounded, size: 14, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                    Text(
                      'Coût: ${NumberFormat.currency(locale: 'fr_FR', symbol: maintenance.currency ?? '€').format(maintenance.cost!)}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w500, color: AppColors.surface),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  if (maintenance.status == MaintenanceStatus.scheduled || maintenance.status == MaintenanceStatus.overdue)
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: () => _completeMaintenance(maintenance),
                        icon: const Icon(Icons.check_rounded, size: 18),
                        label: const Text('Terminer'),
                        style: FilledButton.styleFrom(
                          backgroundColor: Colors.green,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                    ),
                  if (maintenance.status != MaintenanceStatus.completed && maintenance.status != MaintenanceStatus.cancelled) ...[
                    if (maintenance.status == MaintenanceStatus.scheduled || maintenance.status == MaintenanceStatus.overdue) const SizedBox(width: 8),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => context.push('/assets/maintenance/${maintenance.id}'),
                        icon: const Icon(Icons.edit_rounded, size: 18),
                        label: const Text('Détails'),
                        style: OutlinedButton.styleFrom(
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
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

  Widget _buildCheckoutsTab() {
    final checkoutsAsync = ref.watch(_checkoutsProvider);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: () => context.push('/assets/checkout/create'),
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('Nouvel emprunt'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.blue),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => ref.refresh(_checkoutsProvider),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Actualiser'),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_checkoutsProvider.future),
            child: checkoutsAsync.when(
              data: (checkouts) {
                if (checkouts.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucun emprunt',
                    action: () => context.push('/assets/checkout/create'),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: checkouts.length,
                  itemBuilder: (context, index) {
                    final checkout = checkouts[index];
                    return _buildCheckoutCard(checkout);
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildCheckoutCard(AssetCheckout checkout) {
    final isOverdue = checkout.isOverdue;
    final statusColor = checkout.isOverdue ? Colors.red : _getCheckoutStatusColor(checkout.status);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: Colors.blue.withOpacity(0.2),
                  child: const Icon(Icons.inventory_2_rounded, color: Colors.blue),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        checkout.assetName,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      Text(
                        'Par: ${checkout.checkedOutByName}',
                        style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: (checkout.isOverdue ? Colors.red : _getCheckoutStatusColor(checkout.status)).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      checkout.status.displayName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: checkout.isOverdue ? Colors.red : _getCheckoutStatusColor(checkout.status)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _buildInfoChip(Icons.calendar_today_rounded, 'Emprunt: ${DateFormat('dd/MM/yyyy').format(checkout.checkoutDate.toLocal())}', Colors.blue),
                  if (checkout.expectedReturnDate != null) ...[
                    const SizedBox(width: 12),
                    _buildInfoChip(
                      Icons.calendar_today_rounded,
                      'Retour prévu: ${DateFormat('dd/MM/yyyy').format(checkout.expectedReturnDate!.toLocal())}',
                      checkout.isOverdue ? Colors.red : Colors.green,
                    ),
                  ],
                ],
              ),
              if (checkout.actualReturnDate != null) ...[
                const SizedBox(height: 8),
                _buildInfoChip(Icons.check_circle_rounded, 'Retourné: ${DateFormat('dd/MM/yyyy').format(checkout.actualReturnDate!.toLocal())}', Colors.green),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  if (checkout.status == CheckoutStatus.checkedOut)
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: () => _returnAsset(checkout),
                        icon: const Icon(Icons.keyboard_return_rounded, size: 18),
                        label: const Text('Retourner'),
                        style: FilledButton.styleFrom(
                          backgroundColor: Colors.green,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                    ),
                  if (checkout.status == CheckoutStatus.checkedOut) const SizedBox(width: 8),
                  if (checkout.status == CheckoutStatus.pending)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _approveCheckout(checkout),
                        icon: const Icon(Icons.check_rounded, size: 18),
                        label: const Text('Approuver'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.green,
                          side: const BorderSide(color: Colors.green),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                    ),
                  if (checkout.status == CheckoutStatus.pending) const SizedBox(width: 8),
                  if (checkout.status == CheckoutStatus.pending)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _rejectCheckout(checkout),
                        icon: const Icon(Icons.close_rounded, size: 18),
                        label: const Text('Refuser', style: TextStyle(color: Colors.red)),
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: Colors.red),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInventoryTab() {
    final inventoryAsync = ref.watch(_inventoryProvider);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: () => context.push('/assets/inventory/create'),
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('Nouvel article'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.green),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => ref.refresh(_inventoryProvider),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Actualiser'),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => ref.refresh(_inventoryProvider.future),
            child: inventoryAsync.when(
              data: (items) {
                if (items.isEmpty) {
                  return _buildEmptyState(
                    message: 'Aucun article en inventaire',
                    action: () => context.push('/assets/inventory/create'),
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: items.length,
                  itemBuilder: (context, index) {
                    final item = items[index];
                    return _buildInventoryCard(item);
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInventoryCard(InventoryItem item) {
    final isLowStock = item.isLowStock;
    final isOverstock = item.isOverstocked;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: isLowStock ? Colors.orange.withOpacity(0.2) : (isOverstock ? Colors.blue.withOpacity(0.2) : Colors.green.withOpacity(0.2)),
                  child: Icon(
                    isLowStock ? Icons.warning_amber_rounded : (isOverstock ? Icons.inventory_2_rounded : Icons.check_circle_rounded),
                    color: isLowStock ? Colors.orange : (isOverstock ? Colors.blue : Colors.green),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.name,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                      ),
                      Text(
                        '${item.category.displayName} • ${item.unit.displayName}',
                        style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: isLowStock ? Colors.orange.withOpacity(0.2) : (isOverstock ? Colors.blue.withOpacity(0.2) : Colors.green.withOpacity(0.2)),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      isLowStock ? 'STOCK BAS' : (isOverstock ? 'SURSTOCK' : 'OK'),
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: isLowStock ? Colors.orange : (isOverstock ? Colors.blue : Colors.green)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _buildInfoChip(Icons.inventory_2_rounded, 'Stock: ${item.availableQuantity}/${item.currentStock}', isLowStock ? Colors.orange : Colors.green, Icons.inventory_2_rounded),
                  const SizedBox(width: 12),
                  _buildInfoChip(Icons.warning_rounded, 'Min: ${item.minStockLevel}', isLowStock ? Colors.orange : Colors.blue, Icons.warning_rounded),
                  const SizedBox(width: 12),
                  _buildInfoChip(Icons.attach_money_rounded, '${NumberFormat.currency(locale: 'fr_FR', symbol: '').format(item.unitCost ?? 0)} ${item.currency ?? ''}', Colors.purple, Icons.attach_money_rounded),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String label, Color color, IconData iconData) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(iconData, size: 12, color: color),
          const SizedBox(width: 4),
          Text(label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: color)),
        ],
      ),
    );
  }

  Widget _buildEmptyState({required String message, VoidCallback? action}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.inventory_2_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(message, style: TextStyle(color: AppColors.surface.withOpacity(0.7))),
          if (action != null) ...[
            const SizedBox(height: 24),
            FilledButton.icon(onPressed: action, icon: const Icon(Icons.add_rounded), label: const Text('Créer')),
          ],
        ],
      ),
    );
  }

  Color _getCheckoutStatusColor(CheckoutStatus status) {
    switch (status) {
      case CheckoutStatus.pending:
        return Colors.orange;
      case CheckoutStatus.approved:
        return Colors.blue;
      case CheckoutStatus.checkedOut:
        return Colors.green;
      case CheckoutStatus.returned:
        return Colors.grey;
      case CheckoutStatus.overdue:
        return Colors.red;
      case CheckoutStatus.cancelled:
        return Colors.red;
    }
  }

  Future<void> _completeMaintenance(AssetMaintenance maintenance) async {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Fonctionnalité bientôt disponible')));
  }

  Future<void> _returnAsset(AssetCheckout checkout) async {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Fonctionnalité bientôt disponible')));
  }

  Future<void> _approveCheckout(AssetCheckout checkout) async {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Fonctionnalité bientôt disponible')));
  }

  Future<void> _rejectCheckout(AssetCheckout checkout) async {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Fonctionnalité bientôt disponible')));
  }
}

// Providers
final _assetsProvider = FutureProvider.family<List<Asset>, (AssetType?, AssetStatus?, bool, bool)>((ref, params) async {
  final (type, status, needsMaintenance, underWarranty) = params;
  final service = ref.watch(assets_service.AssetsServiceProvider);
  return service.getAssets(
    type: type,
    status: status,
    needsMaintenance: needsMaintenance,
    underWarranty: underWarranty,
  );
});

final _maintenancesProvider = FutureProvider<List<AssetMaintenance>>((ref) async {
  final service = ref.watch(assets_service.AssetsServiceProvider);
  return service.getMaintenances();
});

final _checkoutsProvider = FutureProvider<List<AssetCheckout>>((ref) async {
  final service = ref.watch(assets_service.AssetsServiceProvider);
  return service.getCheckouts();
});

final _inventoryProvider = FutureProvider<List<InventoryItem>>((ref) async {
  final service = ref.watch(assets_service.AssetsServiceProvider);
  return service.getInventoryItems();
});