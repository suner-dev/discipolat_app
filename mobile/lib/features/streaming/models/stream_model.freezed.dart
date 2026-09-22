// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'stream_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

StreamModel _$StreamModelFromJson(Map<String, dynamic> json) {
  return _StreamModel.fromJson(json);
}

/// @nodoc
mixin _$StreamModel {
  int get id => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  StreamStatus get status => throw _privateConstructorUsedError;
  String? get streamUrl => throw _privateConstructorUsedError;
  String? get thumbnailUrl => throw _privateConstructorUsedError;
  String? get recordingUrl => throw _privateConstructorUsedError;
  DateTime get scheduledAt => throw _privateConstructorUsedError;
  DateTime? get startedAt => throw _privateConstructorUsedError;
  DateTime? get endedAt => throw _privateConstructorUsedError;
  int get viewerCount => throw _privateConstructorUsedError;
  int get totalViews => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;

  /// Serializes this StreamModel to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StreamModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StreamModelCopyWith<StreamModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StreamModelCopyWith<$Res> {
  factory $StreamModelCopyWith(
          StreamModel value, $Res Function(StreamModel) then) =
      _$StreamModelCopyWithImpl<$Res, StreamModel>;
  @useResult
  $Res call(
      {int id,
      String title,
      String? description,
      StreamStatus status,
      String? streamUrl,
      String? thumbnailUrl,
      String? recordingUrl,
      DateTime scheduledAt,
      DateTime? startedAt,
      DateTime? endedAt,
      int viewerCount,
      int totalViews,
      DateTime createdAt});
}

/// @nodoc
class _$StreamModelCopyWithImpl<$Res, $Val extends StreamModel>
    implements $StreamModelCopyWith<$Res> {
  _$StreamModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StreamModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? title = null,
    Object? description = freezed,
    Object? status = null,
    Object? streamUrl = freezed,
    Object? thumbnailUrl = freezed,
    Object? recordingUrl = freezed,
    Object? scheduledAt = null,
    Object? startedAt = freezed,
    Object? endedAt = freezed,
    Object? viewerCount = null,
    Object? totalViews = null,
    Object? createdAt = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as StreamStatus,
      streamUrl: freezed == streamUrl
          ? _value.streamUrl
          : streamUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      thumbnailUrl: freezed == thumbnailUrl
          ? _value.thumbnailUrl
          : thumbnailUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      recordingUrl: freezed == recordingUrl
          ? _value.recordingUrl
          : recordingUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      scheduledAt: null == scheduledAt
          ? _value.scheduledAt
          : scheduledAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      startedAt: freezed == startedAt
          ? _value.startedAt
          : startedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      endedAt: freezed == endedAt
          ? _value.endedAt
          : endedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      viewerCount: null == viewerCount
          ? _value.viewerCount
          : viewerCount // ignore: cast_nullable_to_non_nullable
              as int,
      totalViews: null == totalViews
          ? _value.totalViews
          : totalViews // ignore: cast_nullable_to_non_nullable
              as int,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StreamModelImplCopyWith<$Res>
    implements $StreamModelCopyWith<$Res> {
  factory _$$StreamModelImplCopyWith(
          _$StreamModelImpl value, $Res Function(_$StreamModelImpl) then) =
      __$$StreamModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String title,
      String? description,
      StreamStatus status,
      String? streamUrl,
      String? thumbnailUrl,
      String? recordingUrl,
      DateTime scheduledAt,
      DateTime? startedAt,
      DateTime? endedAt,
      int viewerCount,
      int totalViews,
      DateTime createdAt});
}

