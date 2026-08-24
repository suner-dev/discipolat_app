import '../../models/group_message.dart';
import '../../models/group_thread.dart';
import 'api_service.dart';

/// P10 — Service messagerie de groupe (mobile).
///
/// S'appuie sur l'API backend Spring Boot exposée par
/// `GroupMessageController` (`/api/v1/group-messages` & `/api/v1/departments`).
///
/// NOTE : les ids sont manipulés en `String` (représentation texte d'un UUID)
/// car le mobile n'a pas d'objet UUID natif et le backend les echange en texte.
class GroupMessageService {
  final ApiService _api;

  GroupMessageService(this._api);

  /// Charge les groupes (departements) auxquels l'utilisateur appartient.
  Future<List<GroupThread>> fetchGroups() async {
    try {
      final res = await _api.get('/departments');
      final payload = res.data;
      if (payload is Map<String, dynamic>) {
        final items = payload['content'] as List<dynamic>? ?? [];
        return items
            .map((e) => GroupThread.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList();
      } else if (payload is List) {
        return payload
            .map((e) => GroupThread.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList();
      }
      return [];
    } catch (_) {
      // Repli offline : aucune donnee sensible n'est stockee localement.
      return [];
    }
  }

  /// Charge l'historique d'un groupe.
  Future<List<GroupMessage>> fetchMessages(String groupId) async {
    try {
      final res = await _api.get('/group-messages/group/$groupId');
      final payload = _asList(res.data);
      return payload
          .map((e) => GroupMessage.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } catch (_) {
      return [];
    }
  }

  /// Envoie un message dans le groupe.
  Future<GroupMessage> sendMessage({
    required String groupId,
    required String senderId,
    required String groupType,
    required String content,
  }) async {
    final res = await _api.post(
      '/group-messages',
      data: {
        'groupId': groupId,
        'groupType': groupType,
        'senderId': senderId,
        'content': content,
        'messageType': 'TEXT',
      },
    );
    return GroupMessage.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  /// P10 — Recherche plein-texte dans les messages d'un groupe.
  Future<List<GroupMessage>> search(String groupId, String q) async {
    final res = await _api.get(
      '/group-messages/search',
      params: {'groupId': groupId, 'q': q},
    );
    final payload = _asList(res.data);
    return payload
        .map((e) => GroupMessage.fromJson(Map<String, dynamic>.from(e as Map)))
        .toList();
  }

  /// React (+1) a un message.
  Future<GroupMessage> react(String messageId) async {
    final res = await _api.post('/group-messages/$messageId/reaction');
    return GroupMessage.fromJson(Map<String, dynamic>.from(res.data as Map));
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
