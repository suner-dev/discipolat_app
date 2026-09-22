// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'event_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Event _$EventFromJson(Map<String, dynamic> json) {
  return _Event.fromJson(json);
}

/// @nodoc
mixin _$Event {
  int get id => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  EventType get type => throw _privateConstructorUsedError;
  DateTime get startAt => throw _privateConstructorUsedError;
  DateTime get endAt => throw _privateConstructorUsedError;
  String? get location => throw _privateConstructorUsedError;
  double? get latitude => throw _privateConstructorUsedError;
  double? get longitude => throw _privateConstructorUsedError;
  int? get maxAttendees => throw _privateConstructorUsedError;
  int get currentAttendees => throw _privateConstructorUsedError;
  EventStatus get status => throw _privateConstructorUsedError;
  String? get spaceId => throw _privateConstructorUsedError;
  String? get spaceName => throw _privateConstructorUsedError;
  String? get dressCodeId => throw _privateConstructorUsedError;
  String? get dressCodeName => throw _privateConstructorUsedError;
  String? get dressCodeDescription => throw _privateConstructorUsedError;
  bool get isPublic => throw _privateConstructorUsedError;
  bool get requiresRegistration => throw _privateConstructorUsedError;
  bool get hasCheckIn => throw _privateConstructorUsedError;
  bool get hasGeofencing => throw _privateConstructorUsedError;
  bool get hasFaceCheckIn => throw _privateConstructorUsedError;
  String? get checkInQrCode => throw _privateConstructorUsedError;
  String? get thumbnailUrl => throw _privateConstructorUsedError;
  List<String>? get tags => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;
  bool get isRegistered => throw _privateConstructorUsedError;
  bool get isCheckedIn => throw _privateConstructorUsedError;
  bool get isOrganizer => throw _privateConstructorUsedError;
  bool get isTeamMember => throw _privateConstructorUsedError;

  /// Serializes this Event to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Event
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $EventCopyWith<Event> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventCopyWith<$Res> {
  factory $EventCopyWith(Event value, $Res Function(Event) then) =
      _$EventCopyWithImpl<$Res, Event>;
  @useResult
  $Res call(
      {int id,
      String title,
      String? description,
      EventType type,
      DateTime startAt,
      DateTime endAt,
      String? location,
      double? latitude,
      double? longitude,
      int? maxAttendees,
      int currentAttendees,
      EventStatus status,
      String? spaceId,
      String? spaceName,
      String? dressCodeId,
      String? dressCodeName,
      String? dressCodeDescription,
      bool isPublic,
      bool requiresRegistration,
      bool hasCheckIn,
      bool hasGeofencing,
      bool hasFaceCheckIn,
      String? checkInQrCode,
      String? thumbnailUrl,
      List<String>? tags,
      DateTime createdAt,
      DateTime? updatedAt,
      bool isRegistered,
      bool isCheckedIn,
      bool isOrganizer,
      bool isTeamMember});
}

/// @nodoc
class _$EventCopyWithImpl<$Res, $Val extends Event>
    implements $EventCopyWith<$Res> {
  _$EventCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Event
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? title = null,
    Object? description = freezed,
    Object? type = null,
    Object? startAt = null,
    Object? endAt = null,
    Object? location = freezed,
    Object? latitude = freezed,
    Object? longitude = freezed,
    Object? maxAttendees = freezed,
    Object? currentAttendees = null,
    Object? status = null,
    Object? spaceId = freezed,
    Object? spaceName = freezed,
    Object? dressCodeId = freezed,
    Object? dressCodeName = freezed,
    Object? dressCodeDescription = freezed,
    Object? isPublic = null,
    Object? requiresRegistration = null,
    Object? hasCheckIn = null,
    Object? hasGeofencing = null,
    Object? hasFaceCheckIn = null,
    Object? checkInQrCode = freezed,
    Object? thumbnailUrl = freezed,
    Object? tags = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isRegistered = null,
    Object? isCheckedIn = null,
    Object? isOrganizer = null,
    Object? isTeamMember = null,
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
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as EventType,
      startAt: null == startAt
          ? _value.startAt
          : startAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endAt: null == endAt
          ? _value.endAt
          : endAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      latitude: freezed == latitude
          ? _value.latitude
          : latitude // ignore: cast_nullable_to_non_nullable
              as double?,
      longitude: freezed == longitude
          ? _value.longitude
          : longitude // ignore: cast_nullable_to_non_nullable
              as double?,
      maxAttendees: freezed == maxAttendees
          ? _value.maxAttendees
          : maxAttendees // ignore: cast_nullable_to_non_nullable
              as int?,
      currentAttendees: null == currentAttendees
          ? _value.currentAttendees
          : currentAttendees // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as EventStatus,
      spaceId: freezed == spaceId
          ? _value.spaceId
          : spaceId // ignore: cast_nullable_to_non_nullable
              as String?,
      spaceName: freezed == spaceName
          ? _value.spaceName
          : spaceName // ignore: cast_nullable_to_non_nullable
              as String?,
      dressCodeId: freezed == dressCodeId
          ? _value.dressCodeId
          : dressCodeId // ignore: cast_nullable_to_non_nullable
              as String?,
      dressCodeName: freezed == dressCodeName
          ? _value.dressCodeName
          : dressCodeName // ignore: cast_nullable_to_non_nullable
              as String?,
      dressCodeDescription: freezed == dressCodeDescription
          ? _value.dressCodeDescription
          : dressCodeDescription // ignore: cast_nullable_to_non_nullable
              as String?,
      isPublic: null == isPublic
          ? _value.isPublic
          : isPublic // ignore: cast_nullable_to_non_nullable
              as bool,
      requiresRegistration: null == requiresRegistration
          ? _value.requiresRegistration
          : requiresRegistration // ignore: cast_nullable_to_non_nullable
              as bool,
      hasCheckIn: null == hasCheckIn
          ? _value.hasCheckIn
          : hasCheckIn // ignore: cast_nullable_to_non_nullable
              as bool,
      hasGeofencing: null == hasGeofencing
          ? _value.hasGeofencing
          : hasGeofencing // ignore: cast_nullable_to_non_nullable
              as bool,
      hasFaceCheckIn: null == hasFaceCheckIn
          ? _value.hasFaceCheckIn
          : hasFaceCheckIn // ignore: cast_nullable_to_non_nullable
              as bool,
      checkInQrCode: freezed == checkInQrCode
          ? _value.checkInQrCode
          : checkInQrCode // ignore: cast_nullable_to_non_nullable
              as String?,
      thumbnailUrl: freezed == thumbnailUrl
          ? _value.thumbnailUrl
          : thumbnailUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      tags: freezed == tags
          ? _value.tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isRegistered: null == isRegistered
          ? _value.isRegistered
          : isRegistered // ignore: cast_nullable_to_non_nullable
              as bool,
      isCheckedIn: null == isCheckedIn
          ? _value.isCheckedIn
          : isCheckedIn // ignore: cast_nullable_to_non_nullable
              as bool,
      isOrganizer: null == isOrganizer
          ? _value.isOrganizer
          : isOrganizer // ignore: cast_nullable_to_non_nullable
              as bool,
      isTeamMember: null == isTeamMember
          ? _value.isTeamMember
          : isTeamMember // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$EventImplCopyWith<$Res> implements $EventCopyWith<$Res> {
  factory _$$EventImplCopyWith(
          _$EventImpl value, $Res Function(_$EventImpl) then) =
      __$$EventImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String title,
      String? description,
      EventType type,
      DateTime startAt,
      DateTime endAt,
      String? location,
      double? latitude,
      double? longitude,
      int? maxAttendees,
      int currentAttendees,
      EventStatus status,
      String? spaceId,
      String? spaceName,
      String? dressCodeId,
      String? dressCodeName,
      String? dressCodeDescription,
      bool isPublic,
      bool requiresRegistration,
      bool hasCheckIn,
      bool hasGeofencing,
      bool hasFaceCheckIn,
      String? checkInQrCode,
      String? thumbnailUrl,
      List<String>? tags,
      DateTime createdAt,
      DateTime? updatedAt,
      bool isRegistered,
      bool isCheckedIn,
      bool isOrganizer,
      bool isTeamMember});
}