/// @nodoc
class __$$StreamModelImplCopyWithImpl<$Res>
    extends _$StreamModelCopyWithImpl<$Res, _$StreamModelImpl>
    implements _$$StreamModelImplCopyWith<$Res> {
  __$$StreamModelImplCopyWithImpl(
      _$StreamModelImpl _value, $Res Function(_$StreamModelImpl) _then)
      : super(_value, _then);

  /// Create a copy of StreamModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? title = null,
    Object? description = freezed,
    Object? status = null,
    Object? streamUrl = freezed,
    Object? thumbnailUrl = freezed,
    Object? recordingUrl = freezed,
    Object? scheduledAt = null,
    Object? startedAt = freezed,
    Object? endedAt = freezed,
    Object? viewerCount = null,
    Object? totalViews = null,
    Object? createdAt = null,
  }) {
    return _then(_$StreamModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as StreamStatus,
      streamUrl: freezed == streamUrl
          ? _value.streamUrl
          : streamUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      thumbnailUrl: freezed == thumbnailUrl
          ? _value.thumbnailUrl
          : thumbnailUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      recordingUrl: freezed == recordingUrl
          ? _value.recordingUrl
          : recordingUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      scheduledAt: null == scheduledAt
          ? _value.scheduledAt
          : scheduledAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      startedAt: freezed == startedAt
          ? _value.startedAt
          : startedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      endedAt: freezed == endedAt
          ? _value.endedAt
          : endedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      viewerCount: null == viewerCount
          ? _value.viewerCount
          : viewerCount // ignore: cast_nullable_to_non_nullable
              as int,
      totalViews: null == totalViews
          ? _value.totalViews
          : totalViews // ignore: cast_nullable_to_non_nullable
              as int,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StreamModelImpl implements _StreamModel {
  const _$StreamModelImpl(
      {required this.id,
      required this.title,
      this.description,
      required this.status,
      this.streamUrl,
      this.thumbnailUrl,
      this.recordingUrl,
      required this.scheduledAt,
      this.startedAt,
      this.endedAt,
      this.viewerCount = 0,
      this.totalViews = 0,
      required this.createdAt});

  factory _$StreamModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$StreamModelImplFromJson(json);

  @override
  final int id;
  @override
  final String title;
  @override
  final String? description;
  @override
  final StreamStatus status;
  @override
  final String? streamUrl;
  @override
  final String? thumbnailUrl;
  @override
  final String? recordingUrl;
  @override
  final DateTime scheduledAt;
  @override
  final DateTime? startedAt;
  @override
  final DateTime? endedAt;
  @override
  @JsonKey()
  final int viewerCount;
  @override
  @JsonKey()
  final int totalViews;
  @override
  final DateTime createdAt;

  @override
  String toString() {
    return 'StreamModel(id: $id, title: $title, description: $description, status: $status, streamUrl: $streamUrl, thumbnailUrl: $thumbnailUrl, recordingUrl: $recordingUrl, scheduledAt: $scheduledAt, startedAt: $startedAt, endedAt: $endedAt, viewerCount: $viewerCount, totalViews: $totalViews, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StreamModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.streamUrl, streamUrl) ||
                other.streamUrl == streamUrl) &&
            (identical(other.thumbnailUrl, thumbnailUrl) ||
                other.thumbnailUrl == thumbnailUrl) &&
            (identical(other.recordingUrl, recordingUrl) ||
                other.recordingUrl == recordingUrl) &&
            (identical(other.scheduledAt, scheduledAt) ||
                other.scheduledAt == scheduledAt) &&
            (identical(other.startedAt, startedAt) ||
                other.startedAt == startedAt) &&
            (identical(other.endedAt, endedAt) || other.endedAt == endedAt) &&
            (identical(other.viewerCount, viewerCount) ||
                other.viewerCount == viewerCount) &&
            (identical(other.totalViews, totalViews) ||
                other.totalViews == totalViews) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      title,
      description,
      status,
      streamUrl,
      thumbnailUrl,
      recordingUrl,
      scheduledAt,
      startedAt,
      endedAt,
      viewerCount,
      totalViews,
      createdAt);

  /// Create a copy of StreamModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StreamModelImplCopyWith<_$StreamModelImpl> get copyWith =>
      __$$StreamModelImplCopyWithImpl<_$StreamModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StreamModelImplToJson(
      this,
    );
  }
}

