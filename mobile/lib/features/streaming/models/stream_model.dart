import 'package:freezed_annotation/freezed_annotation.dart';

part 'stream_model.freezed.dart';
part 'stream_model.g.dart';

@freezed
class StreamModel with _$StreamModel {
  const factory StreamModel({
    required int id,
    required String title,
    String? description,
    required StreamStatus status,
    String? streamUrl,
    String? thumbnailUrl,
    String? recordingUrl,
    required DateTime scheduledAt,
    DateTime? startedAt,
    DateTime? endedAt,
    @Default(0) int viewerCount,
    @Default(0) int totalViews,
    required DateTime createdAt,
  }) = _StreamModel;

  factory StreamModel.fromJson(Map<String, dynamic> json) => _$StreamModelFromJson(json);
}

@freezed
class StreamChatMessage with _$StreamChatMessage {
  const factory StreamChatMessage({
    required int id,
    required String senderName,
    required String content,
    required String messageType,
    String? emoji,
    required DateTime createdAt,
    @Default(false) bool isSystem,
    @Default(false) bool isOwn,
  }) = _StreamChatMessage;

  factory StreamChatMessage.fromJson(Map<String, dynamic> json) => _$StreamChatMessageFromJson(json);
}

@freezed
class StreamViewerCount with _$StreamViewerCount {
  const factory StreamViewerCount({
    required int count,
    required DateTime timestamp,
  }) = _StreamViewerCount;

  factory StreamViewerCount.fromJson(Map<String, dynamic> json) => _$StreamViewerCountFromJson(json);
}

enum StreamStatus {
  @JsonValue('SCHEDULED')
  scheduled,
  @JsonValue('LIVE')
  live,
  @JsonValue('ENDED')
  ended,
  @JsonValue('CANCELLED')
  cancelled;

  String get displayName {
    switch (this) {
      case StreamStatus.scheduled:
        return 'Planifié';
      case StreamStatus.live:
        return 'En direct';
      case StreamStatus.ended:
        return 'Terminé';
      case StreamStatus.cancelled:
        return 'Annulé';
    }
  }

  String get colorHex {
    switch (this) {
      case StreamStatus.scheduled:
        return '#3B82F6';
      case StreamStatus.live:
        return '#EF4444';
      case StreamStatus.ended:
        return '#6B7280';
      case StreamStatus.cancelled:
        return '#9CA3AF';
    }
  }
}

extension StreamStatusExtension on String {
  StreamStatus toStreamStatus() {
    switch (toUpperCase()) {
      case 'SCHEDULED':
        return StreamStatus.scheduled;
      case 'LIVE':
        return StreamStatus.live;
      case 'ENDED':
        return StreamStatus.ended;
      case 'CANCELLED':
        return StreamStatus.cancelled;
      default:
        return StreamStatus.scheduled;
    }
  }
}