/// @nodoc
class __$$EventImplCopyWithImpl<$Res>
    extends _$EventCopyWithImpl<$Res, _$EventImpl>
    implements _$$EventImplCopyWith<$Res> {
  __$$EventImplCopyWithImpl(
      _$EventImpl _value, $Res Function(_$EventImpl) _then)
      : super(_value, _then);

  /// Create a copy of Event
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? title = null,
    Object? description = freezed,
    Object? type = null,
    Object? startAt = null,
    Object? endAt = null,
    Object? location = freezed,
    Object? latitude = freezed,
    Object? longitude = freezed,
    Object? maxAttendees = freezed,
    Object? currentAttendees = null,
    Object? status = null,
    Object? spaceId = freezed,
    Object? spaceName = freezed,
    Object? dressCodeId = freezed,
    Object? dressCodeName = freezed,
    Object? dressCodeDescription = freezed,
    Object? isPublic = null,
    Object? requiresRegistration = null,
    Object? hasCheckIn = null,
    Object? hasGeofencing = null,
    Object? hasFaceCheckIn = null,
    Object? checkInQrCode = freezed,
    Object? thumbnailUrl = freezed,
    Object? tags = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isRegistered = null,
    Object? isCheckedIn = null,
    Object? isOrganizer = null,
    Object? isTeamMember = null,
  }) {
    return _then(_$EventImpl(
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
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as EventType,
      startAt: null == startAt
          ? _value.startAt
          : startAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endAt: null == endAt
          ? _value.endAt
          : endAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      latitude: freezed == latitude
          ? _value.latitude
          : latitude // ignore: cast_nullable_to_non_nullable
              as double?,
      longitude: freezed == longitude
          ? _value.longitude
          : longitude // ignore: cast_nullable_to_non_nullable
              as double?,
      maxAttendees: freezed == maxAttendees
          ? _value.maxAttendees
          : maxAttendees // ignore: cast_nullable_to_non_nullable
              as int?,
      currentAttendees: null == currentAttendees
          ? _value.currentAttendees
          : currentAttendees // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as EventStatus,
      spaceId: freezed == spaceId
          ? _value.spaceId
          : spaceId // ignore: cast_nullable_to_non_nullable
              as String?,
      spaceName: freezed == spaceName
          ? _value.spaceName
          : spaceName // ignore: cast_nullable_to_non_nullable
              as String?,
      dressCodeId: freezed == dressCodeId
          ? _value.dressCodeId
          : dressCodeId // ignore: cast_nullable_to_non_nullable
              as String?,
      dressCodeName: freezed == dressCodeName
          ? _value.dressCodeName
          : dressCodeName // ignore: cast_nullable_to_non_nullable
              as String?,
      dressCodeDescription: freezed == dressCodeDescription
          ? _value.dressCodeDescription
          : dressCodeDescription // ignore: cast_nullable_to_non_nullable
              as String?,
      isPublic: null == isPublic
          ? _value.isPublic
          : isPublic // ignore: cast_nullable_to_non_nullable
              as bool,
      requiresRegistration: null == requiresRegistration
          ? _value.requiresRegistration
          : requiresRegistration // ignore: cast_nullable_to_non_nullable
              as bool,
      hasCheckIn: null == hasCheckIn
          ? _value.hasCheckIn
          : hasCheckIn // ignore: cast_nullable_to_non_nullable
              as bool,
      hasGeofencing: null == hasGeofencing
          ? _value.hasGeofencing
          : hasGeofencing // ignore: cast_nullable_to_non_nullable
              as bool,
      hasFaceCheckIn: null == hasFaceCheckIn
          ? _value.hasFaceCheckIn
          : hasFaceCheckIn // ignore: cast_nullable_to_non_nullable
              as bool,
      checkInQrCode: freezed == checkInQrCode
          ? _value.checkInQrCode
          : checkInQrCode // ignore: cast_nullable_to_non_nullable
              as String?,
      thumbnailUrl: freezed == thumbnailUrl
          ? _value.thumbnailUrl
          : thumbnailUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      tags: freezed == tags
          ? _value._tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isRegistered: null == isRegistered
          ? _value.isRegistered
          : isRegistered // ignore: cast_nullable_to_non_nullable
              as bool,
      isCheckedIn: null == isCheckedIn
          ? _value.isCheckedIn
          : isCheckedIn // ignore: cast_nullable_to_non_nullable
              as bool,
      isOrganizer: null == isOrganizer
          ? _value.isOrganizer
          : isOrganizer // ignore: cast_nullable_to_non_nullable
              as bool,
      isTeamMember: null == isTeamMember
          ? _value.isTeamMember
          : isTeamMember // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$EventImpl implements _Event {
  const _$EventImpl(
      {required this.id,
      required this.title,
      this.description,
      required this.type,
      required this.startAt,
      required this.endAt,
      this.location,
      this.latitude,
      this.longitude,
      this.maxAttendees,
      this.currentAttendees = 0,
      required this.status,
      this.spaceId,
      this.spaceName,
      this.dressCodeId,
      this.dressCodeName,
      this.dressCodeDescription,
      this.isPublic = false,
      this.requiresRegistration = false,
      this.hasCheckIn = false,
      this.hasGeofencing = false,
      this.hasFaceCheckIn = false,
      this.checkInQrCode,
      this.thumbnailUrl,
      final List<String>? tags,
      required this.createdAt,
      this.updatedAt,
      this.isRegistered = false,
      this.isCheckedIn = false,
      this.isOrganizer = false,
      this.isTeamMember = false})
      : _tags = tags;

  factory _$EventImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventImplFromJson(json);

  @override
  final int id;
  @override
  final String title;
  @override
  final String? description;
  @override
  final EventType type;
  @override
  final DateTime startAt;
  @override
  final DateTime endAt;
  @override
  final String? location;
  @override
  final double? latitude;
  @override
  final double? longitude;
  @override
  final int? maxAttendees;
  @override
  @JsonKey()
  final int currentAttendees;
  @override
  final EventStatus status;
  @override
  final String? spaceId;
  @override
  final String? spaceName;
  @override
  final String? dressCodeId;
  @override
  final String? dressCodeName;
  @override
  final String? dressCodeDescription;
  @override
  @JsonKey()
  final bool isPublic;
  @override
  @JsonKey()
  final bool requiresRegistration;
  @override
  @JsonKey()
  final bool hasCheckIn;
  @override
  @JsonKey()
  final bool hasGeofencing;
  @override
  @JsonKey()
  final bool hasFaceCheckIn;
  @override
  final String? checkInQrCode;
  @override
  final String? thumbnailUrl;
  final List<String>? _tags;
  @override
  List<String>? get tags {
    final value = _tags;
    if (value == null) return null;
    if (_tags is EqualUnmodifiableListView) return _tags;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;
  @override
  @JsonKey()
  final bool isRegistered;
  @override
  @JsonKey()
  final bool isCheckedIn;
  @override
  @JsonKey()
  final bool isOrganizer;
  @override
  @JsonKey()
  final bool isTeamMember;

  @override
  String toString() {
    return 'Event(id: $id, title: $title, description: $description, type: $type, startAt: $startAt, endAt: $endAt, location: $location, latitude: $latitude, longitude: $longitude, maxAttendees: $maxAttendees, currentAttendees: $currentAttendees, status: $status, spaceId: $spaceId, spaceName: $spaceName, dressCodeId: $dressCodeId, dressCodeName: $dressCodeName, dressCodeDescription: $dressCodeDescription, isPublic: $isPublic, requiresRegistration: $requiresRegistration, hasCheckIn: $hasCheckIn, hasGeofencing: $hasGeofencing, hasFaceCheckIn: $hasFaceCheckIn, checkInQrCode: $checkInQrCode, thumbnailUrl: $thumbnailUrl, tags: $tags, createdAt: $createdAt, updatedAt: $updatedAt, isRegistered: $isRegistered, isCheckedIn: $isCheckedIn, isOrganizer: $isOrganizer, isTeamMember: $isTeamMember)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.startAt, startAt) || other.startAt == startAt) &&
            (identical(other.endAt, endAt) || other.endAt == endAt) &&
            (identical(other.location, location) ||
                other.location == location) &&
            (identical(other.latitude, latitude) ||
                other.latitude == latitude) &&
            (identical(other.longitude, longitude) ||
                other.longitude == longitude) &&
            (identical(other.maxAttendees, maxAttendees) ||
                other.maxAttendees == maxAttendees) &&
            (identical(other.currentAttendees, currentAttendees) ||
                other.currentAttendees == currentAttendees) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.spaceId, spaceId) || other.spaceId == spaceId) &&
            (identical(other.spaceName, spaceName) ||
                other.spaceName == spaceName) &&
            (identical(other.dressCodeId, dressCodeId) ||
                other.dressCodeId == dressCodeId) &&
            (identical(other.dressCodeName, dressCodeName) ||
                other.dressCodeName == dressCodeName) &&
            (identical(other.dressCodeDescription, dressCodeDescription) ||
                other.dressCodeDescription == dressCodeDescription) &&
            (identical(other.isPublic, isPublic) ||
                other.isPublic == isPublic) &&
            (identical(other.requiresRegistration, requiresRegistration) ||
                other.requiresRegistration == requiresRegistration) &&
            (identical(other.hasCheckIn, hasCheckIn) ||
                other.hasCheckIn == hasCheckIn) &&
            (identical(other.hasGeofencing, hasGeofencing) ||
                other.hasGeofencing == hasGeofencing) &&
            (identical(other.hasFaceCheckIn, hasFaceCheckIn) ||
                other.hasFaceCheckIn == hasFaceCheckIn) &&
            (identical(other.checkInQrCode, checkInQrCode) ||
                other.checkInQrCode == checkInQrCode) &&
            (identical(other.thumbnailUrl, thumbnailUrl) ||
                other.thumbnailUrl == thumbnailUrl) &&
            const DeepCollectionEquality().equals(other._tags, _tags) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt) &&
            (identical(other.isRegistered, isRegistered) ||
                other.isRegistered == isRegistered) &&
            (identical(other.isCheckedIn, isCheckedIn) ||
                other.isCheckedIn == isCheckedIn) &&
            (identical(other.isOrganizer, isOrganizer) ||
                other.isOrganizer == isOrganizer) &&
            (identical(other.isTeamMember, isTeamMember) ||
                other.isTeamMember == isTeamMember));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hashAll([
        runtimeType,
        id,
        title,
        description,
        type,
        startAt,
        endAt,
        location,
        latitude,
        longitude,
        maxAttendees,
        currentAttendees,
        status,
        spaceId,
        spaceName,
        dressCodeId,
        dressCodeName,
        dressCodeDescription,
        isPublic,
        requiresRegistration,
        hasCheckIn,
        hasGeofencing,
        hasFaceCheckIn,
        checkInQrCode,
        thumbnailUrl,
        const DeepCollectionEquality().hash(_tags),
        createdAt,
        updatedAt,
        isRegistered,
        isCheckedIn,
        isOrganizer,
        isTeamMember
      ]);

  /// Create a copy of Event
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventImplCopyWith<_$EventImpl> get copyWith =>
      __$$EventImplCopyWithImpl<_$EventImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventImplToJson(
      this,
    );
  }
}

