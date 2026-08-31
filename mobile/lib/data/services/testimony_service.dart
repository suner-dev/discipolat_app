import '../../models/testimony.dart';
import 'api_service.dart';

/// Service for Testimony operations (mobile).
/// Backend: TestimonyController (/api/v1/testimonies)
class TestimonyService {
  final ApiService _api;

  TestimonyService(this._api);

  /// Fetch all approved testimonies (community feed).
  Future<List<Testimony>> fetchAll() async {
    try {
      final res = await _api.get('/testimonies');
      final payload = _asList(res.data);
      return payload
          .map((e) => Testimony.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } catch (_) {
      return [];
    }
  }

  /// Create a new testimony.
  Future<Testimony> create({
    required String titre,
    required String contenu,
    required String categorie,
  }) async {
    final res = await _api.post('/testimonies', data: {
      'titre': titre,
      'contenu': contenu,
      'categorie': categorie,
    });
    return Testimony.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  /// Like a testimony.
  Future<void> like(String id) async {
    await _api.post('/testimonies/$id/like');
  }

  List<dynamic> _asList(dynamic data) {
    if (data is List) return data;
    if (data is Map<String, dynamic>) {
      final items = data['content'] ?? data['items'];
      if (items is List) return items;
    }
    return [];
  }
}
