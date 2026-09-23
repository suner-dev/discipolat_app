import 'dart:async';

import 'package:dio/dio.dart';
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

class MessagesService {
  final ApiService _api;
  WebSocketChannel? _wsChannel;
  final _messageController = StreamController<Message>.broadcast();
  final _reactionController = StreamController<MessageReaction>.broadcast();
  final _typingController = StreamController<TypingIndicator>.broadcast();

  MessagesService(this._api);

  Stream<Message> get onNewMessage => _messageController.stream;
  Stream<MessageReaction> get onReaction => _reactionController.stream;
  Stream<TypingIndicator> get onTyping => _typingController.stream;

  // Conversations
  Future<List<Conversation>> getConversations(
      {int page = 0, int size = 20, String? type}) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (type != null) 'type': type,
      };
      final response = await _api.get('/conversations', params: queryParams);
      final data = response.data as List;
      return data
          .map((json) => Conversation.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des conversations: $e');
    }
  }

  Future<Conversation> getConversation(int id) async {
    try {
      final response = await _api.get('/conversations/$id');
      return Conversation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la conversation: $e');
    }
  }

  Future<Conversation> createDirectConversation(int participantId) async {
    try {
      final response = await _api.post('/conversations/direct', data: {
        'participantId': participantId,
      });
      return Conversation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création de la conversation: $e');
    }
  }

  Future<Conversation> createGroupConversation(
      String title, List<int> participantIds,
      {String? avatarUrl}) async {
    try {
      final response = await _api.post('/conversations/group', data: {
        'title': title,
        'participantIds': participantIds,
        if (avatarUrl != null) 'avatarUrl': avatarUrl,
      });
      return Conversation.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création du groupe: $e');
    }
  }

  Future<void> updateConversation(int id,
      {String? title,
      String? avatarUrl,
      bool? isPinned,
      bool? isMuted,
      bool? isArchived}) async {
    try {
      final data = <String, dynamic>{};
      if (title != null) data['title'] = title;
      if (avatarUrl != null) data['avatarUrl'] = avatarUrl;
      if (isPinned != null) data['isPinned'] = isPinned;
      if (isMuted != null) data['isMuted'] = isMuted;
      if (isArchived != null) data['isArchived'] = isArchived;
      await _api.patch('/conversations/$id', data: data);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> deleteConversation(int id) async {
    try {
      await _api.delete('/conversations/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // Participants
  Future<List<ConversationParticipant>> getParticipants(
      int conversationId) async {
    try {
      final response =
          await _api.get('/conversations/$conversationId/participants');
      final data = response.data as List;
      return data
          .map((json) =>
              ConversationParticipant.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des participants: $e');
    }
  }

  Future<void> addParticipants(int conversationId, List<int> userIds) async {
    try {
      await _api.post('/conversations/$conversationId/participants', data: {
        'userIds': userIds,
      });
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout de participants: $e');
    }
  }

  Future<void> removeParticipant(int conversationId, int userId) async {
    try {
      await _api.delete('/conversations/$conversationId/participants/$userId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression du participant: $e');
    }
  }

  Future<void> updateParticipantRole(
      int conversationId, int userId, ConversationRole role) async {
    try {
      await _api
          .patch('/conversations/$conversationId/participants/$userId', data: {
        'role': role.name,
      });
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour du rôle: $e');
    }
  }

  // Messages
  Future<List<Message>> getMessages(int conversationId,
      {int page = 0, int size = 50, int? beforeMessageId}) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (beforeMessageId != null) 'before': beforeMessageId,
      };
      final response = await _api.get('/conversations/$conversationId/messages',
          params: queryParams);
      final data = response.data as List;
      return data
          .map((json) => Message.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des messages: $e');
    }
  }

  Future<Message> sendMessage(int conversationId, String content,
      {MessageType type = MessageType.text,
      List<String>? mediaUrls,
      int? replyToMessageId}) async {
    try {
      final response =
          await _api.post('/conversations/$conversationId/messages', data: {
        'content': content,
        'type': type.name,
        if (mediaUrls != null) 'mediaUrls': mediaUrls,
        if (replyToMessageId != null) 'replyToMessageId': replyToMessageId,
      });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du message: $e');
    }
  }

  Future<Message> sendMediaMessage(
      int conversationId, String filePath, MessageType type) async {
    try {
      // TODO: Upload file first, then send message with media URL
      // For now, placeholder
      throw Exception('Upload de média à implémenter');
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du média: $e');
    }
  }

  Future<Message> editMessage(int messageId, String newContent) async {
    try {
      final response = await _api.patch('/messages/$messageId', data: {
        'content': newContent,
      });
      return Message.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la modification: $e');
    }
  }

  Future<void> deleteMessage(int messageId, {bool forEveryone = false}) async {
    try {
      await _api.delete('/messages/$messageId', params: {
        'forEveryone': forEveryone.toString(),
      });
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  Future<void> forwardMessage(int messageId, List<int> conversationIds) async {
    try {
      await _api.post('/messages/$messageId/forward', data: {
        'conversationIds': conversationIds,
      });
    } catch (e) {
      throw Exception('Erreur lors du transfert: $e');
    }
  }

  // Reactions
  Future<void> addReaction(int messageId, String emoji) async {
    try {
      await _api.post('/messages/$messageId/reactions', data: {
        'emoji': emoji,
      });
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout de la réaction: $e');
    }
  }

  Future<void> removeReaction(int messageId, String emoji) async {
    try {
      await _api.delete('/messages/$messageId/reactions/$emoji');
    } catch (e) {
      throw Exception('Erreur lors de la suppression de la réaction: $e');
    }
  }

  Future<List<MessageReaction>> getReactions(int messageId) async {
    try {
      final response = await _api.get('/messages/$messageId/reactions');
      final data = response.data as List;
      return data
          .map((json) => MessageReaction.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des réactions: $e');
    }
  }

  // Read status
  Future<void> markAsRead(int conversationId, int messageId) async {
    try {
      await _api.post('/conversations/$conversationId/read', data: {
        'messageId': messageId,
      });
    } catch (e) {
      // Silently fail for read status
    }
  }

  Future<int> getUnreadCount(int conversationId) async {
    try {
      final response =
          await _api.get('/conversations/$conversationId/unread-count');
      return (response.data as Map<String, dynamic>)['count'] as int? ?? 0;
    } catch (e) {
      return 0;
    }
  }

  // Search
  Future<List<Message>> searchMessages(
      {required String query,
      int? conversationId,
      int page = 0,
      int size = 20}) async {
    try {
      final queryParams = <String, dynamic>{
        'q': query,
        'page': page,
        'size': size,
        if (conversationId != null) 'conversationId': conversationId,
      };
      final response = await _api.get('/messages/search', params: queryParams);
      final data = response.data as List;
      return data
          .map((json) => Message.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Erreur lors de la recherche: $e');
    }
  }

  // WebSocket for real-time
  void connectWebSocket(String token) {
    if (_wsChannel != null) return;

    try {
      final wsUrl = _api.dio.options.baseUrl
          .replaceFirst('http', 'ws')
          .replaceFirst('/api', '/ws');
      _wsChannel =
          WebSocketChannel.connect(Uri.parse('$wsUrl/messages?token=$token'));

      _wsChannel!.stream.listen(
        (data) {
          final json = data as Map<String, dynamic>;
          final type = json['type'] as String;

          switch (type) {
            case 'NEW_MESSAGE':
              _messageController.add(
                  Message.fromJson(json['payload'] as Map<String, dynamic>));
              break;
            case 'REACTION_ADDED':
            case 'REACTION_REMOVED':
              _reactionController.add(MessageReaction.fromJson(
                  json['payload'] as Map<String, dynamic>));
              break;
            case 'TYPING_START':
            case 'TYPING_STOP':
              _typingController.add(TypingIndicator.fromJson(
                  json['payload'] as Map<String, dynamic>));
              break;
            case 'MESSAGE_READ':
              // Handle read receipts
              break;
            case 'MESSAGE_DELETED':
              // Handle deletion
              break;
            case 'MESSAGE_EDITED':
              // Handle edit
              break;
          }
        },
        onError: (error) {
          print('WebSocket error: $error');
        },
        onDone: () {
          print('WebSocket disconnected');
        },
      );
    } catch (e) {
      print('WebSocket connection error: $e');
    }
  }

  void disconnectWebSocket() {
    _wsChannel?.sink.close();
    _wsChannel = null;
  }

  void sendTyping(int conversationId, bool isTyping) {
    _wsChannel?.sink.add({
      'type': isTyping ? 'TYPING_START' : 'TYPING_STOP',
      'conversationId': conversationId,
    });
  }

  void dispose() {
    disconnectWebSocket();
    _messageController.close();
    _reactionController.close();
    _typingController.close();
  }
}
