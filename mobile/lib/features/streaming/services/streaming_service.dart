import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';

part 'streaming_service.g.dart';

@riverpod
StreamingService streamingService(StreamingServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return StreamingService(api);
}

/// Service streaming aligné sur le contrat serveur V240
/// (LiveStreamController / StreamChatController) :
/// - le tenant n'est JAMAIS envoyé par le client : il vient du JWT
///   (TenantContext serveur). Un ancien `tenantId` en query param était une
///   faille IDOR — elle n'existe plus côté serveur, donc plus ici.
/// - GET /streams et GET /stream-chat/{id} répondent des LISTES JSON brutes
///   (pas de PageResponse) — vérifié sur les controllers.
/// - POST/PUT /streams attendent un corps LiveStream : le serveur ignore
///   id/tenantId/createdBy/status envoyés (ils sont forcés ou transitionnés
///   serveur), d'où `editBody()` côté modèle.
/// Les erreurs Dio ne sont pas enveloppées dans `Exception` : l'écran affiche
/// déjà `Erreur: $e` et une DioException porte un libellé plus parlant
/// (convention health_service).
class StreamingService {
  final ApiService _api;

  StreamingService(this._api);

  // ── Stream CRUD ───────────────────────────────────────────────────────────

  /// GET /streams — liste complète du tenant courant (le serveur ne connaît
  /// aucun filtre `status` : c'est au client de filtrer, comme le web).
  Future<List<StreamModel>> getStreams() async {
    final response = await _api.get('/streams');
    return _listOf(response.data).map(StreamModel.fromJson).toList();
  }

  /// GET /streams/live — seuls les streams LIVE (filtre serveur).
  Future<List<StreamModel>> getLiveStreams() async {
    final response = await _api.get('/streams/live');
    return _listOf(response.data).map(StreamModel.fromJson).toList();
  }

  /// GET /streams/{id} — ajouté en V240 ; 404 si hors tenant ou inexistant.
  Future<StreamModel> getStream(int id) async {
    final response = await _api.get('/streams/$id');
    return StreamModel.fromJson(_mapOf(response.data));
  }

  /// POST /streams — corps = champs éditables uniquement (rôles
  /// ADMIN/PASTEUR/RESPONSABLE côté serveur).
  Future<StreamModel> createStream(Map<String, dynamic> body) async {
    final response = await _api.post('/streams', data: body);
    return StreamModel.fromJson(_mapOf(response.data));
  }

  /// PUT /streams/{id} — le serveur n'applique que les champs éditables
  /// (title requis, description/streamUrl/thumbnailUrl/recordingUrl,
  /// scheduledAt seulement hors LIVE).
  Future<StreamModel> updateStream(int id, Map<String, dynamic> body) async {
    final response = await _api.put('/streams/$id', data: body);
    return StreamModel.fromJson(_mapOf(response.data));
  }

  /// DELETE /streams/{id} — 400 si le stream est LIVE (à arrêter d'abord).
  Future<void> deleteStream(int id) async {
    await _api.delete('/streams/$id');
  }

  Future<StreamModel> goLive(int id) async {
    final response = await _api.post('/streams/$id/go-live');
    return StreamModel.fromJson(_mapOf(response.data));
  }

  Future<StreamModel> endStream(int id) async {
    final response = await _api.post('/streams/$id/end');
    return StreamModel.fromJson(_mapOf(response.data));
  }

  /// POST /streams/{id}/viewer — incrémente le compteur serveur. Best
  /// effort : un échec ici ne doit jamais casser l'affichage d'un direct.
  Future<void> incrementViewer(int id) async {
    try {
      await _api.post('/streams/$id/viewer');
    } catch (_) {
      // volontairement silencieux (compteur indicatif)
    }
  }

  // ── Chat ──────────────────────────────────────────────────────────────────

  /// GET /stream-chat/{streamId} — liste brute, scopée tenant serveur.
  Future<List<StreamChatMessage>> getChatMessages(int streamId) async {
    final response = await _api.get('/stream-chat/$streamId');
    return _listOf(response.data).map(StreamChatMessage.fromJson).toList();
  }

  /// POST /stream-chat/{streamId} — le serveur vérifie que le stream
  /// appartient au tenant (anti-IDOR en écriture) et dérive l'expéditeur
  /// du JWT ; `senderName` n'est qu'un libellé d'affichage.
  Future<void> sendChatMessage(
    int streamId,
    String content, {
    String? emoji,
    String? senderName,
  }) async {
    await _api.post('/stream-chat/$streamId', data: {
      'content': content,
      if (emoji != null) 'emoji': emoji,
      if (senderName != null && senderName.isNotEmpty) 'senderName': senderName,
    });
  }

  /// GET /stream-chat/{streamId}/count — ATTENTION : le serveur compte des
  /// MESSAGES, pas des spectateurs (le compteur de spectateurs est
  /// `LiveStream.viewerCount`). Nom conservé tel quel pour ne pas vendre
  /// autre chose que ce que la route fournit.
  Future<int> getChatMessageCount(int streamId) async {
    final response = await _api.get('/stream-chat/$streamId/count');
    final data = _mapOf(response.data);
    return (data['count'] as num?)?.toInt() ?? 0;
  }
}

/// Les listes serveur sont des JSON brutes (`ResponseEntity<List<…>>`),
/// jamais enveloppées : parsing tolérant, pas de cast dur.
List<Map<String, dynamic>> _listOf(Object? data) {
  if (data is! List) return const [];
  return data.whereType<Map<String, dynamic>>().toList();
}

Map<String, dynamic> _mapOf(Object? data) =>
    data is Map<String, dynamic> ? data : const {};
