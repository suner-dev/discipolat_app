import 'package:discipolat_mobile/data/services/api_service.dart';
import '../models/asset_model.dart';

/// Service inventaire mobile — contrat serveur vérifié (`InventoryController`
/// et `AssetController`/`AssetMaintenanceController`, V240) :
/// - la liste n'est PAS `/assets` (cette route de liste n'existe pas) : c'est
///   `GET /inventory`, qui renvoie un `Page<InventoryItem>` (`{content: [...]}`) ;
/// - le détail est `GET /inventory/{id}` ;
/// - maintenance et prêts : `GET /assets/{itemId}/maintenance` et
///   `GET /assets/{itemId}/checkouts` (ces deux routes existent bien).
/// Tous les identifiants sont des UUID **String** (le serveur génère des UUID ;
/// un ancien `int id` faisait planter le cast `as int` sur la valeur réelle).
/// Les erreurs Dio remontent typées et sont absorbées ici (le service renvoie
/// des collections vides), comme le service health/streaming.
class AssetsService {
  final ApiService _api = ApiService();

  Future<List<Asset>> getAssets({
    String? categorie,
    String? statut,
    String? q,
    int page = 0,
    int size = 100,
  }) async {
    try {
      final response = await _api.get('/inventory', queryParameters: {
        'page': page,
        'size': size,
        if (categorie != null && categorie.isNotEmpty) 'categorie': categorie,
        if (statut != null && statut.isNotEmpty) 'statut': statut,
        if (q != null && q.isNotEmpty) 'q': q,
      });
      return _contentOf(response.data).map(Asset.fromJson).toList();
    } catch (e) {
      return [];
    }
  }

  Future<Asset?> getAsset(String id) async {
    try {
      final response = await _api.get('/inventory/$id');
      final data = response.data;
      if (data is Map) {
        return Asset.fromJson(data.cast<String, dynamic>());
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  Future<List<AssetMaintenance>> getMaintenanceRecords(String itemId) async {
    try {
      final response = await _api.get('/assets/$itemId/maintenance');
      return _listOf(response.data).map(AssetMaintenance.fromJson).toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<AssetCheckout>> getCheckouts(String itemId) async {
    try {
      final response = await _api.get('/assets/$itemId/checkouts');
      return _listOf(response.data).map(AssetCheckout.fromJson).toList();
    } catch (e) {
      return [];
    }
  }
}

/// `GET /inventory` renvoie un `Page` (`{content: [...]}`) ; on tolère aussi une
/// liste brute au cas où l'enveloppe changerait (mêmes gardes que health_service).
List<Map<String, dynamic>> _contentOf(dynamic data) {
  if (data is Map && data['content'] is List) {
    return (data['content'] as List)
        .whereType<Map<dynamic, dynamic>>()
        .map((e) => e.cast<String, dynamic>())
        .toList();
  }
  return _listOf(data);
}

/// Listes serveur brutes (`ResponseEntity<List<…>>`) : parsing tolérant,
/// aucun cast dur sur un élément non‑Map.
List<Map<String, dynamic>> _listOf(dynamic data) {
  if (data is! List) return const [];
  return data
      .whereType<Map<dynamic, dynamic>>()
      .map((e) => e.cast<String, dynamic>())
      .toList();
}