abstract class _Event implements Event {
  const factory _Event(
      {required final int id,
      required final String title,
      final String? description,
      required final EventType type,
      required final DateTime startAt,
      required final DateTime endAt,
      final String? location,
      final double? latitude,
      final double? longitude,
      final int? maxAttendees,
      final int currentAttendees,
      required final EventStatus status,
      final String? spaceId,
      final String? spaceName,
      final String? dressCodeId,
      final String? dressCodeName,
      final String? dressCodeDescription,
      final bool isPublic,
      final bool requiresRegistration,
      final bool hasCheckIn,
      final bool hasGeofencing,
      final bool hasFaceCheckIn,
      final String? checkInQrCode,
      final String? thumbnailUrl,
      final List<String>? tags,
      required final DateTime createdAt,
      final DateTime? updatedAt,
      final bool isRegistered,
      final bool isCheckedIn,
      final bool isOrganizer,
      final bool isTeamMember}) = _$EventImpl;

  factory _Event.fromJson(Map<String, dynamic> json) = _$EventImpl.fromJson;

  @override
  int get id;
  @override
  String get title;
  @override
  String? get description;
  @override
  EventType get type;
  @override
  DateTime get startAt;
  @override
  DateTime get endAt;
  @override
  String? get location;
  @override
  double? get latitude;
  @override
  double? get longitude;
  @override
  int? get maxAttendees;
  @override
  int get currentAttendees;
  @override
  EventStatus get status;
  @override
  String? get spaceId;
  @override
  String? get spaceName;
  @override
  String? get dressCodeId;
  @override
  String? get dressCodeName;
  @override
  String? get dressCodeDescription;
  @override
  bool get isPublic;
  @override
  bool get requiresRegistration;
  @override
  bool get hasCheckIn;
  @override
  bool get hasGeofencing;
  @override
  bool get hasFaceCheckIn;
  @override
  String? get checkInQrCode;
  @override
  String? get thumbnailUrl;
  @override
  List<String>? get tags;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;
  @override
  bool get isRegistered;
  @override
  bool get isCheckedIn;
  @override
  bool get isOrganizer;
  @override
  bool get isTeamMember;

