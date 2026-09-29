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
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

Event _$EventFromJson(Map<String, dynamic> json) {
  return _Event.fromJson(json);
}

/// @nodoc
mixin _$Event {
  String get id => throw _privateConstructorUsedError;
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
  String? get streamUrl => throw _privateConstructorUsedError;
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
  $Res call({
    String id,
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
    String? streamUrl,
    String? thumbnailUrl,
    List<String>? tags,
    DateTime createdAt,
    DateTime? updatedAt,
    bool isRegistered,
    bool isCheckedIn,
    bool isOrganizer,
    bool isTeamMember,
  });
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
    Object? streamUrl = freezed,
    Object? thumbnailUrl = freezed,
    Object? tags = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isRegistered = null,
    Object? isCheckedIn = null,
    Object? isOrganizer = null,
    Object? isTeamMember = null,
  }) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
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
            streamUrl: freezed == streamUrl
                ? _value.streamUrl
                : streamUrl // ignore: cast_nullable_to_non_nullable
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
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$EventImplCopyWith<$Res> implements $EventCopyWith<$Res> {
  factory _$$EventImplCopyWith(
    _$EventImpl value,
    $Res Function(_$EventImpl) then,
  ) = __$$EventImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String id,
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
    String? streamUrl,
    String? thumbnailUrl,
    List<String>? tags,
    DateTime createdAt,
    DateTime? updatedAt,
    bool isRegistered,
    bool isCheckedIn,
    bool isOrganizer,
    bool isTeamMember,
  });
}

/// @nodoc
class __$$EventImplCopyWithImpl<$Res>
    extends _$EventCopyWithImpl<$Res, _$EventImpl>
    implements _$$EventImplCopyWith<$Res> {
  __$$EventImplCopyWithImpl(
    _$EventImpl _value,
    $Res Function(_$EventImpl) _then,
  ) : super(_value, _then);

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
    Object? streamUrl = freezed,
    Object? thumbnailUrl = freezed,
    Object? tags = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isRegistered = null,
    Object? isCheckedIn = null,
    Object? isOrganizer = null,
    Object? isTeamMember = null,
  }) {
    return _then(
      _$EventImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
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
        streamUrl: freezed == streamUrl
            ? _value.streamUrl
            : streamUrl // ignore: cast_nullable_to_non_nullable
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
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$EventImpl implements _Event {
  const _$EventImpl({
    required this.id,
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
    this.streamUrl,
    this.thumbnailUrl,
    final List<String>? tags,
    required this.createdAt,
    this.updatedAt,
    this.isRegistered = false,
    this.isCheckedIn = false,
    this.isOrganizer = false,
    this.isTeamMember = false,
  }) : _tags = tags;

  factory _$EventImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventImplFromJson(json);

  @override
  final String id;
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
  final String? streamUrl;
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
    return 'Event(id: $id, title: $title, description: $description, type: $type, startAt: $startAt, endAt: $endAt, location: $location, latitude: $latitude, longitude: $longitude, maxAttendees: $maxAttendees, currentAttendees: $currentAttendees, status: $status, spaceId: $spaceId, spaceName: $spaceName, dressCodeId: $dressCodeId, dressCodeName: $dressCodeName, dressCodeDescription: $dressCodeDescription, isPublic: $isPublic, requiresRegistration: $requiresRegistration, hasCheckIn: $hasCheckIn, hasGeofencing: $hasGeofencing, hasFaceCheckIn: $hasFaceCheckIn, checkInQrCode: $checkInQrCode, streamUrl: $streamUrl, thumbnailUrl: $thumbnailUrl, tags: $tags, createdAt: $createdAt, updatedAt: $updatedAt, isRegistered: $isRegistered, isCheckedIn: $isCheckedIn, isOrganizer: $isOrganizer, isTeamMember: $isTeamMember)';
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
            (identical(other.streamUrl, streamUrl) ||
                other.streamUrl == streamUrl) &&
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
    streamUrl,
    thumbnailUrl,
    const DeepCollectionEquality().hash(_tags),
    createdAt,
    updatedAt,
    isRegistered,
    isCheckedIn,
    isOrganizer,
    isTeamMember,
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
    return _$$EventImplToJson(this);
  }
}

abstract class _Event implements Event {
  const factory _Event({
    required final String id,
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
    final String? streamUrl,
    final String? thumbnailUrl,
    final List<String>? tags,
    required final DateTime createdAt,
    final DateTime? updatedAt,
    final bool isRegistered,
    final bool isCheckedIn,
    final bool isOrganizer,
    final bool isTeamMember,
  }) = _$EventImpl;

  factory _Event.fromJson(Map<String, dynamic> json) = _$EventImpl.fromJson;

  @override
  String get id;
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
  String? get streamUrl;
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
  String get id => throw _privateConstructorUsedError;
  String get eventId => throw _privateConstructorUsedError;
  String get personId => throw _privateConstructorUsedError;
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
    EventRegistration value,
    $Res Function(EventRegistration) then,
  ) = _$EventRegistrationCopyWithImpl<$Res, EventRegistration>;
  @useResult
  $Res call({
    String id,
    String eventId,
    String personId,
    RegistrationStatus status,
    DateTime? checkedInAt,
    String? checkInMethod,
    bool hasGuest,
    int? guestCount,
    DateTime? registeredAt,
    DateTime? cancelledAt,
  });
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
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            eventId: null == eventId
                ? _value.eventId
                : eventId // ignore: cast_nullable_to_non_nullable
                      as String,
            personId: null == personId
                ? _value.personId
                : personId // ignore: cast_nullable_to_non_nullable
                      as String,
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
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$EventRegistrationImplCopyWith<$Res>
    implements $EventRegistrationCopyWith<$Res> {
  factory _$$EventRegistrationImplCopyWith(
    _$EventRegistrationImpl value,
    $Res Function(_$EventRegistrationImpl) then,
  ) = __$$EventRegistrationImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String id,
    String eventId,
    String personId,
    RegistrationStatus status,
    DateTime? checkedInAt,
    String? checkInMethod,
    bool hasGuest,
    int? guestCount,
    DateTime? registeredAt,
    DateTime? cancelledAt,
  });
}

