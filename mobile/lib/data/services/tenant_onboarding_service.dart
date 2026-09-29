// B7 — Service du wizard d'onboarding (contrat §3.1).
//
// Chemins : préfixe `/api/v1` géré par ApiService.
// AUCUN champ inventé, AUCUN endpoint inventé (gate G-B.4).

import '../../models/onboarding_step.dart';
import 'api_service.dart';

class TenantOnboardingService {
  TenantOnboardingService({ApiService? apiService})
      : _apiService = apiService ?? ApiService();

  final ApiService _apiService;

  static const String _base = '/onboarding-wizard';

  Future<List<OnboardingStep>> fetchSteps() async {
    final response = await _apiService.get(_base);
    final list = _asList(response.data);
    return list.map(OnboardingStep.fromJson).toList()
      ..sort((a, b) => a.stepOrder.compareTo(b.stepOrder));
  }

  Future<OnboardingProgress> fetchProgress() async {
    final response = await _apiService.get('$_base/progress');
    return OnboardingProgress.fromJson(_asMap(response.data));
  }

  Future<OnboardingStatus> fetchStatus() async {
    final response = await _apiService.get('$_base/status');
    return OnboardingStatus.fromJson(_asMap(response.data));
  }

  Future<List<OnboardingStep>> initialize() async {
    final response = await _apiService.post('$_base/initialize');
    return _asList(response.data)
        .map(OnboardingStep.fromJson)
        .toList()
      ..sort((a, b) => a.stepOrder.compareTo(b.stepOrder));
  }

  Future<OnboardingStep> startStep(String stepId) async {
    final response = await _apiService.post('$_base/$stepId/start');
    return OnboardingStep.fromJson(_asMap(response.data));
  }

  /// D7 : le corps est facultatif. `[data]` vide -> corps `{}` (valide côté backend).
  /// On envoie `data` comme **objet JSON**, jamais comme String (le backend attend un Map).
  Future<OnboardingStep> completeStep(String stepId, [Map<String, dynamic>? data]) async {
    final response =
        await _apiService.post('$_base/$stepId/complete', data: <String, dynamic>{
      if (data != null) 'data': data,
    });
    return OnboardingStep.fromJson(_asMap(response.data));
  }

  Future<OnboardingStep> skipStep(String stepId, {String? reason}) async {
    final response = await _apiService.post('$_base/$stepId/skip', data: <String, dynamic>{
      if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
    });
    return OnboardingStep.fromJson(_asMap(response.data));
  }

  // --- Décodage strict : une réponse hors contrat doit échouer, pas se dégrader ---

  static Map<String, dynamic> _asMap(dynamic data) {
    if (data is Map<String, dynamic>) return data;
    if (data is Map) return data.cast<String, dynamic>();
    throw FormatException('Réponse inattendue (objet attendu) : ${data.runtimeType}');
  }

  static List<Map<String, dynamic>> _asList(dynamic data) {
    if (data is! List) {
      throw FormatException('Réponse inattendue (liste attendue) : ${data.runtimeType}');
    }
    return data
        .map((e) => e is Map ? e.cast<String, dynamic>() : null)
        .whereType<Map<String, dynamic>>()
        .toList();
  }
}