  /// Create a copy of Event
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$EventImplCopyWith<_$EventImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

EventRegistration _$EventRegistrationFromJson(Map<String, dynamic> json) {
  return _EventRegistration.fromJson(json);
}

/// @nodoc
mixin _$EventRegistration {
  int get id => throw _privateConstructorUsedError;
  int get eventId => throw _privateConstructorUsedError;
  int get personId => throw _privateConstructorUsedError;
  RegistrationStatus get status => throw _privateConstructorUsedError;
  DateTime? get checkedInAt => throw _privateConstructorUsedError;
  String? get checkInMethod => throw _privateConstructorUsedError;
  bool get hasGuest => throw _privateConstructorUsedError;
  int? get guestCount => throw _privateConstructorUsedError;
  DateTime? get registeredAt => throw _privateConstructorUsedError;
  DateTime? get cancelledAt => throw _privateConstructorUsedError;

  /// Serializes this EventRegistration to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of EventRegistration
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $EventRegistrationCopyWith<EventRegistration> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventRegistrationCopyWith<$Res> {
  factory $EventRegistrationCopyWith(
          EventRegistration value, $Res Function(EventRegistration) then) =
      _$EventRegistrationCopyWithImpl<$Res, EventRegistration>;
  @useResult
  $Res call(
      {int id,
      int eventId,
      int personId,
      RegistrationStatus status,
      DateTime? checkedInAt,
      String? checkInMethod,
      bool hasGuest,
      int? guestCount,
      DateTime? registeredAt,
      DateTime? cancelledAt});
}

/// @nodoc
class _$EventRegistrationCopyWithImpl<$Res, $Val extends EventRegistration>
    implements $EventRegistrationCopyWith<$Res> {
  _$EventRegistrationCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of EventRegistration
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? eventId = null,
    Object? personId = null,
    Object? status = null,
    Object? checkedInAt = freezed,
    Object? checkInMethod = freezed,
    Object? hasGuest = null,
    Object? guestCount = freezed,
    Object? registeredAt = freezed,
    Object? cancelledAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      eventId: null == eventId
          ? _value.eventId
          : eventId // ignore: cast_nullable_to_non_nullable
              as int,
      personId: null == personId
          ? _value.personId
          : personId // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as RegistrationStatus,
      checkedInAt: freezed == checkedInAt
          ? _value.checkedInAt
          : checkedInAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      checkInMethod: freezed == checkInMethod
          ? _value.checkInMethod
          : checkInMethod // ignore: cast_nullable_to_non_nullable
              as String?,
      hasGuest: null == hasGuest
          ? _value.hasGuest
          : hasGuest // ignore: cast_nullable_to_non_nullable
              as bool,
      guestCount: freezed == guestCount
          ? _value.guestCount
          : guestCount // ignore: cast_nullable_to_non_nullable
              as int?,
      registeredAt: freezed == registeredAt
          ? _value.registeredAt
          : registeredAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      cancelledAt: freezed == cancelledAt
          ? _value.cancelledAt
          : cancelledAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$EventRegistrationImplCopyWith<$Res>
    implements $EventRegistrationCopyWith<$Res> {
  factory _$$EventRegistrationImplCopyWith(_$EventRegistrationImpl value,
          $Res Function(_$EventRegistrationImpl) then) =
      __$$EventRegistrationImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int eventId,
      int personId,
      RegistrationStatus status,
      DateTime? checkedInAt,
      String? checkInMethod,
      bool hasGuest,
      int? guestCount,
      DateTime? registeredAt,
      DateTime? cancelledAt});
}

/// @nodoc
class __$$EventRegistrationImplCopyWithImpl<$Res>
    extends _$EventRegistrationCopyWithImpl<$Res, _$EventRegistrationImpl>
    implements _$$EventRegistrationImplCopyWith<$Res> {
  __$$EventRegistrationImplCopyWithImpl(_$EventRegistrationImpl _value,
      $Res Function(_$EventRegistrationImpl) _then)
      : super(_value, _then);

  /// Create a copy of EventRegistration
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? eventId = null,
    Object? personId = null,
    Object? status = null,
    Object? checkedInAt = freezed,
    Object? checkInMethod = freezed,
    Object? hasGuest = null,
    Object? guestCount = freezed,
    Object? registeredAt = freezed,
    Object? cancelledAt = freezed,
  }) {
    return _then(_$EventRegistrationImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      eventId: null == eventId
          ? _value.eventId
          : eventId // ignore: cast_nullable_to_non_nullable
              as int,
      personId: null == personId
          ? _value.personId
          : personId // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as RegistrationStatus,
      checkedInAt: freezed == checkedInAt
          ? _value.checkedInAt
          : checkedInAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      checkInMethod: freezed == checkInMethod
          ? _value.checkInMethod
          : checkInMethod // ignore: cast_nullable_to_non_nullable
              as String?,
      hasGuest: null == hasGuest
          ? _value.hasGuest
          : hasGuest // ignore: cast_nullable_to_non_nullable
              as bool,
      guestCount: freezed == guestCount
          ? _value.guestCount
          : guestCount // ignore: cast_nullable_to_non_nullable
              as int?,
      registeredAt: freezed == registeredAt
          ? _value.registeredAt
          : registeredAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      cancelledAt: freezed == cancelledAt
          ? _value.cancelledAt
          : cancelledAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$EventRegistrationImpl implements _EventRegistration {
  const _$EventRegistrationImpl(
      {required this.id,
      required this.eventId,
      required this.personId,
      required this.status,
      this.checkedInAt,
      this.checkInMethod,
      this.hasGuest = false,
      this.guestCount,
      this.registeredAt,
      this.cancelledAt});

  factory _$EventRegistrationImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventRegistrationImplFromJson(json);

  @override
  final int id;
  @override
  final int eventId;
  @override
  final int personId;
  @override
  final RegistrationStatus status;
  @override
  final DateTime? checkedInAt;
  @override
  final String? checkInMethod;
  @override
  @JsonKey()
  final bool hasGuest;
  @override
  final int? guestCount;
  @override
  final DateTime? registeredAt;
  @override
  final DateTime? cancelledAt;

  @override
  String toString() {
    return 'EventRegistration(id: $id, eventId: $eventId, personId: $personId, status: $status, checkedInAt: $checkedInAt, checkInMethod: $checkInMethod, hasGuest: $hasGuest, guestCount: $guestCount, registeredAt: $registeredAt, cancelledAt: $cancelledAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventRegistrationImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.eventId, eventId) || other.eventId == eventId) &&
            (identical(other.personId, personId) ||
                other.personId == personId) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.checkedInAt, checkedInAt) ||
                other.checkedInAt == checkedInAt) &&
            (identical(other.checkInMethod, checkInMethod) ||
                other.checkInMethod == checkInMethod) &&
            (identical(other.hasGuest, hasGuest) ||
                other.hasGuest == hasGuest) &&
            (identical(other.guestCount, guestCount) ||
                other.guestCount == guestCount) &&
            (identical(other.registeredAt, registeredAt) ||
                other.registeredAt == registeredAt) &&
            (identical(other.cancelledAt, cancelledAt) ||
                other.cancelledAt == cancelledAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      eventId,
      personId,
      status,
      checkedInAt,
      checkInMethod,
      hasGuest,
      guestCount,
      registeredAt,
      cancelledAt);

  /// Create a copy of EventRegistration
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventRegistrationImplCopyWith<_$EventRegistrationImpl> get copyWith =>
      __$$EventRegistrationImplCopyWithImpl<_$EventRegistrationImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventRegistrationImplToJson(
      this,
    );
  }
}