/// @nodoc
class __$$EventRegistrationImplCopyWithImpl<$Res>
    extends _$EventRegistrationCopyWithImpl<$Res, _$EventRegistrationImpl>
    implements _$$EventRegistrationImplCopyWith<$Res> {
  __$$EventRegistrationImplCopyWithImpl(
    _$EventRegistrationImpl _value,
    $Res Function(_$EventRegistrationImpl) _then,
  ) : super(_value, _then);

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
    return _then(
      _$EventRegistrationImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        eventId: null == eventId
            ? _value.eventId
            : eventId // ignore: cast_nullable_to_non_nullable
                  as String,
        personId: null == personId
            ? _value.personId
            : personId // ignore: cast_nullable_to_non_nullable
                  as String,
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
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$EventRegistrationImpl implements _EventRegistration {
  const _$EventRegistrationImpl({
    required this.id,
    required this.eventId,
    required this.personId,
    required this.status,
    this.checkedInAt,
    this.checkInMethod,
    this.hasGuest = false,
    this.guestCount,
    this.registeredAt,
    this.cancelledAt,
  });

  factory _$EventRegistrationImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventRegistrationImplFromJson(json);

  @override
  final String id;
  @override
  final String eventId;
  @override
  final String personId;
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
    cancelledAt,
  );

  /// Create a copy of EventRegistration
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventRegistrationImplCopyWith<_$EventRegistrationImpl> get copyWith =>
      __$$EventRegistrationImplCopyWithImpl<_$EventRegistrationImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$EventRegistrationImplToJson(this);
  }
}

abstract class _EventRegistration implements EventRegistration {
  const factory _EventRegistration({
    required final String id,
    required final String eventId,
    required final String personId,
    required final RegistrationStatus status,
    final DateTime? checkedInAt,
    final String? checkInMethod,
    final bool hasGuest,
    final int? guestCount,
    final DateTime? registeredAt,
    final DateTime? cancelledAt,
  }) = _$EventRegistrationImpl;

  factory _EventRegistration.fromJson(Map<String, dynamic> json) =
      _$EventRegistrationImpl.fromJson;

