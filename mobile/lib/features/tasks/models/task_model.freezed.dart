// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'task_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Task _$TaskFromJson(Map<String, dynamic> json) {
  return _Task.fromJson(json);
}

/// @nodoc
mixin _$Task {
  int get id => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  TaskType get type => throw _privateConstructorUsedError;
  TaskPriority get priority => throw _privateConstructorUsedError;
  TaskStatus get status => throw _privateConstructorUsedError;
  int? get projectId => throw _privateConstructorUsedError;
  String? get projectName => throw _privateConstructorUsedError;
  int? get assignedToId => throw _privateConstructorUsedError;
  String? get assignedToName => throw _privateConstructorUsedError;
  int? get assignedById => throw _privateConstructorUsedError;
  String? get assignedByName => throw _privateConstructorUsedError;
  int? get departmentId => throw _privateConstructorUsedError;
  String? get departmentName => throw _privateConstructorUsedError;
  DateTime? get dueDate => throw _privateConstructorUsedError;
  DateTime? get startDate => throw _privateConstructorUsedError;
  DateTime? get completedDate => throw _privateConstructorUsedError;
  int? get estimatedHours => throw _privateConstructorUsedError;
  int? get actualHours => throw _privateConstructorUsedError;
  List<String>? get tags => throw _privateConstructorUsedError;
  List<TaskAttachment>? get attachments => throw _privateConstructorUsedError;
  List<TaskComment>? get comments => throw _privateConstructorUsedError;
  List<TaskDependency>? get dependencies => throw _privateConstructorUsedError;
  int? get parentTaskId => throw _privateConstructorUsedError;
  String? get parentTaskTitle => throw _privateConstructorUsedError;
  int? get recurrenceRuleId => throw _privateConstructorUsedError;
  String? get recurrencePattern => throw _privateConstructorUsedError;
  DateTime? get recurrenceEndDate => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this Task to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Task
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskCopyWith<Task> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskCopyWith<$Res> {
  factory $TaskCopyWith(Task value, $Res Function(Task) then) =
      _$TaskCopyWithImpl<$Res, Task>;
  @useResult
  $Res call(
      {int id,
      String title,
      String? description,
      TaskType type,
      TaskPriority priority,
      TaskStatus status,
      int? projectId,
      String? projectName,
      int? assignedToId,
      String? assignedToName,
      int? assignedById,
      String? assignedByName,
      int? departmentId,
      String? departmentName,
      DateTime? dueDate,
      DateTime? startDate,
      DateTime? completedDate,
      int? estimatedHours,
      int? actualHours,
      List<String>? tags,
      List<TaskAttachment>? attachments,
      List<TaskComment>? comments,
      List<TaskDependency>? dependencies,
      int? parentTaskId,
      String? parentTaskTitle,
      int? recurrenceRuleId,
      String? recurrencePattern,
      DateTime? recurrenceEndDate,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$TaskCopyWithImpl<$Res, $Val extends Task>
    implements $TaskCopyWith<$Res> {
  _$TaskCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Task
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? title = null,
    Object? description = freezed,
    Object? type = null,
    Object? priority = null,
    Object? status = null,
    Object? projectId = freezed,
    Object? projectName = freezed,
    Object? assignedToId = freezed,
    Object? assignedToName = freezed,
    Object? assignedById = freezed,
    Object? assignedByName = freezed,
    Object? departmentId = freezed,
    Object? departmentName = freezed,
    Object? dueDate = freezed,
    Object? startDate = freezed,
    Object? completedDate = freezed,
    Object? estimatedHours = freezed,
    Object? actualHours = freezed,
    Object? tags = freezed,
    Object? attachments = freezed,
    Object? comments = freezed,
    Object? dependencies = freezed,
    Object? parentTaskId = freezed,
    Object? parentTaskTitle = freezed,
    Object? recurrenceRuleId = freezed,
    Object? recurrencePattern = freezed,
    Object? recurrenceEndDate = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
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
              as TaskType,
      priority: null == priority
          ? _value.priority
          : priority // ignore: cast_nullable_to_non_nullable
              as TaskPriority,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TaskStatus,
      projectId: freezed == projectId
          ? _value.projectId
          : projectId // ignore: cast_nullable_to_non_nullable
              as int?,
      projectName: freezed == projectName
          ? _value.projectName
          : projectName // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedToId: freezed == assignedToId
          ? _value.assignedToId
          : assignedToId // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedToName: freezed == assignedToName
          ? _value.assignedToName
          : assignedToName // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedById: freezed == assignedById
          ? _value.assignedById
          : assignedById // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedByName: freezed == assignedByName
          ? _value.assignedByName
          : assignedByName // ignore: cast_nullable_to_non_nullable
              as String?,
      departmentId: freezed == departmentId
          ? _value.departmentId
          : departmentId // ignore: cast_nullable_to_non_nullable
              as int?,
      departmentName: freezed == departmentName
          ? _value.departmentName
          : departmentName // ignore: cast_nullable_to_non_nullable
              as String?,
      dueDate: freezed == dueDate
          ? _value.dueDate
          : dueDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      startDate: freezed == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      completedDate: freezed == completedDate
          ? _value.completedDate
          : completedDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      estimatedHours: freezed == estimatedHours
          ? _value.estimatedHours
          : estimatedHours // ignore: cast_nullable_to_non_nullable
              as int?,
      actualHours: freezed == actualHours
          ? _value.actualHours
          : actualHours // ignore: cast_nullable_to_non_nullable
              as int?,
      tags: freezed == tags
          ? _value.tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      attachments: freezed == attachments
          ? _value.attachments
          : attachments // ignore: cast_nullable_to_non_nullable
              as List<TaskAttachment>?,
      comments: freezed == comments
          ? _value.comments
          : comments // ignore: cast_nullable_to_non_nullable
              as List<TaskComment>?,
      dependencies: freezed == dependencies
          ? _value.dependencies
          : dependencies // ignore: cast_nullable_to_non_nullable
              as List<TaskDependency>?,
      parentTaskId: freezed == parentTaskId
          ? _value.parentTaskId
          : parentTaskId // ignore: cast_nullable_to_non_nullable
              as int?,
      parentTaskTitle: freezed == parentTaskTitle
          ? _value.parentTaskTitle
          : parentTaskTitle // ignore: cast_nullable_to_non_nullable
              as String?,
      recurrenceRuleId: freezed == recurrenceRuleId
          ? _value.recurrenceRuleId
          : recurrenceRuleId // ignore: cast_nullable_to_non_nullable
              as int?,
      recurrencePattern: freezed == recurrencePattern
          ? _value.recurrencePattern
          : recurrencePattern // ignore: cast_nullable_to_non_nullable
              as String?,
      recurrenceEndDate: freezed == recurrenceEndDate
          ? _value.recurrenceEndDate
          : recurrenceEndDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
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
abstract class _$$TaskImplCopyWith<$Res> implements $TaskCopyWith<$Res> {
  factory _$$TaskImplCopyWith(
          _$TaskImpl value, $Res Function(_$TaskImpl) then) =
      __$$TaskImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String title,
      String? description,
      TaskType type,
      TaskPriority priority,
      TaskStatus status,
      int? projectId,
      String? projectName,
      int? assignedToId,
      String? assignedToName,
      int? assignedById,
      String? assignedByName,
      int? departmentId,
      String? departmentName,
      DateTime? dueDate,
      DateTime? startDate,
      DateTime? completedDate,
      int? estimatedHours,
      int? actualHours,
      List<String>? tags,
      List<TaskAttachment>? attachments,
      List<TaskComment>? comments,
      List<TaskDependency>? dependencies,
      int? parentTaskId,
      String? parentTaskTitle,
      int? recurrenceRuleId,
      String? recurrencePattern,
      DateTime? recurrenceEndDate,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$TaskImplCopyWithImpl<$Res>
    extends _$TaskCopyWithImpl<$Res, _$TaskImpl>
    implements _$$TaskImplCopyWith<$Res> {
  __$$TaskImplCopyWithImpl(_$TaskImpl _value, $Res Function(_$TaskImpl) _then)
      : super(_value, _then);

  /// Create a copy of Task
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? title = null,
    Object? description = freezed,
    Object? type = null,
    Object? priority = null,
    Object? status = null,
    Object? projectId = freezed,
    Object? projectName = freezed,
    Object? assignedToId = freezed,
    Object? assignedToName = freezed,
    Object? assignedById = freezed,
    Object? assignedByName = freezed,
    Object? departmentId = freezed,
    Object? departmentName = freezed,
    Object? dueDate = freezed,
    Object? startDate = freezed,
    Object? completedDate = freezed,
    Object? estimatedHours = freezed,
    Object? actualHours = freezed,
    Object? tags = freezed,
    Object? attachments = freezed,
    Object? comments = freezed,
    Object? dependencies = freezed,
    Object? parentTaskId = freezed,
    Object? parentTaskTitle = freezed,
    Object? recurrenceRuleId = freezed,
    Object? recurrencePattern = freezed,
    Object? recurrenceEndDate = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$TaskImpl(
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
              as TaskType,
      priority: null == priority
          ? _value.priority
          : priority // ignore: cast_nullable_to_non_nullable
              as TaskPriority,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TaskStatus,
      projectId: freezed == projectId
          ? _value.projectId
          : projectId // ignore: cast_nullable_to_non_nullable
              as int?,
      projectName: freezed == projectName
          ? _value.projectName
          : projectName // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedToId: freezed == assignedToId
          ? _value.assignedToId
          : assignedToId // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedToName: freezed == assignedToName
          ? _value.assignedToName
          : assignedToName // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedById: freezed == assignedById
          ? _value.assignedById
          : assignedById // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedByName: freezed == assignedByName
          ? _value.assignedByName
          : assignedByName // ignore: cast_nullable_to_non_nullable
              as String?,
      departmentId: freezed == departmentId
          ? _value.departmentId
          : departmentId // ignore: cast_nullable_to_non_nullable
              as int?,
      departmentName: freezed == departmentName
          ? _value.departmentName
          : departmentName // ignore: cast_nullable_to_non_nullable
              as String?,
      dueDate: freezed == dueDate
          ? _value.dueDate
          : dueDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      startDate: freezed == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      completedDate: freezed == completedDate
          ? _value.completedDate
          : completedDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      estimatedHours: freezed == estimatedHours
          ? _value.estimatedHours
          : estimatedHours // ignore: cast_nullable_to_non_nullable
              as int?,
      actualHours: freezed == actualHours
          ? _value.actualHours
          : actualHours // ignore: cast_nullable_to_non_nullable
              as int?,
      tags: freezed == tags
          ? _value._tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      attachments: freezed == attachments
          ? _value._attachments
          : attachments // ignore: cast_nullable_to_non_nullable
              as List<TaskAttachment>?,
      comments: freezed == comments
          ? _value._comments
          : comments // ignore: cast_nullable_to_non_nullable
              as List<TaskComment>?,
      dependencies: freezed == dependencies
          ? _value._dependencies
          : dependencies // ignore: cast_nullable_to_non_nullable
              as List<TaskDependency>?,
      parentTaskId: freezed == parentTaskId
          ? _value.parentTaskId
          : parentTaskId // ignore: cast_nullable_to_non_nullable
              as int?,
      parentTaskTitle: freezed == parentTaskTitle
          ? _value.parentTaskTitle
          : parentTaskTitle // ignore: cast_nullable_to_non_nullable
              as String?,
      recurrenceRuleId: freezed == recurrenceRuleId
          ? _value.recurrenceRuleId
          : recurrenceRuleId // ignore: cast_nullable_to_non_nullable
              as int?,
      recurrencePattern: freezed == recurrencePattern
          ? _value.recurrencePattern
          : recurrencePattern // ignore: cast_nullable_to_non_nullable
              as String?,
      recurrenceEndDate: freezed == recurrenceEndDate
          ? _value.recurrenceEndDate
          : recurrenceEndDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
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
class _$TaskImpl extends _Task {
  const _$TaskImpl(
      {required this.id,
      required this.title,
      this.description,
      required this.type,
      required this.priority,
      required this.status,
      this.projectId,
      this.projectName,
      this.assignedToId,
      this.assignedToName,
      this.assignedById,
      this.assignedByName,
      this.departmentId,
      this.departmentName,
      this.dueDate,
      this.startDate,
      this.completedDate,
      this.estimatedHours,
      this.actualHours,
      final List<String>? tags,
      final List<TaskAttachment>? attachments,
      final List<TaskComment>? comments,
      final List<TaskDependency>? dependencies,
      this.parentTaskId,
      this.parentTaskTitle,
      this.recurrenceRuleId,
      this.recurrencePattern,
      this.recurrenceEndDate,
      required this.createdAt,
      this.updatedAt})
      : _tags = tags,
        _attachments = attachments,
        _comments = comments,
        _dependencies = dependencies,
        super._();

  factory _$TaskImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskImplFromJson(json);

  @override
  final int id;
  @override
  final String title;
  @override
  final String? description;
  @override
  final TaskType type;
  @override
  final TaskPriority priority;
  @override
  final TaskStatus status;
  @override
  final int? projectId;
  @override
  final String? projectName;
  @override
  final int? assignedToId;
  @override
  final String? assignedToName;
  @override
  final int? assignedById;
  @override
  final String? assignedByName;
  @override
  final int? departmentId;
  @override
  final String? departmentName;
  @override
  final DateTime? dueDate;
  @override
  final DateTime? startDate;
  @override
  final DateTime? completedDate;
  @override
  final int? estimatedHours;
  @override
  final int? actualHours;
  final List<String>? _tags;
  @override
  List<String>? get tags {
    final value = _tags;
    if (value == null) return null;
    if (_tags is EqualUnmodifiableListView) return _tags;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<TaskAttachment>? _attachments;
  @override
  List<TaskAttachment>? get attachments {
    final value = _attachments;
    if (value == null) return null;
    if (_attachments is EqualUnmodifiableListView) return _attachments;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<TaskComment>? _comments;
  @override
  List<TaskComment>? get comments {
    final value = _comments;
    if (value == null) return null;
    if (_comments is EqualUnmodifiableListView) return _comments;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<TaskDependency>? _dependencies;
  @override
  List<TaskDependency>? get dependencies {
    final value = _dependencies;
    if (value == null) return null;
    if (_dependencies is EqualUnmodifiableListView) return _dependencies;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final int? parentTaskId;
  @override
  final String? parentTaskTitle;
  @override
  final int? recurrenceRuleId;
  @override
  final String? recurrencePattern;
  @override
  final DateTime? recurrenceEndDate;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'Task(id: $id, title: $title, description: $description, type: $type, priority: $priority, status: $status, projectId: $projectId, projectName: $projectName, assignedToId: $assignedToId, assignedToName: $assignedToName, assignedById: $assignedById, assignedByName: $assignedByName, departmentId: $departmentId, departmentName: $departmentName, dueDate: $dueDate, startDate: $startDate, completedDate: $completedDate, estimatedHours: $estimatedHours, actualHours: $actualHours, tags: $tags, attachments: $attachments, comments: $comments, dependencies: $dependencies, parentTaskId: $parentTaskId, parentTaskTitle: $parentTaskTitle, recurrenceRuleId: $recurrenceRuleId, recurrencePattern: $recurrencePattern, recurrenceEndDate: $recurrenceEndDate, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.priority, priority) ||
                other.priority == priority) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.projectId, projectId) ||
                other.projectId == projectId) &&
            (identical(other.projectName, projectName) ||
                other.projectName == projectName) &&
            (identical(other.assignedToId, assignedToId) ||
                other.assignedToId == assignedToId) &&
            (identical(other.assignedToName, assignedToName) ||
                other.assignedToName == assignedToName) &&
            (identical(other.assignedById, assignedById) ||
                other.assignedById == assignedById) &&
            (identical(other.assignedByName, assignedByName) ||
                other.assignedByName == assignedByName) &&
            (identical(other.departmentId, departmentId) ||
                other.departmentId == departmentId) &&
            (identical(other.departmentName, departmentName) ||
                other.departmentName == departmentName) &&
            (identical(other.dueDate, dueDate) || other.dueDate == dueDate) &&
            (identical(other.startDate, startDate) ||
                other.startDate == startDate) &&
            (identical(other.completedDate, completedDate) ||
                other.completedDate == completedDate) &&
            (identical(other.estimatedHours, estimatedHours) ||
                other.estimatedHours == estimatedHours) &&
            (identical(other.actualHours, actualHours) ||
                other.actualHours == actualHours) &&
            const DeepCollectionEquality().equals(other._tags, _tags) &&
            const DeepCollectionEquality()
                .equals(other._attachments, _attachments) &&
            const DeepCollectionEquality().equals(other._comments, _comments) &&
            const DeepCollectionEquality()
                .equals(other._dependencies, _dependencies) &&
            (identical(other.parentTaskId, parentTaskId) ||
                other.parentTaskId == parentTaskId) &&
            (identical(other.parentTaskTitle, parentTaskTitle) ||
                other.parentTaskTitle == parentTaskTitle) &&
            (identical(other.recurrenceRuleId, recurrenceRuleId) ||
                other.recurrenceRuleId == recurrenceRuleId) &&
            (identical(other.recurrencePattern, recurrencePattern) ||
                other.recurrencePattern == recurrencePattern) &&
            (identical(other.recurrenceEndDate, recurrenceEndDate) ||
                other.recurrenceEndDate == recurrenceEndDate) &&
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
        title,
        description,
        type,
        priority,
        status,
        projectId,
        projectName,
        assignedToId,
        assignedToName,
        assignedById,
        assignedByName,
        departmentId,
        departmentName,
        dueDate,
        startDate,
        completedDate,
        estimatedHours,
        actualHours,
        const DeepCollectionEquality().hash(_tags),
        const DeepCollectionEquality().hash(_attachments),
        const DeepCollectionEquality().hash(_comments),
        const DeepCollectionEquality().hash(_dependencies),
        parentTaskId,
        parentTaskTitle,
        recurrenceRuleId,
        recurrencePattern,
        recurrenceEndDate,
        createdAt,
        updatedAt
      ]);

  /// Create a copy of Task
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskImplCopyWith<_$TaskImpl> get copyWith =>
      __$$TaskImplCopyWithImpl<_$TaskImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskImplToJson(
      this,
    );
  }
}

abstract class _Task extends Task {
  const factory _Task(
      {required final int id,
      required final String title,
      final String? description,
      required final TaskType type,
      required final TaskPriority priority,
      required final TaskStatus status,
      final int? projectId,
      final String? projectName,
      final int? assignedToId,
      final String? assignedToName,
      final int? assignedById,
      final String? assignedByName,
      final int? departmentId,
      final String? departmentName,
      final DateTime? dueDate,
      final DateTime? startDate,
      final DateTime? completedDate,
      final int? estimatedHours,
      final int? actualHours,
      final List<String>? tags,
      final List<TaskAttachment>? attachments,
      final List<TaskComment>? comments,
      final List<TaskDependency>? dependencies,
      final int? parentTaskId,
      final String? parentTaskTitle,
      final int? recurrenceRuleId,
      final String? recurrencePattern,
      final DateTime? recurrenceEndDate,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$TaskImpl;
  const _Task._() : super._();

  factory _Task.fromJson(Map<String, dynamic> json) = _$TaskImpl.fromJson;

  @override
  int get id;
  @override
  String get title;
  @override
  String? get description;
  @override
  TaskType get type;
  @override
  TaskPriority get priority;
  @override
  TaskStatus get status;
  @override
  int? get projectId;
  @override
  String? get projectName;
  @override
  int? get assignedToId;
  @override
  String? get assignedToName;
  @override
  int? get assignedById;
  @override
  String? get assignedByName;
  @override
  int? get departmentId;
  @override
  String? get departmentName;
  @override
  DateTime? get dueDate;
  @override
  DateTime? get startDate;
  @override
  DateTime? get completedDate;
  @override
  int? get estimatedHours;
  @override
  int? get actualHours;
  @override
  List<String>? get tags;
  @override
  List<TaskAttachment>? get attachments;
  @override
  List<TaskComment>? get comments;
  @override
  List<TaskDependency>? get dependencies;
  @override
  int? get parentTaskId;
  @override
  String? get parentTaskTitle;
  @override
  int? get recurrenceRuleId;
  @override
  String? get recurrencePattern;
  @override
  DateTime? get recurrenceEndDate;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of Task
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskImplCopyWith<_$TaskImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TaskAttachment _$TaskAttachmentFromJson(Map<String, dynamic> json) {
  return _TaskAttachment.fromJson(json);
}

/// @nodoc
mixin _$TaskAttachment {
  int get id => throw _privateConstructorUsedError;
  int get taskId => throw _privateConstructorUsedError;
  String get fileName => throw _privateConstructorUsedError;
  String get fileUrl => throw _privateConstructorUsedError;
  String get mimeType => throw _privateConstructorUsedError;
  int get fileSize => throw _privateConstructorUsedError;
  int? get uploadedById => throw _privateConstructorUsedError;
  String? get uploadedByName => throw _privateConstructorUsedError;
  DateTime get uploadedAt => throw _privateConstructorUsedError;

  /// Serializes this TaskAttachment to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TaskAttachment
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskAttachmentCopyWith<TaskAttachment> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskAttachmentCopyWith<$Res> {
  factory $TaskAttachmentCopyWith(
          TaskAttachment value, $Res Function(TaskAttachment) then) =
      _$TaskAttachmentCopyWithImpl<$Res, TaskAttachment>;
  @useResult
  $Res call(
      {int id,
      int taskId,
      String fileName,
      String fileUrl,
      String mimeType,
      int fileSize,
      int? uploadedById,
      String? uploadedByName,
      DateTime uploadedAt});
}

/// @nodoc
class _$TaskAttachmentCopyWithImpl<$Res, $Val extends TaskAttachment>
    implements $TaskAttachmentCopyWith<$Res> {
  _$TaskAttachmentCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TaskAttachment
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? fileName = null,
    Object? fileUrl = null,
    Object? mimeType = null,
    Object? fileSize = null,
    Object? uploadedById = freezed,
    Object? uploadedByName = freezed,
    Object? uploadedAt = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      fileName: null == fileName
          ? _value.fileName
          : fileName // ignore: cast_nullable_to_non_nullable
              as String,
      fileUrl: null == fileUrl
          ? _value.fileUrl
          : fileUrl // ignore: cast_nullable_to_non_nullable
              as String,
      mimeType: null == mimeType
          ? _value.mimeType
          : mimeType // ignore: cast_nullable_to_non_nullable
              as String,
      fileSize: null == fileSize
          ? _value.fileSize
          : fileSize // ignore: cast_nullable_to_non_nullable
              as int,
      uploadedById: freezed == uploadedById
          ? _value.uploadedById
          : uploadedById // ignore: cast_nullable_to_non_nullable
              as int?,
      uploadedByName: freezed == uploadedByName
          ? _value.uploadedByName
          : uploadedByName // ignore: cast_nullable_to_non_nullable
              as String?,
      uploadedAt: null == uploadedAt
          ? _value.uploadedAt
          : uploadedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TaskAttachmentImplCopyWith<$Res>
    implements $TaskAttachmentCopyWith<$Res> {
  factory _$$TaskAttachmentImplCopyWith(_$TaskAttachmentImpl value,
          $Res Function(_$TaskAttachmentImpl) then) =
      __$$TaskAttachmentImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int taskId,
      String fileName,
      String fileUrl,
      String mimeType,
      int fileSize,
      int? uploadedById,
      String? uploadedByName,
      DateTime uploadedAt});
}

/// @nodoc
class __$$TaskAttachmentImplCopyWithImpl<$Res>
    extends _$TaskAttachmentCopyWithImpl<$Res, _$TaskAttachmentImpl>
    implements _$$TaskAttachmentImplCopyWith<$Res> {
  __$$TaskAttachmentImplCopyWithImpl(
      _$TaskAttachmentImpl _value, $Res Function(_$TaskAttachmentImpl) _then)
      : super(_value, _then);

  /// Create a copy of TaskAttachment
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? fileName = null,
    Object? fileUrl = null,
    Object? mimeType = null,
    Object? fileSize = null,
    Object? uploadedById = freezed,
    Object? uploadedByName = freezed,
    Object? uploadedAt = null,
  }) {
    return _then(_$TaskAttachmentImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      fileName: null == fileName
          ? _value.fileName
          : fileName // ignore: cast_nullable_to_non_nullable
              as String,
      fileUrl: null == fileUrl
          ? _value.fileUrl
          : fileUrl // ignore: cast_nullable_to_non_nullable
              as String,
      mimeType: null == mimeType
          ? _value.mimeType
          : mimeType // ignore: cast_nullable_to_non_nullable
              as String,
      fileSize: null == fileSize
          ? _value.fileSize
          : fileSize // ignore: cast_nullable_to_non_nullable
              as int,
      uploadedById: freezed == uploadedById
          ? _value.uploadedById
          : uploadedById // ignore: cast_nullable_to_non_nullable
              as int?,
      uploadedByName: freezed == uploadedByName
          ? _value.uploadedByName
          : uploadedByName // ignore: cast_nullable_to_non_nullable
              as String?,
      uploadedAt: null == uploadedAt
          ? _value.uploadedAt
          : uploadedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TaskAttachmentImpl implements _TaskAttachment {
  const _$TaskAttachmentImpl(
      {required this.id,
      required this.taskId,
      required this.fileName,
      required this.fileUrl,
      required this.mimeType,
      required this.fileSize,
      this.uploadedById,
      this.uploadedByName,
      required this.uploadedAt});

  factory _$TaskAttachmentImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskAttachmentImplFromJson(json);

  @override
  final int id;
  @override
  final int taskId;
  @override
  final String fileName;
  @override
  final String fileUrl;
  @override
  final String mimeType;
  @override
  final int fileSize;
  @override
  final int? uploadedById;
  @override
  final String? uploadedByName;
  @override
  final DateTime uploadedAt;

  @override
  String toString() {
    return 'TaskAttachment(id: $id, taskId: $taskId, fileName: $fileName, fileUrl: $fileUrl, mimeType: $mimeType, fileSize: $fileSize, uploadedById: $uploadedById, uploadedByName: $uploadedByName, uploadedAt: $uploadedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskAttachmentImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.taskId, taskId) || other.taskId == taskId) &&
            (identical(other.fileName, fileName) ||
                other.fileName == fileName) &&
            (identical(other.fileUrl, fileUrl) || other.fileUrl == fileUrl) &&
            (identical(other.mimeType, mimeType) ||
                other.mimeType == mimeType) &&
            (identical(other.fileSize, fileSize) ||
                other.fileSize == fileSize) &&
            (identical(other.uploadedById, uploadedById) ||
                other.uploadedById == uploadedById) &&
            (identical(other.uploadedByName, uploadedByName) ||
                other.uploadedByName == uploadedByName) &&
            (identical(other.uploadedAt, uploadedAt) ||
                other.uploadedAt == uploadedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, taskId, fileName, fileUrl,
      mimeType, fileSize, uploadedById, uploadedByName, uploadedAt);

  /// Create a copy of TaskAttachment
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskAttachmentImplCopyWith<_$TaskAttachmentImpl> get copyWith =>
      __$$TaskAttachmentImplCopyWithImpl<_$TaskAttachmentImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskAttachmentImplToJson(
      this,
    );
  }
}

abstract class _TaskAttachment implements TaskAttachment {
  const factory _TaskAttachment(
      {required final int id,
      required final int taskId,
      required final String fileName,
      required final String fileUrl,
      required final String mimeType,
      required final int fileSize,
      final int? uploadedById,
      final String? uploadedByName,
      required final DateTime uploadedAt}) = _$TaskAttachmentImpl;

  factory _TaskAttachment.fromJson(Map<String, dynamic> json) =
      _$TaskAttachmentImpl.fromJson;

  @override
  int get id;
  @override
  int get taskId;
  @override
  String get fileName;
  @override
  String get fileUrl;
  @override
  String get mimeType;
  @override
  int get fileSize;
  @override
  int? get uploadedById;
  @override
  String? get uploadedByName;
  @override
  DateTime get uploadedAt;

  /// Create a copy of TaskAttachment
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskAttachmentImplCopyWith<_$TaskAttachmentImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TaskComment _$TaskCommentFromJson(Map<String, dynamic> json) {
  return _TaskComment.fromJson(json);
}

/// @nodoc
mixin _$TaskComment {
  int get id => throw _privateConstructorUsedError;
  int get taskId => throw _privateConstructorUsedError;
  int get authorId => throw _privateConstructorUsedError;
  String get authorName => throw _privateConstructorUsedError;
  String get content => throw _privateConstructorUsedError;
  int? get parentCommentId => throw _privateConstructorUsedError;
  String? get parentAuthorName => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;
  bool get isSystem => throw _privateConstructorUsedError;

  /// Serializes this TaskComment to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TaskComment
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskCommentCopyWith<TaskComment> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskCommentCopyWith<$Res> {
  factory $TaskCommentCopyWith(
          TaskComment value, $Res Function(TaskComment) then) =
      _$TaskCommentCopyWithImpl<$Res, TaskComment>;
  @useResult
  $Res call(
      {int id,
      int taskId,
      int authorId,
      String authorName,
      String content,
      int? parentCommentId,
      String? parentAuthorName,
      DateTime createdAt,
      DateTime? updatedAt,
      bool isSystem});
}

/// @nodoc
class _$TaskCommentCopyWithImpl<$Res, $Val extends TaskComment>
    implements $TaskCommentCopyWith<$Res> {
  _$TaskCommentCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TaskComment
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? authorId = null,
    Object? authorName = null,
    Object? content = null,
    Object? parentCommentId = freezed,
    Object? parentAuthorName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isSystem = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      authorId: null == authorId
          ? _value.authorId
          : authorId // ignore: cast_nullable_to_non_nullable
              as int,
      authorName: null == authorName
          ? _value.authorName
          : authorName // ignore: cast_nullable_to_non_nullable
              as String,
      content: null == content
          ? _value.content
          : content // ignore: cast_nullable_to_non_nullable
              as String,
      parentCommentId: freezed == parentCommentId
          ? _value.parentCommentId
          : parentCommentId // ignore: cast_nullable_to_non_nullable
              as int?,
      parentAuthorName: freezed == parentAuthorName
          ? _value.parentAuthorName
          : parentAuthorName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isSystem: null == isSystem
          ? _value.isSystem
          : isSystem // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TaskCommentImplCopyWith<$Res>
    implements $TaskCommentCopyWith<$Res> {
  factory _$$TaskCommentImplCopyWith(
          _$TaskCommentImpl value, $Res Function(_$TaskCommentImpl) then) =
      __$$TaskCommentImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int taskId,
      int authorId,
      String authorName,
      String content,
      int? parentCommentId,
      String? parentAuthorName,
      DateTime createdAt,
      DateTime? updatedAt,
      bool isSystem});
}

/// @nodoc
class __$$TaskCommentImplCopyWithImpl<$Res>
    extends _$TaskCommentCopyWithImpl<$Res, _$TaskCommentImpl>
    implements _$$TaskCommentImplCopyWith<$Res> {
  __$$TaskCommentImplCopyWithImpl(
      _$TaskCommentImpl _value, $Res Function(_$TaskCommentImpl) _then)
      : super(_value, _then);

  /// Create a copy of TaskComment
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? authorId = null,
    Object? authorName = null,
    Object? content = null,
    Object? parentCommentId = freezed,
    Object? parentAuthorName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isSystem = null,
  }) {
    return _then(_$TaskCommentImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      authorId: null == authorId
          ? _value.authorId
          : authorId // ignore: cast_nullable_to_non_nullable
              as int,
      authorName: null == authorName
          ? _value.authorName
          : authorName // ignore: cast_nullable_to_non_nullable
              as String,
      content: null == content
          ? _value.content
          : content // ignore: cast_nullable_to_non_nullable
              as String,
      parentCommentId: freezed == parentCommentId
          ? _value.parentCommentId
          : parentCommentId // ignore: cast_nullable_to_non_nullable
              as int?,
      parentAuthorName: freezed == parentAuthorName
          ? _value.parentAuthorName
          : parentAuthorName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isSystem: null == isSystem
          ? _value.isSystem
          : isSystem // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TaskCommentImpl implements _TaskComment {
  const _$TaskCommentImpl(
      {required this.id,
      required this.taskId,
      required this.authorId,
      required this.authorName,
      required this.content,
      this.parentCommentId,
      this.parentAuthorName,
      required this.createdAt,
      this.updatedAt,
      this.isSystem = false});

  factory _$TaskCommentImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskCommentImplFromJson(json);

  @override
  final int id;
  @override
  final int taskId;
  @override
  final int authorId;
  @override
  final String authorName;
  @override
  final String content;
  @override
  final int? parentCommentId;
  @override
  final String? parentAuthorName;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;
  @override
  @JsonKey()
  final bool isSystem;

  @override
  String toString() {
    return 'TaskComment(id: $id, taskId: $taskId, authorId: $authorId, authorName: $authorName, content: $content, parentCommentId: $parentCommentId, parentAuthorName: $parentAuthorName, createdAt: $createdAt, updatedAt: $updatedAt, isSystem: $isSystem)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskCommentImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.taskId, taskId) || other.taskId == taskId) &&
            (identical(other.authorId, authorId) ||
                other.authorId == authorId) &&
            (identical(other.authorName, authorName) ||
                other.authorName == authorName) &&
            (identical(other.content, content) || other.content == content) &&
            (identical(other.parentCommentId, parentCommentId) ||
                other.parentCommentId == parentCommentId) &&
            (identical(other.parentAuthorName, parentAuthorName) ||
                other.parentAuthorName == parentAuthorName) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt) &&
            (identical(other.isSystem, isSystem) ||
                other.isSystem == isSystem));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      taskId,
      authorId,
      authorName,
      content,
      parentCommentId,
      parentAuthorName,
      createdAt,
      updatedAt,
      isSystem);

  /// Create a copy of TaskComment
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskCommentImplCopyWith<_$TaskCommentImpl> get copyWith =>
      __$$TaskCommentImplCopyWithImpl<_$TaskCommentImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskCommentImplToJson(
      this,
    );
  }
}

abstract class _TaskComment implements TaskComment {
  const factory _TaskComment(
      {required final int id,
      required final int taskId,
      required final int authorId,
      required final String authorName,
      required final String content,
      final int? parentCommentId,
      final String? parentAuthorName,
      required final DateTime createdAt,
      final DateTime? updatedAt,
      final bool isSystem}) = _$TaskCommentImpl;

  factory _TaskComment.fromJson(Map<String, dynamic> json) =
      _$TaskCommentImpl.fromJson;

  @override
  int get id;
  @override
  int get taskId;
  @override
  int get authorId;
  @override
  String get authorName;
  @override
  String get content;
  @override
  int? get parentCommentId;
  @override
  String? get parentAuthorName;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;
  @override
  bool get isSystem;

  /// Create a copy of TaskComment
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskCommentImplCopyWith<_$TaskCommentImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TaskDependency _$TaskDependencyFromJson(Map<String, dynamic> json) {
  return _TaskDependency.fromJson(json);
}

/// @nodoc
mixin _$TaskDependency {
  int get id => throw _privateConstructorUsedError;
  int get taskId => throw _privateConstructorUsedError;
  int get dependsOnTaskId => throw _privateConstructorUsedError;
  String get dependsOnTaskTitle => throw _privateConstructorUsedError;
  DependencyType get type => throw _privateConstructorUsedError;

  /// Serializes this TaskDependency to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TaskDependency
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskDependencyCopyWith<TaskDependency> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskDependencyCopyWith<$Res> {
  factory $TaskDependencyCopyWith(
          TaskDependency value, $Res Function(TaskDependency) then) =
      _$TaskDependencyCopyWithImpl<$Res, TaskDependency>;
  @useResult
  $Res call(
      {int id,
      int taskId,
      int dependsOnTaskId,
      String dependsOnTaskTitle,
      DependencyType type});
}

/// @nodoc
class _$TaskDependencyCopyWithImpl<$Res, $Val extends TaskDependency>
    implements $TaskDependencyCopyWith<$Res> {
  _$TaskDependencyCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TaskDependency
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? dependsOnTaskId = null,
    Object? dependsOnTaskTitle = null,
    Object? type = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      dependsOnTaskId: null == dependsOnTaskId
          ? _value.dependsOnTaskId
          : dependsOnTaskId // ignore: cast_nullable_to_non_nullable
              as int,
      dependsOnTaskTitle: null == dependsOnTaskTitle
          ? _value.dependsOnTaskTitle
          : dependsOnTaskTitle // ignore: cast_nullable_to_non_nullable
              as String,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as DependencyType,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TaskDependencyImplCopyWith<$Res>
    implements $TaskDependencyCopyWith<$Res> {
  factory _$$TaskDependencyImplCopyWith(_$TaskDependencyImpl value,
          $Res Function(_$TaskDependencyImpl) then) =
      __$$TaskDependencyImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int taskId,
      int dependsOnTaskId,
      String dependsOnTaskTitle,
      DependencyType type});
}

/// @nodoc
class __$$TaskDependencyImplCopyWithImpl<$Res>
    extends _$TaskDependencyCopyWithImpl<$Res, _$TaskDependencyImpl>
    implements _$$TaskDependencyImplCopyWith<$Res> {
  __$$TaskDependencyImplCopyWithImpl(
      _$TaskDependencyImpl _value, $Res Function(_$TaskDependencyImpl) _then)
      : super(_value, _then);

  /// Create a copy of TaskDependency
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? dependsOnTaskId = null,
    Object? dependsOnTaskTitle = null,
    Object? type = null,
  }) {
    return _then(_$TaskDependencyImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      dependsOnTaskId: null == dependsOnTaskId
          ? _value.dependsOnTaskId
          : dependsOnTaskId // ignore: cast_nullable_to_non_nullable
              as int,
      dependsOnTaskTitle: null == dependsOnTaskTitle
          ? _value.dependsOnTaskTitle
          : dependsOnTaskTitle // ignore: cast_nullable_to_non_nullable
              as String,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as DependencyType,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TaskDependencyImpl implements _TaskDependency {
  const _$TaskDependencyImpl(
      {required this.id,
      required this.taskId,
      required this.dependsOnTaskId,
      required this.dependsOnTaskTitle,
      required this.type});

  factory _$TaskDependencyImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskDependencyImplFromJson(json);

  @override
  final int id;
  @override
  final int taskId;
  @override
  final int dependsOnTaskId;
  @override
  final String dependsOnTaskTitle;
  @override
  final DependencyType type;

  @override
  String toString() {
    return 'TaskDependency(id: $id, taskId: $taskId, dependsOnTaskId: $dependsOnTaskId, dependsOnTaskTitle: $dependsOnTaskTitle, type: $type)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskDependencyImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.taskId, taskId) || other.taskId == taskId) &&
            (identical(other.dependsOnTaskId, dependsOnTaskId) ||
                other.dependsOnTaskId == dependsOnTaskId) &&
            (identical(other.dependsOnTaskTitle, dependsOnTaskTitle) ||
                other.dependsOnTaskTitle == dependsOnTaskTitle) &&
            (identical(other.type, type) || other.type == type));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, taskId, dependsOnTaskId, dependsOnTaskTitle, type);

  /// Create a copy of TaskDependency
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskDependencyImplCopyWith<_$TaskDependencyImpl> get copyWith =>
      __$$TaskDependencyImplCopyWithImpl<_$TaskDependencyImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskDependencyImplToJson(
      this,
    );
  }
}

abstract class _TaskDependency implements TaskDependency {
  const factory _TaskDependency(
      {required final int id,
      required final int taskId,
      required final int dependsOnTaskId,
      required final String dependsOnTaskTitle,
      required final DependencyType type}) = _$TaskDependencyImpl;

  factory _TaskDependency.fromJson(Map<String, dynamic> json) =
      _$TaskDependencyImpl.fromJson;

  @override
  int get id;
  @override
  int get taskId;
  @override
  int get dependsOnTaskId;
  @override
  String get dependsOnTaskTitle;
  @override
  DependencyType get type;

  /// Create a copy of TaskDependency
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskDependencyImplCopyWith<_$TaskDependencyImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TaskTemplate _$TaskTemplateFromJson(Map<String, dynamic> json) {
  return _TaskTemplate.fromJson(json);
}

/// @nodoc
mixin _$TaskTemplate {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  TaskType get type => throw _privateConstructorUsedError;
  TaskPriority get priority => throw _privateConstructorUsedError;
  String? get estimatedHours => throw _privateConstructorUsedError;
  List<String>? get defaultTags => throw _privateConstructorUsedError;
  List<TaskTemplateSubtask>? get subtasks => throw _privateConstructorUsedError;
  int? get departmentId => throw _privateConstructorUsedError;
  String? get departmentName => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this TaskTemplate to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TaskTemplate
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskTemplateCopyWith<TaskTemplate> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskTemplateCopyWith<$Res> {
  factory $TaskTemplateCopyWith(
          TaskTemplate value, $Res Function(TaskTemplate) then) =
      _$TaskTemplateCopyWithImpl<$Res, TaskTemplate>;
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      TaskType type,
      TaskPriority priority,
      String? estimatedHours,
      List<String>? defaultTags,
      List<TaskTemplateSubtask>? subtasks,
      int? departmentId,
      String? departmentName,
      bool isActive,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$TaskTemplateCopyWithImpl<$Res, $Val extends TaskTemplate>
    implements $TaskTemplateCopyWith<$Res> {
  _$TaskTemplateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TaskTemplate
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? priority = null,
    Object? estimatedHours = freezed,
    Object? defaultTags = freezed,
    Object? subtasks = freezed,
    Object? departmentId = freezed,
    Object? departmentName = freezed,
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
              as TaskType,
      priority: null == priority
          ? _value.priority
          : priority // ignore: cast_nullable_to_non_nullable
              as TaskPriority,
      estimatedHours: freezed == estimatedHours
          ? _value.estimatedHours
          : estimatedHours // ignore: cast_nullable_to_non_nullable
              as String?,
      defaultTags: freezed == defaultTags
          ? _value.defaultTags
          : defaultTags // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      subtasks: freezed == subtasks
          ? _value.subtasks
          : subtasks // ignore: cast_nullable_to_non_nullable
              as List<TaskTemplateSubtask>?,
      departmentId: freezed == departmentId
          ? _value.departmentId
          : departmentId // ignore: cast_nullable_to_non_nullable
              as int?,
      departmentName: freezed == departmentName
          ? _value.departmentName
          : departmentName // ignore: cast_nullable_to_non_nullable
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
abstract class _$$TaskTemplateImplCopyWith<$Res>
    implements $TaskTemplateCopyWith<$Res> {
  factory _$$TaskTemplateImplCopyWith(
          _$TaskTemplateImpl value, $Res Function(_$TaskTemplateImpl) then) =
      __$$TaskTemplateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      TaskType type,
      TaskPriority priority,
      String? estimatedHours,
      List<String>? defaultTags,
      List<TaskTemplateSubtask>? subtasks,
      int? departmentId,
      String? departmentName,
      bool isActive,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$TaskTemplateImplCopyWithImpl<$Res>
    extends _$TaskTemplateCopyWithImpl<$Res, _$TaskTemplateImpl>
    implements _$$TaskTemplateImplCopyWith<$Res> {
  __$$TaskTemplateImplCopyWithImpl(
      _$TaskTemplateImpl _value, $Res Function(_$TaskTemplateImpl) _then)
      : super(_value, _then);

  /// Create a copy of TaskTemplate
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? priority = null,
    Object? estimatedHours = freezed,
    Object? defaultTags = freezed,
    Object? subtasks = freezed,
    Object? departmentId = freezed,
    Object? departmentName = freezed,
    Object? isActive = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$TaskTemplateImpl(
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
              as TaskType,
      priority: null == priority
          ? _value.priority
          : priority // ignore: cast_nullable_to_non_nullable
              as TaskPriority,
      estimatedHours: freezed == estimatedHours
          ? _value.estimatedHours
          : estimatedHours // ignore: cast_nullable_to_non_nullable
              as String?,
      defaultTags: freezed == defaultTags
          ? _value._defaultTags
          : defaultTags // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      subtasks: freezed == subtasks
          ? _value._subtasks
          : subtasks // ignore: cast_nullable_to_non_nullable
              as List<TaskTemplateSubtask>?,
      departmentId: freezed == departmentId
          ? _value.departmentId
          : departmentId // ignore: cast_nullable_to_non_nullable
              as int?,
      departmentName: freezed == departmentName
          ? _value.departmentName
          : departmentName // ignore: cast_nullable_to_non_nullable
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
class _$TaskTemplateImpl implements _TaskTemplate {
  const _$TaskTemplateImpl(
      {required this.id,
      required this.name,
      this.description,
      required this.type,
      required this.priority,
      this.estimatedHours,
      final List<String>? defaultTags,
      final List<TaskTemplateSubtask>? subtasks,
      this.departmentId,
      this.departmentName,
      this.isActive = true,
      required this.createdAt,
      this.updatedAt})
      : _defaultTags = defaultTags,
        _subtasks = subtasks;

  factory _$TaskTemplateImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskTemplateImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String? description;
  @override
  final TaskType type;
  @override
  final TaskPriority priority;
  @override
  final String? estimatedHours;
  final List<String>? _defaultTags;
  @override
  List<String>? get defaultTags {
    final value = _defaultTags;
    if (value == null) return null;
    if (_defaultTags is EqualUnmodifiableListView) return _defaultTags;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<TaskTemplateSubtask>? _subtasks;
  @override
  List<TaskTemplateSubtask>? get subtasks {
    final value = _subtasks;
    if (value == null) return null;
    if (_subtasks is EqualUnmodifiableListView) return _subtasks;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final int? departmentId;
  @override
  final String? departmentName;
  @override
  @JsonKey()
  final bool isActive;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'TaskTemplate(id: $id, name: $name, description: $description, type: $type, priority: $priority, estimatedHours: $estimatedHours, defaultTags: $defaultTags, subtasks: $subtasks, departmentId: $departmentId, departmentName: $departmentName, isActive: $isActive, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskTemplateImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.priority, priority) ||
                other.priority == priority) &&
            (identical(other.estimatedHours, estimatedHours) ||
                other.estimatedHours == estimatedHours) &&
            const DeepCollectionEquality()
                .equals(other._defaultTags, _defaultTags) &&
            const DeepCollectionEquality().equals(other._subtasks, _subtasks) &&
            (identical(other.departmentId, departmentId) ||
                other.departmentId == departmentId) &&
            (identical(other.departmentName, departmentName) ||
                other.departmentName == departmentName) &&
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
      priority,
      estimatedHours,
      const DeepCollectionEquality().hash(_defaultTags),
      const DeepCollectionEquality().hash(_subtasks),
      departmentId,
      departmentName,
      isActive,
      createdAt,
      updatedAt);

  /// Create a copy of TaskTemplate
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskTemplateImplCopyWith<_$TaskTemplateImpl> get copyWith =>
      __$$TaskTemplateImplCopyWithImpl<_$TaskTemplateImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskTemplateImplToJson(
      this,
    );
  }
}

abstract class _TaskTemplate implements TaskTemplate {
  const factory _TaskTemplate(
      {required final int id,
      required final String name,
      final String? description,
      required final TaskType type,
      required final TaskPriority priority,
      final String? estimatedHours,
      final List<String>? defaultTags,
      final List<TaskTemplateSubtask>? subtasks,
      final int? departmentId,
      final String? departmentName,
      final bool isActive,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$TaskTemplateImpl;

  factory _TaskTemplate.fromJson(Map<String, dynamic> json) =
      _$TaskTemplateImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String? get description;
  @override
  TaskType get type;
  @override
  TaskPriority get priority;
  @override
  String? get estimatedHours;
  @override
  List<String>? get defaultTags;
  @override
  List<TaskTemplateSubtask>? get subtasks;
  @override
  int? get departmentId;
  @override
  String? get departmentName;
  @override
  bool get isActive;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of TaskTemplate
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskTemplateImplCopyWith<_$TaskTemplateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TaskTemplateSubtask _$TaskTemplateSubtaskFromJson(Map<String, dynamic> json) {
  return _TaskTemplateSubtask.fromJson(json);
}

/// @nodoc
mixin _$TaskTemplateSubtask {
  int get id => throw _privateConstructorUsedError;
  int get templateId => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  TaskPriority? get priority => throw _privateConstructorUsedError;
  int? get estimatedHours => throw _privateConstructorUsedError;
  int? get order => throw _privateConstructorUsedError;

  /// Serializes this TaskTemplateSubtask to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TaskTemplateSubtask
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskTemplateSubtaskCopyWith<TaskTemplateSubtask> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskTemplateSubtaskCopyWith<$Res> {
  factory $TaskTemplateSubtaskCopyWith(
          TaskTemplateSubtask value, $Res Function(TaskTemplateSubtask) then) =
      _$TaskTemplateSubtaskCopyWithImpl<$Res, TaskTemplateSubtask>;
  @useResult
  $Res call(
      {int id,
      int templateId,
      String title,
      String? description,
      TaskPriority? priority,
      int? estimatedHours,
      int? order});
}

/// @nodoc
class _$TaskTemplateSubtaskCopyWithImpl<$Res, $Val extends TaskTemplateSubtask>
    implements $TaskTemplateSubtaskCopyWith<$Res> {
  _$TaskTemplateSubtaskCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TaskTemplateSubtask
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? templateId = null,
    Object? title = null,
    Object? description = freezed,
    Object? priority = freezed,
    Object? estimatedHours = freezed,
    Object? order = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      templateId: null == templateId
          ? _value.templateId
          : templateId // ignore: cast_nullable_to_non_nullable
              as int,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      priority: freezed == priority
          ? _value.priority
          : priority // ignore: cast_nullable_to_non_nullable
              as TaskPriority?,
      estimatedHours: freezed == estimatedHours
          ? _value.estimatedHours
          : estimatedHours // ignore: cast_nullable_to_non_nullable
              as int?,
      order: freezed == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TaskTemplateSubtaskImplCopyWith<$Res>
    implements $TaskTemplateSubtaskCopyWith<$Res> {
  factory _$$TaskTemplateSubtaskImplCopyWith(_$TaskTemplateSubtaskImpl value,
          $Res Function(_$TaskTemplateSubtaskImpl) then) =
      __$$TaskTemplateSubtaskImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int templateId,
      String title,
      String? description,
      TaskPriority? priority,
      int? estimatedHours,
      int? order});
}

/// @nodoc
class __$$TaskTemplateSubtaskImplCopyWithImpl<$Res>
    extends _$TaskTemplateSubtaskCopyWithImpl<$Res, _$TaskTemplateSubtaskImpl>
    implements _$$TaskTemplateSubtaskImplCopyWith<$Res> {
  __$$TaskTemplateSubtaskImplCopyWithImpl(_$TaskTemplateSubtaskImpl _value,
      $Res Function(_$TaskTemplateSubtaskImpl) _then)
      : super(_value, _then);

  /// Create a copy of TaskTemplateSubtask
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? templateId = null,
    Object? title = null,
    Object? description = freezed,
    Object? priority = freezed,
    Object? estimatedHours = freezed,
    Object? order = freezed,
  }) {
    return _then(_$TaskTemplateSubtaskImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      templateId: null == templateId
          ? _value.templateId
          : templateId // ignore: cast_nullable_to_non_nullable
              as int,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      priority: freezed == priority
          ? _value.priority
          : priority // ignore: cast_nullable_to_non_nullable
              as TaskPriority?,
      estimatedHours: freezed == estimatedHours
          ? _value.estimatedHours
          : estimatedHours // ignore: cast_nullable_to_non_nullable
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
class _$TaskTemplateSubtaskImpl implements _TaskTemplateSubtask {
  const _$TaskTemplateSubtaskImpl(
      {required this.id,
      required this.templateId,
      required this.title,
      this.description,
      this.priority,
      this.estimatedHours,
      this.order});

  factory _$TaskTemplateSubtaskImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskTemplateSubtaskImplFromJson(json);

  @override
  final int id;
  @override
  final int templateId;
  @override
  final String title;
  @override
  final String? description;
  @override
  final TaskPriority? priority;
  @override
  final int? estimatedHours;
  @override
  final int? order;

  @override
  String toString() {
    return 'TaskTemplateSubtask(id: $id, templateId: $templateId, title: $title, description: $description, priority: $priority, estimatedHours: $estimatedHours, order: $order)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskTemplateSubtaskImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.templateId, templateId) ||
                other.templateId == templateId) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.priority, priority) ||
                other.priority == priority) &&
            (identical(other.estimatedHours, estimatedHours) ||
                other.estimatedHours == estimatedHours) &&
            (identical(other.order, order) || other.order == order));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, templateId, title,
      description, priority, estimatedHours, order);

  /// Create a copy of TaskTemplateSubtask
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskTemplateSubtaskImplCopyWith<_$TaskTemplateSubtaskImpl> get copyWith =>
      __$$TaskTemplateSubtaskImplCopyWithImpl<_$TaskTemplateSubtaskImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskTemplateSubtaskImplToJson(
      this,
    );
  }
}

abstract class _TaskTemplateSubtask implements TaskTemplateSubtask {
  const factory _TaskTemplateSubtask(
      {required final int id,
      required final int templateId,
      required final String title,
      final String? description,
      final TaskPriority? priority,
      final int? estimatedHours,
      final int? order}) = _$TaskTemplateSubtaskImpl;

  factory _TaskTemplateSubtask.fromJson(Map<String, dynamic> json) =
      _$TaskTemplateSubtaskImpl.fromJson;

  @override
  int get id;
  @override
  int get templateId;
  @override
  String get title;
  @override
  String? get description;
  @override
  TaskPriority? get priority;
  @override
  int? get estimatedHours;
  @override
  int? get order;

  /// Create a copy of TaskTemplateSubtask
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskTemplateSubtaskImplCopyWith<_$TaskTemplateSubtaskImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

KanbanColumn _$KanbanColumnFromJson(Map<String, dynamic> json) {
  return _KanbanColumn.fromJson(json);
}

/// @nodoc
mixin _$KanbanColumn {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  TaskStatus get status => throw _privateConstructorUsedError;
  int get order => throw _privateConstructorUsedError;
  int? get wipLimit => throw _privateConstructorUsedError;
  String? get color => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;

  /// Serializes this KanbanColumn to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of KanbanColumn
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $KanbanColumnCopyWith<KanbanColumn> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $KanbanColumnCopyWith<$Res> {
  factory $KanbanColumnCopyWith(
          KanbanColumn value, $Res Function(KanbanColumn) then) =
      _$KanbanColumnCopyWithImpl<$Res, KanbanColumn>;
  @useResult
  $Res call(
      {int id,
      String name,
      TaskStatus status,
      int order,
      int? wipLimit,
      String? color,
      bool isActive});
}

/// @nodoc
class _$KanbanColumnCopyWithImpl<$Res, $Val extends KanbanColumn>
    implements $KanbanColumnCopyWith<$Res> {
  _$KanbanColumnCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of KanbanColumn
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? status = null,
    Object? order = null,
    Object? wipLimit = freezed,
    Object? color = freezed,
    Object? isActive = null,
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
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TaskStatus,
      order: null == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int,
      wipLimit: freezed == wipLimit
          ? _value.wipLimit
          : wipLimit // ignore: cast_nullable_to_non_nullable
              as int?,
      color: freezed == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$KanbanColumnImplCopyWith<$Res>
    implements $KanbanColumnCopyWith<$Res> {
  factory _$$KanbanColumnImplCopyWith(
          _$KanbanColumnImpl value, $Res Function(_$KanbanColumnImpl) then) =
      __$$KanbanColumnImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      TaskStatus status,
      int order,
      int? wipLimit,
      String? color,
      bool isActive});
}

/// @nodoc
class __$$KanbanColumnImplCopyWithImpl<$Res>
    extends _$KanbanColumnCopyWithImpl<$Res, _$KanbanColumnImpl>
    implements _$$KanbanColumnImplCopyWith<$Res> {
  __$$KanbanColumnImplCopyWithImpl(
      _$KanbanColumnImpl _value, $Res Function(_$KanbanColumnImpl) _then)
      : super(_value, _then);

  /// Create a copy of KanbanColumn
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? status = null,
    Object? order = null,
    Object? wipLimit = freezed,
    Object? color = freezed,
    Object? isActive = null,
  }) {
    return _then(_$KanbanColumnImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TaskStatus,
      order: null == order
          ? _value.order
          : order // ignore: cast_nullable_to_non_nullable
              as int,
      wipLimit: freezed == wipLimit
          ? _value.wipLimit
          : wipLimit // ignore: cast_nullable_to_non_nullable
              as int?,
      color: freezed == color
          ? _value.color
          : color // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$KanbanColumnImpl implements _KanbanColumn {
  const _$KanbanColumnImpl(
      {required this.id,
      required this.name,
      required this.status,
      required this.order,
      this.wipLimit,
      this.color,
      this.isActive = true});

  factory _$KanbanColumnImpl.fromJson(Map<String, dynamic> json) =>
      _$$KanbanColumnImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final TaskStatus status;
  @override
  final int order;
  @override
  final int? wipLimit;
  @override
  final String? color;
  @override
  @JsonKey()
  final bool isActive;

  @override
  String toString() {
    return 'KanbanColumn(id: $id, name: $name, status: $status, order: $order, wipLimit: $wipLimit, color: $color, isActive: $isActive)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$KanbanColumnImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.order, order) || other.order == order) &&
            (identical(other.wipLimit, wipLimit) ||
                other.wipLimit == wipLimit) &&
            (identical(other.color, color) || other.color == color) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, name, status, order, wipLimit, color, isActive);

  /// Create a copy of KanbanColumn
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$KanbanColumnImplCopyWith<_$KanbanColumnImpl> get copyWith =>
      __$$KanbanColumnImplCopyWithImpl<_$KanbanColumnImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$KanbanColumnImplToJson(
      this,
    );
  }
}

abstract class _KanbanColumn implements KanbanColumn {
  const factory _KanbanColumn(
      {required final int id,
      required final String name,
      required final TaskStatus status,
      required final int order,
      final int? wipLimit,
      final String? color,
      final bool isActive}) = _$KanbanColumnImpl;

  factory _KanbanColumn.fromJson(Map<String, dynamic> json) =
      _$KanbanColumnImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  TaskStatus get status;
  @override
  int get order;
  @override
  int? get wipLimit;
  @override
  String? get color;
  @override
  bool get isActive;

  /// Create a copy of KanbanColumn
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$KanbanColumnImplCopyWith<_$KanbanColumnImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TaskTimeEntry _$TaskTimeEntryFromJson(Map<String, dynamic> json) {
  return _TaskTimeEntry.fromJson(json);
}

/// @nodoc
mixin _$TaskTimeEntry {
  int get id => throw _privateConstructorUsedError;
  int get taskId => throw _privateConstructorUsedError;
  int get userId => throw _privateConstructorUsedError;
  String get userName => throw _privateConstructorUsedError;
  DateTime get startTime => throw _privateConstructorUsedError;
  DateTime? get endTime => throw _privateConstructorUsedError;
  int? get durationMinutes => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;

  /// Serializes this TaskTimeEntry to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TaskTimeEntry
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TaskTimeEntryCopyWith<TaskTimeEntry> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TaskTimeEntryCopyWith<$Res> {
  factory $TaskTimeEntryCopyWith(
          TaskTimeEntry value, $Res Function(TaskTimeEntry) then) =
      _$TaskTimeEntryCopyWithImpl<$Res, TaskTimeEntry>;
  @useResult
  $Res call(
      {int id,
      int taskId,
      int userId,
      String userName,
      DateTime startTime,
      DateTime? endTime,
      int? durationMinutes,
      String? description,
      DateTime createdAt});
}

/// @nodoc
class _$TaskTimeEntryCopyWithImpl<$Res, $Val extends TaskTimeEntry>
    implements $TaskTimeEntryCopyWith<$Res> {
  _$TaskTimeEntryCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TaskTimeEntry
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? userId = null,
    Object? userName = null,
    Object? startTime = null,
    Object? endTime = freezed,
    Object? durationMinutes = freezed,
    Object? description = freezed,
    Object? createdAt = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      userId: null == userId
          ? _value.userId
          : userId // ignore: cast_nullable_to_non_nullable
              as int,
      userName: null == userName
          ? _value.userName
          : userName // ignore: cast_nullable_to_non_nullable
              as String,
      startTime: null == startTime
          ? _value.startTime
          : startTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endTime: freezed == endTime
          ? _value.endTime
          : endTime // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      durationMinutes: freezed == durationMinutes
          ? _value.durationMinutes
          : durationMinutes // ignore: cast_nullable_to_non_nullable
              as int?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TaskTimeEntryImplCopyWith<$Res>
    implements $TaskTimeEntryCopyWith<$Res> {
  factory _$$TaskTimeEntryImplCopyWith(
          _$TaskTimeEntryImpl value, $Res Function(_$TaskTimeEntryImpl) then) =
      __$$TaskTimeEntryImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int taskId,
      int userId,
      String userName,
      DateTime startTime,
      DateTime? endTime,
      int? durationMinutes,
      String? description,
      DateTime createdAt});
}

/// @nodoc
class __$$TaskTimeEntryImplCopyWithImpl<$Res>
    extends _$TaskTimeEntryCopyWithImpl<$Res, _$TaskTimeEntryImpl>
    implements _$$TaskTimeEntryImplCopyWith<$Res> {
  __$$TaskTimeEntryImplCopyWithImpl(
      _$TaskTimeEntryImpl _value, $Res Function(_$TaskTimeEntryImpl) _then)
      : super(_value, _then);

  /// Create a copy of TaskTimeEntry
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? taskId = null,
    Object? userId = null,
    Object? userName = null,
    Object? startTime = null,
    Object? endTime = freezed,
    Object? durationMinutes = freezed,
    Object? description = freezed,
    Object? createdAt = null,
  }) {
    return _then(_$TaskTimeEntryImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      taskId: null == taskId
          ? _value.taskId
          : taskId // ignore: cast_nullable_to_non_nullable
              as int,
      userId: null == userId
          ? _value.userId
          : userId // ignore: cast_nullable_to_non_nullable
              as int,
      userName: null == userName
          ? _value.userName
          : userName // ignore: cast_nullable_to_non_nullable
              as String,
      startTime: null == startTime
          ? _value.startTime
          : startTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endTime: freezed == endTime
          ? _value.endTime
          : endTime // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      durationMinutes: freezed == durationMinutes
          ? _value.durationMinutes
          : durationMinutes // ignore: cast_nullable_to_non_nullable
              as int?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TaskTimeEntryImpl implements _TaskTimeEntry {
  const _$TaskTimeEntryImpl(
      {required this.id,
      required this.taskId,
      required this.userId,
      required this.userName,
      required this.startTime,
      this.endTime,
      this.durationMinutes,
      this.description,
      required this.createdAt});

  factory _$TaskTimeEntryImpl.fromJson(Map<String, dynamic> json) =>
      _$$TaskTimeEntryImplFromJson(json);

  @override
  final int id;
  @override
  final int taskId;
  @override
  final int userId;
  @override
  final String userName;
  @override
  final DateTime startTime;
  @override
  final DateTime? endTime;
  @override
  final int? durationMinutes;
  @override
  final String? description;
  @override
  final DateTime createdAt;

  @override
  String toString() {
    return 'TaskTimeEntry(id: $id, taskId: $taskId, userId: $userId, userName: $userName, startTime: $startTime, endTime: $endTime, durationMinutes: $durationMinutes, description: $description, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TaskTimeEntryImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.taskId, taskId) || other.taskId == taskId) &&
            (identical(other.userId, userId) || other.userId == userId) &&
            (identical(other.userName, userName) ||
                other.userName == userName) &&
            (identical(other.startTime, startTime) ||
                other.startTime == startTime) &&
            (identical(other.endTime, endTime) || other.endTime == endTime) &&
            (identical(other.durationMinutes, durationMinutes) ||
                other.durationMinutes == durationMinutes) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, taskId, userId, userName,
      startTime, endTime, durationMinutes, description, createdAt);

  /// Create a copy of TaskTimeEntry
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TaskTimeEntryImplCopyWith<_$TaskTimeEntryImpl> get copyWith =>
      __$$TaskTimeEntryImplCopyWithImpl<_$TaskTimeEntryImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TaskTimeEntryImplToJson(
      this,
    );
  }
}

abstract class _TaskTimeEntry implements TaskTimeEntry {
  const factory _TaskTimeEntry(
      {required final int id,
      required final int taskId,
      required final int userId,
      required final String userName,
      required final DateTime startTime,
      final DateTime? endTime,
      final int? durationMinutes,
      final String? description,
      required final DateTime createdAt}) = _$TaskTimeEntryImpl;

  factory _TaskTimeEntry.fromJson(Map<String, dynamic> json) =
      _$TaskTimeEntryImpl.fromJson;

  @override
  int get id;
  @override
  int get taskId;
  @override
  int get userId;
  @override
  String get userName;
  @override
  DateTime get startTime;
  @override
  DateTime? get endTime;
  @override
  int? get durationMinutes;
  @override
  String? get description;
  @override
  DateTime get createdAt;

  /// Create a copy of TaskTimeEntry
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TaskTimeEntryImplCopyWith<_$TaskTimeEntryImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