abstract class _EventRegistration implements EventRegistration {
  const factory _EventRegistration(
      {required final int id,
      required final int eventId,
      required final int personId,
      required final RegistrationStatus status,
      final DateTime? checkedInAt,
      final String? checkInMethod,
      final bool hasGuest,
      final int? guestCount,
      final DateTime? registeredAt,
      final DateTime? cancelledAt}) = _$EventRegistrationImpl;

  factory _EventRegistration.fromJson(Map<String, dynamic> json) =
      _$EventRegistrationImpl.fromJson;

  @override
  int get id;
  @override
  int get eventId;
  @override
  int get personId;
  @override
  RegistrationStatus get status;
  @override
  DateTime? get checkedInAt;
  @override
  String? get checkInMethod;
  @override
  bool get hasGuest;
  @override
  int? get guestCount;
  @override
  DateTime? get registeredAt;
  @override
  DateTime? get cancelledAt;

  /// Create a copy of EventRegistration
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$EventRegistrationImplCopyWith<_$EventRegistrationImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

EventTeamMember _$EventTeamMemberFromJson(Map<String, dynamic> json) {
  return _EventTeamMember.fromJson(json);
}

/// @nodoc
mixin _$EventTeamMember {
  int get id => throw _privateConstructorUsedError;
  int get eventId => throw _privateConstructorUsedError;
  int get personId => throw _privateConstructorUsedError;
  String get role => throw _privateConstructorUsedError;
  TeamRole get teamRole => throw _privateConstructorUsedError;
  String? get responsibilities => throw _privateConstructorUsedError;
  DateTime? get assignedAt => throw _privateConstructorUsedError;

  /// Serializes this EventTeamMember to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of EventTeamMember
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $EventTeamMemberCopyWith<EventTeamMember> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventTeamMemberCopyWith<$Res> {
  factory $EventTeamMemberCopyWith(
          EventTeamMember value, $Res Function(EventTeamMember) then) =
      _$EventTeamMemberCopyWithImpl<$Res, EventTeamMember>;
  @useResult
  $Res call(
      {int id,
      int eventId,
      int personId,
      String role,
      TeamRole teamRole,
      String? responsibilities,
      DateTime? assignedAt});
}

/// @nodoc
class _$EventTeamMemberCopyWithImpl<$Res, $Val extends EventTeamMember>
    implements $EventTeamMemberCopyWith<$Res> {
  _$EventTeamMemberCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of EventTeamMember
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? eventId = null,
    Object? personId = null,
    Object? role = null,
    Object? teamRole = null,
    Object? responsibilities = freezed,
    Object? assignedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      eventId: null == eventId
          ? _value.eventId
          : eventId // ignore: cast_nullable_to_non_nullable
              as int,
      personId: null == personId
          ? _value.personId
          : personId // ignore: cast_nullable_to_non_nullable
              as int,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as String,
      teamRole: null == teamRole
          ? _value.teamRole
          : teamRole // ignore: cast_nullable_to_non_nullable
              as TeamRole,
      responsibilities: freezed == responsibilities
          ? _value.responsibilities
          : responsibilities // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedAt: freezed == assignedAt
          ? _value.assignedAt
          : assignedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$EventTeamMemberImplCopyWith<$Res>
    implements $EventTeamMemberCopyWith<$Res> {
  factory _$$EventTeamMemberImplCopyWith(_$EventTeamMemberImpl value,
          $Res Function(_$EventTeamMemberImpl) then) =
      __$$EventTeamMemberImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int eventId,
      int personId,
      String role,
      TeamRole teamRole,
      String? responsibilities,
      DateTime? assignedAt});
}

/// @nodoc
class __$$EventTeamMemberImplCopyWithImpl<$Res>
    extends _$EventTeamMemberCopyWithImpl<$Res, _$EventTeamMemberImpl>
    implements _$$EventTeamMemberImplCopyWith<$Res> {
  __$$EventTeamMemberImplCopyWithImpl(
      _$EventTeamMemberImpl _value, $Res Function(_$EventTeamMemberImpl) _then)
      : super(_value, _then);

  /// Create a copy of EventTeamMember
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? eventId = null,
    Object? personId = null,
    Object? role = null,
    Object? teamRole = null,
    Object? responsibilities = freezed,
    Object? assignedAt = freezed,
  }) {
    return _then(_$EventTeamMemberImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      eventId: null == eventId
          ? _value.eventId
          : eventId // ignore: cast_nullable_to_non_nullable
              as int,
      personId: null == personId
          ? _value.personId
          : personId // ignore: cast_nullable_to_non_nullable
              as int,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as String,
      teamRole: null == teamRole
          ? _value.teamRole
          : teamRole // ignore: cast_nullable_to_non_nullable
              as TeamRole,
      responsibilities: freezed == responsibilities
          ? _value.responsibilities
          : responsibilities // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedAt: freezed == assignedAt
          ? _value.assignedAt
          : assignedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$EventTeamMemberImpl implements _EventTeamMember {
  const _$EventTeamMemberImpl(
      {required this.id,
      required this.eventId,
      required this.personId,
      required this.role,
      required this.teamRole,
      this.responsibilities,
      this.assignedAt});

  factory _$EventTeamMemberImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventTeamMemberImplFromJson(json);

  @override
  final int id;
  @override
  final int eventId;
  @override
  final int personId;
  @override
  final String role;
  @override
  final TeamRole teamRole;
  @override
  final String? responsibilities;
  @override
  final DateTime? assignedAt;

  @override
  String toString() {
    return 'EventTeamMember(id: $id, eventId: $eventId, personId: $personId, role: $role, teamRole: $teamRole, responsibilities: $responsibilities, assignedAt: $assignedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventTeamMemberImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.eventId, eventId) || other.eventId == eventId) &&
            (identical(other.personId, personId) ||
                other.personId == personId) &&
            (identical(other.role, role) || other.role == role) &&
            (identical(other.teamRole, teamRole) ||
                other.teamRole == teamRole) &&
            (identical(other.responsibilities, responsibilities) ||
                other.responsibilities == responsibilities) &&
            (identical(other.assignedAt, assignedAt) ||
                other.assignedAt == assignedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, eventId, personId, role,
      teamRole, responsibilities, assignedAt);

  /// Create a copy of EventTeamMember
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventTeamMemberImplCopyWith<_$EventTeamMemberImpl> get copyWith =>
      __$$EventTeamMemberImplCopyWithImpl<_$EventTeamMemberImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventTeamMemberImplToJson(
      this,
    );
  }
}

abstract class _EventTeamMember implements EventTeamMember {
  const factory _EventTeamMember(
      {required final int id,
      required final int eventId,
      required final int personId,
      required final String role,
      required final TeamRole teamRole,
      final String? responsibilities,
      final DateTime? assignedAt}) = _$EventTeamMemberImpl;

  factory _EventTeamMember.fromJson(Map<String, dynamic> json) =
      _$EventTeamMemberImpl.fromJson;

  @override
  int get id;
  @override
  int get eventId;
  @override
  int get personId;
  @override
  String get role;
  @override
  TeamRole get teamRole;
  @override
  String? get responsibilities;
  @override
  DateTime? get assignedAt;

  /// Create a copy of EventTeamMember
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$EventTeamMemberImplCopyWith<_$EventTeamMemberImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

EventChecklist _$EventChecklistFromJson(Map<String, dynamic> json) {
  return _EventChecklist.fromJson(json);
}

/// @nodoc
mixin _$EventChecklist {
  int get id => throw _privateConstructorUsedError;
  int get eventId => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  bool get isRequired => throw _privateConstructorUsedError;
  int? get assignedToId => throw _privateConstructorUsedError;
  String? get assignedToName => throw _privateConstructorUsedError;
  bool get isCompleted => throw _privateConstructorUsedError;
  DateTime? get completedAt => throw _privateConstructorUsedError;
  int? get completedById => throw _privateConstructorUsedError;
  int? get order => throw _privateConstructorUsedError;

  /// Serializes this EventChecklist to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $EventChecklistCopyWith<EventChecklist> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventChecklistCopyWith<$Res> {
  factory $EventChecklistCopyWith(
          EventChecklist value, $Res Function(EventChecklist) then) =
      _$EventChecklistCopyWithImpl<$Res, EventChecklist>;
  @useResult
  $Res call(
      {int id,
      int eventId,
      String title,
      String? description,
      bool isRequired,
      int? assignedToId,
      String? assignedToName,
      bool isCompleted,
      DateTime? completedAt,
      int? completedById,
      int? order});
}

/// @nodoc
class _$EventChecklistCopyWithImpl<$Res, $Val extends EventChecklist>
    implements $EventChecklistCopyWith<$Res> {
  _$EventChecklistCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? eventId = null,
    Object? title = null,
    Object? description = freezed,
    Object? isRequired = null,
    Object? assignedToId = freezed,
    Object? assignedToName = freezed,
    Object? isCompleted = null,
    Object? completedAt = freezed,
    Object? completedById = freezed,
    Object? order = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      eventId: null == eventId
          ? _value.eventId
          : eventId // ignore: cast_nullable_to_non_nullable
              as int,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
      assignedToId: freezed == assignedToId
          ? _value.assignedToId
          : assignedToId // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedToName: freezed == assignedToName
          ? _value.assignedToName
          : assignedToName // ignore: cast_nullable_to_non_nullable
              as String?,
      isCompleted: null == isCompleted
          ? _value.isCompleted
          : isCompleted // ignore: cast_nullable_to_non_nullable
              as bool,
      completedAt: freezed == completedAt
          ? _value.completedAt
          : completedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      completedById: freezed == completedById
          ? _value.completedById
          : completedById // ignore: cast_nullable_to_non_nullable
              as int?,
      order: freezed == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$EventChecklistImplCopyWith<$Res>
    implements $EventChecklistCopyWith<$Res> {
  factory _$$EventChecklistImplCopyWith(_$EventChecklistImpl value,
          $Res Function(_$EventChecklistImpl) then) =
      __$$EventChecklistImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int eventId,
      String title,
      String? description,
      bool isRequired,
      int? assignedToId,
      String? assignedToName,
      bool isCompleted,
      DateTime? completedAt,
      int? completedById,
      int? order});
}

/// @nodoc
class __$$EventChecklistImplCopyWithImpl<$Res>
    extends _$EventChecklistCopyWithImpl<$Res, _$EventChecklistImpl>
    implements _$$EventChecklistImplCopyWith<$Res> {
  __$$EventChecklistImplCopyWithImpl(
      _$EventChecklistImpl _value, $Res Function(_$EventChecklistImpl) _then)
      : super(_value, _then);

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? eventId = null,
    Object? title = null,
    Object? description = freezed,
    Object? isRequired = null,
    Object? assignedToId = freezed,
    Object? assignedToName = freezed,
    Object? isCompleted = null,
    Object? completedAt = freezed,
    Object? completedById = freezed,
    Object? order = freezed,
  }) {
    return _then(_$EventChecklistImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      eventId: null == eventId
          ? _value.eventId
          : eventId // ignore: cast_nullable_to_non_nullable
              as int,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
      assignedToId: freezed == assignedToId
          ? _value.assignedToId
          : assignedToId // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedToName: freezed == assignedToName
          ? _value.assignedToName
          : assignedToName // ignore: cast_nullable_to_non_nullable
              as String?,
      isCompleted: null == isCompleted
          ? _value.isCompleted
          : isCompleted // ignore: cast_nullable_to_non_nullable
              as bool,
      completedAt: freezed == completedAt
          ? _value.completedAt
          : completedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      completedById: freezed == completedById
          ? _value.completedById
          : completedById // ignore: cast_nullable_to_non_nullable
              as int?,
      order: freezed == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$EventChecklistImpl implements _EventChecklist {
  const _$EventChecklistImpl(
      {required this.id,
      required this.eventId,
      required this.title,
      this.description,
      this.isRequired = false,
      this.assignedToId,
      this.assignedToName,
      this.isCompleted = false,
      this.completedAt,
      this.completedById,
      this.order});

  factory _$EventChecklistImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventChecklistImplFromJson(json);

  @override
  final int id;
  @override
  final int eventId;
  @override
  final String title;
  @override
  final String? description;
  @override
  @JsonKey()
  final bool isRequired;
  @override
  final int? assignedToId;
  @override
  final String? assignedToName;
  @override
  @JsonKey()
  final bool isCompleted;
  @override
  final DateTime? completedAt;
  @override
  final int? completedById;
  @override
  final int? order;

  @override
  String toString() {
    return 'EventChecklist(id: $id, eventId: $eventId, title: $title, description: $description, isRequired: $isRequired, assignedToId: $assignedToId, assignedToName: $assignedToName, isCompleted: $isCompleted, completedAt: $completedAt, completedById: $completedById, order: $order)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventChecklistImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.eventId, eventId) || other.eventId == eventId) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.isRequired, isRequired) ||
                other.isRequired == isRequired) &&
            (identical(other.assignedToId, assignedToId) ||
                other.assignedToId == assignedToId) &&
            (identical(other.assignedToName, assignedToName) ||
                other.assignedToName == assignedToName) &&
            (identical(other.isCompleted, isCompleted) ||
                other.isCompleted == isCompleted) &&
            (identical(other.completedAt, completedAt) ||
                other.completedAt == completedAt) &&
            (identical(other.completedById, completedById) ||
                other.completedById == completedById) &&
            (identical(other.order, order) || other.order == order));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      eventId,
      title,
      description,
      isRequired,
      assignedToId,
      assignedToName,
      isCompleted,
      completedAt,
      completedById,
      order);

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventChecklistImplCopyWith<_$EventChecklistImpl> get copyWith =>
      __$$EventChecklistImplCopyWithImpl<_$EventChecklistImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventChecklistImplToJson(
      this,
    );
  }
}

