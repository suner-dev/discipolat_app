import '../../models/form_template.dart';
import 'api_service.dart';

/// Service for FormTemplate operations (mobile).
/// Backend: FormController (/api/v1/forms)
class FormService {
  final ApiService _api;

  FormService(this._api);

  /// Fetch all form templates.
  Future<List<FormTemplate>> fetchAll() async {
    try {
      final res = await _api.get('/forms');
      final payload = _asList(res.data);
      return payload
          .map((e) => FormTemplate.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } catch (_) {
      return [];
    }
  }

  /// Create a new form template.
  Future<FormTemplate> create({
    required String titre,
    String? description,
    String? categorie,
    String fieldsJson = '[]',
  }) async {
    final res = await _api.post('/forms', data: {
      'titre': titre,
      'description': description,
      'categorie': categorie,
      'fieldsJson': fieldsJson,
    });
    return FormTemplate.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  /// Publish a form template.
  Future<void> publish(String id) async {
    await _api.post('/forms/$id/publish');
  }

  /// Archive a form template.
  Future<void> archive(String id) async {
    await _api.post('/forms/$id/archive');
  }

  /// Delete a form template.
  Future<void> delete(String id) async {
    await _api.delete('/forms/$id');
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
