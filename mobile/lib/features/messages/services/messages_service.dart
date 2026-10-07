import 'dart:async';
import 'dart:convert';

import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/messages/models/message_model.dart';
import 'package:discipolat_mobile/features/messages/models/typing_indicator.dart';

part 'messages_service.g.dart';

@riverpod
MessagesService messagesService(MessagesServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return MessagesService(api);
}

/// Client de la messagerie — contrat vérifié contre :
/// - MessageController (/api/v1/messages/conversations…) — 1:1 ;
/// - EnhancedMessageController (/api/v1/messages/groups…, /reactions,
///   /replies, /reply, /voice, /search, enhanced) ;
/// - MessageWebSocketController + WebSocketConfig : broker STOMP sur /ws,
///   destination d'envoi /app/conversations/{id}/send et /typing,
///   diffusion /topic/conversations/{id} (+ /typing).
///
/// Règles : aucun paramètre inventé — le serveur ne page pas les
/// conversations ni les messages 1:1 ; seuls les groupes et la recherche
/// acceptent page/size. Les ids sont des UUID String (AuthState().userId
/// a le même domaine).
class MessagesService {
  final ApiService _api;

  MessagesService(this._api);

  // ── Conversations 1:1 ────────────────────────────────────────────────────

  Future<List<Conversation>> getConversations() async {
    try {
      final response = await _api.get('/messages/conversations');
      final data = response.data as List;
      return data
          .map((json) => Conversation.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des conversations: $e');
    }
  }

  /// Ouvre (ou retrouve) la conversation avec [otherUserId] — corps exact
  /// StartConversationRequest {otherUserId: UUID}.
  Future<Conversation> startConversation(String otherUserId) async {
    try {
      final response = await _api.post('/messages/conversations',
          data: {'otherUserId': otherUserId});
      return Conversation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création de la conversation: $e');
    }
  }

  Future<void> markConversationAsRead(String conversationId) async {
    try {
      await _api.patch('/messages/conversations/$conversationId/read');
    } catch (e) {
      // Silently fail for read status
    }
  }

  /// GET /conversations/unread-total → {total: long}
  Future<int> getUnreadTotal() async {
    try {
      final response = await _api.get('/messages/conversations/unread-total');
      return (response.data as Map<String, dynamic>)['total'] as int? ?? 0;
    } catch (e) {
      return 0;
    }
  }

  // ── Messages 1:1 ─────────────────────────────────────────────────────────

  /// Historique enrichi (réactions + réponses) — même choix que le web
  /// (MessagesPage.tsx) : la version basique ne porte pas reactionCounts.
  Future<List<Message>> getMessages(String conversationId) async {
    try {
      final response = await _api
          .get('/messages/conversations/$conversationId/messages/enhanced');
      final data = response.data as List;
      return data
          .map((json) => Message.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des messages: $e');
    }
  }

  /// Envoi simple — corps exact SendMessageRequest {content}, @NotBlank,
  /// max 5000 (validé serveur, garde client pour éviter un aller-retour 400).
  Future<Message> sendMessage(String conversationId, String content) async {
    final trimmed = content.trim();
    if (trimmed.isEmpty) {
      throw Exception('Le message ne peut pas être vide');
    }
    if (trimmed.length > 5000) {
      throw Exception('Message trop long (max 5000 caractères)');
    }
    try {
      final response = await _api
          .post('/messages/conversations/$conversationId/messages',
              data: {'content': trimmed});
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du message: $e');
    }
  }

  /// Envoi enrichi — corps exact SendEnhancedMessageRequest.
  Future<Message> sendEnhancedMessage(
    String conversationId, {
    String? content,
    MessageType messageType = MessageType.text,
    String? mediaUrl,
    int? mediaDuration,
    String? replyToId,
  }) async {
    try {
      final response = await _api
          .post('/messages/conversations/$conversationId/messages/enhanced',
              data: {
                'content': content,
                'messageType': messageType.wire,
                if (mediaUrl != null) 'mediaUrl': mediaUrl,
                if (mediaDuration != null) 'mediaDuration': mediaDuration,
                if (replyToId != null) 'replyToId': replyToId,
              });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du message: $e');
    }
  }

  /// Réponse à un message — POST /conversations/{id}/reply
  /// body `Map<String,String>` {replyToId, content} (le serveur fait
  /// UUID.fromString sur replyToId).
  Future<Message> sendReply(
      String conversationId, String replyToId, String content) async {
    try {
      final response = await _api
          .post('/messages/conversations/$conversationId/reply', data: {
        'replyToId': replyToId,
        'content': content,
      });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi de la réponse: $e');
    }
  }

  /// Message vocal — POST /conversations/{id}/voice {audioUrl, duration}.
  /// L'audio doit d'abord être téléversé pour obtenir [audioUrl].
  Future<Message> sendVoice(
      String conversationId, String audioUrl, int? durationSeconds) async {
    try {
      final response =
          await _api.post('/messages/conversations/$conversationId/voice',
              data: {
                'audioUrl': audioUrl,
                if (durationSeconds != null) 'duration': durationSeconds,
              });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du vocal: $e');
    }
  }

  Future<List<Message>> getReplies(String messageId) async {
    try {
      final response = await _api.get('/messages/messages/$messageId/replies');
      final data = response.data as List;
      return data
          .map((json) => Message.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des réponses: $e');
    }
  }

  // ── Groupes ──────────────────────────────────────────────────────────────

  Future<List<GroupConversation>> getGroups() async {
    try {
      final response = await _api.get('/messages/groups');
      final data = response.data as List;
      return data
          .map((json) =>
              GroupConversation.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des groupes: $e');
    }
  }

  Future<GroupConversation> getGroup(String groupId) async {
    try {
      final response = await _api.get('/messages/groups/$groupId');
      return GroupConversation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du groupe: $e');
    }
  }

  Future<GroupConversation> createGroup({
    required String name,
    String? description,
    String? groupType,
    List<String> memberIds = const [],
  }) async {
    try {
      final response = await _api.post('/messages/groups',
          data: GroupConversation.createBody(
            name: name,
            description: description,
            groupType: groupType,
            memberIds: memberIds,
          ));
      return GroupConversation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création du groupe: $e');
    }
  }

  /// POST /groups/{id}/members — body `{userIds: List<UUID>}` ; réservé admin.
  Future<void> addGroupMembers(String groupId, List<String> userIds) async {
    try {
      await _api.post('/messages/groups/$groupId/members',
          data: {'userIds': userIds});
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout de membres: $e');
    }
  }

  Future<void> removeGroupMember(String groupId, String userId) async {
    try {
      await _api.delete('/messages/groups/$groupId/members/$userId');
    } catch (e) {
      throw Exception('Erreur lors du retrait du membre: $e');
    }
  }

  Future<List<Message>> getGroupMessages(String groupId,
      {int page = 0, int size = 50}) async {
    try {
      final response = await _api.get('/messages/groups/$groupId/messages',
          params: {'page': page, 'size': size});
      final data = response.data as List;
      return data
          .map((json) => Message.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des messages du groupe: $e');
    }
  }

  Future<Message> sendGroupMessage(
    String groupId, {
    String? content,
    MessageType messageType = MessageType.text,
    String? mediaUrl,
    String? replyToId,
  }) async {
    try {
      final response = await _api.post('/messages/groups/$groupId/messages',
          data: {
            'content': content,
            'messageType': messageType.wire,
            if (mediaUrl != null) 'mediaUrl': mediaUrl,
            if (replyToId != null) 'replyToId': replyToId,
          });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi au groupe: $e');
    }
  }

  Future<Message> sendGroupVoice(
      String groupId, String audioUrl, int? durationSeconds) async {
    try {
      final response = await _api.post('/messages/groups/$groupId/voice',
          data: {
            'audioUrl': audioUrl,
            if (durationSeconds != null) 'duration': durationSeconds,
          });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du vocal au groupe: $e');
    }
  }

  // ── Réactions ────────────────────────────────────────────────────────────

  /// Toggle serveur : même emoji = retire, sinon remplace. Le serveur
  /// renvoie {added, reactionCounts, userReaction} — la réponse fait foi.
  Future<ReactionSummary> toggleReaction(
      String messageId, String emoji) async {
    try {
      final response =
          await _api.post('/messages/messages/$messageId/reactions',
              data: {'emoji': emoji});
      return ReactionSummary.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la réaction: $e');
    }
  }

  Future<ReactionSummary> getReactions(String messageId) async {
    try {
      final response = await _api.get('/messages/messages/$messageId/reactions');
      return ReactionSummary.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement des réactions: $e');
    }
  }

  // ── Recherche ────────────────────────────────────────────────────────────

  /// GET /messages/search → `Page<MessageResponse>` (champ content).
  Future<List<Message>> searchMessages(String query,
      {int page = 0, int size = 20}) async {
    try {
      final response = await _api.get('/messages/search',
          params: {'q': query, 'page': page, 'size': size});
      final data = response.data;
      final content = data is Map ? data['content'] : null;
      if (content is! List) return const [];
      return content
          .map((json) => Message.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors de la recherche: $e');
    }
  }

  // ── WebSocket temps réel (STOMP sur /ws, cf. WebSocketConfig) ────────────

  WebSocketChannel? _wsChannel;
  bool _stompConnected = false;
  String? _subscribedConversationId;
  final _messageController = StreamController<Message>.broadcast();
  final _typingController = StreamController<TypingIndicator>.broadcast();
  String _frameBuffer = '';

  Stream<Message> get onNewMessage => _messageController.stream;
  Stream<TypingIndicator> get onTyping => _typingController.stream;
  bool get isRealtimeConnected => _stompConnected;

  /// Le contexte servlet expose /ws à la racine (les contrôleurs REST sont
  /// préfixés /api/v1, mais pas le broker) : http(s)://host/api/v1 →
  /// ws(s)://host/ws.
  Uri _stompUri() {
    final base = Uri.parse(_api.dio.options.baseUrl);
    final scheme = base.scheme == 'https' ? 'wss' : 'ws';
    return base.replace(scheme: scheme, path: '/ws');
  }

  /// Se connecte (ou se ré-abonne) sur la conversation [conversationId].
  void connectRealtime(String conversationId) {
    if (_subscribedConversationId == conversationId && _wsChannel != null) {
      return;
    }
    disconnectRealtime();
    _subscribedConversationId = conversationId;
    try {
      final channel = WebSocketChannel.connect(_stompUri());
      _wsChannel = channel;
      _frameBuffer = '';
      // STOMP 1.1 exige l'en-tête host dans CONNECT.
      _sendFrame('CONNECT', {
        'accept-version': '1.1,1.0',
        'host': '/ws',
      });
      channel.stream.listen(
        (data) => _handleChunk(data.toString()),
        onDone: () => _stompConnected = false,
        onError: (_) => _stompConnected = false,
        cancelOnError: true,
      );
    } catch (e) {
      _stompConnected = false;
      _wsChannel = null;
    }
  }

  void _handleChunk(String chunk) {
    _frameBuffer += chunk;
    // Les frames STOMP sont terminées par NUL ; la dernière partie est
    // le résidu incomplet.
    final parts = _frameBuffer.split('\x00');
    _frameBuffer = parts.last;
    for (final raw in parts.take(parts.length - 1)) {
      _handleFrame(raw);
    }
  }

  void _handleFrame(String frame) {
    final trimmed = frame.trim();
    if (trimmed.isEmpty) return;
    if (trimmed.startsWith('CONNECTED')) {
      _stompConnected = true;
      _subscribeToConversation();
      return;
    }
    if (!trimmed.startsWith('MESSAGE')) return;

    final convId = _subscribedConversationId;
    if (convId == null) return;

    final headerEnd = trimmed.indexOf('\n\n');
    if (headerEnd == -1) return;
    final headers = trimmed.substring(0, headerEnd);
    final isTypingTopic = headers.contains('/typing');
    final body = trimmed.substring(headerEnd + 2).trim();
    if (body.isEmpty) return;

    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      if (isTypingTopic) {
        _typingController.add(TypingIndicator.fromJson(convId, json));
      } else {
        _messageController.add(Message.fromJson(json));
      }
    } catch (_) {
      // Frame partielle ou corps non JSON : ignorer sans corrompre le flux.
    }
  }

  void _subscribeToConversation() {
    final convId = _subscribedConversationId;
    if (convId == null || !_stompConnected) return;
    _sendFrame('SUBSCRIBE',
        {'id': 'msgs-$convId', 'destination': '/topic/conversations/$convId'});
    _sendFrame('SUBSCRIBE', {
      'id': 'typing-$convId',
      'destination': '/topic/conversations/$convId/typing'
    });
  }

  /// Envoi via le broker (broadcast @SendTo vers les participants) ;
  /// fallback REST à privilégier si [isRealtimeConnected] est false.
  void sendViaRealtime(String content) {
    if (!_stompConnected || _wsChannel == null) return;
    _sendFrame(
      'SEND',
      {
        'destination': '/app/conversations/$_subscribedConversationId/send',
        'content-type': 'application/json',
      },
      jsonEncode({'content': content}),
    );
  }

  /// Indicateur « écrit… » — payload {typing: bool} attendu par le serveur.
  void sendTyping(bool typing) {
    if (!_stompConnected || _wsChannel == null) return;
    _sendFrame(
      'SEND',
      {
        'destination': '/app/conversations/$_subscribedConversationId/typing',
        'content-type': 'application/json',
      },
      jsonEncode({'typing': typing}),
    );
  }

  void _sendFrame(String command, Map<String, String> headers, [String? body]) {
    final sink = _wsChannel?.sink;
    if (sink == null) return;
    final sb = StringBuffer(command);
    headers.forEach((k, v) => sb.write('\n$k:$v'));
    sb.write('\n\n');
    if (body != null) sb.write(body);
    sb.write('\x00');
    sink.add(sb.toString());
  }

  void disconnectRealtime() {
    if (_wsChannel != null) {
      if (_stompConnected) {
        _sendFrame('DISCONNECT', {});
      }
      _wsChannel!.sink.close();
    }
    _wsChannel = null;
    _stompConnected = false;
    _subscribedConversationId = null;
    _frameBuffer = '';
  }

  void dispose() {
    disconnectRealtime();
    _messageController.close();
    _typingController.close();
  }
}