abstract class _EventChecklist implements EventChecklist {
  const factory _EventChecklist(
      {required final int id,
      required final int eventId,
      required final String title,
      final String? description,
      final bool isRequired,
      final int? assignedToId,
      final String? assignedToName,
      final bool isCompleted,
      final DateTime? completedAt,
      final int? completedById,
      final int? order}) = _$EventChecklistImpl;

  factory _EventChecklist.fromJson(Map<String, dynamic> json) =
      _$EventChecklistImpl.fromJson;

  @override
  int get id;
  @override
  int get eventId;
  @override
  String get title;
  @override
  String? get description;
  @override
  bool get isRequired;
  @override
  int? get assignedToId;
  @override
  String? get assignedToName;
  @override
  bool get isCompleted;
  @override
  DateTime? get completedAt;
  @override
  int? get completedById;
  @override
  int? get order;

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$EventChecklistImplCopyWith<_$EventChecklistImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DressCode _$DressCodeFromJson(Map<String, dynamic> json) {
  return _DressCode.fromJson(json);
}

/// @nodoc
mixin _$DressCode {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get colorCode => throw _privateConstructorUsedError;
  String? get iconUrl => throw _privateConstructorUsedError;
  List<DressCodeItem>? get items => throw _privateConstructorUsedError;
  bool get isRequired => throw _privateConstructorUsedError;

  /// Serializes this DressCode to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DressCodeCopyWith<DressCode> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DressCodeCopyWith<$Res> {
  factory $DressCodeCopyWith(DressCode value, $Res Function(DressCode) then) =
      _$DressCodeCopyWithImpl<$Res, DressCode>;
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      String? colorCode,
      String? iconUrl,
      List<DressCodeItem>? items,
      bool isRequired});
}

