import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'api_service.dart';
import '../models/branding.dart';

final apiServiceProvider = Provider<ApiService>((ref) {
  return ApiService();
});

/// Identité de l'église (thème dynamique).
///
/// Endpoint public `GET /api/v1/public/settings` (aucune authentification
/// requise). En cas d'échec réseau (hors-ligne, ancien backend) le branding
/// retombe sur l'identité par défaut — l'application démarre toujours.
final brandingProvider = FutureProvider<Branding>((ref) async {
  final api = ref.watch(apiServiceProvider);
  try {
    final response = await api.get('/public/settings');
    if (response.data is Map<String, dynamic>) {
      return Branding.fromJson(response.data as Map<String, dynamic>);
    }
    return const Branding();
  } catch (_) {
    return const Branding();
  }
});
