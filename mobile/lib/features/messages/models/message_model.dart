import 'package:freezed_annotation/freezed_annotation.dart';

part 'message_model.freezed.dart';
part 'message_model.g.dart';

@freezed
class Conversation with _$Conversation {
  const factory Conversation({
    required int id,
    required String title,
    required ConversationType type,
    String? avatarUrl,
    @Default([]) List<int> participantIds,
    @Default(0) int unreadCount,
    Message? lastMessage,
    DateTime? updatedAt,
    @Default(false) bool isPinned,
    @Default(false) bool isMuted,
    @Default(false) bool isArchived,
  }) = _Conversation;

  factory Conversation.fromJson(Map<String, dynamic> json) => _$ConversationFromJson(json);
}

@freezed
class Message with _$Message {
  const factory Message({
    required int id,
    required int conversationId,
    required int senderId,
    required String senderName,
    String? senderAvatarUrl,
    required String content,
    required MessageType type,
    @Default([]) List<String> mediaUrls,
    String? replyToMessageId,
    @Default(false) bool isSystem,
    @Default(false) bool isOwn,
    @Default(0) int reactionsCount,
    @Default({}) Map<String, List<int>> reactions,
    required DateTime createdAt,
    DateTime? editedAt,
  }) = _Message;

  factory Message.fromJson(Map<String, dynamic> json) => _$MessageFromJson(json);
}

@freezed
class ConversationParticipant with _$ConversationParticipant {
  const factory ConversationParticipant({
    required int userId,
    required String name,
    String? avatarUrl,
    required ConversationRole role,
    DateTime? joinedAt,
    @Default(false) bool isOnline,
    DateTime? lastSeenAt,
  }) = _ConversationParticipant;

  factory ConversationParticipant.fromJson(Map<String, dynamic> json) => _$ConversationParticipantFromJson(json);
}

@freezed
class MessageReaction with _$MessageReaction {
  const factory MessageReaction({
    required int messageId,
    required String emoji,
    required int userId,
    required String userName,
    required DateTime createdAt,
  }) = _MessageReaction;

  factory MessageReaction.fromJson(Map<String, dynamic> json) => _$MessageReactionFromJson(json);
}

enum ConversationType {
  @JsonValue('DIRECT')
  direct,
  @JsonValue('GROUP')
  group,
  @JsonValue('CHANNEL')
  channel,
}

enum ConversationRole {
  @JsonValue('ADMIN')
  admin,
  @JsonValue('MODERATOR')
  moderator,
  @JsonValue('MEMBER')
  member,
  @JsonValue('MUTED')
  muted,
}

enum MessageType {
  @JsonValue('TEXT')
  text,
  @JsonValue('IMAGE')
  image,
  @JsonValue('VIDEO')
  video,
  @JsonValue('AUDIO')
  audio,
  @JsonValue('FILE')
  file,
  @JsonValue('LOCATION')
  location,
  @JsonValue('CONTACT')
  contact,
  @JsonValue('SYSTEM')
  system,
  @JsonValue('REPLY')
  reply,
  @JsonValue('FORWARD')
  forward,
}