/// @nodoc
class _$DressCodeCopyWithImpl<$Res, $Val extends DressCode>
    implements $DressCodeCopyWith<$Res> {
  _$DressCodeCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? colorCode = freezed,
    Object? iconUrl = freezed,
    Object? items = freezed,
    Object? isRequired = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      colorCode: freezed == colorCode
          ? _value.colorCode
          : colorCode // ignore: cast_nullable_to_non_nullable
              as String?,
      iconUrl: freezed == iconUrl
          ? _value.iconUrl
          : iconUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      items: freezed == items
          ? _value.items
          : items // ignore: cast_nullable_to_non_nullable
              as List<DressCodeItem>?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DressCodeImplCopyWith<$Res>
    implements $DressCodeCopyWith<$Res> {
  factory _$$DressCodeImplCopyWith(
          _$DressCodeImpl value, $Res Function(_$DressCodeImpl) then) =
      __$$DressCodeImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      String? colorCode,
      String? iconUrl,
      List<DressCodeItem>? items,
      bool isRequired});
}

/// @nodoc
class __$$DressCodeImplCopyWithImpl<$Res>
    extends _$DressCodeCopyWithImpl<$Res, _$DressCodeImpl>
    implements _$$DressCodeImplCopyWith<$Res> {
  __$$DressCodeImplCopyWithImpl(
      _$DressCodeImpl _value, $Res Function(_$DressCodeImpl) _then)
      : super(_value, _then);

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? colorCode = freezed,
    Object? iconUrl = freezed,
    Object? items = freezed,
    Object? isRequired = null,
  }) {
    return _then(_$DressCodeImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      colorCode: freezed == colorCode
          ? _value.colorCode
          : colorCode // ignore: cast_nullable_to_non_nullable
              as String?,
      iconUrl: freezed == iconUrl
          ? _value.iconUrl
          : iconUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      items: freezed == items
          ? _value._items
          : items // ignore: cast_nullable_to_non_nullable
              as List<DressCodeItem>?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DressCodeImpl implements _DressCode {
  const _$DressCodeImpl(
      {required this.id,
      required this.name,
      this.description,
      this.colorCode,
      this.iconUrl,
      final List<DressCodeItem>? items,
      this.isRequired = false})
      : _items = items;

  factory _$DressCodeImpl.fromJson(Map<String, dynamic> json) =>
      _$$DressCodeImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String? description;
  @override
  final String? colorCode;
  @override
  final String? iconUrl;
  final List<DressCodeItem>? _items;
  @override
  List<DressCodeItem>? get items {
    final value = _items;
    if (value == null) return null;
    if (_items is EqualUnmodifiableListView) return _items;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  @JsonKey()
  final bool isRequired;

  @override
  String toString() {
    return 'DressCode(id: $id, name: $name, description: $description, colorCode: $colorCode, iconUrl: $iconUrl, items: $items, isRequired: $isRequired)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DressCodeImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.colorCode, colorCode) ||
                other.colorCode == colorCode) &&
            (identical(other.iconUrl, iconUrl) || other.iconUrl == iconUrl) &&
            const DeepCollectionEquality().equals(other._items, _items) &&
            (identical(other.isRequired, isRequired) ||
                other.isRequired == isRequired));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, description, colorCode,
      iconUrl, const DeepCollectionEquality().hash(_items), isRequired);

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DressCodeImplCopyWith<_$DressCodeImpl> get copyWith =>
      __$$DressCodeImplCopyWithImpl<_$DressCodeImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DressCodeImplToJson(
      this,
    );
  }
}

