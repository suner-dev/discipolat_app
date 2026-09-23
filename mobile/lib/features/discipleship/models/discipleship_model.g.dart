// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'discipleship_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$DiscipleshipJourneyImpl _$$DiscipleshipJourneyImplFromJson(
        Map<String, dynamic> json) =>
    _$DiscipleshipJourneyImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      type: $enumDecode(_$JourneyTypeEnumMap, json['type']),
      totalStages: (json['totalStages'] as num).toInt(),
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: json['endDate'] == null
          ? null
          : DateTime.parse(json['endDate'] as String),
      createdById: (json['createdById'] as num?)?.toInt(),
      createdByName: json['createdByName'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$DiscipleshipJourneyImplToJson(
        _$DiscipleshipJourneyImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'type': _$JourneyTypeEnumMap[instance.type]!,
      'totalStages': instance.totalStages,
      'startDate': instance.startDate.toIso8601String(),
      'endDate': instance.endDate?.toIso8601String(),
      'createdById': instance.createdById,
      'createdByName': instance.createdByName,
      'isActive': instance.isActive,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$JourneyTypeEnumMap = {
  JourneyType.newBeliever: 'NEW_BELIEVER',
  JourneyType.growth: 'GROWTH',
  JourneyType.leadership: 'LEADERSHIP',
  JourneyType.ministry: 'MINISTRY',
  JourneyType.custom: 'CUSTOM',
};

_$DiscipleshipStageImpl _$$DiscipleshipStageImplFromJson(
        Map<String, dynamic> json) =>
    _$DiscipleshipStageImpl(
      id: (json['id'] as num).toInt(),
      journeyId: (json['journeyId'] as num).toInt(),
      order: (json['order'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      color: json['color'] as String?,
      icon: json['icon'] as String?,
      requirements: (json['requirements'] as List<dynamic>?)
          ?.map((e) => StageRequirement.fromJson(e as Map<String, dynamic>))
          .toList(),
      rewards: (json['rewards'] as List<dynamic>?)
          ?.map((e) => StageReward.fromJson(e as Map<String, dynamic>))
          .toList(),
      durationDays: (json['durationDays'] as num?)?.toInt(),
      isOptional: json['isOptional'] as bool,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$DiscipleshipStageImplToJson(
        _$DiscipleshipStageImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'journeyId': instance.journeyId,
      'order': instance.order,
      'name': instance.name,
      'description': instance.description,
      'color': instance.color,
      'icon': instance.icon,
      'requirements': instance.requirements,
      'rewards': instance.rewards,
      'durationDays': instance.durationDays,
      'isOptional': instance.isOptional,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

_$StageRequirementImpl _$$StageRequirementImplFromJson(
        Map<String, dynamic> json) =>
    _$StageRequirementImpl(
      id: (json['id'] as num).toInt(),
      stageId: (json['stageId'] as num).toInt(),
      type: $enumDecode(_$RequirementTypeEnumMap, json['type']),
      description: json['description'] as String?,
      referenceId: json['referenceId'] as String?,
      referenceName: json['referenceName'] as String?,
      isRequired: json['isRequired'] as bool,
      order: (json['order'] as num).toInt(),
    );

Map<String, dynamic> _$$StageRequirementImplToJson(
        _$StageRequirementImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'stageId': instance.stageId,
      'type': _$RequirementTypeEnumMap[instance.type]!,
      'description': instance.description,
      'referenceId': instance.referenceId,
      'referenceName': instance.referenceName,
      'isRequired': instance.isRequired,
      'order': instance.order,
    };

const _$RequirementTypeEnumMap = {
  RequirementType.attendMeeting: 'ATTEND_MEETING',
  RequirementType.completeStudy: 'COMPLETE_STUDY',
  RequirementType.memorizeVerse: 'MEMORIZE_VERSE',
  RequirementType.practiceHabit: 'PRACTICE_HABIT',
  RequirementType.serve: 'SERVE',
  RequirementType.shareTestimony: 'SHARE_TESTIMONY',
  RequirementType.readBook: 'READ_BOOK',
  RequirementType.completeCourse: 'COMPLETE_COURSE',
  RequirementType.attendEvent: 'ATTEND_EVENT',
  RequirementType.custom: 'CUSTOM',
};

_$StageRewardImpl _$$StageRewardImplFromJson(Map<String, dynamic> json) =>
    _$StageRewardImpl(
      id: (json['id'] as num).toInt(),
      stageId: (json['stageId'] as num).toInt(),
      type: $enumDecode(_$RewardTypeEnumMap, json['type']),
      name: json['name'] as String?,
      description: json['description'] as String?,
      icon: json['icon'] as String?,
      imageUrl: json['imageUrl'] as String?,
      points: (json['points'] as num?)?.toInt(),
      badgeId: json['badgeId'] as String?,
      badgeName: json['badgeName'] as String?,
    );

Map<String, dynamic> _$$StageRewardImplToJson(_$StageRewardImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'stageId': instance.stageId,
      'type': _$RewardTypeEnumMap[instance.type]!,
      'name': instance.name,
      'description': instance.description,
      'icon': instance.icon,
      'imageUrl': instance.imageUrl,
      'points': instance.points,
      'badgeId': instance.badgeId,
      'badgeName': instance.badgeName,
    };

const _$RewardTypeEnumMap = {
  RewardType.badge: 'BADGE',
  RewardType.certificate: 'CERTIFICATE',
  RewardType.points: 'POINTS',
  RewardType.item: 'ITEM',
  RewardType.privilege: 'PRIVILEGE',
  RewardType.recognition: 'RECOGNITION',
};

_$DiscipleProgressImpl _$$DiscipleProgressImplFromJson(
        Map<String, dynamic> json) =>
    _$DiscipleProgressImpl(
      id: (json['id'] as num).toInt(),
      discipleId: (json['discipleId'] as num).toInt(),
      discipleName: json['discipleName'] as String,
      journeyId: (json['journeyId'] as num).toInt(),
      journeyName: json['journeyName'] as String,
      currentStageId: (json['currentStageId'] as num).toInt(),
      currentStageName: json['currentStageName'] as String,
      currentStageOrder: (json['currentStageOrder'] as num).toInt(),
      completedStages: (json['completedStages'] as num?)?.toInt() ?? 0,
      totalStages: (json['totalStages'] as num).toInt(),
      completedRequirements:
          (json['completedRequirements'] as num?)?.toInt() ?? 0,
      totalRequirements: (json['totalRequirements'] as num).toInt(),
      startedAt: json['startedAt'] == null
          ? null
          : DateTime.parse(json['startedAt'] as String),
      lastActivityAt: json['lastActivityAt'] == null
          ? null
          : DateTime.parse(json['lastActivityAt'] as String),
      completedAt: json['completedAt'] == null
          ? null
          : DateTime.parse(json['completedAt'] as String),
      status: $enumDecode(_$ProgressStatusEnumMap, json['status']),
      requirementProgress:
          (json['requirementProgress'] as Map<String, dynamic>?)?.map(
        (k, e) => MapEntry(int.parse(k),
            RequirementProgress.fromJson(e as Map<String, dynamic>)),
      ),
      nextMilestoneDate: json['nextMilestoneDate'] == null
          ? null
          : DateTime.parse(json['nextMilestoneDate'] as String),
      nextMilestoneName: json['nextMilestoneName'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$DiscipleProgressImplToJson(
        _$DiscipleProgressImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'discipleId': instance.discipleId,
      'discipleName': instance.discipleName,
      'journeyId': instance.journeyId,
      'journeyName': instance.journeyName,
      'currentStageId': instance.currentStageId,
      'currentStageName': instance.currentStageName,
      'currentStageOrder': instance.currentStageOrder,
      'completedStages': instance.completedStages,
      'totalStages': instance.totalStages,
      'completedRequirements': instance.completedRequirements,
      'totalRequirements': instance.totalRequirements,
      'startedAt': instance.startedAt?.toIso8601String(),
      'lastActivityAt': instance.lastActivityAt?.toIso8601String(),
      'completedAt': instance.completedAt?.toIso8601String(),
      'status': _$ProgressStatusEnumMap[instance.status]!,
      'requirementProgress': instance.requirementProgress
          ?.map((k, e) => MapEntry(k.toString(), e)),
      'nextMilestoneDate': instance.nextMilestoneDate?.toIso8601String(),
      'nextMilestoneName': instance.nextMilestoneName,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$ProgressStatusEnumMap = {
  ProgressStatus.notStarted: 'NOT_STARTED',
  ProgressStatus.inProgress: 'IN_PROGRESS',
  ProgressStatus.stalled: 'STALLED',
  ProgressStatus.completed: 'COMPLETED',
  ProgressStatus.abandoned: 'ABANDONED',
};

_$RequirementProgressImpl _$$RequirementProgressImplFromJson(
        Map<String, dynamic> json) =>
    _$RequirementProgressImpl(
      requirementId: (json['requirementId'] as num).toInt(),
      requirementName: json['requirementName'] as String,
      status: $enumDecode(_$RequirementStatusEnumMap, json['status']),
      completedAt: json['completedAt'] == null
          ? null
          : DateTime.parse(json['completedAt'] as String),
      evidence: json['evidence'] as String?,
      notes: json['notes'] as String?,
      verifiedById: (json['verifiedById'] as num?)?.toInt(),
      verifiedByName: json['verifiedByName'] as String?,
    );

Map<String, dynamic> _$$RequirementProgressImplToJson(
        _$RequirementProgressImpl instance) =>
    <String, dynamic>{
      'requirementId': instance.requirementId,
      'requirementName': instance.requirementName,
      'status': _$RequirementStatusEnumMap[instance.status]!,
      'completedAt': instance.completedAt?.toIso8601String(),
      'evidence': instance.evidence,
      'notes': instance.notes,
      'verifiedById': instance.verifiedById,
      'verifiedByName': instance.verifiedByName,
    };

const _$RequirementStatusEnumMap = {
  RequirementStatus.pending: 'PENDING',
  RequirementStatus.inProgress: 'IN_PROGRESS',
  RequirementStatus.completed: 'COMPLETED',
  RequirementStatus.verified: 'VERIFIED',
  RequirementStatus.waived: 'WAIVED',
};

_$MentorAssignmentImpl _$$MentorAssignmentImplFromJson(
        Map<String, dynamic> json) =>
    _$MentorAssignmentImpl(
      id: (json['id'] as num).toInt(),
      mentorId: (json['mentorId'] as num).toInt(),
      mentorName: json['mentorName'] as String,
      discipleId: (json['discipleId'] as num).toInt(),
      discipleName: json['discipleName'] as String,
      journeyId: (json['journeyId'] as num).toInt(),
      assignedAt: DateTime.parse(json['assignedAt'] as String),
      endedAt: json['endedAt'] == null
          ? null
          : DateTime.parse(json['endedAt'] as String),
      status: $enumDecode(_$AssignmentStatusEnumMap, json['status']),
      notes: json['notes'] as String?,
      meetingFrequencyDays:
          (json['meetingFrequencyDays'] as num?)?.toInt() ?? 7,
      lastMeetingAt: json['lastMeetingAt'] == null
          ? null
          : DateTime.parse(json['lastMeetingAt'] as String),
      nextMeetingAt: json['nextMeetingAt'] == null
          ? null
          : DateTime.parse(json['nextMeetingAt'] as String),
    );

Map<String, dynamic> _$$MentorAssignmentImplToJson(
        _$MentorAssignmentImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'mentorId': instance.mentorId,
      'mentorName': instance.mentorName,
      'discipleId': instance.discipleId,
      'discipleName': instance.discipleName,
      'journeyId': instance.journeyId,
      'assignedAt': instance.assignedAt.toIso8601String(),
      'endedAt': instance.endedAt?.toIso8601String(),
      'status': _$AssignmentStatusEnumMap[instance.status]!,
      'notes': instance.notes,
      'meetingFrequencyDays': instance.meetingFrequencyDays,
      'lastMeetingAt': instance.lastMeetingAt?.toIso8601String(),
      'nextMeetingAt': instance.nextMeetingAt?.toIso8601String(),
    };

const _$AssignmentStatusEnumMap = {
  AssignmentStatus.pending: 'PENDING',
  AssignmentStatus.active: 'ACTIVE',
  AssignmentStatus.ended: 'ENDED',
  AssignmentStatus.cancelled: 'CANCELLED',
};

_$MentorMeetingImpl _$$MentorMeetingImplFromJson(Map<String, dynamic> json) =>
    _$MentorMeetingImpl(
      id: (json['id'] as num).toInt(),
      assignmentId: (json['assignmentId'] as num).toInt(),
      mentorId: (json['mentorId'] as num).toInt(),
      discipleId: (json['discipleId'] as num).toInt(),
      scheduledAt: DateTime.parse(json['scheduledAt'] as String),
      actualAt: json['actualAt'] == null
          ? null
          : DateTime.parse(json['actualAt'] as String),
      status: $enumDecode(_$MeetingStatusEnumMap, json['status']),
      notes: json['notes'] as String?,
      actionItems: json['actionItems'] as String?,
      nextSteps: json['nextSteps'] as String?,
      durationMinutes: (json['durationMinutes'] as num?)?.toInt(),
      location: json['location'] as String?,
    );

Map<String, dynamic> _$$MentorMeetingImplToJson(_$MentorMeetingImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'assignmentId': instance.assignmentId,
      'mentorId': instance.mentorId,
      'discipleId': instance.discipleId,
      'scheduledAt': instance.scheduledAt.toIso8601String(),
      'actualAt': instance.actualAt?.toIso8601String(),
      'status': _$MeetingStatusEnumMap[instance.status]!,
      'notes': instance.notes,
      'actionItems': instance.actionItems,
      'nextSteps': instance.nextSteps,
      'durationMinutes': instance.durationMinutes,
      'location': instance.location,
    };

const _$MeetingStatusEnumMap = {
  MeetingStatus.scheduled: 'SCHEDULED',
  MeetingStatus.completed: 'COMPLETED',
  MeetingStatus.cancelled: 'CANCELLED',
  MeetingStatus.rescheduled: 'RESCHEDULED',
};

_$DiscipleshipReportImpl _$$DiscipleshipReportImplFromJson(
        Map<String, dynamic> json) =>
    _$DiscipleshipReportImpl(
      journeyId: (json['journeyId'] as num).toInt(),
      journeyName: json['journeyName'] as String,
      totalDisciples: (json['totalDisciples'] as num).toInt(),
      activeDisciples: (json['activeDisciples'] as num).toInt(),
      completedDisciples: (json['completedDisciples'] as num).toInt(),
      stalledDisciples: (json['stalledDisciples'] as num).toInt(),
      averageCompletion: (json['averageCompletion'] as num).toDouble(),
      totalMeetings: (json['totalMeetings'] as num).toInt(),
      completedMeetings: (json['completedMeetings'] as num).toInt(),
      stageDistribution:
          Map<String, int>.from(json['stageDistribution'] as Map),
      statusDistribution:
          Map<String, int>.from(json['statusDistribution'] as Map),
      topMentors: (json['topMentors'] as List<dynamic>)
          .map((e) => TopMentor.fromJson(e as Map<String, dynamic>))
          .toList(),
      generatedAt: DateTime.parse(json['generatedAt'] as String),
    );

Map<String, dynamic> _$$DiscipleshipReportImplToJson(
        _$DiscipleshipReportImpl instance) =>
    <String, dynamic>{
      'journeyId': instance.journeyId,
      'journeyName': instance.journeyName,
      'totalDisciples': instance.totalDisciples,
      'activeDisciples': instance.activeDisciples,
      'completedDisciples': instance.completedDisciples,
      'stalledDisciples': instance.stalledDisciples,
      'averageCompletion': instance.averageCompletion,
      'totalMeetings': instance.totalMeetings,
      'completedMeetings': instance.completedMeetings,
      'stageDistribution': instance.stageDistribution,
      'statusDistribution': instance.statusDistribution,
      'topMentors': instance.topMentors,
      'generatedAt': instance.generatedAt.toIso8601String(),
    };

_$TopMentorImpl _$$TopMentorImplFromJson(Map<String, dynamic> json) =>
    _$TopMentorImpl(
      mentorId: (json['mentorId'] as num).toInt(),
      mentorName: json['mentorName'] as String,
      discipleCount: (json['discipleCount'] as num).toInt(),
      meetingCount: (json['meetingCount'] as num).toInt(),
      averageCompletion: (json['averageCompletion'] as num).toDouble(),
    );

Map<String, dynamic> _$$TopMentorImplToJson(_$TopMentorImpl instance) =>
    <String, dynamic>{
      'mentorId': instance.mentorId,
      'mentorName': instance.mentorName,
      'discipleCount': instance.discipleCount,
      'meetingCount': instance.meetingCount,
      'averageCompletion': instance.averageCompletion,
    };
