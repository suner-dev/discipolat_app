// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'task_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$TaskImpl _$$TaskImplFromJson(Map<String, dynamic> json) => _$TaskImpl(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      description: json['description'] as String?,
      type: $enumDecode(_$TaskTypeEnumMap, json['type']),
      priority: $enumDecode(_$TaskPriorityEnumMap, json['priority']),
      status: $enumDecode(_$TaskStatusEnumMap, json['status']),
      projectId: (json['projectId'] as num?)?.toInt(),
      projectName: json['projectName'] as String?,
      assignedToId: (json['assignedToId'] as num?)?.toInt(),
      assignedToName: json['assignedToName'] as String?,
      assignedById: (json['assignedById'] as num?)?.toInt(),
      assignedByName: json['assignedByName'] as String?,
      departmentId: (json['departmentId'] as num?)?.toInt(),
      departmentName: json['departmentName'] as String?,
      dueDate: json['dueDate'] == null
          ? null
          : DateTime.parse(json['dueDate'] as String),
      startDate: json['startDate'] == null
          ? null
          : DateTime.parse(json['startDate'] as String),
      completedDate: json['completedDate'] == null
          ? null
          : DateTime.parse(json['completedDate'] as String),
      estimatedHours: (json['estimatedHours'] as num?)?.toInt(),
      actualHours: (json['actualHours'] as num?)?.toInt(),
      tags: (json['tags'] as List<dynamic>?)?.map((e) => e as String).toList(),
      attachments: (json['attachments'] as List<dynamic>?)
          ?.map((e) => TaskAttachment.fromJson(e as Map<String, dynamic>))
          .toList(),
      comments: (json['comments'] as List<dynamic>?)
          ?.map((e) => TaskComment.fromJson(e as Map<String, dynamic>))
          .toList(),
      dependencies: (json['dependencies'] as List<dynamic>?)
          ?.map((e) => TaskDependency.fromJson(e as Map<String, dynamic>))
          .toList(),
      parentTaskId: (json['parentTaskId'] as num?)?.toInt(),
      parentTaskTitle: json['parentTaskTitle'] as String?,
      recurrenceRuleId: (json['recurrenceRuleId'] as num?)?.toInt(),
      recurrencePattern: json['recurrencePattern'] as String?,
      recurrenceEndDate: json['recurrenceEndDate'] == null
          ? null
          : DateTime.parse(json['recurrenceEndDate'] as String),
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$TaskImplToJson(_$TaskImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'description': instance.description,
      'type': _$TaskTypeEnumMap[instance.type]!,
      'priority': _$TaskPriorityEnumMap[instance.priority]!,
      'status': _$TaskStatusEnumMap[instance.status]!,
      'projectId': instance.projectId,
      'projectName': instance.projectName,
      'assignedToId': instance.assignedToId,
      'assignedToName': instance.assignedToName,
      'assignedById': instance.assignedById,
      'assignedByName': instance.assignedByName,
      'departmentId': instance.departmentId,
      'departmentName': instance.departmentName,
      'dueDate': instance.dueDate?.toIso8601String(),
      'startDate': instance.startDate?.toIso8601String(),
      'completedDate': instance.completedDate?.toIso8601String(),
      'estimatedHours': instance.estimatedHours,
      'actualHours': instance.actualHours,
      'tags': instance.tags,
      'attachments': instance.attachments,
      'comments': instance.comments,
      'dependencies': instance.dependencies,
      'parentTaskId': instance.parentTaskId,
      'parentTaskTitle': instance.parentTaskTitle,
      'recurrenceRuleId': instance.recurrenceRuleId,
      'recurrencePattern': instance.recurrencePattern,
      'recurrenceEndDate': instance.recurrenceEndDate?.toIso8601String(),
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$TaskTypeEnumMap = {
  TaskType.task: 'TASK',
  TaskType.subtask: 'SUBTASK',
  TaskType.epic: 'EPIC',
  TaskType.story: 'STORY',
  TaskType.bug: 'BUG',
  TaskType.feature: 'FEATURE',
  TaskType.chores: 'CHORES',
  TaskType.meeting: 'MEETING',
  TaskType.call: 'CALL',
  TaskType.review: 'REVIEW',
};

const _$TaskPriorityEnumMap = {
  TaskPriority.low: 'LOW',
  TaskPriority.medium: 'MEDIUM',
  TaskPriority.high: 'HIGH',
  TaskPriority.urgent: 'URGENT',
  TaskPriority.critical: 'CRITICAL',
};

const _$TaskStatusEnumMap = {
  TaskStatus.backlog: 'BACKLOG',
  TaskStatus.todo: 'TODO',
  TaskStatus.inProgress: 'IN_PROGRESS',
  TaskStatus.inReview: 'IN_REVIEW',
  TaskStatus.blocked: 'BLOCKED',
  TaskStatus.done: 'DONE',
  TaskStatus.cancelled: 'CANCELLED',
};

_$TaskAttachmentImpl _$$TaskAttachmentImplFromJson(Map<String, dynamic> json) =>
    _$TaskAttachmentImpl(
      id: (json['id'] as num).toInt(),
      taskId: (json['taskId'] as num).toInt(),
      fileName: json['fileName'] as String,
      fileUrl: json['fileUrl'] as String,
      mimeType: json['mimeType'] as String,
      fileSize: (json['fileSize'] as num).toInt(),
      uploadedById: (json['uploadedById'] as num?)?.toInt(),
      uploadedByName: json['uploadedByName'] as String?,
      uploadedAt: DateTime.parse(json['uploadedAt'] as String),
    );

Map<String, dynamic> _$$TaskAttachmentImplToJson(
        _$TaskAttachmentImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'taskId': instance.taskId,
      'fileName': instance.fileName,
      'fileUrl': instance.fileUrl,
      'mimeType': instance.mimeType,
      'fileSize': instance.fileSize,
      'uploadedById': instance.uploadedById,
      'uploadedByName': instance.uploadedByName,
      'uploadedAt': instance.uploadedAt.toIso8601String(),
    };

_$TaskCommentImpl _$$TaskCommentImplFromJson(Map<String, dynamic> json) =>
    _$TaskCommentImpl(
      id: (json['id'] as num).toInt(),
      taskId: (json['taskId'] as num).toInt(),
      authorId: (json['authorId'] as num).toInt(),
      authorName: json['authorName'] as String,
      content: json['content'] as String,
      parentCommentId: (json['parentCommentId'] as num?)?.toInt(),
      parentAuthorName: json['parentAuthorName'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
      isSystem: json['isSystem'] as bool? ?? false,
    );

Map<String, dynamic> _$$TaskCommentImplToJson(_$TaskCommentImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'taskId': instance.taskId,
      'authorId': instance.authorId,
      'authorName': instance.authorName,
      'content': instance.content,
      'parentCommentId': instance.parentCommentId,
      'parentAuthorName': instance.parentAuthorName,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
      'isSystem': instance.isSystem,
    };

_$TaskDependencyImpl _$$TaskDependencyImplFromJson(Map<String, dynamic> json) =>
    _$TaskDependencyImpl(
      id: (json['id'] as num).toInt(),
      taskId: (json['taskId'] as num).toInt(),
      dependsOnTaskId: (json['dependsOnTaskId'] as num).toInt(),
      dependsOnTaskTitle: json['dependsOnTaskTitle'] as String,
      type: $enumDecode(_$DependencyTypeEnumMap, json['type']),
    );

Map<String, dynamic> _$$TaskDependencyImplToJson(
        _$TaskDependencyImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'taskId': instance.taskId,
      'dependsOnTaskId': instance.dependsOnTaskId,
      'dependsOnTaskTitle': instance.dependsOnTaskTitle,
      'type': _$DependencyTypeEnumMap[instance.type]!,
    };

const _$DependencyTypeEnumMap = {
  DependencyType.blocks: 'BLOCKS',
  DependencyType.isBlockedBy: 'IS_BLOCKED_BY',
  DependencyType.relatesTo: 'RELATES_TO',
  DependencyType.duplicates: 'DUPLICATES',
  DependencyType.isDuplicatedBy: 'IS_DUPLICATED_BY',
};

_$TaskTemplateImpl _$$TaskTemplateImplFromJson(Map<String, dynamic> json) =>
    _$TaskTemplateImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      type: $enumDecode(_$TaskTypeEnumMap, json['type']),
      priority: $enumDecode(_$TaskPriorityEnumMap, json['priority']),
      estimatedHours: json['estimatedHours'] as String?,
      defaultTags: (json['defaultTags'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      subtasks: (json['subtasks'] as List<dynamic>?)
          ?.map((e) => TaskTemplateSubtask.fromJson(e as Map<String, dynamic>))
          .toList(),
      departmentId: (json['departmentId'] as num?)?.toInt(),
      departmentName: json['departmentName'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$TaskTemplateImplToJson(_$TaskTemplateImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'type': _$TaskTypeEnumMap[instance.type]!,
      'priority': _$TaskPriorityEnumMap[instance.priority]!,
      'estimatedHours': instance.estimatedHours,
      'defaultTags': instance.defaultTags,
      'subtasks': instance.subtasks,
      'departmentId': instance.departmentId,
      'departmentName': instance.departmentName,
      'isActive': instance.isActive,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

_$TaskTemplateSubtaskImpl _$$TaskTemplateSubtaskImplFromJson(
        Map<String, dynamic> json) =>
    _$TaskTemplateSubtaskImpl(
      id: (json['id'] as num).toInt(),
      templateId: (json['templateId'] as num).toInt(),
      title: json['title'] as String,
      description: json['description'] as String?,
      priority: $enumDecodeNullable(_$TaskPriorityEnumMap, json['priority']),
      estimatedHours: (json['estimatedHours'] as num?)?.toInt(),
      order: (json['order'] as num?)?.toInt(),
    );

Map<String, dynamic> _$$TaskTemplateSubtaskImplToJson(
        _$TaskTemplateSubtaskImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'templateId': instance.templateId,
      'title': instance.title,
      'description': instance.description,
      'priority': _$TaskPriorityEnumMap[instance.priority],
      'estimatedHours': instance.estimatedHours,
      'order': instance.order,
    };

_$KanbanColumnImpl _$$KanbanColumnImplFromJson(Map<String, dynamic> json) =>
    _$KanbanColumnImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      status: $enumDecode(_$TaskStatusEnumMap, json['status']),
      order: (json['order'] as num).toInt(),
      wipLimit: (json['wipLimit'] as num?)?.toInt(),
      color: json['color'] as String?,
      isActive: json['isActive'] as bool? ?? true,
    );

Map<String, dynamic> _$$KanbanColumnImplToJson(_$KanbanColumnImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'status': _$TaskStatusEnumMap[instance.status]!,
      'order': instance.order,
      'wipLimit': instance.wipLimit,
      'color': instance.color,
      'isActive': instance.isActive,
    };

_$TaskTimeEntryImpl _$$TaskTimeEntryImplFromJson(Map<String, dynamic> json) =>
    _$TaskTimeEntryImpl(
      id: (json['id'] as num).toInt(),
      taskId: (json['taskId'] as num).toInt(),
      userId: (json['userId'] as num).toInt(),
      userName: json['userName'] as String,
      startTime: DateTime.parse(json['startTime'] as String),
      endTime: json['endTime'] == null
          ? null
          : DateTime.parse(json['endTime'] as String),
      durationMinutes: (json['durationMinutes'] as num?)?.toInt(),
      description: json['description'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$TaskTimeEntryImplToJson(_$TaskTimeEntryImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'taskId': instance.taskId,
      'userId': instance.userId,
      'userName': instance.userName,
      'startTime': instance.startTime.toIso8601String(),
      'endTime': instance.endTime?.toIso8601String(),
      'durationMinutes': instance.durationMinutes,
      'description': instance.description,
      'createdAt': instance.createdAt.toIso8601String(),
    };
