// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'discipleship_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

DiscipleshipJourney _$DiscipleshipJourneyFromJson(Map<String, dynamic> json) {
  return _DiscipleshipJourney.fromJson(json);
}

/// @nodoc
mixin _$DiscipleshipJourney {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  JourneyType get type => throw _privateConstructorUsedError;
  int get totalStages => throw _privateConstructorUsedError;
  DateTime get startDate => throw _privateConstructorUsedError;
  DateTime? get endDate => throw _privateConstructorUsedError;
  int? get createdById => throw _privateConstructorUsedError;
  String? get createdByName => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this DiscipleshipJourney to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DiscipleshipJourney
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DiscipleshipJourneyCopyWith<DiscipleshipJourney> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DiscipleshipJourneyCopyWith<$Res> {
  factory $DiscipleshipJourneyCopyWith(
          DiscipleshipJourney value, $Res Function(DiscipleshipJourney) then) =
      _$DiscipleshipJourneyCopyWithImpl<$Res, DiscipleshipJourney>;
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      JourneyType type,
      int totalStages,
      DateTime startDate,
      DateTime? endDate,
      int? createdById,
      String? createdByName,
      bool isActive,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$DiscipleshipJourneyCopyWithImpl<$Res, $Val extends DiscipleshipJourney>
    implements $DiscipleshipJourneyCopyWith<$Res> {
  _$DiscipleshipJourneyCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DiscipleshipJourney
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? totalStages = null,
    Object? startDate = null,
    Object? endDate = freezed,
    Object? createdById = freezed,
    Object? createdByName = freezed,
    Object? isActive = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
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
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as JourneyType,
      totalStages: null == totalStages
          ? _value.totalStages
          : totalStages // ignore: cast_nullable_to_non_nullable
              as int,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: freezed == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      createdById: freezed == createdById
          ? _value.createdById
          : createdById // ignore: cast_nullable_to_non_nullable
              as int?,
      createdByName: freezed == createdByName
          ? _value.createdByName
          : createdByName // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DiscipleshipJourneyImplCopyWith<$Res>
    implements $DiscipleshipJourneyCopyWith<$Res> {
  factory _$$DiscipleshipJourneyImplCopyWith(_$DiscipleshipJourneyImpl value,
          $Res Function(_$DiscipleshipJourneyImpl) then) =
      __$$DiscipleshipJourneyImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      JourneyType type,
      int totalStages,
      DateTime startDate,
      DateTime? endDate,
      int? createdById,
      String? createdByName,
      bool isActive,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$DiscipleshipJourneyImplCopyWithImpl<$Res>
    extends _$DiscipleshipJourneyCopyWithImpl<$Res, _$DiscipleshipJourneyImpl>
    implements _$$DiscipleshipJourneyImplCopyWith<$Res> {
  __$$DiscipleshipJourneyImplCopyWithImpl(_$DiscipleshipJourneyImpl _value,
      $Res Function(_$DiscipleshipJourneyImpl) _then)
      : super(_value, _then);

  /// Create a copy of DiscipleshipJourney
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? totalStages = null,
    Object? startDate = null,
    Object? endDate = freezed,
    Object? createdById = freezed,
    Object? createdByName = freezed,
    Object? isActive = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$DiscipleshipJourneyImpl(
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
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as JourneyType,
      totalStages: null == totalStages
          ? _value.totalStages
          : totalStages // ignore: cast_nullable_to_non_nullable
              as int,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: freezed == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      createdById: freezed == createdById
          ? _value.createdById
          : createdById // ignore: cast_nullable_to_non_nullable
              as int?,
      createdByName: freezed == createdByName
          ? _value.createdByName
          : createdByName // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DiscipleshipJourneyImpl implements _DiscipleshipJourney {
  const _$DiscipleshipJourneyImpl(
      {required this.id,
      required this.name,
      this.description,
      required this.type,
      required this.totalStages,
      required this.startDate,
      this.endDate,
      this.createdById,
      this.createdByName,
      this.isActive = true,
      required this.createdAt,
      this.updatedAt});

  factory _$DiscipleshipJourneyImpl.fromJson(Map<String, dynamic> json) =>
      _$$DiscipleshipJourneyImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String? description;
  @override
  final JourneyType type;
  @override
  final int totalStages;
  @override
  final DateTime startDate;
  @override
  final DateTime? endDate;
  @override
  final int? createdById;
  @override
  final String? createdByName;
  @override
  @JsonKey()
  final bool isActive;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'DiscipleshipJourney(id: $id, name: $name, description: $description, type: $type, totalStages: $totalStages, startDate: $startDate, endDate: $endDate, createdById: $createdById, createdByName: $createdByName, isActive: $isActive, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DiscipleshipJourneyImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.totalStages, totalStages) ||
                other.totalStages == totalStages) &&
            (identical(other.startDate, startDate) ||
                other.startDate == startDate) &&
            (identical(other.endDate, endDate) || other.endDate == endDate) &&
            (identical(other.createdById, createdById) ||
                other.createdById == createdById) &&
            (identical(other.createdByName, createdByName) ||
                other.createdByName == createdByName) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive) &&
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
      name,
      description,
      type,
      totalStages,
      startDate,
      endDate,
      createdById,
      createdByName,
      isActive,
      createdAt,
      updatedAt);

  /// Create a copy of DiscipleshipJourney
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DiscipleshipJourneyImplCopyWith<_$DiscipleshipJourneyImpl> get copyWith =>
      __$$DiscipleshipJourneyImplCopyWithImpl<_$DiscipleshipJourneyImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DiscipleshipJourneyImplToJson(
      this,
    );
  }
}

abstract class _DiscipleshipJourney implements DiscipleshipJourney {
  const factory _DiscipleshipJourney(
      {required final int id,
      required final String name,
      final String? description,
      required final JourneyType type,
      required final int totalStages,
      required final DateTime startDate,
      final DateTime? endDate,
      final int? createdById,
      final String? createdByName,
      final bool isActive,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$DiscipleshipJourneyImpl;

  factory _DiscipleshipJourney.fromJson(Map<String, dynamic> json) =
      _$DiscipleshipJourneyImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String? get description;
  @override
  JourneyType get type;
  @override
  int get totalStages;
  @override
  DateTime get startDate;
  @override
  DateTime? get endDate;
  @override
  int? get createdById;
  @override
  String? get createdByName;
  @override
  bool get isActive;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of DiscipleshipJourney
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DiscipleshipJourneyImplCopyWith<_$DiscipleshipJourneyImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DiscipleshipStage _$DiscipleshipStageFromJson(Map<String, dynamic> json) {
  return _DiscipleshipStage.fromJson(json);
}

/// @nodoc
mixin _$DiscipleshipStage {
  int get id => throw _privateConstructorUsedError;
  int get journeyId => throw _privateConstructorUsedError;
  int get order => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get color => throw _privateConstructorUsedError;
  String? get icon => throw _privateConstructorUsedError;
  List<StageRequirement>? get requirements =>
      throw _privateConstructorUsedError;
  List<StageReward>? get rewards => throw _privateConstructorUsedError;
  int? get durationDays => throw _privateConstructorUsedError;
  bool get isOptional => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this DiscipleshipStage to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DiscipleshipStage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DiscipleshipStageCopyWith<DiscipleshipStage> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DiscipleshipStageCopyWith<$Res> {
  factory $DiscipleshipStageCopyWith(
          DiscipleshipStage value, $Res Function(DiscipleshipStage) then) =
      _$DiscipleshipStageCopyWithImpl<$Res, DiscipleshipStage>;
  @useResult
  $Res call(
      {int id,
      int journeyId,
      int order,
      String name,
      String? description,
      String? color,
      String? icon,
      List<StageRequirement>? requirements,
      List<StageReward>? rewards,
      int? durationDays,
      bool isOptional,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$DiscipleshipStageCopyWithImpl<$Res, $Val extends DiscipleshipStage>
    implements $DiscipleshipStageCopyWith<$Res> {
  _$DiscipleshipStageCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DiscipleshipStage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? journeyId = null,
    Object? order = null,
    Object? name = null,
    Object? description = freezed,
    Object? color = freezed,
    Object? icon = freezed,
    Object? requirements = freezed,
    Object? rewards = freezed,
    Object? durationDays = freezed,
    Object? isOptional = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      order: null == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
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
      requirements: freezed == requirements
          ? _value.requirements
          : requirements // ignore: cast_nullable_to_non_nullable
              as List<StageRequirement>?,
      rewards: freezed == rewards
          ? _value.rewards
          : rewards // ignore: cast_nullable_to_non_nullable
              as List<StageReward>?,
      durationDays: freezed == durationDays
          ? _value.durationDays
          : durationDays // ignore: cast_nullable_to_non_nullable
              as int?,
      isOptional: null == isOptional
          ? _value.isOptional
          : isOptional // ignore: cast_nullable_to_non_nullable
              as bool,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DiscipleshipStageImplCopyWith<$Res>
    implements $DiscipleshipStageCopyWith<$Res> {
  factory _$$DiscipleshipStageImplCopyWith(_$DiscipleshipStageImpl value,
          $Res Function(_$DiscipleshipStageImpl) then) =
      __$$DiscipleshipStageImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int journeyId,
      int order,
      String name,
      String? description,
      String? color,
      String? icon,
      List<StageRequirement>? requirements,
      List<StageReward>? rewards,
      int? durationDays,
      bool isOptional,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$DiscipleshipStageImplCopyWithImpl<$Res>
    extends _$DiscipleshipStageCopyWithImpl<$Res, _$DiscipleshipStageImpl>
    implements _$$DiscipleshipStageImplCopyWith<$Res> {
  __$$DiscipleshipStageImplCopyWithImpl(_$DiscipleshipStageImpl _value,
      $Res Function(_$DiscipleshipStageImpl) _then)
      : super(_value, _then);

  /// Create a copy of DiscipleshipStage
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? journeyId = null,
    Object? order = null,
    Object? name = null,
    Object? description = freezed,
    Object? color = freezed,
    Object? icon = freezed,
    Object? requirements = freezed,
    Object? rewards = freezed,
    Object? durationDays = freezed,
    Object? isOptional = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$DiscipleshipStageImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      order: null == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
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
      requirements: freezed == requirements
          ? _value._requirements
          : requirements // ignore: cast_nullable_to_non_nullable
              as List<StageRequirement>?,
      rewards: freezed == rewards
          ? _value._rewards
          : rewards // ignore: cast_nullable_to_non_nullable
              as List<StageReward>?,
      durationDays: freezed == durationDays
          ? _value.durationDays
          : durationDays // ignore: cast_nullable_to_non_nullable
              as int?,
      isOptional: null == isOptional
          ? _value.isOptional
          : isOptional // ignore: cast_nullable_to_non_nullable
              as bool,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DiscipleshipStageImpl implements _DiscipleshipStage {
  const _$DiscipleshipStageImpl(
      {required this.id,
      required this.journeyId,
      required this.order,
      required this.name,
      this.description,
      this.color,
      this.icon,
      final List<StageRequirement>? requirements,
      final List<StageReward>? rewards,
      this.durationDays,
      required this.isOptional,
      required this.createdAt,
      this.updatedAt})
      : _requirements = requirements,
        _rewards = rewards;

  factory _$DiscipleshipStageImpl.fromJson(Map<String, dynamic> json) =>
      _$$DiscipleshipStageImplFromJson(json);

  @override
  final int id;
  @override
  final int journeyId;
  @override
  final int order;
  @override
  final String name;
  @override
  final String? description;
  @override
  final String? color;
  @override
  final String? icon;
  final List<StageRequirement>? _requirements;
  @override
  List<StageRequirement>? get requirements {
    final value = _requirements;
    if (value == null) return null;
    if (_requirements is EqualUnmodifiableListView) return _requirements;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<StageReward>? _rewards;
  @override
  List<StageReward>? get rewards {
    final value = _rewards;
    if (value == null) return null;
    if (_rewards is EqualUnmodifiableListView) return _rewards;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final int? durationDays;
  @override
  final bool isOptional;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'DiscipleshipStage(id: $id, journeyId: $journeyId, order: $order, name: $name, description: $description, color: $color, icon: $icon, requirements: $requirements, rewards: $rewards, durationDays: $durationDays, isOptional: $isOptional, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DiscipleshipStageImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.journeyId, journeyId) ||
                other.journeyId == journeyId) &&
            (identical(other.order, order) || other.order == order) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.color, color) || other.color == color) &&
            (identical(other.icon, icon) || other.icon == icon) &&
            const DeepCollectionEquality()
                .equals(other._requirements, _requirements) &&
            const DeepCollectionEquality().equals(other._rewards, _rewards) &&
            (identical(other.durationDays, durationDays) ||
                other.durationDays == durationDays) &&
            (identical(other.isOptional, isOptional) ||
                other.isOptional == isOptional) &&
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
      journeyId,
      order,
      name,
      description,
      color,
      icon,
      const DeepCollectionEquality().hash(_requirements),
      const DeepCollectionEquality().hash(_rewards),
      durationDays,
      isOptional,
      createdAt,
      updatedAt);

  /// Create a copy of DiscipleshipStage
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DiscipleshipStageImplCopyWith<_$DiscipleshipStageImpl> get copyWith =>
      __$$DiscipleshipStageImplCopyWithImpl<_$DiscipleshipStageImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DiscipleshipStageImplToJson(
      this,
    );
  }
}

abstract class _DiscipleshipStage implements DiscipleshipStage {
  const factory _DiscipleshipStage(
      {required final int id,
      required final int journeyId,
      required final int order,
      required final String name,
      final String? description,
      final String? color,
      final String? icon,
      final List<StageRequirement>? requirements,
      final List<StageReward>? rewards,
      final int? durationDays,
      required final bool isOptional,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$DiscipleshipStageImpl;

  factory _DiscipleshipStage.fromJson(Map<String, dynamic> json) =
      _$DiscipleshipStageImpl.fromJson;

  @override
  int get id;
  @override
  int get journeyId;
  @override
  int get order;
  @override
  String get name;
  @override
  String? get description;
  @override
  String? get color;
  @override
  String? get icon;
  @override
  List<StageRequirement>? get requirements;
  @override
  List<StageReward>? get rewards;
  @override
  int? get durationDays;
  @override
  bool get isOptional;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of DiscipleshipStage
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DiscipleshipStageImplCopyWith<_$DiscipleshipStageImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

StageRequirement _$StageRequirementFromJson(Map<String, dynamic> json) {
  return _StageRequirement.fromJson(json);
}

/// @nodoc
mixin _$StageRequirement {
  int get id => throw _privateConstructorUsedError;
  int get stageId => throw _privateConstructorUsedError;
  RequirementType get type => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get referenceId => throw _privateConstructorUsedError;
  String? get referenceName => throw _privateConstructorUsedError;
  bool get isRequired => throw _privateConstructorUsedError;
  int get order => throw _privateConstructorUsedError;

  /// Serializes this StageRequirement to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StageRequirement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StageRequirementCopyWith<StageRequirement> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StageRequirementCopyWith<$Res> {
  factory $StageRequirementCopyWith(
          StageRequirement value, $Res Function(StageRequirement) then) =
      _$StageRequirementCopyWithImpl<$Res, StageRequirement>;
  @useResult
  $Res call(
      {int id,
      int stageId,
      RequirementType type,
      String? description,
      String? referenceId,
      String? referenceName,
      bool isRequired,
      int order});
}

/// @nodoc
class _$StageRequirementCopyWithImpl<$Res, $Val extends StageRequirement>
    implements $StageRequirementCopyWith<$Res> {
  _$StageRequirementCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StageRequirement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? stageId = null,
    Object? type = null,
    Object? description = freezed,
    Object? referenceId = freezed,
    Object? referenceName = freezed,
    Object? isRequired = null,
    Object? order = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      stageId: null == stageId
          ? _value.stageId
          : stageId // ignore: cast_nullable_to_non_nullable
              as int,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as RequirementType,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      referenceId: freezed == referenceId
          ? _value.referenceId
          : referenceId // ignore: cast_nullable_to_non_nullable
              as String?,
      referenceName: freezed == referenceName
          ? _value.referenceName
          : referenceName // ignore: cast_nullable_to_non_nullable
              as String?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
      order: null == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StageRequirementImplCopyWith<$Res>
    implements $StageRequirementCopyWith<$Res> {
  factory _$$StageRequirementImplCopyWith(_$StageRequirementImpl value,
          $Res Function(_$StageRequirementImpl) then) =
      __$$StageRequirementImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int stageId,
      RequirementType type,
      String? description,
      String? referenceId,
      String? referenceName,
      bool isRequired,
      int order});
}

/// @nodoc
class __$$StageRequirementImplCopyWithImpl<$Res>
    extends _$StageRequirementCopyWithImpl<$Res, _$StageRequirementImpl>
    implements _$$StageRequirementImplCopyWith<$Res> {
  __$$StageRequirementImplCopyWithImpl(_$StageRequirementImpl _value,
      $Res Function(_$StageRequirementImpl) _then)
      : super(_value, _then);

  /// Create a copy of StageRequirement
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? stageId = null,
    Object? type = null,
    Object? description = freezed,
    Object? referenceId = freezed,
    Object? referenceName = freezed,
    Object? isRequired = null,
    Object? order = null,
  }) {
    return _then(_$StageRequirementImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      stageId: null == stageId
          ? _value.stageId
          : stageId // ignore: cast_nullable_to_non_nullable
              as int,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as RequirementType,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      referenceId: freezed == referenceId
          ? _value.referenceId
          : referenceId // ignore: cast_nullable_to_non_nullable
              as String?,
      referenceName: freezed == referenceName
          ? _value.referenceName
          : referenceName // ignore: cast_nullable_to_non_nullable
              as String?,
      isRequired: null == isRequired
          ? _value.isRequired
          : isRequired // ignore: cast_nullable_to_non_nullable
              as bool,
      order: null == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StageRequirementImpl implements _StageRequirement {
  const _$StageRequirementImpl(
      {required this.id,
      required this.stageId,
      required this.type,
      this.description,
      this.referenceId,
      this.referenceName,
      required this.isRequired,
      required this.order});

  factory _$StageRequirementImpl.fromJson(Map<String, dynamic> json) =>
      _$$StageRequirementImplFromJson(json);

  @override
  final int id;
  @override
  final int stageId;
  @override
  final RequirementType type;
  @override
  final String? description;
  @override
  final String? referenceId;
  @override
  final String? referenceName;
  @override
  final bool isRequired;
  @override
  final int order;

  @override
  String toString() {
    return 'StageRequirement(id: $id, stageId: $stageId, type: $type, description: $description, referenceId: $referenceId, referenceName: $referenceName, isRequired: $isRequired, order: $order)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StageRequirementImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.stageId, stageId) || other.stageId == stageId) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.referenceId, referenceId) ||
                other.referenceId == referenceId) &&
            (identical(other.referenceName, referenceName) ||
                other.referenceName == referenceName) &&
            (identical(other.isRequired, isRequired) ||
                other.isRequired == isRequired) &&
            (identical(other.order, order) || other.order == order));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, stageId, type, description,
      referenceId, referenceName, isRequired, order);

  /// Create a copy of StageRequirement
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StageRequirementImplCopyWith<_$StageRequirementImpl> get copyWith =>
      __$$StageRequirementImplCopyWithImpl<_$StageRequirementImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StageRequirementImplToJson(
      this,
    );
  }
}

abstract class _StageRequirement implements StageRequirement {
  const factory _StageRequirement(
      {required final int id,
      required final int stageId,
      required final RequirementType type,
      final String? description,
      final String? referenceId,
      final String? referenceName,
      required final bool isRequired,
      required final int order}) = _$StageRequirementImpl;

  factory _StageRequirement.fromJson(Map<String, dynamic> json) =
      _$StageRequirementImpl.fromJson;

  @override
  int get id;
  @override
  int get stageId;
  @override
  RequirementType get type;
  @override
  String? get description;
  @override
  String? get referenceId;
  @override
  String? get referenceName;
  @override
  bool get isRequired;
  @override
  int get order;

  /// Create a copy of StageRequirement
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StageRequirementImplCopyWith<_$StageRequirementImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

StageReward _$StageRewardFromJson(Map<String, dynamic> json) {
  return _StageReward.fromJson(json);
}

/// @nodoc
mixin _$StageReward {
  int get id => throw _privateConstructorUsedError;
  int get stageId => throw _privateConstructorUsedError;
  RewardType get type => throw _privateConstructorUsedError;
  String? get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get icon => throw _privateConstructorUsedError;
  String? get imageUrl => throw _privateConstructorUsedError;
  int? get points => throw _privateConstructorUsedError;
  String? get badgeId => throw _privateConstructorUsedError;
  String? get badgeName => throw _privateConstructorUsedError;

  /// Serializes this StageReward to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StageReward
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StageRewardCopyWith<StageReward> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StageRewardCopyWith<$Res> {
  factory $StageRewardCopyWith(
          StageReward value, $Res Function(StageReward) then) =
      _$StageRewardCopyWithImpl<$Res, StageReward>;
  @useResult
  $Res call(
      {int id,
      int stageId,
      RewardType type,
      String? name,
      String? description,
      String? icon,
      String? imageUrl,
      int? points,
      String? badgeId,
      String? badgeName});
}

/// @nodoc
class _$StageRewardCopyWithImpl<$Res, $Val extends StageReward>
    implements $StageRewardCopyWith<$Res> {
  _$StageRewardCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StageReward
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? stageId = null,
    Object? type = null,
    Object? name = freezed,
    Object? description = freezed,
    Object? icon = freezed,
    Object? imageUrl = freezed,
    Object? points = freezed,
    Object? badgeId = freezed,
    Object? badgeName = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      stageId: null == stageId
          ? _value.stageId
          : stageId // ignore: cast_nullable_to_non_nullable
              as int,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as RewardType,
      name: freezed == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      icon: freezed == icon
          ? _value.icon
          : icon // ignore: cast_nullable_to_non_nullable
              as String?,
      imageUrl: freezed == imageUrl
          ? _value.imageUrl
          : imageUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      points: freezed == points
          ? _value.points
          : points // ignore: cast_nullable_to_non_nullable
              as int?,
      badgeId: freezed == badgeId
          ? _value.badgeId
          : badgeId // ignore: cast_nullable_to_non_nullable
              as String?,
      badgeName: freezed == badgeName
          ? _value.badgeName
          : badgeName // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StageRewardImplCopyWith<$Res>
    implements $StageRewardCopyWith<$Res> {
  factory _$$StageRewardImplCopyWith(
          _$StageRewardImpl value, $Res Function(_$StageRewardImpl) then) =
      __$$StageRewardImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int stageId,
      RewardType type,
      String? name,
      String? description,
      String? icon,
      String? imageUrl,
      int? points,
      String? badgeId,
      String? badgeName});
}

/// @nodoc
class __$$StageRewardImplCopyWithImpl<$Res>
    extends _$StageRewardCopyWithImpl<$Res, _$StageRewardImpl>
    implements _$$StageRewardImplCopyWith<$Res> {
  __$$StageRewardImplCopyWithImpl(
      _$StageRewardImpl _value, $Res Function(_$StageRewardImpl) _then)
      : super(_value, _then);

  /// Create a copy of StageReward
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? stageId = null,
    Object? type = null,
    Object? name = freezed,
    Object? description = freezed,
    Object? icon = freezed,
    Object? imageUrl = freezed,
    Object? points = freezed,
    Object? badgeId = freezed,
    Object? badgeName = freezed,
  }) {
    return _then(_$StageRewardImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      stageId: null == stageId
          ? _value.stageId
          : stageId // ignore: cast_nullable_to_non_nullable
              as int,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as RewardType,
      name: freezed == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      icon: freezed == icon
          ? _value.icon
          : icon // ignore: cast_nullable_to_non_nullable
              as String?,
      imageUrl: freezed == imageUrl
          ? _value.imageUrl
          : imageUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      points: freezed == points
          ? _value.points
          : points // ignore: cast_nullable_to_non_nullable
              as int?,
      badgeId: freezed == badgeId
          ? _value.badgeId
          : badgeId // ignore: cast_nullable_to_non_nullable
              as String?,
      badgeName: freezed == badgeName
          ? _value.badgeName
          : badgeName // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StageRewardImpl implements _StageReward {
  const _$StageRewardImpl(
      {required this.id,
      required this.stageId,
      required this.type,
      this.name,
      this.description,
      this.icon,
      this.imageUrl,
      this.points,
      this.badgeId,
      this.badgeName});

  factory _$StageRewardImpl.fromJson(Map<String, dynamic> json) =>
      _$$StageRewardImplFromJson(json);

  @override
  final int id;
  @override
  final int stageId;
  @override
  final RewardType type;
  @override
  final String? name;
  @override
  final String? description;
  @override
  final String? icon;
  @override
  final String? imageUrl;
  @override
  final int? points;
  @override
  final String? badgeId;
  @override
  final String? badgeName;

  @override
  String toString() {
    return 'StageReward(id: $id, stageId: $stageId, type: $type, name: $name, description: $description, icon: $icon, imageUrl: $imageUrl, points: $points, badgeId: $badgeId, badgeName: $badgeName)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StageRewardImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.stageId, stageId) || other.stageId == stageId) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.icon, icon) || other.icon == icon) &&
            (identical(other.imageUrl, imageUrl) ||
                other.imageUrl == imageUrl) &&
            (identical(other.points, points) || other.points == points) &&
            (identical(other.badgeId, badgeId) || other.badgeId == badgeId) &&
            (identical(other.badgeName, badgeName) ||
                other.badgeName == badgeName));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, stageId, type, name,
      description, icon, imageUrl, points, badgeId, badgeName);

  /// Create a copy of StageReward
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StageRewardImplCopyWith<_$StageRewardImpl> get copyWith =>
      __$$StageRewardImplCopyWithImpl<_$StageRewardImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StageRewardImplToJson(
      this,
    );
  }
}

abstract class _StageReward implements StageReward {
  const factory _StageReward(
      {required final int id,
      required final int stageId,
      required final RewardType type,
      final String? name,
      final String? description,
      final String? icon,
      final String? imageUrl,
      final int? points,
      final String? badgeId,
      final String? badgeName}) = _$StageRewardImpl;

  factory _StageReward.fromJson(Map<String, dynamic> json) =
      _$StageRewardImpl.fromJson;

  @override
  int get id;
  @override
  int get stageId;
  @override
  RewardType get type;
  @override
  String? get name;
  @override
  String? get description;
  @override
  String? get icon;
  @override
  String? get imageUrl;
  @override
  int? get points;
  @override
  String? get badgeId;
  @override
  String? get badgeName;

  /// Create a copy of StageReward
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StageRewardImplCopyWith<_$StageRewardImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DiscipleProgress _$DiscipleProgressFromJson(Map<String, dynamic> json) {
  return _DiscipleProgress.fromJson(json);
}

/// @nodoc
mixin _$DiscipleProgress {
  int get id => throw _privateConstructorUsedError;
  int get discipleId => throw _privateConstructorUsedError;
  String get discipleName => throw _privateConstructorUsedError;
  int get journeyId => throw _privateConstructorUsedError;
  String get journeyName => throw _privateConstructorUsedError;
  int get currentStageId => throw _privateConstructorUsedError;
  String get currentStageName => throw _privateConstructorUsedError;
  int get currentStageOrder => throw _privateConstructorUsedError;
  int get completedStages => throw _privateConstructorUsedError;
  int get totalStages => throw _privateConstructorUsedError;
  int get completedRequirements => throw _privateConstructorUsedError;
  int get totalRequirements => throw _privateConstructorUsedError;
  DateTime? get startedAt => throw _privateConstructorUsedError;
  DateTime? get lastActivityAt => throw _privateConstructorUsedError;
  DateTime? get completedAt => throw _privateConstructorUsedError;
  ProgressStatus get status => throw _privateConstructorUsedError;
  Map<int, RequirementProgress>? get requirementProgress =>
      throw _privateConstructorUsedError;
  DateTime? get nextMilestoneDate => throw _privateConstructorUsedError;
  String? get nextMilestoneName => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this DiscipleProgress to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DiscipleProgress
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DiscipleProgressCopyWith<DiscipleProgress> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DiscipleProgressCopyWith<$Res> {
  factory $DiscipleProgressCopyWith(
          DiscipleProgress value, $Res Function(DiscipleProgress) then) =
      _$DiscipleProgressCopyWithImpl<$Res, DiscipleProgress>;
  @useResult
  $Res call(
      {int id,
      int discipleId,
      String discipleName,
      int journeyId,
      String journeyName,
      int currentStageId,
      String currentStageName,
      int currentStageOrder,
      int completedStages,
      int totalStages,
      int completedRequirements,
      int totalRequirements,
      DateTime? startedAt,
      DateTime? lastActivityAt,
      DateTime? completedAt,
      ProgressStatus status,
      Map<int, RequirementProgress>? requirementProgress,
      DateTime? nextMilestoneDate,
      String? nextMilestoneName,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$DiscipleProgressCopyWithImpl<$Res, $Val extends DiscipleProgress>
    implements $DiscipleProgressCopyWith<$Res> {
  _$DiscipleProgressCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DiscipleProgress
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? discipleId = null,
    Object? discipleName = null,
    Object? journeyId = null,
    Object? journeyName = null,
    Object? currentStageId = null,
    Object? currentStageName = null,
    Object? currentStageOrder = null,
    Object? completedStages = null,
    Object? totalStages = null,
    Object? completedRequirements = null,
    Object? totalRequirements = null,
    Object? startedAt = freezed,
    Object? lastActivityAt = freezed,
    Object? completedAt = freezed,
    Object? status = null,
    Object? requirementProgress = freezed,
    Object? nextMilestoneDate = freezed,
    Object? nextMilestoneName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      discipleId: null == discipleId
          ? _value.discipleId
          : discipleId // ignore: cast_nullable_to_non_nullable
              as int,
      discipleName: null == discipleName
          ? _value.discipleName
          : discipleName // ignore: cast_nullable_to_non_nullable
              as String,
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      journeyName: null == journeyName
          ? _value.journeyName
          : journeyName // ignore: cast_nullable_to_non_nullable
              as String,
      currentStageId: null == currentStageId
          ? _value.currentStageId
          : currentStageId // ignore: cast_nullable_to_non_nullable
              as int,
      currentStageName: null == currentStageName
          ? _value.currentStageName
          : currentStageName // ignore: cast_nullable_to_non_nullable
              as String,
      currentStageOrder: null == currentStageOrder
          ? _value.currentStageOrder
          : currentStageOrder // ignore: cast_nullable_to_non_nullable
              as int,
      completedStages: null == completedStages
          ? _value.completedStages
          : completedStages // ignore: cast_nullable_to_non_nullable
              as int,
      totalStages: null == totalStages
          ? _value.totalStages
          : totalStages // ignore: cast_nullable_to_non_nullable
              as int,
      completedRequirements: null == completedRequirements
          ? _value.completedRequirements
          : completedRequirements // ignore: cast_nullable_to_non_nullable
              as int,
      totalRequirements: null == totalRequirements
          ? _value.totalRequirements
          : totalRequirements // ignore: cast_nullable_to_non_nullable
              as int,
      startedAt: freezed == startedAt
          ? _value.startedAt
          : startedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      lastActivityAt: freezed == lastActivityAt
          ? _value.lastActivityAt
          : lastActivityAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      completedAt: freezed == completedAt
          ? _value.completedAt
          : completedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as ProgressStatus,
      requirementProgress: freezed == requirementProgress
          ? _value.requirementProgress
          : requirementProgress // ignore: cast_nullable_to_non_nullable
              as Map<int, RequirementProgress>?,
      nextMilestoneDate: freezed == nextMilestoneDate
          ? _value.nextMilestoneDate
          : nextMilestoneDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      nextMilestoneName: freezed == nextMilestoneName
          ? _value.nextMilestoneName
          : nextMilestoneName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DiscipleProgressImplCopyWith<$Res>
    implements $DiscipleProgressCopyWith<$Res> {
  factory _$$DiscipleProgressImplCopyWith(_$DiscipleProgressImpl value,
          $Res Function(_$DiscipleProgressImpl) then) =
      __$$DiscipleProgressImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int discipleId,
      String discipleName,
      int journeyId,
      String journeyName,
      int currentStageId,
      String currentStageName,
      int currentStageOrder,
      int completedStages,
      int totalStages,
      int completedRequirements,
      int totalRequirements,
      DateTime? startedAt,
      DateTime? lastActivityAt,
      DateTime? completedAt,
      ProgressStatus status,
      Map<int, RequirementProgress>? requirementProgress,
      DateTime? nextMilestoneDate,
      String? nextMilestoneName,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$DiscipleProgressImplCopyWithImpl<$Res>
    extends _$DiscipleProgressCopyWithImpl<$Res, _$DiscipleProgressImpl>
    implements _$$DiscipleProgressImplCopyWith<$Res> {
  __$$DiscipleProgressImplCopyWithImpl(_$DiscipleProgressImpl _value,
      $Res Function(_$DiscipleProgressImpl) _then)
      : super(_value, _then);

  /// Create a copy of DiscipleProgress
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? discipleId = null,
    Object? discipleName = null,
    Object? journeyId = null,
    Object? journeyName = null,
    Object? currentStageId = null,
    Object? currentStageName = null,
    Object? currentStageOrder = null,
    Object? completedStages = null,
    Object? totalStages = null,
    Object? completedRequirements = null,
    Object? totalRequirements = null,
    Object? startedAt = freezed,
    Object? lastActivityAt = freezed,
    Object? completedAt = freezed,
    Object? status = null,
    Object? requirementProgress = freezed,
    Object? nextMilestoneDate = freezed,
    Object? nextMilestoneName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$DiscipleProgressImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      discipleId: null == discipleId
          ? _value.discipleId
          : discipleId // ignore: cast_nullable_to_non_nullable
              as int,
      discipleName: null == discipleName
          ? _value.discipleName
          : discipleName // ignore: cast_nullable_to_non_nullable
              as String,
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      journeyName: null == journeyName
          ? _value.journeyName
          : journeyName // ignore: cast_nullable_to_non_nullable
              as String,
      currentStageId: null == currentStageId
          ? _value.currentStageId
          : currentStageId // ignore: cast_nullable_to_non_nullable
              as int,
      currentStageName: null == currentStageName
          ? _value.currentStageName
          : currentStageName // ignore: cast_nullable_to_non_nullable
              as String,
      currentStageOrder: null == currentStageOrder
          ? _value.currentStageOrder
          : currentStageOrder // ignore: cast_nullable_to_non_nullable
              as int,
      completedStages: null == completedStages
          ? _value.completedStages
          : completedStages // ignore: cast_nullable_to_non_nullable
              as int,
      totalStages: null == totalStages
          ? _value.totalStages
          : totalStages // ignore: cast_nullable_to_non_nullable
              as int,
      completedRequirements: null == completedRequirements
          ? _value.completedRequirements
          : completedRequirements // ignore: cast_nullable_to_non_nullable
              as int,
      totalRequirements: null == totalRequirements
          ? _value.totalRequirements
          : totalRequirements // ignore: cast_nullable_to_non_nullable
              as int,
      startedAt: freezed == startedAt
          ? _value.startedAt
          : startedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      lastActivityAt: freezed == lastActivityAt
          ? _value.lastActivityAt
          : lastActivityAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      completedAt: freezed == completedAt
          ? _value.completedAt
          : completedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as ProgressStatus,
      requirementProgress: freezed == requirementProgress
          ? _value._requirementProgress
          : requirementProgress // ignore: cast_nullable_to_non_nullable
              as Map<int, RequirementProgress>?,
      nextMilestoneDate: freezed == nextMilestoneDate
          ? _value.nextMilestoneDate
          : nextMilestoneDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      nextMilestoneName: freezed == nextMilestoneName
          ? _value.nextMilestoneName
          : nextMilestoneName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DiscipleProgressImpl extends _DiscipleProgress {
  const _$DiscipleProgressImpl(
      {required this.id,
      required this.discipleId,
      required this.discipleName,
      required this.journeyId,
      required this.journeyName,
      required this.currentStageId,
      required this.currentStageName,
      required this.currentStageOrder,
      this.completedStages = 0,
      required this.totalStages,
      this.completedRequirements = 0,
      required this.totalRequirements,
      this.startedAt,
      this.lastActivityAt,
      this.completedAt,
      required this.status,
      final Map<int, RequirementProgress>? requirementProgress,
      this.nextMilestoneDate,
      this.nextMilestoneName,
      required this.createdAt,
      this.updatedAt})
      : _requirementProgress = requirementProgress,
        super._();

  factory _$DiscipleProgressImpl.fromJson(Map<String, dynamic> json) =>
      _$$DiscipleProgressImplFromJson(json);

  @override
  final int id;
  @override
  final int discipleId;
  @override
  final String discipleName;
  @override
  final int journeyId;
  @override
  final String journeyName;
  @override
  final int currentStageId;
  @override
  final String currentStageName;
  @override
  final int currentStageOrder;
  @override
  @JsonKey()
  final int completedStages;
  @override
  final int totalStages;
  @override
  @JsonKey()
  final int completedRequirements;
  @override
  final int totalRequirements;
  @override
  final DateTime? startedAt;
  @override
  final DateTime? lastActivityAt;
  @override
  final DateTime? completedAt;
  @override
  final ProgressStatus status;
  final Map<int, RequirementProgress>? _requirementProgress;
  @override
  Map<int, RequirementProgress>? get requirementProgress {
    final value = _requirementProgress;
    if (value == null) return null;
    if (_requirementProgress is EqualUnmodifiableMapView)
      return _requirementProgress;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(value);
  }

  @override
  final DateTime? nextMilestoneDate;
  @override
  final String? nextMilestoneName;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'DiscipleProgress(id: $id, discipleId: $discipleId, discipleName: $discipleName, journeyId: $journeyId, journeyName: $journeyName, currentStageId: $currentStageId, currentStageName: $currentStageName, currentStageOrder: $currentStageOrder, completedStages: $completedStages, totalStages: $totalStages, completedRequirements: $completedRequirements, totalRequirements: $totalRequirements, startedAt: $startedAt, lastActivityAt: $lastActivityAt, completedAt: $completedAt, status: $status, requirementProgress: $requirementProgress, nextMilestoneDate: $nextMilestoneDate, nextMilestoneName: $nextMilestoneName, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DiscipleProgressImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.discipleId, discipleId) ||
                other.discipleId == discipleId) &&
            (identical(other.discipleName, discipleName) ||
                other.discipleName == discipleName) &&
            (identical(other.journeyId, journeyId) ||
                other.journeyId == journeyId) &&
            (identical(other.journeyName, journeyName) ||
                other.journeyName == journeyName) &&
            (identical(other.currentStageId, currentStageId) ||
                other.currentStageId == currentStageId) &&
            (identical(other.currentStageName, currentStageName) ||
                other.currentStageName == currentStageName) &&
            (identical(other.currentStageOrder, currentStageOrder) ||
                other.currentStageOrder == currentStageOrder) &&
            (identical(other.completedStages, completedStages) ||
                other.completedStages == completedStages) &&
            (identical(other.totalStages, totalStages) ||
                other.totalStages == totalStages) &&
            (identical(other.completedRequirements, completedRequirements) ||
                other.completedRequirements == completedRequirements) &&
            (identical(other.totalRequirements, totalRequirements) ||
                other.totalRequirements == totalRequirements) &&
            (identical(other.startedAt, startedAt) ||
                other.startedAt == startedAt) &&
            (identical(other.lastActivityAt, lastActivityAt) ||
                other.lastActivityAt == lastActivityAt) &&
            (identical(other.completedAt, completedAt) ||
                other.completedAt == completedAt) &&
            (identical(other.status, status) || other.status == status) &&
            const DeepCollectionEquality()
                .equals(other._requirementProgress, _requirementProgress) &&
            (identical(other.nextMilestoneDate, nextMilestoneDate) ||
                other.nextMilestoneDate == nextMilestoneDate) &&
            (identical(other.nextMilestoneName, nextMilestoneName) ||
                other.nextMilestoneName == nextMilestoneName) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hashAll([
        runtimeType,
        id,
        discipleId,
        discipleName,
        journeyId,
        journeyName,
        currentStageId,
        currentStageName,
        currentStageOrder,
        completedStages,
        totalStages,
        completedRequirements,
        totalRequirements,
        startedAt,
        lastActivityAt,
        completedAt,
        status,
        const DeepCollectionEquality().hash(_requirementProgress),
        nextMilestoneDate,
        nextMilestoneName,
        createdAt,
        updatedAt
      ]);

  /// Create a copy of DiscipleProgress
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DiscipleProgressImplCopyWith<_$DiscipleProgressImpl> get copyWith =>
      __$$DiscipleProgressImplCopyWithImpl<_$DiscipleProgressImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DiscipleProgressImplToJson(
      this,
    );
  }
}

abstract class _DiscipleProgress extends DiscipleProgress {
  const factory _DiscipleProgress(
      {required final int id,
      required final int discipleId,
      required final String discipleName,
      required final int journeyId,
      required final String journeyName,
      required final int currentStageId,
      required final String currentStageName,
      required final int currentStageOrder,
      final int completedStages,
      required final int totalStages,
      final int completedRequirements,
      required final int totalRequirements,
      final DateTime? startedAt,
      final DateTime? lastActivityAt,
      final DateTime? completedAt,
      required final ProgressStatus status,
      final Map<int, RequirementProgress>? requirementProgress,
      final DateTime? nextMilestoneDate,
      final String? nextMilestoneName,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$DiscipleProgressImpl;
  const _DiscipleProgress._() : super._();

  factory _DiscipleProgress.fromJson(Map<String, dynamic> json) =
      _$DiscipleProgressImpl.fromJson;

  @override
  int get id;
  @override
  int get discipleId;
  @override
  String get discipleName;
  @override
  int get journeyId;
  @override
  String get journeyName;
  @override
  int get currentStageId;
  @override
  String get currentStageName;
  @override
  int get currentStageOrder;
  @override
  int get completedStages;
  @override
  int get totalStages;
  @override
  int get completedRequirements;
  @override
  int get totalRequirements;
  @override
  DateTime? get startedAt;
  @override
  DateTime? get lastActivityAt;
  @override
  DateTime? get completedAt;
  @override
  ProgressStatus get status;
  @override
  Map<int, RequirementProgress>? get requirementProgress;
  @override
  DateTime? get nextMilestoneDate;
  @override
  String? get nextMilestoneName;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of DiscipleProgress
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DiscipleProgressImplCopyWith<_$DiscipleProgressImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

RequirementProgress _$RequirementProgressFromJson(Map<String, dynamic> json) {
  return _RequirementProgress.fromJson(json);
}

/// @nodoc
mixin _$RequirementProgress {
  int get requirementId => throw _privateConstructorUsedError;
  String get requirementName => throw _privateConstructorUsedError;
  RequirementStatus get status => throw _privateConstructorUsedError;
  DateTime? get completedAt => throw _privateConstructorUsedError;
  String? get evidence => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  int? get verifiedById => throw _privateConstructorUsedError;
  String? get verifiedByName => throw _privateConstructorUsedError;

  /// Serializes this RequirementProgress to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of RequirementProgress
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $RequirementProgressCopyWith<RequirementProgress> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $RequirementProgressCopyWith<$Res> {
  factory $RequirementProgressCopyWith(
          RequirementProgress value, $Res Function(RequirementProgress) then) =
      _$RequirementProgressCopyWithImpl<$Res, RequirementProgress>;
  @useResult
  $Res call(
      {int requirementId,
      String requirementName,
      RequirementStatus status,
      DateTime? completedAt,
      String? evidence,
      String? notes,
      int? verifiedById,
      String? verifiedByName});
}

/// @nodoc
class _$RequirementProgressCopyWithImpl<$Res, $Val extends RequirementProgress>
    implements $RequirementProgressCopyWith<$Res> {
  _$RequirementProgressCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of RequirementProgress
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? requirementId = null,
    Object? requirementName = null,
    Object? status = null,
    Object? completedAt = freezed,
    Object? evidence = freezed,
    Object? notes = freezed,
    Object? verifiedById = freezed,
    Object? verifiedByName = freezed,
  }) {
    return _then(_value.copyWith(
      requirementId: null == requirementId
          ? _value.requirementId
          : requirementId // ignore: cast_nullable_to_non_nullable
              as int,
      requirementName: null == requirementName
          ? _value.requirementName
          : requirementName // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as RequirementStatus,
      completedAt: freezed == completedAt
          ? _value.completedAt
          : completedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      evidence: freezed == evidence
          ? _value.evidence
          : evidence // ignore: cast_nullable_to_non_nullable
              as String?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      verifiedById: freezed == verifiedById
          ? _value.verifiedById
          : verifiedById // ignore: cast_nullable_to_non_nullable
              as int?,
      verifiedByName: freezed == verifiedByName
          ? _value.verifiedByName
          : verifiedByName // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$RequirementProgressImplCopyWith<$Res>
    implements $RequirementProgressCopyWith<$Res> {
  factory _$$RequirementProgressImplCopyWith(_$RequirementProgressImpl value,
          $Res Function(_$RequirementProgressImpl) then) =
      __$$RequirementProgressImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int requirementId,
      String requirementName,
      RequirementStatus status,
      DateTime? completedAt,
      String? evidence,
      String? notes,
      int? verifiedById,
      String? verifiedByName});
}

/// @nodoc
class __$$RequirementProgressImplCopyWithImpl<$Res>
    extends _$RequirementProgressCopyWithImpl<$Res, _$RequirementProgressImpl>
    implements _$$RequirementProgressImplCopyWith<$Res> {
  __$$RequirementProgressImplCopyWithImpl(_$RequirementProgressImpl _value,
      $Res Function(_$RequirementProgressImpl) _then)
      : super(_value, _then);

  /// Create a copy of RequirementProgress
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? requirementId = null,
    Object? requirementName = null,
    Object? status = null,
    Object? completedAt = freezed,
    Object? evidence = freezed,
    Object? notes = freezed,
    Object? verifiedById = freezed,
    Object? verifiedByName = freezed,
  }) {
    return _then(_$RequirementProgressImpl(
      requirementId: null == requirementId
          ? _value.requirementId
          : requirementId // ignore: cast_nullable_to_non_nullable
              as int,
      requirementName: null == requirementName
          ? _value.requirementName
          : requirementName // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as RequirementStatus,
      completedAt: freezed == completedAt
          ? _value.completedAt
          : completedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      evidence: freezed == evidence
          ? _value.evidence
          : evidence // ignore: cast_nullable_to_non_nullable
              as String?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      verifiedById: freezed == verifiedById
          ? _value.verifiedById
          : verifiedById // ignore: cast_nullable_to_non_nullable
              as int?,
      verifiedByName: freezed == verifiedByName
          ? _value.verifiedByName
          : verifiedByName // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$RequirementProgressImpl implements _RequirementProgress {
  const _$RequirementProgressImpl(
      {required this.requirementId,
      required this.requirementName,
      required this.status,
      this.completedAt,
      this.evidence,
      this.notes,
      this.verifiedById,
      this.verifiedByName});

  factory _$RequirementProgressImpl.fromJson(Map<String, dynamic> json) =>
      _$$RequirementProgressImplFromJson(json);

  @override
  final int requirementId;
  @override
  final String requirementName;
  @override
  final RequirementStatus status;
  @override
  final DateTime? completedAt;
  @override
  final String? evidence;
  @override
  final String? notes;
  @override
  final int? verifiedById;
  @override
  final String? verifiedByName;

  @override
  String toString() {
    return 'RequirementProgress(requirementId: $requirementId, requirementName: $requirementName, status: $status, completedAt: $completedAt, evidence: $evidence, notes: $notes, verifiedById: $verifiedById, verifiedByName: $verifiedByName)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RequirementProgressImpl &&
            (identical(other.requirementId, requirementId) ||
                other.requirementId == requirementId) &&
            (identical(other.requirementName, requirementName) ||
                other.requirementName == requirementName) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.completedAt, completedAt) ||
                other.completedAt == completedAt) &&
            (identical(other.evidence, evidence) ||
                other.evidence == evidence) &&
            (identical(other.notes, notes) || other.notes == notes) &&
            (identical(other.verifiedById, verifiedById) ||
                other.verifiedById == verifiedById) &&
            (identical(other.verifiedByName, verifiedByName) ||
                other.verifiedByName == verifiedByName));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, requirementId, requirementName,
      status, completedAt, evidence, notes, verifiedById, verifiedByName);

  /// Create a copy of RequirementProgress
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RequirementProgressImplCopyWith<_$RequirementProgressImpl> get copyWith =>
      __$$RequirementProgressImplCopyWithImpl<_$RequirementProgressImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$RequirementProgressImplToJson(
      this,
    );
  }
}

abstract class _RequirementProgress implements RequirementProgress {
  const factory _RequirementProgress(
      {required final int requirementId,
      required final String requirementName,
      required final RequirementStatus status,
      final DateTime? completedAt,
      final String? evidence,
      final String? notes,
      final int? verifiedById,
      final String? verifiedByName}) = _$RequirementProgressImpl;

  factory _RequirementProgress.fromJson(Map<String, dynamic> json) =
      _$RequirementProgressImpl.fromJson;

  @override
  int get requirementId;
  @override
  String get requirementName;
  @override
  RequirementStatus get status;
  @override
  DateTime? get completedAt;
  @override
  String? get evidence;
  @override
  String? get notes;
  @override
  int? get verifiedById;
  @override
  String? get verifiedByName;

  /// Create a copy of RequirementProgress
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RequirementProgressImplCopyWith<_$RequirementProgressImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

MentorAssignment _$MentorAssignmentFromJson(Map<String, dynamic> json) {
  return _MentorAssignment.fromJson(json);
}

/// @nodoc
mixin _$MentorAssignment {
  int get id => throw _privateConstructorUsedError;
  int get mentorId => throw _privateConstructorUsedError;
  String get mentorName => throw _privateConstructorUsedError;
  int get discipleId => throw _privateConstructorUsedError;
  String get discipleName => throw _privateConstructorUsedError;
  int get journeyId => throw _privateConstructorUsedError;
  DateTime get assignedAt => throw _privateConstructorUsedError;
  DateTime? get endedAt => throw _privateConstructorUsedError;
  AssignmentStatus get status => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  int get meetingFrequencyDays => throw _privateConstructorUsedError;
  DateTime? get lastMeetingAt => throw _privateConstructorUsedError;
  DateTime? get nextMeetingAt => throw _privateConstructorUsedError;

  /// Serializes this MentorAssignment to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of MentorAssignment
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MentorAssignmentCopyWith<MentorAssignment> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MentorAssignmentCopyWith<$Res> {
  factory $MentorAssignmentCopyWith(
          MentorAssignment value, $Res Function(MentorAssignment) then) =
      _$MentorAssignmentCopyWithImpl<$Res, MentorAssignment>;
  @useResult
  $Res call(
      {int id,
      int mentorId,
      String mentorName,
      int discipleId,
      String discipleName,
      int journeyId,
      DateTime assignedAt,
      DateTime? endedAt,
      AssignmentStatus status,
      String? notes,
      int meetingFrequencyDays,
      DateTime? lastMeetingAt,
      DateTime? nextMeetingAt});
}

/// @nodoc
class _$MentorAssignmentCopyWithImpl<$Res, $Val extends MentorAssignment>
    implements $MentorAssignmentCopyWith<$Res> {
  _$MentorAssignmentCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of MentorAssignment
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? mentorId = null,
    Object? mentorName = null,
    Object? discipleId = null,
    Object? discipleName = null,
    Object? journeyId = null,
    Object? assignedAt = null,
    Object? endedAt = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? meetingFrequencyDays = null,
    Object? lastMeetingAt = freezed,
    Object? nextMeetingAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      mentorId: null == mentorId
          ? _value.mentorId
          : mentorId // ignore: cast_nullable_to_non_nullable
              as int,
      mentorName: null == mentorName
          ? _value.mentorName
          : mentorName // ignore: cast_nullable_to_non_nullable
              as String,
      discipleId: null == discipleId
          ? _value.discipleId
          : discipleId // ignore: cast_nullable_to_non_nullable
              as int,
      discipleName: null == discipleName
          ? _value.discipleName
          : discipleName // ignore: cast_nullable_to_non_nullable
              as String,
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      assignedAt: null == assignedAt
          ? _value.assignedAt
          : assignedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endedAt: freezed == endedAt
          ? _value.endedAt
          : endedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as AssignmentStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      meetingFrequencyDays: null == meetingFrequencyDays
          ? _value.meetingFrequencyDays
          : meetingFrequencyDays // ignore: cast_nullable_to_non_nullable
              as int,
      lastMeetingAt: freezed == lastMeetingAt
          ? _value.lastMeetingAt
          : lastMeetingAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      nextMeetingAt: freezed == nextMeetingAt
          ? _value.nextMeetingAt
          : nextMeetingAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$MentorAssignmentImplCopyWith<$Res>
    implements $MentorAssignmentCopyWith<$Res> {
  factory _$$MentorAssignmentImplCopyWith(_$MentorAssignmentImpl value,
          $Res Function(_$MentorAssignmentImpl) then) =
      __$$MentorAssignmentImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int mentorId,
      String mentorName,
      int discipleId,
      String discipleName,
      int journeyId,
      DateTime assignedAt,
      DateTime? endedAt,
      AssignmentStatus status,
      String? notes,
      int meetingFrequencyDays,
      DateTime? lastMeetingAt,
      DateTime? nextMeetingAt});
}

/// @nodoc
class __$$MentorAssignmentImplCopyWithImpl<$Res>
    extends _$MentorAssignmentCopyWithImpl<$Res, _$MentorAssignmentImpl>
    implements _$$MentorAssignmentImplCopyWith<$Res> {
  __$$MentorAssignmentImplCopyWithImpl(_$MentorAssignmentImpl _value,
      $Res Function(_$MentorAssignmentImpl) _then)
      : super(_value, _then);

  /// Create a copy of MentorAssignment
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? mentorId = null,
    Object? mentorName = null,
    Object? discipleId = null,
    Object? discipleName = null,
    Object? journeyId = null,
    Object? assignedAt = null,
    Object? endedAt = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? meetingFrequencyDays = null,
    Object? lastMeetingAt = freezed,
    Object? nextMeetingAt = freezed,
  }) {
    return _then(_$MentorAssignmentImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      mentorId: null == mentorId
          ? _value.mentorId
          : mentorId // ignore: cast_nullable_to_non_nullable
              as int,
      mentorName: null == mentorName
          ? _value.mentorName
          : mentorName // ignore: cast_nullable_to_non_nullable
              as String,
      discipleId: null == discipleId
          ? _value.discipleId
          : discipleId // ignore: cast_nullable_to_non_nullable
              as int,
      discipleName: null == discipleName
          ? _value.discipleName
          : discipleName // ignore: cast_nullable_to_non_nullable
              as String,
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      assignedAt: null == assignedAt
          ? _value.assignedAt
          : assignedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endedAt: freezed == endedAt
          ? _value.endedAt
          : endedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as AssignmentStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      meetingFrequencyDays: null == meetingFrequencyDays
          ? _value.meetingFrequencyDays
          : meetingFrequencyDays // ignore: cast_nullable_to_non_nullable
              as int,
      lastMeetingAt: freezed == lastMeetingAt
          ? _value.lastMeetingAt
          : lastMeetingAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      nextMeetingAt: freezed == nextMeetingAt
          ? _value.nextMeetingAt
          : nextMeetingAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$MentorAssignmentImpl implements _MentorAssignment {
  const _$MentorAssignmentImpl(
      {required this.id,
      required this.mentorId,
      required this.mentorName,
      required this.discipleId,
      required this.discipleName,
      required this.journeyId,
      required this.assignedAt,
      this.endedAt,
      required this.status,
      this.notes,
      this.meetingFrequencyDays = 7,
      this.lastMeetingAt,
      this.nextMeetingAt});

  factory _$MentorAssignmentImpl.fromJson(Map<String, dynamic> json) =>
      _$$MentorAssignmentImplFromJson(json);

  @override
  final int id;
  @override
  final int mentorId;
  @override
  final String mentorName;
  @override
  final int discipleId;
  @override
  final String discipleName;
  @override
  final int journeyId;
  @override
  final DateTime assignedAt;
  @override
  final DateTime? endedAt;
  @override
  final AssignmentStatus status;
  @override
  final String? notes;
  @override
  @JsonKey()
  final int meetingFrequencyDays;
  @override
  final DateTime? lastMeetingAt;
  @override
  final DateTime? nextMeetingAt;

  @override
  String toString() {
    return 'MentorAssignment(id: $id, mentorId: $mentorId, mentorName: $mentorName, discipleId: $discipleId, discipleName: $discipleName, journeyId: $journeyId, assignedAt: $assignedAt, endedAt: $endedAt, status: $status, notes: $notes, meetingFrequencyDays: $meetingFrequencyDays, lastMeetingAt: $lastMeetingAt, nextMeetingAt: $nextMeetingAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MentorAssignmentImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.mentorId, mentorId) ||
                other.mentorId == mentorId) &&
            (identical(other.mentorName, mentorName) ||
                other.mentorName == mentorName) &&
            (identical(other.discipleId, discipleId) ||
                other.discipleId == discipleId) &&
            (identical(other.discipleName, discipleName) ||
                other.discipleName == discipleName) &&
            (identical(other.journeyId, journeyId) ||
                other.journeyId == journeyId) &&
            (identical(other.assignedAt, assignedAt) ||
                other.assignedAt == assignedAt) &&
            (identical(other.endedAt, endedAt) || other.endedAt == endedAt) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.notes, notes) || other.notes == notes) &&
            (identical(other.meetingFrequencyDays, meetingFrequencyDays) ||
                other.meetingFrequencyDays == meetingFrequencyDays) &&
            (identical(other.lastMeetingAt, lastMeetingAt) ||
                other.lastMeetingAt == lastMeetingAt) &&
            (identical(other.nextMeetingAt, nextMeetingAt) ||
                other.nextMeetingAt == nextMeetingAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      mentorId,
      mentorName,
      discipleId,
      discipleName,
      journeyId,
      assignedAt,
      endedAt,
      status,
      notes,
      meetingFrequencyDays,
      lastMeetingAt,
      nextMeetingAt);

  /// Create a copy of MentorAssignment
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MentorAssignmentImplCopyWith<_$MentorAssignmentImpl> get copyWith =>
      __$$MentorAssignmentImplCopyWithImpl<_$MentorAssignmentImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MentorAssignmentImplToJson(
      this,
    );
  }
}

abstract class _MentorAssignment implements MentorAssignment {
  const factory _MentorAssignment(
      {required final int id,
      required final int mentorId,
      required final String mentorName,
      required final int discipleId,
      required final String discipleName,
      required final int journeyId,
      required final DateTime assignedAt,
      final DateTime? endedAt,
      required final AssignmentStatus status,
      final String? notes,
      final int meetingFrequencyDays,
      final DateTime? lastMeetingAt,
      final DateTime? nextMeetingAt}) = _$MentorAssignmentImpl;

  factory _MentorAssignment.fromJson(Map<String, dynamic> json) =
      _$MentorAssignmentImpl.fromJson;

  @override
  int get id;
  @override
  int get mentorId;
  @override
  String get mentorName;
  @override
  int get discipleId;
  @override
  String get discipleName;
  @override
  int get journeyId;
  @override
  DateTime get assignedAt;
  @override
  DateTime? get endedAt;
  @override
  AssignmentStatus get status;
  @override
  String? get notes;
  @override
  int get meetingFrequencyDays;
  @override
  DateTime? get lastMeetingAt;
  @override
  DateTime? get nextMeetingAt;

  /// Create a copy of MentorAssignment
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MentorAssignmentImplCopyWith<_$MentorAssignmentImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

MentorMeeting _$MentorMeetingFromJson(Map<String, dynamic> json) {
  return _MentorMeeting.fromJson(json);
}

/// @nodoc
mixin _$MentorMeeting {
  int get id => throw _privateConstructorUsedError;
  int get assignmentId => throw _privateConstructorUsedError;
  int get mentorId => throw _privateConstructorUsedError;
  int get discipleId => throw _privateConstructorUsedError;
  DateTime get scheduledAt => throw _privateConstructorUsedError;
  DateTime? get actualAt => throw _privateConstructorUsedError;
  MeetingStatus get status => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  String? get actionItems => throw _privateConstructorUsedError;
  String? get nextSteps => throw _privateConstructorUsedError;
  int? get durationMinutes => throw _privateConstructorUsedError;
  String? get location => throw _privateConstructorUsedError;

  /// Serializes this MentorMeeting to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of MentorMeeting
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MentorMeetingCopyWith<MentorMeeting> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MentorMeetingCopyWith<$Res> {
  factory $MentorMeetingCopyWith(
          MentorMeeting value, $Res Function(MentorMeeting) then) =
      _$MentorMeetingCopyWithImpl<$Res, MentorMeeting>;
  @useResult
  $Res call(
      {int id,
      int assignmentId,
      int mentorId,
      int discipleId,
      DateTime scheduledAt,
      DateTime? actualAt,
      MeetingStatus status,
      String? notes,
      String? actionItems,
      String? nextSteps,
      int? durationMinutes,
      String? location});
}

/// @nodoc
class _$MentorMeetingCopyWithImpl<$Res, $Val extends MentorMeeting>
    implements $MentorMeetingCopyWith<$Res> {
  _$MentorMeetingCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of MentorMeeting
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? assignmentId = null,
    Object? mentorId = null,
    Object? discipleId = null,
    Object? scheduledAt = null,
    Object? actualAt = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? actionItems = freezed,
    Object? nextSteps = freezed,
    Object? durationMinutes = freezed,
    Object? location = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      assignmentId: null == assignmentId
          ? _value.assignmentId
          : assignmentId // ignore: cast_nullable_to_non_nullable
              as int,
      mentorId: null == mentorId
          ? _value.mentorId
          : mentorId // ignore: cast_nullable_to_non_nullable
              as int,
      discipleId: null == discipleId
          ? _value.discipleId
          : discipleId // ignore: cast_nullable_to_non_nullable
              as int,
      scheduledAt: null == scheduledAt
          ? _value.scheduledAt
          : scheduledAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      actualAt: freezed == actualAt
          ? _value.actualAt
          : actualAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as MeetingStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      actionItems: freezed == actionItems
          ? _value.actionItems
          : actionItems // ignore: cast_nullable_to_non_nullable
              as String?,
      nextSteps: freezed == nextSteps
          ? _value.nextSteps
          : nextSteps // ignore: cast_nullable_to_non_nullable
              as String?,
      durationMinutes: freezed == durationMinutes
          ? _value.durationMinutes
          : durationMinutes // ignore: cast_nullable_to_non_nullable
              as int?,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$MentorMeetingImplCopyWith<$Res>
    implements $MentorMeetingCopyWith<$Res> {
  factory _$$MentorMeetingImplCopyWith(
          _$MentorMeetingImpl value, $Res Function(_$MentorMeetingImpl) then) =
      __$$MentorMeetingImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int assignmentId,
      int mentorId,
      int discipleId,
      DateTime scheduledAt,
      DateTime? actualAt,
      MeetingStatus status,
      String? notes,
      String? actionItems,
      String? nextSteps,
      int? durationMinutes,
      String? location});
}

/// @nodoc
class __$$MentorMeetingImplCopyWithImpl<$Res>
    extends _$MentorMeetingCopyWithImpl<$Res, _$MentorMeetingImpl>
    implements _$$MentorMeetingImplCopyWith<$Res> {
  __$$MentorMeetingImplCopyWithImpl(
      _$MentorMeetingImpl _value, $Res Function(_$MentorMeetingImpl) _then)
      : super(_value, _then);

  /// Create a copy of MentorMeeting
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? assignmentId = null,
    Object? mentorId = null,
    Object? discipleId = null,
    Object? scheduledAt = null,
    Object? actualAt = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? actionItems = freezed,
    Object? nextSteps = freezed,
    Object? durationMinutes = freezed,
    Object? location = freezed,
  }) {
    return _then(_$MentorMeetingImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      assignmentId: null == assignmentId
          ? _value.assignmentId
          : assignmentId // ignore: cast_nullable_to_non_nullable
              as int,
      mentorId: null == mentorId
          ? _value.mentorId
          : mentorId // ignore: cast_nullable_to_non_nullable
              as int,
      discipleId: null == discipleId
          ? _value.discipleId
          : discipleId // ignore: cast_nullable_to_non_nullable
              as int,
      scheduledAt: null == scheduledAt
          ? _value.scheduledAt
          : scheduledAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      actualAt: freezed == actualAt
          ? _value.actualAt
          : actualAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as MeetingStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      actionItems: freezed == actionItems
          ? _value.actionItems
          : actionItems // ignore: cast_nullable_to_non_nullable
              as String?,
      nextSteps: freezed == nextSteps
          ? _value.nextSteps
          : nextSteps // ignore: cast_nullable_to_non_nullable
              as String?,
      durationMinutes: freezed == durationMinutes
          ? _value.durationMinutes
          : durationMinutes // ignore: cast_nullable_to_non_nullable
              as int?,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$MentorMeetingImpl implements _MentorMeeting {
  const _$MentorMeetingImpl(
      {required this.id,
      required this.assignmentId,
      required this.mentorId,
      required this.discipleId,
      required this.scheduledAt,
      this.actualAt,
      required this.status,
      this.notes,
      this.actionItems,
      this.nextSteps,
      this.durationMinutes,
      this.location});

  factory _$MentorMeetingImpl.fromJson(Map<String, dynamic> json) =>
      _$$MentorMeetingImplFromJson(json);

  @override
  final int id;
  @override
  final int assignmentId;
  @override
  final int mentorId;
  @override
  final int discipleId;
  @override
  final DateTime scheduledAt;
  @override
  final DateTime? actualAt;
  @override
  final MeetingStatus status;
  @override
  final String? notes;
  @override
  final String? actionItems;
  @override
  final String? nextSteps;
  @override
  final int? durationMinutes;
  @override
  final String? location;

  @override
  String toString() {
    return 'MentorMeeting(id: $id, assignmentId: $assignmentId, mentorId: $mentorId, discipleId: $discipleId, scheduledAt: $scheduledAt, actualAt: $actualAt, status: $status, notes: $notes, actionItems: $actionItems, nextSteps: $nextSteps, durationMinutes: $durationMinutes, location: $location)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MentorMeetingImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.assignmentId, assignmentId) ||
                other.assignmentId == assignmentId) &&
            (identical(other.mentorId, mentorId) ||
                other.mentorId == mentorId) &&
            (identical(other.discipleId, discipleId) ||
                other.discipleId == discipleId) &&
            (identical(other.scheduledAt, scheduledAt) ||
                other.scheduledAt == scheduledAt) &&
            (identical(other.actualAt, actualAt) ||
                other.actualAt == actualAt) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.notes, notes) || other.notes == notes) &&
            (identical(other.actionItems, actionItems) ||
                other.actionItems == actionItems) &&
            (identical(other.nextSteps, nextSteps) ||
                other.nextSteps == nextSteps) &&
            (identical(other.durationMinutes, durationMinutes) ||
                other.durationMinutes == durationMinutes) &&
            (identical(other.location, location) ||
                other.location == location));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      assignmentId,
      mentorId,
      discipleId,
      scheduledAt,
      actualAt,
      status,
      notes,
      actionItems,
      nextSteps,
      durationMinutes,
      location);

  /// Create a copy of MentorMeeting
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MentorMeetingImplCopyWith<_$MentorMeetingImpl> get copyWith =>
      __$$MentorMeetingImplCopyWithImpl<_$MentorMeetingImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MentorMeetingImplToJson(
      this,
    );
  }
}

abstract class _MentorMeeting implements MentorMeeting {
  const factory _MentorMeeting(
      {required final int id,
      required final int assignmentId,
      required final int mentorId,
      required final int discipleId,
      required final DateTime scheduledAt,
      final DateTime? actualAt,
      required final MeetingStatus status,
      final String? notes,
      final String? actionItems,
      final String? nextSteps,
      final int? durationMinutes,
      final String? location}) = _$MentorMeetingImpl;

  factory _MentorMeeting.fromJson(Map<String, dynamic> json) =
      _$MentorMeetingImpl.fromJson;

  @override
  int get id;
  @override
  int get assignmentId;
  @override
  int get mentorId;
  @override
  int get discipleId;
  @override
  DateTime get scheduledAt;
  @override
  DateTime? get actualAt;
  @override
  MeetingStatus get status;
  @override
  String? get notes;
  @override
  String? get actionItems;
  @override
  String? get nextSteps;
  @override
  int? get durationMinutes;
  @override
  String? get location;

  /// Create a copy of MentorMeeting
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MentorMeetingImplCopyWith<_$MentorMeetingImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DiscipleshipReport _$DiscipleshipReportFromJson(Map<String, dynamic> json) {
  return _DiscipleshipReport.fromJson(json);
}

/// @nodoc
mixin _$DiscipleshipReport {
  int get journeyId => throw _privateConstructorUsedError;
  String get journeyName => throw _privateConstructorUsedError;
  int get totalDisciples => throw _privateConstructorUsedError;
  int get activeDisciples => throw _privateConstructorUsedError;
  int get completedDisciples => throw _privateConstructorUsedError;
  int get stalledDisciples => throw _privateConstructorUsedError;
  double get averageCompletion => throw _privateConstructorUsedError;
  int get totalMeetings => throw _privateConstructorUsedError;
  int get completedMeetings => throw _privateConstructorUsedError;
  Map<String, int> get stageDistribution => throw _privateConstructorUsedError;
  Map<String, int> get statusDistribution => throw _privateConstructorUsedError;
  List<TopMentor> get topMentors => throw _privateConstructorUsedError;
  DateTime get generatedAt => throw _privateConstructorUsedError;

  /// Serializes this DiscipleshipReport to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of DiscipleshipReport
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DiscipleshipReportCopyWith<DiscipleshipReport> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DiscipleshipReportCopyWith<$Res> {
  factory $DiscipleshipReportCopyWith(
          DiscipleshipReport value, $Res Function(DiscipleshipReport) then) =
      _$DiscipleshipReportCopyWithImpl<$Res, DiscipleshipReport>;
  @useResult
  $Res call(
      {int journeyId,
      String journeyName,
      int totalDisciples,
      int activeDisciples,
      int completedDisciples,
      int stalledDisciples,
      double averageCompletion,
      int totalMeetings,
      int completedMeetings,
      Map<String, int> stageDistribution,
      Map<String, int> statusDistribution,
      List<TopMentor> topMentors,
      DateTime generatedAt});
}

/// @nodoc
class _$DiscipleshipReportCopyWithImpl<$Res, $Val extends DiscipleshipReport>
    implements $DiscipleshipReportCopyWith<$Res> {
  _$DiscipleshipReportCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of DiscipleshipReport
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? journeyId = null,
    Object? journeyName = null,
    Object? totalDisciples = null,
    Object? activeDisciples = null,
    Object? completedDisciples = null,
    Object? stalledDisciples = null,
    Object? averageCompletion = null,
    Object? totalMeetings = null,
    Object? completedMeetings = null,
    Object? stageDistribution = null,
    Object? statusDistribution = null,
    Object? topMentors = null,
    Object? generatedAt = null,
  }) {
    return _then(_value.copyWith(
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      journeyName: null == journeyName
          ? _value.journeyName
          : journeyName // ignore: cast_nullable_to_non_nullable
              as String,
      totalDisciples: null == totalDisciples
          ? _value.totalDisciples
          : totalDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      activeDisciples: null == activeDisciples
          ? _value.activeDisciples
          : activeDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      completedDisciples: null == completedDisciples
          ? _value.completedDisciples
          : completedDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      stalledDisciples: null == stalledDisciples
          ? _value.stalledDisciples
          : stalledDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      averageCompletion: null == averageCompletion
          ? _value.averageCompletion
          : averageCompletion // ignore: cast_nullable_to_non_nullable
              as double,
      totalMeetings: null == totalMeetings
          ? _value.totalMeetings
          : totalMeetings // ignore: cast_nullable_to_non_nullable
              as int,
      completedMeetings: null == completedMeetings
          ? _value.completedMeetings
          : completedMeetings // ignore: cast_nullable_to_non_nullable
              as int,
      stageDistribution: null == stageDistribution
          ? _value.stageDistribution
          : stageDistribution // ignore: cast_nullable_to_non_nullable
              as Map<String, int>,
      statusDistribution: null == statusDistribution
          ? _value.statusDistribution
          : statusDistribution // ignore: cast_nullable_to_non_nullable
              as Map<String, int>,
      topMentors: null == topMentors
          ? _value.topMentors
          : topMentors // ignore: cast_nullable_to_non_nullable
              as List<TopMentor>,
      generatedAt: null == generatedAt
          ? _value.generatedAt
          : generatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DiscipleshipReportImplCopyWith<$Res>
    implements $DiscipleshipReportCopyWith<$Res> {
  factory _$$DiscipleshipReportImplCopyWith(_$DiscipleshipReportImpl value,
          $Res Function(_$DiscipleshipReportImpl) then) =
      __$$DiscipleshipReportImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int journeyId,
      String journeyName,
      int totalDisciples,
      int activeDisciples,
      int completedDisciples,
      int stalledDisciples,
      double averageCompletion,
      int totalMeetings,
      int completedMeetings,
      Map<String, int> stageDistribution,
      Map<String, int> statusDistribution,
      List<TopMentor> topMentors,
      DateTime generatedAt});
}

/// @nodoc
class __$$DiscipleshipReportImplCopyWithImpl<$Res>
    extends _$DiscipleshipReportCopyWithImpl<$Res, _$DiscipleshipReportImpl>
    implements _$$DiscipleshipReportImplCopyWith<$Res> {
  __$$DiscipleshipReportImplCopyWithImpl(_$DiscipleshipReportImpl _value,
      $Res Function(_$DiscipleshipReportImpl) _then)
      : super(_value, _then);

  /// Create a copy of DiscipleshipReport
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? journeyId = null,
    Object? journeyName = null,
    Object? totalDisciples = null,
    Object? activeDisciples = null,
    Object? completedDisciples = null,
    Object? stalledDisciples = null,
    Object? averageCompletion = null,
    Object? totalMeetings = null,
    Object? completedMeetings = null,
    Object? stageDistribution = null,
    Object? statusDistribution = null,
    Object? topMentors = null,
    Object? generatedAt = null,
  }) {
    return _then(_$DiscipleshipReportImpl(
      journeyId: null == journeyId
          ? _value.journeyId
          : journeyId // ignore: cast_nullable_to_non_nullable
              as int,
      journeyName: null == journeyName
          ? _value.journeyName
          : journeyName // ignore: cast_nullable_to_non_nullable
              as String,
      totalDisciples: null == totalDisciples
          ? _value.totalDisciples
          : totalDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      activeDisciples: null == activeDisciples
          ? _value.activeDisciples
          : activeDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      completedDisciples: null == completedDisciples
          ? _value.completedDisciples
          : completedDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      stalledDisciples: null == stalledDisciples
          ? _value.stalledDisciples
          : stalledDisciples // ignore: cast_nullable_to_non_nullable
              as int,
      averageCompletion: null == averageCompletion
          ? _value.averageCompletion
          : averageCompletion // ignore: cast_nullable_to_non_nullable
              as double,
      totalMeetings: null == totalMeetings
          ? _value.totalMeetings
          : totalMeetings // ignore: cast_nullable_to_non_nullable
              as int,
      completedMeetings: null == completedMeetings
          ? _value.completedMeetings
          : completedMeetings // ignore: cast_nullable_to_non_nullable
              as int,
      stageDistribution: null == stageDistribution
          ? _value._stageDistribution
          : stageDistribution // ignore: cast_nullable_to_non_nullable
              as Map<String, int>,
      statusDistribution: null == statusDistribution
          ? _value._statusDistribution
          : statusDistribution // ignore: cast_nullable_to_non_nullable
              as Map<String, int>,
      topMentors: null == topMentors
          ? _value._topMentors
          : topMentors // ignore: cast_nullable_to_non_nullable
              as List<TopMentor>,
      generatedAt: null == generatedAt
          ? _value.generatedAt
          : generatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DiscipleshipReportImpl implements _DiscipleshipReport {
  const _$DiscipleshipReportImpl(
      {required this.journeyId,
      required this.journeyName,
      required this.totalDisciples,
      required this.activeDisciples,
      required this.completedDisciples,
      required this.stalledDisciples,
      required this.averageCompletion,
      required this.totalMeetings,
      required this.completedMeetings,
      required final Map<String, int> stageDistribution,
      required final Map<String, int> statusDistribution,
      required final List<TopMentor> topMentors,
      required this.generatedAt})
      : _stageDistribution = stageDistribution,
        _statusDistribution = statusDistribution,
        _topMentors = topMentors;

  factory _$DiscipleshipReportImpl.fromJson(Map<String, dynamic> json) =>
      _$$DiscipleshipReportImplFromJson(json);

  @override
  final int journeyId;
  @override
  final String journeyName;
  @override
  final int totalDisciples;
  @override
  final int activeDisciples;
  @override
  final int completedDisciples;
  @override
  final int stalledDisciples;
  @override
  final double averageCompletion;
  @override
  final int totalMeetings;
  @override
  final int completedMeetings;
  final Map<String, int> _stageDistribution;
  @override
  Map<String, int> get stageDistribution {
    if (_stageDistribution is EqualUnmodifiableMapView)
      return _stageDistribution;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(_stageDistribution);
  }

  final Map<String, int> _statusDistribution;
  @override
  Map<String, int> get statusDistribution {
    if (_statusDistribution is EqualUnmodifiableMapView)
      return _statusDistribution;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(_statusDistribution);
  }

  final List<TopMentor> _topMentors;
  @override
  List<TopMentor> get topMentors {
    if (_topMentors is EqualUnmodifiableListView) return _topMentors;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_topMentors);
  }

  @override
  final DateTime generatedAt;

  @override
  String toString() {
    return 'DiscipleshipReport(journeyId: $journeyId, journeyName: $journeyName, totalDisciples: $totalDisciples, activeDisciples: $activeDisciples, completedDisciples: $completedDisciples, stalledDisciples: $stalledDisciples, averageCompletion: $averageCompletion, totalMeetings: $totalMeetings, completedMeetings: $completedMeetings, stageDistribution: $stageDistribution, statusDistribution: $statusDistribution, topMentors: $topMentors, generatedAt: $generatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DiscipleshipReportImpl &&
            (identical(other.journeyId, journeyId) ||
                other.journeyId == journeyId) &&
            (identical(other.journeyName, journeyName) ||
                other.journeyName == journeyName) &&
            (identical(other.totalDisciples, totalDisciples) ||
                other.totalDisciples == totalDisciples) &&
            (identical(other.activeDisciples, activeDisciples) ||
                other.activeDisciples == activeDisciples) &&
            (identical(other.completedDisciples, completedDisciples) ||
                other.completedDisciples == completedDisciples) &&
            (identical(other.stalledDisciples, stalledDisciples) ||
                other.stalledDisciples == stalledDisciples) &&
            (identical(other.averageCompletion, averageCompletion) ||
                other.averageCompletion == averageCompletion) &&
            (identical(other.totalMeetings, totalMeetings) ||
                other.totalMeetings == totalMeetings) &&
            (identical(other.completedMeetings, completedMeetings) ||
                other.completedMeetings == completedMeetings) &&
            const DeepCollectionEquality()
                .equals(other._stageDistribution, _stageDistribution) &&
            const DeepCollectionEquality()
                .equals(other._statusDistribution, _statusDistribution) &&
            const DeepCollectionEquality()
                .equals(other._topMentors, _topMentors) &&
            (identical(other.generatedAt, generatedAt) ||
                other.generatedAt == generatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      journeyId,
      journeyName,
      totalDisciples,
      activeDisciples,
      completedDisciples,
      stalledDisciples,
      averageCompletion,
      totalMeetings,
      completedMeetings,
      const DeepCollectionEquality().hash(_stageDistribution),
      const DeepCollectionEquality().hash(_statusDistribution),
      const DeepCollectionEquality().hash(_topMentors),
      generatedAt);

  /// Create a copy of DiscipleshipReport
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DiscipleshipReportImplCopyWith<_$DiscipleshipReportImpl> get copyWith =>
      __$$DiscipleshipReportImplCopyWithImpl<_$DiscipleshipReportImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DiscipleshipReportImplToJson(
      this,
    );
  }
}

abstract class _DiscipleshipReport implements DiscipleshipReport {
  const factory _DiscipleshipReport(
      {required final int journeyId,
      required final String journeyName,
      required final int totalDisciples,
      required final int activeDisciples,
      required final int completedDisciples,
      required final int stalledDisciples,
      required final double averageCompletion,
      required final int totalMeetings,
      required final int completedMeetings,
      required final Map<String, int> stageDistribution,
      required final Map<String, int> statusDistribution,
      required final List<TopMentor> topMentors,
      required final DateTime generatedAt}) = _$DiscipleshipReportImpl;

  factory _DiscipleshipReport.fromJson(Map<String, dynamic> json) =
      _$DiscipleshipReportImpl.fromJson;

  @override
  int get journeyId;
  @override
  String get journeyName;
  @override
  int get totalDisciples;
  @override
  int get activeDisciples;
  @override
  int get completedDisciples;
  @override
  int get stalledDisciples;
  @override
  double get averageCompletion;
  @override
  int get totalMeetings;
  @override
  int get completedMeetings;
  @override
  Map<String, int> get stageDistribution;
  @override
  Map<String, int> get statusDistribution;
  @override
  List<TopMentor> get topMentors;
  @override
  DateTime get generatedAt;

  /// Create a copy of DiscipleshipReport
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DiscipleshipReportImplCopyWith<_$DiscipleshipReportImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TopMentor _$TopMentorFromJson(Map<String, dynamic> json) {
  return _TopMentor.fromJson(json);
}

/// @nodoc
mixin _$TopMentor {
  int get mentorId => throw _privateConstructorUsedError;
  String get mentorName => throw _privateConstructorUsedError;
  int get discipleCount => throw _privateConstructorUsedError;
  int get meetingCount => throw _privateConstructorUsedError;
  double get averageCompletion => throw _privateConstructorUsedError;

  /// Serializes this TopMentor to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TopMentor
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TopMentorCopyWith<TopMentor> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TopMentorCopyWith<$Res> {
  factory $TopMentorCopyWith(TopMentor value, $Res Function(TopMentor) then) =
      _$TopMentorCopyWithImpl<$Res, TopMentor>;
  @useResult
  $Res call(
      {int mentorId,
      String mentorName,
      int discipleCount,
      int meetingCount,
      double averageCompletion});
}

/// @nodoc
class _$TopMentorCopyWithImpl<$Res, $Val extends TopMentor>
    implements $TopMentorCopyWith<$Res> {
  _$TopMentorCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TopMentor
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? mentorId = null,
    Object? mentorName = null,
    Object? discipleCount = null,
    Object? meetingCount = null,
    Object? averageCompletion = null,
  }) {
    return _then(_value.copyWith(
      mentorId: null == mentorId
          ? _value.mentorId
          : mentorId // ignore: cast_nullable_to_non_nullable
              as int,
      mentorName: null == mentorName
          ? _value.mentorName
          : mentorName // ignore: cast_nullable_to_non_nullable
              as String,
      discipleCount: null == discipleCount
          ? _value.discipleCount
          : discipleCount // ignore: cast_nullable_to_non_nullable
              as int,
      meetingCount: null == meetingCount
          ? _value.meetingCount
          : meetingCount // ignore: cast_nullable_to_non_nullable
              as int,
      averageCompletion: null == averageCompletion
          ? _value.averageCompletion
          : averageCompletion // ignore: cast_nullable_to_non_nullable
              as double,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TopMentorImplCopyWith<$Res>
    implements $TopMentorCopyWith<$Res> {
  factory _$$TopMentorImplCopyWith(
          _$TopMentorImpl value, $Res Function(_$TopMentorImpl) then) =
      __$$TopMentorImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int mentorId,
      String mentorName,
      int discipleCount,
      int meetingCount,
      double averageCompletion});
}

/// @nodoc
class __$$TopMentorImplCopyWithImpl<$Res>
    extends _$TopMentorCopyWithImpl<$Res, _$TopMentorImpl>
    implements _$$TopMentorImplCopyWith<$Res> {
  __$$TopMentorImplCopyWithImpl(
      _$TopMentorImpl _value, $Res Function(_$TopMentorImpl) _then)
      : super(_value, _then);

  /// Create a copy of TopMentor
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? mentorId = null,
    Object? mentorName = null,
    Object? discipleCount = null,
    Object? meetingCount = null,
    Object? averageCompletion = null,
  }) {
    return _then(_$TopMentorImpl(
      mentorId: null == mentorId
          ? _value.mentorId
          : mentorId // ignore: cast_nullable_to_non_nullable
              as int,
      mentorName: null == mentorName
          ? _value.mentorName
          : mentorName // ignore: cast_nullable_to_non_nullable
              as String,
      discipleCount: null == discipleCount
          ? _value.discipleCount
          : discipleCount // ignore: cast_nullable_to_non_nullable
              as int,
      meetingCount: null == meetingCount
          ? _value.meetingCount
          : meetingCount // ignore: cast_nullable_to_non_nullable
              as int,
      averageCompletion: null == averageCompletion
          ? _value.averageCompletion
          : averageCompletion // ignore: cast_nullable_to_non_nullable
              as double,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TopMentorImpl implements _TopMentor {
  const _$TopMentorImpl(
      {required this.mentorId,
      required this.mentorName,
      required this.discipleCount,
      required this.meetingCount,
      required this.averageCompletion});

  factory _$TopMentorImpl.fromJson(Map<String, dynamic> json) =>
      _$$TopMentorImplFromJson(json);

  @override
  final int mentorId;
  @override
  final String mentorName;
  @override
  final int discipleCount;
  @override
  final int meetingCount;
  @override
  final double averageCompletion;

  @override
  String toString() {
    return 'TopMentor(mentorId: $mentorId, mentorName: $mentorName, discipleCount: $discipleCount, meetingCount: $meetingCount, averageCompletion: $averageCompletion)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TopMentorImpl &&
            (identical(other.mentorId, mentorId) ||
                other.mentorId == mentorId) &&
            (identical(other.mentorName, mentorName) ||
                other.mentorName == mentorName) &&
            (identical(other.discipleCount, discipleCount) ||
                other.discipleCount == discipleCount) &&
            (identical(other.meetingCount, meetingCount) ||
                other.meetingCount == meetingCount) &&
            (identical(other.averageCompletion, averageCompletion) ||
                other.averageCompletion == averageCompletion));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, mentorId, mentorName,
      discipleCount, meetingCount, averageCompletion);

  /// Create a copy of TopMentor
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TopMentorImplCopyWith<_$TopMentorImpl> get copyWith =>
      __$$TopMentorImplCopyWithImpl<_$TopMentorImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TopMentorImplToJson(
      this,
    );
  }
}

abstract class _TopMentor implements TopMentor {
  const factory _TopMentor(
      {required final int mentorId,
      required final String mentorName,
      required final int discipleCount,
      required final int meetingCount,
      required final double averageCompletion}) = _$TopMentorImpl;

  factory _TopMentor.fromJson(Map<String, dynamic> json) =
      _$TopMentorImpl.fromJson;

  @override
  int get mentorId;
  @override
  String get mentorName;
  @override
  int get discipleCount;
  @override
  int get meetingCount;
  @override
  double get averageCompletion;

  /// Create a copy of TopMentor
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TopMentorImplCopyWith<_$TopMentorImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
