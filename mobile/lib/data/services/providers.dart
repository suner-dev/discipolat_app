import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'api_service.dart';
import 'group_message_service.dart';
import 'testimony_service.dart';
import 'announcement_service.dart';
import 'form_service.dart';
import '../models/branding.dart';
import '../models/platform_meta.dart';

final apiServiceProvider = Provider<ApiService>((ref) {
  return ApiService();
});

/// Méta-données publiques de la plateforme (profil bêta, version, comptes démo).
///
/// Endpoint public `GET /api/v1/public/meta` (aucune authentification requise).
/// En cas d'échec réseau, retombe sur un environnement neutre (fail-closed :
/// aucun mode bêta) — l'application démarre toujours et n'affiche jamais de
/// données de test hors environnement bêta.
final metaProvider = FutureProvider<PlatformMeta>((ref) async {
  final api = ref.watch(apiServiceProvider);
  try {
    final response = await api.get('/public/meta');
    if (response.data is Map<String, dynamic>) {
      return PlatformMeta.fromJson(response.data as Map<String, dynamic>);
    }
    return const PlatformMeta();
  } catch (_) {
    return const PlatformMeta();
  }
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

/// P10 — Service messagerie de groupe (backend GroupMessageController).
final groupMessageServiceProvider = Provider<GroupMessageService>((ref) {
  final api = ref.watch(apiServiceProvider);
  return GroupMessageService(api);
});

/// Service communautaire (témoignages) — backend TestimonyController.
final testimonyServiceProvider = Provider<TestimonyService>((ref) {
  final api = ref.watch(apiServiceProvider);
  return TestimonyService(api);
});

/// Service annonces planifiées — backend AnnouncementController.
final announcementServiceProvider = Provider<AnnouncementService>((ref) {
  final api = ref.watch(apiServiceProvider);
  return AnnouncementService(api);
});

/// Service formulaires — backend FormController.
final formServiceProvider = Provider<FormService>((ref) {
  final api = ref.watch(apiServiceProvider);
  return FormService(api);
});

