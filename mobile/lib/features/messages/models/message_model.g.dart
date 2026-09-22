// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'message_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ConversationImpl _$$ConversationImplFromJson(Map<String, dynamic> json) =>
    _$ConversationImpl(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      type: $enumDecode(_$ConversationTypeEnumMap, json['type']),
      avatarUrl: json['avatarUrl'] as String?,
      participantIds: (json['participantIds'] as List<dynamic>?)
              ?.map((e) => (e as num).toInt())
              .toList() ??
          const [],
      unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
      lastMessage: json['lastMessage'] == null
          ? null
          : Message.fromJson(json['lastMessage'] as Map<String, dynamic>),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
      isPinned: json['isPinned'] as bool? ?? false,
      isMuted: json['isMuted'] as bool? ?? false,
      isArchived: json['isArchived'] as bool? ?? false,
    );

Map<String, dynamic> _$$ConversationImplToJson(_$ConversationImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'type': _$ConversationTypeEnumMap[instance.type]!,
      'avatarUrl': instance.avatarUrl,
      'participantIds': instance.participantIds,
      'unreadCount': instance.unreadCount,
      'lastMessage': instance.lastMessage,
      'updatedAt': instance.updatedAt?.toIso8601String(),
      'isPinned': instance.isPinned,
      'isMuted': instance.isMuted,
      'isArchived': instance.isArchived,
    };

const _$ConversationTypeEnumMap = {
  ConversationType.direct: 'DIRECT',
  ConversationType.group: 'GROUP',
  ConversationType.channel: 'CHANNEL',
};

_$MessageImpl _$$MessageImplFromJson(Map<String, dynamic> json) =>
    _$MessageImpl(
      id: (json['id'] as num).toInt(),
      conversationId: (json['conversationId'] as num).toInt(),
      senderId: (json['senderId'] as num).toInt(),
      senderName: json['senderName'] as String,
      senderAvatarUrl: json['senderAvatarUrl'] as String?,
      content: json['content'] as String,
      type: $enumDecode(_$MessageTypeEnumMap, json['type']),
      mediaUrls: (json['mediaUrls'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const [],
      replyToMessageId: json['replyToMessageId'] as String?,
      isSystem: json['isSystem'] as bool? ?? false,
      isOwn: json['isOwn'] as bool? ?? false,
      reactionsCount: (json['reactionsCount'] as num?)?.toInt() ?? 0,
      reactions: (json['reactions'] as Map<String, dynamic>?)?.map(
            (k, e) => MapEntry(k,
                (e as List<dynamic>).map((e) => (e as num).toInt()).toList()),
          ) ??
          const {},
      createdAt: DateTime.parse(json['createdAt'] as String),
      editedAt: json['editedAt'] == null
          ? null
          : DateTime.parse(json['editedAt'] as String),
    );

Map<String, dynamic> _$$MessageImplToJson(_$MessageImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'conversationId': instance.conversationId,
      'senderId': instance.senderId,
      'senderName': instance.senderName,
      'senderAvatarUrl': instance.senderAvatarUrl,
      'content': instance.content,
      'type': _$MessageTypeEnumMap[instance.type]!,
      'mediaUrls': instance.mediaUrls,
      'replyToMessageId': instance.replyToMessageId,
      'isSystem': instance.isSystem,
      'isOwn': instance.isOwn,
      'reactionsCount': instance.reactionsCount,
      'reactions': instance.reactions,
      'createdAt': instance.createdAt.toIso8601String(),
      'editedAt': instance.editedAt?.toIso8601String(),
    };

const _$MessageTypeEnumMap = {
  MessageType.text: 'TEXT',
  MessageType.image: 'IMAGE',
  MessageType.video: 'VIDEO',
  MessageType.audio: 'AUDIO',
  MessageType.file: 'FILE',
  MessageType.location: 'LOCATION',
  MessageType.contact: 'CONTACT',
  MessageType.system: 'SYSTEM',
  MessageType.reply: 'REPLY',
  MessageType.forward: 'FORWARD',
};

_$ConversationParticipantImpl _$$ConversationParticipantImplFromJson(
        Map<String, dynamic> json) =>
    _$ConversationParticipantImpl(
      userId: (json['userId'] as num).toInt(),
      name: json['name'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      role: $enumDecode(_$ConversationRoleEnumMap, json['role']),
      joinedAt: json['joinedAt'] == null
          ? null
          : DateTime.parse(json['joinedAt'] as String),
      isOnline: json['isOnline'] as bool? ?? false,
      lastSeenAt: json['lastSeenAt'] == null
          ? null
          : DateTime.parse(json['lastSeenAt'] as String),
    );

Map<String, dynamic> _$$ConversationParticipantImplToJson(
        _$ConversationParticipantImpl instance) =>
    <String, dynamic>{
      'userId': instance.userId,
      'name': instance.name,
      'avatarUrl': instance.avatarUrl,
      'role': _$ConversationRoleEnumMap[instance.role]!,
      'joinedAt': instance.joinedAt?.toIso8601String(),
      'isOnline': instance.isOnline,
      'lastSeenAt': instance.lastSeenAt?.toIso8601String(),
    };

const _$ConversationRoleEnumMap = {
  ConversationRole.admin: 'ADMIN',
  ConversationRole.moderator: 'MODERATOR',
  ConversationRole.member: 'MEMBER',
  ConversationRole.muted: 'MUTED',
};

_$MessageReactionImpl _$$MessageReactionImplFromJson(
        Map<String, dynamic> json) =>
    _$MessageReactionImpl(
      messageId: (json['messageId'] as num).toInt(),
      emoji: json['emoji'] as String,
      userId: (json['userId'] as num).toInt(),
      userName: json['userName'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$MessageReactionImplToJson(
        _$MessageReactionImpl instance) =>
    <String, dynamic>{
      'messageId': instance.messageId,
      'emoji': instance.emoji,
      'userId': instance.userId,
      'userName': instance.userName,
      'createdAt': instance.createdAt.toIso8601String(),
    };