  @override
  String get id;
  @override
  String get eventId;
  @override
  String get personId;
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

EventTeam _$EventTeamFromJson(Map<String, dynamic> json) {
  return _EventTeam.fromJson(json);
}

/// @nodoc
mixin _$EventTeam {
  String get id => throw _privateConstructorUsedError;
  String? get tenantId => throw _privateConstructorUsedError;
  String? get churchEventId => throw _privateConstructorUsedError;
  String? get spaceId => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get leadPersonId => throw _privateConstructorUsedError;
  String? get color => throw _privateConstructorUsedError;
  DateTime? get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this EventTeam to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of EventTeam
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $EventTeamCopyWith<EventTeam> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventTeamCopyWith<$Res> {
  factory $EventTeamCopyWith(EventTeam value, $Res Function(EventTeam) then) =
      _$EventTeamCopyWithImpl<$Res, EventTeam>;
  @useResult
  $Res call({
    String id,
    String? tenantId,
    String? churchEventId,
    String? spaceId,
    String name,
    String? description,
    String? leadPersonId,
    String? color,
    DateTime? createdAt,
    DateTime? updatedAt,
  });
}

/// @nodoc
class _$EventTeamCopyWithImpl<$Res, $Val extends EventTeam>
    implements $EventTeamCopyWith<$Res> {
  _$EventTeamCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of EventTeam
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tenantId = freezed,
    Object? churchEventId = freezed,
    Object? spaceId = freezed,
    Object? name = null,
    Object? description = freezed,
    Object? leadPersonId = freezed,
    Object? color = freezed,
    Object? createdAt = freezed,
    Object? updatedAt = freezed,
  }) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            tenantId: freezed == tenantId
                ? _value.tenantId
                : tenantId // ignore: cast_nullable_to_non_nullable
                      as String?,
            churchEventId: freezed == churchEventId
                ? _value.churchEventId
                : churchEventId // ignore: cast_nullable_to_non_nullable
                      as String?,
            spaceId: freezed == spaceId
                ? _value.spaceId
                : spaceId // ignore: cast_nullable_to_non_nullable
                      as String?,
            name: null == name
                ? _value.name
                : name // ignore: cast_nullable_to_non_nullable
                      as String,
            description: freezed == description
                ? _value.description
                : description // ignore: cast_nullable_to_non_nullable
                      as String?,
            leadPersonId: freezed == leadPersonId
                ? _value.leadPersonId
                : leadPersonId // ignore: cast_nullable_to_non_nullable
                      as String?,
            color: freezed == color
                ? _value.color
                : color // ignore: cast_nullable_to_non_nullable
                      as String?,
            createdAt: freezed == createdAt
                ? _value.createdAt
                : createdAt // ignore: cast_nullable_to_non_nullable
                      as DateTime?,
            updatedAt: freezed == updatedAt
                ? _value.updatedAt
                : updatedAt // ignore: cast_nullable_to_non_nullable
                      as DateTime?,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$EventTeamImplCopyWith<$Res>
    implements $EventTeamCopyWith<$Res> {
  factory _$$EventTeamImplCopyWith(
    _$EventTeamImpl value,
    $Res Function(_$EventTeamImpl) then,
  ) = __$$EventTeamImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String id,
    String? tenantId,
    String? churchEventId,
    String? spaceId,
    String name,
    String? description,
    String? leadPersonId,
    String? color,
    DateTime? createdAt,
    DateTime? updatedAt,
  });
}