abstract class _StreamModel implements StreamModel {
  const factory _StreamModel(
      {required final int id,
      required final String title,
      final String? description,
      required final StreamStatus status,
      final String? streamUrl,
      final String? thumbnailUrl,
      final String? recordingUrl,
      required final DateTime scheduledAt,
      final DateTime? startedAt,
      final DateTime? endedAt,
      final int viewerCount,
      final int totalViews,
      required final DateTime createdAt}) = _$StreamModelImpl;

  factory _StreamModel.fromJson(Map<String, dynamic> json) =
      _$StreamModelImpl.fromJson;

  @override
  int get id;
  @override
  String get title;
  @override
  String? get description;
  @override
  StreamStatus get status;
  @override
  String? get streamUrl;
  @override
  String? get thumbnailUrl;
  @override
  String? get recordingUrl;
  @override
  DateTime get scheduledAt;
  @override
  DateTime? get startedAt;
  @override
  DateTime? get endedAt;
  @override
  int get viewerCount;
  @override
  int get totalViews;
  @override
  DateTime get createdAt;

  /// Create a copy of StreamModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StreamModelImplCopyWith<_$StreamModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

StreamChatMessage _$StreamChatMessageFromJson(Map<String, dynamic> json) {
  return _StreamChatMessage.fromJson(json);
}

/// @nodoc
mixin _$StreamChatMessage {
  int get id => throw _privateConstructorUsedError;
  String get senderName => throw _privateConstructorUsedError;
  String get content => throw _privateConstructorUsedError;
  String get messageType => throw _privateConstructorUsedError;
  String? get emoji => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  bool get isSystem => throw _privateConstructorUsedError;
  bool get isOwn => throw _privateConstructorUsedError;

  /// Serializes this StreamChatMessage to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StreamChatMessage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StreamChatMessageCopyWith<StreamChatMessage> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StreamChatMessageCopyWith<$Res> {
  factory $StreamChatMessageCopyWith(
          StreamChatMessage value, $Res Function(StreamChatMessage) then) =
      _$StreamChatMessageCopyWithImpl<$Res, StreamChatMessage>;
  @useResult
  $Res call(
      {int id,
      String senderName,
      String content,
      String messageType,
      String? emoji,
      DateTime createdAt,
      bool isSystem,
      bool isOwn});
}

/// @nodoc
class _$StreamChatMessageCopyWithImpl<$Res, $Val extends StreamChatMessage>
    implements $StreamChatMessageCopyWith<$Res> {
  _$StreamChatMessageCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StreamChatMessage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? senderName = null,
    Object? content = null,
    Object? messageType = null,
    Object? emoji = freezed,
    Object? createdAt = null,
    Object? isSystem = null,
    Object? isOwn = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      senderName: null == senderName
          ? _value.senderName
          : senderName // ignore: cast_nullable_to_non_nullable
              as String,
      content: null == content
          ? _value.content
          : content // ignore: cast_nullable_to_non_nullable
              as String,
      messageType: null == messageType
          ? _value.messageType
          : messageType // ignore: cast_nullable_to_non_nullable
              as String,
      emoji: freezed == emoji
          ? _value.emoji
          : emoji // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      isSystem: null == isSystem
          ? _value.isSystem
          : isSystem // ignore: cast_nullable_to_non_nullable
              as bool,
      isOwn: null == isOwn
          ? _value.isOwn
          : isOwn // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StreamChatMessageImplCopyWith<$Res>
    implements $StreamChatMessageCopyWith<$Res> {
  factory _$$StreamChatMessageImplCopyWith(_$StreamChatMessageImpl value,
          $Res Function(_$StreamChatMessageImpl) then) =
      __$$StreamChatMessageImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String senderName,
      String content,
      String messageType,
      String? emoji,
      DateTime createdAt,
      bool isSystem,
      bool isOwn});
}

