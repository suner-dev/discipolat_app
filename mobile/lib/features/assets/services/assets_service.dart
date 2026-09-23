import 'package:discipolat_mobile/data/services/api_service.dart';
import '../models/asset_model.dart';

class AssetsService {
  final ApiService _api = ApiService();

  Future<List<Asset>> getAssets() async {
    try {
      final response = await _api.get('/assets');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => Asset.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  Future<Asset?> getAsset(int id) async {
    try {
      final response = await _api.get('/assets/$id');
      if (response.data != null) {
        return Asset.fromJson(response.data as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  Future<List<AssetMaintenance>> getMaintenanceRecords(int assetId) async {
    try {
      final response = await _api.get('/assets/$assetId/maintenance');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => AssetMaintenance.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  Future<List<AssetCheckout>> getCheckouts(int assetId) async {
    try {
      final response = await _api.get('/assets/$assetId/checkouts');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => AssetCheckout.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
}