/// @nodoc
class __$$EventTeamImplCopyWithImpl<$Res>
    extends _$EventTeamCopyWithImpl<$Res, _$EventTeamImpl>
    implements _$$EventTeamImplCopyWith<$Res> {
  __$$EventTeamImplCopyWithImpl(
    _$EventTeamImpl _value,
    $Res Function(_$EventTeamImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of EventTeam
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tenantId = freezed,
    Object? churchEventId = freezed,
    Object? spaceId = freezed,
    Object? name = null,
    Object? description = freezed,
    Object? leadPersonId = freezed,
    Object? color = freezed,
    Object? createdAt = freezed,
    Object? updatedAt = freezed,
  }) {
    return _then(
      _$EventTeamImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        tenantId: freezed == tenantId
            ? _value.tenantId
            : tenantId // ignore: cast_nullable_to_non_nullable
                  as String?,
        churchEventId: freezed == churchEventId
            ? _value.churchEventId
            : churchEventId // ignore: cast_nullable_to_non_nullable
                  as String?,
        spaceId: freezed == spaceId
            ? _value.spaceId
            : spaceId // ignore: cast_nullable_to_non_nullable
                  as String?,
        name: null == name
            ? _value.name
            : name // ignore: cast_nullable_to_non_nullable
                  as String,
        description: freezed == description
            ? _value.description
            : description // ignore: cast_nullable_to_non_nullable
                  as String?,
        leadPersonId: freezed == leadPersonId
            ? _value.leadPersonId
            : leadPersonId // ignore: cast_nullable_to_non_nullable
                  as String?,
        color: freezed == color
            ? _value.color
            : color // ignore: cast_nullable_to_non_nullable
                  as String?,
        createdAt: freezed == createdAt
            ? _value.createdAt
            : createdAt // ignore: cast_nullable_to_non_nullable
                  as DateTime?,
        updatedAt: freezed == updatedAt
            ? _value.updatedAt
            : updatedAt // ignore: cast_nullable_to_non_nullable
                  as DateTime?,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$EventTeamImpl implements _EventTeam {
  const _$EventTeamImpl({
    required this.id,
    this.tenantId,
    this.churchEventId,
    this.spaceId,
    required this.name,
    this.description,
    this.leadPersonId,
    this.color,
    this.createdAt,
    this.updatedAt,
  });

  factory _$EventTeamImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventTeamImplFromJson(json);

  @override
  final String id;
  @override
  final String? tenantId;
  @override
  final String? churchEventId;
  @override
  final String? spaceId;
  @override
  final String name;
  @override
  final String? description;
  @override
  final String? leadPersonId;
  @override
  final String? color;
  @override
  final DateTime? createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'EventTeam(id: $id, tenantId: $tenantId, churchEventId: $churchEventId, spaceId: $spaceId, name: $name, description: $description, leadPersonId: $leadPersonId, color: $color, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventTeamImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.tenantId, tenantId) ||
                other.tenantId == tenantId) &&
            (identical(other.churchEventId, churchEventId) ||
                other.churchEventId == churchEventId) &&
            (identical(other.spaceId, spaceId) || other.spaceId == spaceId) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.leadPersonId, leadPersonId) ||
                other.leadPersonId == leadPersonId) &&
            (identical(other.color, color) || other.color == color) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    id,
    tenantId,
    churchEventId,
    spaceId,
    name,
    description,
    leadPersonId,
    color,
    createdAt,
    updatedAt,
  );

  /// Create a copy of EventTeam
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventTeamImplCopyWith<_$EventTeamImpl> get copyWith =>
      __$$EventTeamImplCopyWithImpl<_$EventTeamImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventTeamImplToJson(this);
  }
}

abstract class _EventTeam implements EventTeam {
  const factory _EventTeam({
    required final String id,
    final String? tenantId,
    final String? churchEventId,
    final String? spaceId,
    required final String name,
    final String? description,
    final String? leadPersonId,
    final String? color,
    final DateTime? createdAt,
    final DateTime? updatedAt,
  }) = _$EventTeamImpl;

  factory _EventTeam.fromJson(Map<String, dynamic> json) =
      _$EventTeamImpl.fromJson;

  @override
  String get id;
  @override
  String? get tenantId;
  @override
  String? get churchEventId;
  @override
  String? get spaceId;
  @override
  String get name;
  @override
  String? get description;
  @override
  String? get leadPersonId;
  @override
  String? get color;
  @override
  DateTime? get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of EventTeam
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$EventTeamImplCopyWith<_$EventTeamImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

EventChecklist _$EventChecklistFromJson(Map<String, dynamic> json) {
  return _EventChecklist.fromJson(json);
}

/// @nodoc
mixin _$EventChecklist {
  String get id => throw _privateConstructorUsedError;
  String? get tenantId => throw _privateConstructorUsedError;
  String get eventId => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  ChecklistStatus get status => throw _privateConstructorUsedError;
  String? get assignedTo => throw _privateConstructorUsedError;
  int get orderIndex => throw _privateConstructorUsedError;
  DateTime? get createdAt => throw _privateConstructorUsedError;

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
    EventChecklist value,
    $Res Function(EventChecklist) then,
  ) = _$EventChecklistCopyWithImpl<$Res, EventChecklist>;
  @useResult
  $Res call({
    String id,
    String? tenantId,
    String eventId,
    String title,
    String? description,
    ChecklistStatus status,
    String? assignedTo,
    int orderIndex,
    DateTime? createdAt,
  });
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
    Object? tenantId = freezed,
    Object? eventId = null,
    Object? title = null,
    Object? description = freezed,
    Object? status = null,
    Object? assignedTo = freezed,
    Object? orderIndex = null,
    Object? createdAt = freezed,
  }) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            tenantId: freezed == tenantId
                ? _value.tenantId
                : tenantId // ignore: cast_nullable_to_non_nullable
                      as String?,
            eventId: null == eventId
                ? _value.eventId
                : eventId // ignore: cast_nullable_to_non_nullable
                      as String,
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
                      as ChecklistStatus,
            assignedTo: freezed == assignedTo
                ? _value.assignedTo
                : assignedTo // ignore: cast_nullable_to_non_nullable
                      as String?,
            orderIndex: null == orderIndex
                ? _value.orderIndex
                : orderIndex // ignore: cast_nullable_to_non_nullable
                      as int,
            createdAt: freezed == createdAt
                ? _value.createdAt
                : createdAt // ignore: cast_nullable_to_non_nullable
                      as DateTime?,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$EventChecklistImplCopyWith<$Res>
    implements $EventChecklistCopyWith<$Res> {
  factory _$$EventChecklistImplCopyWith(
    _$EventChecklistImpl value,
    $Res Function(_$EventChecklistImpl) then,
  ) = __$$EventChecklistImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String id,
    String? tenantId,
    String eventId,
    String title,
    String? description,
    ChecklistStatus status,
    String? assignedTo,
    int orderIndex,
    DateTime? createdAt,
  });
}