/// @nodoc
class __$$StreamChatMessageImplCopyWithImpl<$Res>
    extends _$StreamChatMessageCopyWithImpl<$Res, _$StreamChatMessageImpl>
    implements _$$StreamChatMessageImplCopyWith<$Res> {
  __$$StreamChatMessageImplCopyWithImpl(_$StreamChatMessageImpl _value,
      $Res Function(_$StreamChatMessageImpl) _then)
      : super(_value, _then);

  /// Create a copy of StreamChatMessage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? senderName = null,
    Object? content = null,
    Object? messageType = null,
    Object? emoji = freezed,
    Object? createdAt = null,
    Object? isSystem = null,
    Object? isOwn = null,
  }) {
    return _then(_$StreamChatMessageImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      senderName: null == senderName
          ? _value.senderName
          : senderName // ignore: cast_nullable_to_non_nullable
              as String,
      content: null == content
          ? _value.content
          : content // ignore: cast_nullable_to_non_nullable
              as String,
      messageType: null == messageType
          ? _value.messageType
          : messageType // ignore: cast_nullable_to_non_nullable
              as String,
      emoji: freezed == emoji
          ? _value.emoji
          : emoji // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      isSystem: null == isSystem
          ? _value.isSystem
          : isSystem // ignore: cast_nullable_to_non_nullable
              as bool,
      isOwn: null == isOwn
          ? _value.isOwn
          : isOwn // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StreamChatMessageImpl implements _StreamChatMessage {
  const _$StreamChatMessageImpl(
      {required this.id,
      required this.senderName,
      required this.content,
      required this.messageType,
      this.emoji,
      required this.createdAt,
      this.isSystem = false,
      this.isOwn = false});

  factory _$StreamChatMessageImpl.fromJson(Map<String, dynamic> json) =>
      _$$StreamChatMessageImplFromJson(json);

  @override
  final int id;
  @override
  final String senderName;
  @override
  final String content;
  @override
  final String messageType;
  @override
  final String? emoji;
  @override
  final DateTime createdAt;
  @override
  @JsonKey()
  final bool isSystem;
  @override
  @JsonKey()
  final bool isOwn;

  @override
  String toString() {
    return 'StreamChatMessage(id: $id, senderName: $senderName, content: $content, messageType: $messageType, emoji: $emoji, createdAt: $createdAt, isSystem: $isSystem, isOwn: $isOwn)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StreamChatMessageImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.senderName, senderName) ||
                other.senderName == senderName) &&
            (identical(other.content, content) || other.content == content) &&
            (identical(other.messageType, messageType) ||
                other.messageType == messageType) &&
            (identical(other.emoji, emoji) || other.emoji == emoji) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.isSystem, isSystem) ||
                other.isSystem == isSystem) &&
            (identical(other.isOwn, isOwn) || other.isOwn == isOwn));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, senderName, content,
      messageType, emoji, createdAt, isSystem, isOwn);

  /// Create a copy of StreamChatMessage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StreamChatMessageImplCopyWith<_$StreamChatMessageImpl> get copyWith =>
      __$$StreamChatMessageImplCopyWithImpl<_$StreamChatMessageImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StreamChatMessageImplToJson(
      this,
    );
  }
}

abstract class _StreamChatMessage implements StreamChatMessage {
  const factory _StreamChatMessage(
      {required final int id,
      required final String senderName,
      required final String content,
      required final String messageType,
      final String? emoji,
      required final DateTime createdAt,
      final bool isSystem,
      final bool isOwn}) = _$StreamChatMessageImpl;

  factory _StreamChatMessage.fromJson(Map<String, dynamic> json) =
      _$StreamChatMessageImpl.fromJson;

  @override
  int get id;
  @override
  String get senderName;
  @override
  String get content;
  @override
  String get messageType;
  @override
  String? get emoji;
  @override
  DateTime get createdAt;
  @override
  bool get isSystem;
  @override
  bool get isOwn;

