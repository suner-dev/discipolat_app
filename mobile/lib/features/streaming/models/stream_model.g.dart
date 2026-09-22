// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'stream_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$StreamModelImpl _$$StreamModelImplFromJson(Map<String, dynamic> json) =>
    _$StreamModelImpl(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      description: json['description'] as String?,
      status: $enumDecode(_$StreamStatusEnumMap, json['status']),
      streamUrl: json['streamUrl'] as String?,
      thumbnailUrl: json['thumbnailUrl'] as String?,
      recordingUrl: json['recordingUrl'] as String?,
      scheduledAt: DateTime.parse(json['scheduledAt'] as String),
      startedAt: json['startedAt'] == null
          ? null
          : DateTime.parse(json['startedAt'] as String),
      endedAt: json['endedAt'] == null
          ? null
          : DateTime.parse(json['endedAt'] as String),
      viewerCount: (json['viewerCount'] as num?)?.toInt() ?? 0,
      totalViews: (json['totalViews'] as num?)?.toInt() ?? 0,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$StreamModelImplToJson(_$StreamModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'description': instance.description,
      'status': _$StreamStatusEnumMap[instance.status]!,
      'streamUrl': instance.streamUrl,
      'thumbnailUrl': instance.thumbnailUrl,
      'recordingUrl': instance.recordingUrl,
      'scheduledAt': instance.scheduledAt.toIso8601String(),
      'startedAt': instance.startedAt?.toIso8601String(),
      'endedAt': instance.endedAt?.toIso8601String(),
      'viewerCount': instance.viewerCount,
      'totalViews': instance.totalViews,
      'createdAt': instance.createdAt.toIso8601String(),
    };

const _$StreamStatusEnumMap = {
  StreamStatus.scheduled: 'SCHEDULED',
  StreamStatus.live: 'LIVE',
  StreamStatus.ended: 'ENDED',
  StreamStatus.cancelled: 'CANCELLED',
};

_$StreamChatMessageImpl _$$StreamChatMessageImplFromJson(
        Map<String, dynamic> json) =>
    _$StreamChatMessageImpl(
      id: (json['id'] as num).toInt(),
      senderName: json['senderName'] as String,
      content: json['content'] as String,
      messageType: json['messageType'] as String,
      emoji: json['emoji'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      isSystem: json['isSystem'] as bool? ?? false,
      isOwn: json['isOwn'] as bool? ?? false,
    );

Map<String, dynamic> _$$StreamChatMessageImplToJson(
        _$StreamChatMessageImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'senderName': instance.senderName,
      'content': instance.content,
      'messageType': instance.messageType,
      'emoji': instance.emoji,
      'createdAt': instance.createdAt.toIso8601String(),
      'isSystem': instance.isSystem,
      'isOwn': instance.isOwn,
    };

_$StreamViewerCountImpl _$$StreamViewerCountImplFromJson(
        Map<String, dynamic> json) =>
    _$StreamViewerCountImpl(
      count: (json['count'] as num).toInt(),
      timestamp: DateTime.parse(json['timestamp'] as String),
    );

Map<String, dynamic> _$$StreamViewerCountImplToJson(
        _$StreamViewerCountImpl instance) =>
    <String, dynamic>{
      'count': instance.count,
      'timestamp': instance.timestamp.toIso8601String(),
    };