/// @nodoc
class __$$EventChecklistImplCopyWithImpl<$Res>
    extends _$EventChecklistCopyWithImpl<$Res, _$EventChecklistImpl>
    implements _$$EventChecklistImplCopyWith<$Res> {
  __$$EventChecklistImplCopyWithImpl(
    _$EventChecklistImpl _value,
    $Res Function(_$EventChecklistImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tenantId = freezed,
    Object? eventId = null,
    Object? title = null,
    Object? description = freezed,
    Object? status = null,
    Object? assignedTo = freezed,
    Object? orderIndex = null,
    Object? createdAt = freezed,
  }) {
    return _then(
      _$EventChecklistImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        tenantId: freezed == tenantId
            ? _value.tenantId
            : tenantId // ignore: cast_nullable_to_non_nullable
                  as String?,
        eventId: null == eventId
            ? _value.eventId
            : eventId // ignore: cast_nullable_to_non_nullable
                  as String,
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
                  as ChecklistStatus,
        assignedTo: freezed == assignedTo
            ? _value.assignedTo
            : assignedTo // ignore: cast_nullable_to_non_nullable
                  as String?,
        orderIndex: null == orderIndex
            ? _value.orderIndex
            : orderIndex // ignore: cast_nullable_to_non_nullable
                  as int,
        createdAt: freezed == createdAt
            ? _value.createdAt
            : createdAt // ignore: cast_nullable_to_non_nullable
                  as DateTime?,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$EventChecklistImpl implements _EventChecklist {
  const _$EventChecklistImpl({
    required this.id,
    this.tenantId,
    required this.eventId,
    required this.title,
    this.description,
    this.status = ChecklistStatus.pending,
    this.assignedTo,
    this.orderIndex = 0,
    this.createdAt,
  });

  factory _$EventChecklistImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventChecklistImplFromJson(json);

  @override
  final String id;
  @override
  final String? tenantId;
  @override
  final String eventId;
  @override
  final String title;
  @override
  final String? description;
  @override
  @JsonKey()
  final ChecklistStatus status;
  @override
  final String? assignedTo;
  @override
  @JsonKey()
  final int orderIndex;
  @override
  final DateTime? createdAt;

  @override
  String toString() {
    return 'EventChecklist(id: $id, tenantId: $tenantId, eventId: $eventId, title: $title, description: $description, status: $status, assignedTo: $assignedTo, orderIndex: $orderIndex, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventChecklistImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.tenantId, tenantId) ||
                other.tenantId == tenantId) &&
            (identical(other.eventId, eventId) || other.eventId == eventId) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.assignedTo, assignedTo) ||
                other.assignedTo == assignedTo) &&
            (identical(other.orderIndex, orderIndex) ||
                other.orderIndex == orderIndex) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    id,
    tenantId,
    eventId,
    title,
    description,
    status,
    assignedTo,
    orderIndex,
    createdAt,
  );

  /// Create a copy of EventChecklist
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$EventChecklistImplCopyWith<_$EventChecklistImpl> get copyWith =>
      __$$EventChecklistImplCopyWithImpl<_$EventChecklistImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$EventChecklistImplToJson(this);
  }
}