  /// Create a copy of StreamChatMessage
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StreamChatMessageImplCopyWith<_$StreamChatMessageImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

StreamViewerCount _$StreamViewerCountFromJson(Map<String, dynamic> json) {
  return _StreamViewerCount.fromJson(json);
}

/// @nodoc
mixin _$StreamViewerCount {
  int get count => throw _privateConstructorUsedError;
  DateTime get timestamp => throw _privateConstructorUsedError;

  /// Serializes this StreamViewerCount to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StreamViewerCount
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StreamViewerCountCopyWith<StreamViewerCount> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StreamViewerCountCopyWith<$Res> {
  factory $StreamViewerCountCopyWith(
          StreamViewerCount value, $Res Function(StreamViewerCount) then) =
      _$StreamViewerCountCopyWithImpl<$Res, StreamViewerCount>;
  @useResult
  $Res call({int count, DateTime timestamp});
}

/// @nodoc
class _$StreamViewerCountCopyWithImpl<$Res, $Val extends StreamViewerCount>
    implements $StreamViewerCountCopyWith<$Res> {
  _$StreamViewerCountCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StreamViewerCount
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? count = null,
    Object? timestamp = null,
  }) {
    return _then(_value.copyWith(
      count: null == count
          ? _value.count
          : count // ignore: cast_nullable_to_non_nullable
              as int,
      timestamp: null == timestamp
          ? _value.timestamp
          : timestamp // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StreamViewerCountImplCopyWith<$Res>
    implements $StreamViewerCountCopyWith<$Res> {
  factory _$$StreamViewerCountImplCopyWith(_$StreamViewerCountImpl value,
          $Res Function(_$StreamViewerCountImpl) then) =
      __$$StreamViewerCountImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({int count, DateTime timestamp});
}

/// @nodoc
class __$$StreamViewerCountImplCopyWithImpl<$Res>
    extends _$StreamViewerCountCopyWithImpl<$Res, _$StreamViewerCountImpl>
    implements _$$StreamViewerCountImplCopyWith<$Res> {
  __$$StreamViewerCountImplCopyWithImpl(_$StreamViewerCountImpl _value,
      $Res Function(_$StreamViewerCountImpl) _then)
      : super(_value, _then);

  /// Create a copy of StreamViewerCount
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? count = null,
    Object? timestamp = null,
  }) {
    return _then(_$StreamViewerCountImpl(
      count: null == count
          ? _value.count
          : count // ignore: cast_nullable_to_non_nullable
              as int,
      timestamp: null == timestamp
          ? _value.timestamp
          : timestamp // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StreamViewerCountImpl implements _StreamViewerCount {
  const _$StreamViewerCountImpl({required this.count, required this.timestamp});

  factory _$StreamViewerCountImpl.fromJson(Map<String, dynamic> json) =>
      _$$StreamViewerCountImplFromJson(json);

  @override
  final int count;
  @override
  final DateTime timestamp;

  @override
  String toString() {
    return 'StreamViewerCount(count: $count, timestamp: $timestamp)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StreamViewerCountImpl &&
            (identical(other.count, count) || other.count == count) &&
            (identical(other.timestamp, timestamp) ||
                other.timestamp == timestamp));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, count, timestamp);

  /// Create a copy of StreamViewerCount
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StreamViewerCountImplCopyWith<_$StreamViewerCountImpl> get copyWith =>
      __$$StreamViewerCountImplCopyWithImpl<_$StreamViewerCountImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StreamViewerCountImplToJson(
      this,
    );
  }
}

abstract class _StreamViewerCount implements StreamViewerCount {
  const factory _StreamViewerCount(
      {required final int count,
      required final DateTime timestamp}) = _$StreamViewerCountImpl;

  factory _StreamViewerCount.fromJson(Map<String, dynamic> json) =
      _$StreamViewerCountImpl.fromJson;

  @override
  int get count;
  @override
  DateTime get timestamp;

  /// Create a copy of StreamViewerCount
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StreamViewerCountImplCopyWith<_$StreamViewerCountImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
