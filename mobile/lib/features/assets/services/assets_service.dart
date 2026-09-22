import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/assets/models/asset_model.dart';

part 'assets_service.g.dart';

@riverpod
AssetsService assetsService(AssetsServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return AssetsService(api);
}

class AssetsService {
  final ApiService _api;

  AssetsService(this._api);

  // Assets
  Future<List<Asset>> getAssets({
    int page = 0,
    int size = 20,
    AssetType? type,
    AssetStatus? status,
    int? departmentId,
    int? roomId,
    int? assignedToId,
    String? search,
    bool? needsMaintenance,
    bool? underWarranty,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (type != null) 'type': type.name,
        if (status != null) 'status': status.name,
        if (departmentId != null) 'departmentId': departmentId,
        if (roomId != null) 'roomId': roomId,
        if (assignedToId != null) 'assignedToId': assignedToId,
        if (search != null && search.isNotEmpty) 'search': search,
        if (needsMaintenance == true) 'needsMaintenance': 'true',
        if (underWarranty == true) 'underWarranty': 'true',
      };
      final response = await _api.get('/assets', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Asset.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des actifs: $e');
    }
  }

  Future<Asset> getAsset(int id) async {
    try {
      final response = await _api.get('/assets/$id');
      return Asset.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'actif: $e');
    }
  }

  Future<Asset> createAsset(Asset asset) async {
    try {
      final response = await _api.post('/assets', data: asset.toJson());
      return Asset.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<Asset> updateAsset(int id, Asset asset) async {
    try {
      final response = await _api.put('/assets/$id', data: asset.toJson());
      return Asset.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> deleteAsset(int id) async {
    try {
      await _api.delete('/assets/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // Maintenance
  Future<List<AssetMaintenance>> getMaintenances({
    int page = 0,
    int size = 20,
    int? assetId,
    MaintenanceType? type,
    MaintenanceStatus? status,
    DateTime? fromDate,
    DateTime? toDate,
    bool? overdueOnly,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (assetId != null) 'assetId': assetId,
        if (type != null) 'type': type.name,
        if (status != null) 'status': status.name,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
        if (overdueOnly == true) 'overdue': 'true',
      };
      final response = await _api.get('/assets/maintenance', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => AssetMaintenance.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des maintenances: $e');
    }
  }

  Future<AssetMaintenance> getMaintenance(int id) async {
    try {
      final response = await _api.get('/assets/maintenance/$id');
      return AssetMaintenance.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la maintenance: $e');
    }
  }

  Future<AssetMaintenance> createMaintenance(AssetMaintenance maintenance) async {
    try {
      final response = await _api.post('/assets/maintenance', data: maintenance.toJson());
      return AssetMaintenance.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<AssetMaintenance> updateMaintenance(int id, AssetMaintenance maintenance) async {
    try {
      final response = await _api.put('/assets/maintenance/$id', data: maintenance.toJson());
      return AssetMaintenance.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<AssetMaintenance> completeMaintenance(int id, {String? workPerformed, double? cost, String? partsUsed, String? notes, DateTime? nextMaintenanceDate, String? nextMaintenanceNotes}) async {
    try {
      final response = await _api.post('/assets/maintenance/$id/complete', data: {
        'workPerformed': workPerformed,
        'cost': cost,
        'partsUsed': partsUsed,
        'notes': notes,
        'nextMaintenanceDate': nextMaintenanceDate?.toIso8601String(),
        'nextMaintenanceNotes': nextMaintenanceNotes,
      });
      return AssetMaintenance.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la complétion: $e');
    }
  }

  // Checkouts
  Future<List<AssetCheckout>> getCheckouts({
    int page = 0,
    int size = 20,
    int? assetId,
    int? userId,
    CheckoutStatus? status,
    bool? overdueOnly,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (assetId != null) 'assetId': assetId,
        if (userId != null) 'userId': userId,
        if (status != null) 'status': status.name,
        if (overdueOnly == true) 'overdue': 'true',
      };
      final response = await _api.get('/assets/checkouts', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => AssetCheckout.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des emprunts: $e');
    }
  }

  Future<AssetCheckout> getCheckout(int id) async {
    try {
      final response = await _api.get('/assets/checkouts/$id');
      return AssetCheckout.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'emprunt: $e');
    }
  }

  Future<AssetCheckout> createCheckout(AssetCheckout checkout) async {
    try {
      final response = await _api.post('/assets/checkouts', data: checkout.toJson());
      return AssetCheckout.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<AssetCheckout> approveCheckout(int id) async {
    try {
      final response = await _api.post('/assets/checkouts/$id/approve');
      return AssetCheckout.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'approbation: $e');
    }
  }

  Future<AssetCheckout> checkIn(int id, {String? conditionOnReturn, String? notes}) async {
    try {
      final response = await _api.post('/assets/checkouts/$id/checkin', data: {
        'conditionOnReturn': conditionOnReturn,
        'notes': notes,
      });
      return AssetCheckout.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du retour: $e');
    }
  }

  // Transfers
  Future<List<AssetTransfer>> getTransfers({
    int page = 0,
    int size = 20,
    int? assetId,
    TransferStatus? status,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (assetId != null) 'assetId': assetId,
        if (status != null) 'status': status.name,
      };
      final response = await _api.get('/assets/transfers', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => AssetTransfer.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des transferts: $e');
    }
  }

  Future<AssetTransfer> createTransfer(AssetTransfer transfer) async {
    try {
      final response = await _api.post('/assets/transfers', data: transfer.toJson());
      return AssetTransfer.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Inventory
  Future<List<InventoryItem>> getInventoryItems({
    int page = 0,
    int size = 20,
    ItemCategory? category,
    bool? lowStockOnly,
    bool? overstockOnly,
    String? search,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (category != null) 'category': category.name,
        if (lowStockOnly == true) 'lowStock': 'true',
        if (overstockOnly == true) 'overstock': 'true',
        if (search != null && search.isNotEmpty) 'search': search,
      };
      final response = await _api.get('/assets/inventory', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => InventoryItem.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'inventaire: $e');
    }
  }

  Future<InventoryItem> getInventoryItem(int id) async {
    try {
      final response = await _api.get('/assets/inventory/$id');
      return InventoryItem.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement: $e');
    }
  }

  Future<InventoryItem> createInventoryItem(InventoryItem item) async {
    try {
      final response = await _api.post('/assets/inventory', data: item.toJson());
      return InventoryItem.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<InventoryItem> updateInventoryItem(int id, InventoryItem item) async {
    try {
      final response = await _api.put('/assets/inventory/$id', data: item.toJson());
      return InventoryItem.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  // Stock Movements
  Future<List<StockMovement>> getStockMovements({
    int page = 0,
    int size = 20,
    int? itemId,
    MovementType? type,
    DateTime? fromDate,
    DateTime? toDate,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (itemId != null) 'itemId': itemId,
        if (type != null) 'type': type.name,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
      };
      final response = await _api.get('/assets/inventory/movements', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => StockMovement.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des mouvements: $e');
    }
  }

  Future<StockMovement> createStockMovement(StockMovement movement) async {
    try {
      final response = await _api.post('/assets/inventory/movements', data: movement.toJson());
      return StockMovement.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Reports
  Future<Map<String, dynamic>> getAssetSummary() async {
    try {
      final response = await _api.get('/assets/reports/summary');
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement du résumé: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getAssetsByType() async {
    try {
      final response = await _api.get('/assets/reports/by-type');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par type: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getAssetsByStatus() async {
    try {
      final response = await _api.get('/assets/reports/by-status');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par statut: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getMaintenanceCostReport({DateTime? fromDate, DateTime? toDate}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      final response = await _api.get('/assets/reports/maintenance-cost', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport de coûts: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getDepreciationReport() async {
    try {
      final response = await _api.get('/assets/reports/depreciation');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport d\'amortissement: $e');
    }
  }

  // QR/Barcode scanning
  Future<Asset?> getAssetByQrCode(String qrCode) async {
    try {
      final response = await _api.get('/assets/by-qr', queryParameters: {'qrCode': qrCode});
      if (response.data == null) return null;
      return Asset.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<Asset?> getAssetByBarcode(String barcode) async {
    try {
      final response = await _api.get('/assets/by-barcode', queryParameters: {'barcode': barcode});
      if (response.data == null) return null;
      return Asset.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }
}