abstract class _EventChecklist implements EventChecklist {
  const factory _EventChecklist({
    required final String id,
    final String? tenantId,
    required final String eventId,
    required final String title,
    final String? description,
    final ChecklistStatus status,
    final String? assignedTo,
    final int orderIndex,
    final DateTime? createdAt,
  }) = _$EventChecklistImpl;

  factory _EventChecklist.fromJson(Map<String, dynamic> json) =
      _$EventChecklistImpl.fromJson;

  @override
  String get id;
  @override
  String? get tenantId;
  @override
  String get eventId;
  @override
  String get title;
  @override
  String? get description;
  @override
  ChecklistStatus get status;
  @override
  String? get assignedTo;
  @override
  int get orderIndex;
  @override
  DateTime? get createdAt;

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
  String get id => throw _privateConstructorUsedError;
  String? get spaceId => throw _privateConstructorUsedError;
  String? get eventId => throw _privateConstructorUsedError;
  String? get serviceName => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  DateTime? get beginsAt => throw _privateConstructorUsedError;
  DateTime? get endsAt => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  bool get archived => throw _privateConstructorUsedError;
  List<DressCodeRule> get rules => throw _privateConstructorUsedError;

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
  $Res call({
    String id,
    String? spaceId,
    String? eventId,
    String? serviceName,
    String title,
    DateTime? beginsAt,
    DateTime? endsAt,
    String status,
    bool archived,
    List<DressCodeRule> rules,
  });
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
    Object? spaceId = freezed,
    Object? eventId = freezed,
    Object? serviceName = freezed,
    Object? title = null,
    Object? beginsAt = freezed,
    Object? endsAt = freezed,
    Object? status = null,
    Object? archived = null,
    Object? rules = null,
  }) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            spaceId: freezed == spaceId
                ? _value.spaceId
                : spaceId // ignore: cast_nullable_to_non_nullable
                      as String?,
            eventId: freezed == eventId
                ? _value.eventId
                : eventId // ignore: cast_nullable_to_non_nullable
                      as String?,
            serviceName: freezed == serviceName
                ? _value.serviceName
                : serviceName // ignore: cast_nullable_to_non_nullable
                      as String?,
            title: null == title
                ? _value.title
                : title // ignore: cast_nullable_to_non_nullable
                      as String,
            beginsAt: freezed == beginsAt
                ? _value.beginsAt
                : beginsAt // ignore: cast_nullable_to_non_nullable
                      as DateTime?,
            endsAt: freezed == endsAt
                ? _value.endsAt
                : endsAt // ignore: cast_nullable_to_non_nullable
                      as DateTime?,
            status: null == status
                ? _value.status
                : status // ignore: cast_nullable_to_non_nullable
                      as String,
            archived: null == archived
                ? _value.archived
                : archived // ignore: cast_nullable_to_non_nullable
                      as bool,
            rules: null == rules
                ? _value.rules
                : rules // ignore: cast_nullable_to_non_nullable
                      as List<DressCodeRule>,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$DressCodeImplCopyWith<$Res>
    implements $DressCodeCopyWith<$Res> {
  factory _$$DressCodeImplCopyWith(
    _$DressCodeImpl value,
    $Res Function(_$DressCodeImpl) then,
  ) = __$$DressCodeImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String id,
    String? spaceId,
    String? eventId,
    String? serviceName,
    String title,
    DateTime? beginsAt,
    DateTime? endsAt,
    String status,
    bool archived,
    List<DressCodeRule> rules,
  });
}