abstract class _DressCode implements DressCode {
  const factory _DressCode(
      {required final int id,
      required final String name,
      final String? description,
      final String? colorCode,
      final String? iconUrl,
      final List<DressCodeItem>? items,
      final bool isRequired}) = _$DressCodeImpl;

  factory _DressCode.fromJson(Map<String, dynamic> json) =
      _$DressCodeImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String? get description;
  @override
  String? get colorCode;
  @override
  String? get iconUrl;
  @override
  List<DressCodeItem>? get items;
  @override
  bool get isRequired;

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DressCodeImplCopyWith<_$DressCodeImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DressCodeItem _$DressCodeItemFromJson(Map<String, dynamic> json) {
  return _DressCodeItem.fromJson(json);
}

/// @nodoc
mixin _$DressCodeItem {
  int get id => throw _privateConstructorUsedError;
  int get dressCodeId => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get color => throw _privateConstructorUsedError;
  String? get icon => throw _privateConstructorUsedError;
  bool get isRequired => throw _privateConstructorUsedError;
  bool get isAlternative => throw _privateConstructorUsedError;

  /// Serializes this DressCodeItem to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DressCodeItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DressCodeItemCopyWith<DressCodeItem> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DressCodeItemCopyWith<$Res> {
  factory $DressCodeItemCopyWith(
          DressCodeItem value, $Res Function(DressCodeItem) then) =
      _$DressCodeItemCopyWithImpl<$Res, DressCodeItem>;
  @useResult
  $Res call(
      {int id,
      int dressCodeId,
      String name,
      String? description,
      String? color,
      String? icon,
      bool isRequired,
      bool isAlternative});
}

/// @nodoc
class _$DressCodeItemCopyWithImpl<$Res, $Val extends DressCodeItem>
    implements $DressCodeItemCopyWith<$Res> {
  _$DressCodeItemCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DressCodeItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? dressCodeId = null,
    Object? name = null,
    Object? description = freezed,
    Object? color = freezed,
    Object? icon = freezed,
    Object? isRequired = null,
    Object? isAlternative = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      dressCodeId: null == dressCodeId
          ? _value.dressCodeId
          : dressCodeId // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      color: freezed == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as String?,
      icon: freezed == icon
          ? _value.icon
          : icon // ignore: cast_nullable_to_non_nullable
              as String?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
      isAlternative: null == isAlternative
          ? _value.isAlternative
          : isAlternative // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DressCodeItemImplCopyWith<$Res>
    implements $DressCodeItemCopyWith<$Res> {
  factory _$$DressCodeItemImplCopyWith(
          _$DressCodeItemImpl value, $Res Function(_$DressCodeItemImpl) then) =
      __$$DressCodeItemImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int dressCodeId,
      String name,
      String? description,
      String? color,
      String? icon,
      bool isRequired,
      bool isAlternative});
}

/// @nodoc
class __$$DressCodeItemImplCopyWithImpl<$Res>
    extends _$DressCodeItemCopyWithImpl<$Res, _$DressCodeItemImpl>
    implements _$$DressCodeItemImplCopyWith<$Res> {
  __$$DressCodeItemImplCopyWithImpl(
      _$DressCodeItemImpl _value, $Res Function(_$DressCodeItemImpl) _then)
      : super(_value, _then);

  /// Create a copy of DressCodeItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? dressCodeId = null,
    Object? name = null,
    Object? description = freezed,
    Object? color = freezed,
    Object? icon = freezed,
    Object? isRequired = null,
    Object? isAlternative = null,
  }) {
    return _then(_$DressCodeItemImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      dressCodeId: null == dressCodeId
          ? _value.dressCodeId
          : dressCodeId // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      color: freezed == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as String?,
      icon: freezed == icon
          ? _value.icon
          : icon // ignore: cast_nullable_to_non_nullable
              as String?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
      isAlternative: null == isAlternative
          ? _value.isAlternative
          : isAlternative // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DressCodeItemImpl implements _DressCodeItem {
  const _$DressCodeItemImpl(
      {required this.id,
      required this.dressCodeId,
      required this.name,
      this.description,
      this.color,
      this.icon,
      this.isRequired = true,
      this.isAlternative = false});

  factory _$DressCodeItemImpl.fromJson(Map<String, dynamic> json) =>
      _$$DressCodeItemImplFromJson(json);

  @override
  final int id;
  @override
  final int dressCodeId;
  @override
  final String name;
  @override
  final String? description;
  @override
  final String? color;
  @override
  final String? icon;
  @override
  @JsonKey()
  final bool isRequired;
  @override
  @JsonKey()
  final bool isAlternative;

  @override
  String toString() {
    return 'DressCodeItem(id: $id, dressCodeId: $dressCodeId, name: $name, description: $description, color: $color, icon: $icon, isRequired: $isRequired, isAlternative: $isAlternative)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DressCodeItemImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.dressCodeId, dressCodeId) ||
                other.dressCodeId == dressCodeId) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.color, color) || other.color == color) &&
            (identical(other.icon, icon) || other.icon == icon) &&
            (identical(other.isRequired, isRequired) ||
                other.isRequired == isRequired) &&
            (identical(other.isAlternative, isAlternative) ||
                other.isAlternative == isAlternative));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, dressCodeId, name,
      description, color, icon, isRequired, isAlternative);

  /// Create a copy of DressCodeItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DressCodeItemImplCopyWith<_$DressCodeItemImpl> get copyWith =>
      __$$DressCodeItemImplCopyWithImpl<_$DressCodeItemImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DressCodeItemImplToJson(
      this,
    );
  }
}

abstract class _DressCodeItem implements DressCodeItem {
  const factory _DressCodeItem(
      {required final int id,
      required final int dressCodeId,
      required final String name,
      final String? description,
      final String? color,
      final String? icon,
      final bool isRequired,
      final bool isAlternative}) = _$DressCodeItemImpl;

  factory _DressCodeItem.fromJson(Map<String, dynamic> json) =
      _$DressCodeItemImpl.fromJson;

  @override
  int get id;
  @override
  int get dressCodeId;
  @override
  String get name;
  @override
  String? get description;
  @override
  String? get color;
  @override
  String? get icon;
  @override
  bool get isRequired;
  @override
  bool get isAlternative;

  /// Create a copy of DressCodeItem
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DressCodeItemImplCopyWith<_$DressCodeItemImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