/// @nodoc
class __$$DressCodeImplCopyWithImpl<$Res>
    extends _$DressCodeCopyWithImpl<$Res, _$DressCodeImpl>
    implements _$$DressCodeImplCopyWith<$Res> {
  __$$DressCodeImplCopyWithImpl(
    _$DressCodeImpl _value,
    $Res Function(_$DressCodeImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? spaceId = freezed,
    Object? eventId = freezed,
    Object? serviceName = freezed,
    Object? title = null,
    Object? beginsAt = freezed,
    Object? endsAt = freezed,
    Object? status = null,
    Object? archived = null,
    Object? rules = null,
  }) {
    return _then(
      _$DressCodeImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        spaceId: freezed == spaceId
            ? _value.spaceId
            : spaceId // ignore: cast_nullable_to_non_nullable
                  as String?,
        eventId: freezed == eventId
            ? _value.eventId
            : eventId // ignore: cast_nullable_to_non_nullable
                  as String?,
        serviceName: freezed == serviceName
            ? _value.serviceName
            : serviceName // ignore: cast_nullable_to_non_nullable
                  as String?,
        title: null == title
            ? _value.title
            : title // ignore: cast_nullable_to_non_nullable
                  as String,
        beginsAt: freezed == beginsAt
            ? _value.beginsAt
            : beginsAt // ignore: cast_nullable_to_non_nullable
                  as DateTime?,
        endsAt: freezed == endsAt
            ? _value.endsAt
            : endsAt // ignore: cast_nullable_to_non_nullable
                  as DateTime?,
        status: null == status
            ? _value.status
            : status // ignore: cast_nullable_to_non_nullable
                  as String,
        archived: null == archived
            ? _value.archived
            : archived // ignore: cast_nullable_to_non_nullable
                  as bool,
        rules: null == rules
            ? _value._rules
            : rules // ignore: cast_nullable_to_non_nullable
                  as List<DressCodeRule>,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$DressCodeImpl implements _DressCode {
  const _$DressCodeImpl({
    required this.id,
    this.spaceId,
    this.eventId,
    this.serviceName,
    required this.title,
    this.beginsAt,
    this.endsAt,
    this.status = 'ACTIVE',
    this.archived = false,
    final List<DressCodeRule> rules = const <DressCodeRule>[],
  }) : _rules = rules;

  factory _$DressCodeImpl.fromJson(Map<String, dynamic> json) =>
      _$$DressCodeImplFromJson(json);

  @override
  final String id;
  @override
  final String? spaceId;
  @override
  final String? eventId;
  @override
  final String? serviceName;
  @override
  final String title;
  @override
  final DateTime? beginsAt;
  @override
  final DateTime? endsAt;
  @override
  @JsonKey()
  final String status;
  @override
  @JsonKey()
  final bool archived;
  final List<DressCodeRule> _rules;
  @override
  @JsonKey()
  List<DressCodeRule> get rules {
    if (_rules is EqualUnmodifiableListView) return _rules;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_rules);
  }

  @override
  String toString() {
    return 'DressCode(id: $id, spaceId: $spaceId, eventId: $eventId, serviceName: $serviceName, title: $title, beginsAt: $beginsAt, endsAt: $endsAt, status: $status, archived: $archived, rules: $rules)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DressCodeImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.spaceId, spaceId) || other.spaceId == spaceId) &&
            (identical(other.eventId, eventId) || other.eventId == eventId) &&
            (identical(other.serviceName, serviceName) ||
                other.serviceName == serviceName) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.beginsAt, beginsAt) ||
                other.beginsAt == beginsAt) &&
            (identical(other.endsAt, endsAt) || other.endsAt == endsAt) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.archived, archived) ||
                other.archived == archived) &&
            const DeepCollectionEquality().equals(other._rules, _rules));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    id,
    spaceId,
    eventId,
    serviceName,
    title,
    beginsAt,
    endsAt,
    status,
    archived,
    const DeepCollectionEquality().hash(_rules),
  );

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DressCodeImplCopyWith<_$DressCodeImpl> get copyWith =>
      __$$DressCodeImplCopyWithImpl<_$DressCodeImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DressCodeImplToJson(this);
  }
}

abstract class _DressCode implements DressCode {
  const factory _DressCode({
    required final String id,
    final String? spaceId,
    final String? eventId,
    final String? serviceName,
    required final String title,
    final DateTime? beginsAt,
    final DateTime? endsAt,
    final String status,
    final bool archived,
    final List<DressCodeRule> rules,
  }) = _$DressCodeImpl;

  factory _DressCode.fromJson(Map<String, dynamic> json) =
      _$DressCodeImpl.fromJson;

  @override
  String get id;
  @override
  String? get spaceId;
  @override
  String? get eventId;
  @override
  String? get serviceName;
  @override
  String get title;
  @override
  DateTime? get beginsAt;
  @override
  DateTime? get endsAt;
  @override
  String get status;
  @override
  bool get archived;
  @override
  List<DressCodeRule> get rules;

  /// Create a copy of DressCode
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DressCodeImplCopyWith<_$DressCodeImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DressCodeRule _$DressCodeRuleFromJson(Map<String, dynamic> json) {
  return _DressCodeRule.fromJson(json);
}

/// @nodoc
mixin _$DressCodeRule {
  String get groupName => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get imageUrl => throw _privateConstructorUsedError;

  /// Serializes this DressCodeRule to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DressCodeRule
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DressCodeRuleCopyWith<DressCodeRule> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DressCodeRuleCopyWith<$Res> {
  factory $DressCodeRuleCopyWith(
    DressCodeRule value,
    $Res Function(DressCodeRule) then,
  ) = _$DressCodeRuleCopyWithImpl<$Res, DressCodeRule>;
  @useResult
  $Res call({String groupName, String? description, String? imageUrl});
}

/// @nodoc
class _$DressCodeRuleCopyWithImpl<$Res, $Val extends DressCodeRule>
    implements $DressCodeRuleCopyWith<$Res> {
  _$DressCodeRuleCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DressCodeRule
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? groupName = null,
    Object? description = freezed,
    Object? imageUrl = freezed,
  }) {
    return _then(
      _value.copyWith(
            groupName: null == groupName
                ? _value.groupName
                : groupName // ignore: cast_nullable_to_non_nullable
                      as String,
            description: freezed == description
                ? _value.description
                : description // ignore: cast_nullable_to_non_nullable
                      as String?,
            imageUrl: freezed == imageUrl
                ? _value.imageUrl
                : imageUrl // ignore: cast_nullable_to_non_nullable
                      as String?,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$DressCodeRuleImplCopyWith<$Res>
    implements $DressCodeRuleCopyWith<$Res> {
  factory _$$DressCodeRuleImplCopyWith(
    _$DressCodeRuleImpl value,
    $Res Function(_$DressCodeRuleImpl) then,
  ) = __$$DressCodeRuleImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String groupName, String? description, String? imageUrl});
}

/// @nodoc
class __$$DressCodeRuleImplCopyWithImpl<$Res>
    extends _$DressCodeRuleCopyWithImpl<$Res, _$DressCodeRuleImpl>
    implements _$$DressCodeRuleImplCopyWith<$Res> {
  __$$DressCodeRuleImplCopyWithImpl(
    _$DressCodeRuleImpl _value,
    $Res Function(_$DressCodeRuleImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of DressCodeRule
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? groupName = null,
    Object? description = freezed,
    Object? imageUrl = freezed,
  }) {
    return _then(
      _$DressCodeRuleImpl(
        groupName: null == groupName
            ? _value.groupName
            : groupName // ignore: cast_nullable_to_non_nullable
                  as String,
        description: freezed == description
            ? _value.description
            : description // ignore: cast_nullable_to_non_nullable
                  as String?,
        imageUrl: freezed == imageUrl
            ? _value.imageUrl
            : imageUrl // ignore: cast_nullable_to_non_nullable
                  as String?,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$DressCodeRuleImpl implements _DressCodeRule {
  const _$DressCodeRuleImpl({
    required this.groupName,
    this.description,
    this.imageUrl,
  });

  factory _$DressCodeRuleImpl.fromJson(Map<String, dynamic> json) =>
      _$$DressCodeRuleImplFromJson(json);

  @override
  final String groupName;
  @override
  final String? description;
  @override
  final String? imageUrl;

  @override
  String toString() {
    return 'DressCodeRule(groupName: $groupName, description: $description, imageUrl: $imageUrl)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DressCodeRuleImpl &&
            (identical(other.groupName, groupName) ||
                other.groupName == groupName) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.imageUrl, imageUrl) ||
                other.imageUrl == imageUrl));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, groupName, description, imageUrl);

  /// Create a copy of DressCodeRule
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DressCodeRuleImplCopyWith<_$DressCodeRuleImpl> get copyWith =>
      __$$DressCodeRuleImplCopyWithImpl<_$DressCodeRuleImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DressCodeRuleImplToJson(this);
  }
}

abstract class _DressCodeRule implements DressCodeRule {
  const factory _DressCodeRule({
    required final String groupName,
    final String? description,
    final String? imageUrl,
  }) = _$DressCodeRuleImpl;

  factory _DressCodeRule.fromJson(Map<String, dynamic> json) =
      _$DressCodeRuleImpl.fromJson;

  @override
  String get groupName;
  @override
  String? get description;
  @override
  String? get imageUrl;

  /// Create a copy of DressCodeRule
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DressCodeRuleImplCopyWith<_$DressCodeRuleImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
