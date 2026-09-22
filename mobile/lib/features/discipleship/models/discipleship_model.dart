import 'package:freezed_annotation/freezed_annotation.dart';

part 'discipleship_model.freezed.dart';
part 'discipleship_model.g.dart';

@freezed
class DiscipleshipJourney with _$DiscipleshipJourney {
  const factory DiscipleshipJourney({
    required int id,
    required String name,
    String? description,
    required JourneyType type,
    required int totalStages,
    required DateTime startDate,
    DateTime? endDate,
    int? createdById,
    String? createdByName,
    @Default(true) bool isActive,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _DiscipleshipJourney;

  factory DiscipleshipJourney.fromJson(Map<String, dynamic> json) => _$DiscipleshipJourneyFromJson(json);
}

@freezed
class DiscipleshipStage with _$DiscipleshipStage {
  const factory DiscipleshipStage({
    required int id,
    required int journeyId,
    required int order,
    required String name,
    String? description,
    String? color,
    String? icon,
    List<StageRequirement>? requirements,
    List<StageReward>? rewards,
    int? durationDays,
    bool isOptional,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _DiscipleshipStage;

  factory DiscipleshipStage.fromJson(Map<String, dynamic> json) => _$DiscipleshipStageFromJson(json);
}

@freezed
class StageRequirement with _$StageRequirement {
  const factory StageRequirement({
    required int id,
    required int stageId,
    required RequirementType type,
    String? description,
    String? referenceId,
    String? referenceName,
    bool isRequired,
    int order,
  }) = _StageRequirement;

  factory StageRequirement.fromJson(Map<String, dynamic> json) => _$StageRequirementFromJson(json);
}

@freezed
class StageReward with _$StageReward {
  const factory StageReward({
    required int id,
    required int stageId,
    required RewardType type,
    String? name,
    String? description,
    String? icon,
    String? imageUrl,
    int? points,
    String? badgeId,
    String? badgeName,
  }) = _StageReward;

  factory StageReward.fromJson(Map<String, dynamic> json) => _$StageRewardFromJson(json);
}

@freezed
class DiscipleProgress with _$DiscipleProgress {
  const factory DiscipleProgress({
    required int id,
    required int discipleId,
    required String discipleName,
    required int journeyId,
    required String journeyName,
    required int currentStageId,
    required String currentStageName,
    int currentStageOrder,
    @Default(0) int completedStages,
    int totalStages,
    @Default(0) int completedRequirements,
    int totalRequirements,
    DateTime? startedAt,
    DateTime? lastActivityAt,
    DateTime? completedAt,
    ProgressStatus status,
    Map<int, RequirementProgress>? requirementProgress,
    DateTime? nextMilestoneDate,
    String? nextMilestoneName,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _DiscipleProgress;

  factory DiscipleProgress.fromJson(Map<String, dynamic> json) => _$DiscipleProgressFromJson(json);

  double get completionPercentage => totalStages > 0 ? (completedStages / totalStages) * 100 : 0;
  double get requirementsPercentage => totalRequirements > 0 ? (completedRequirements / totalRequirements) * 100 : 0;
}

@freezed
class RequirementProgress with _$RequirementProgress {
  const factory RequirementProgress({
    required int requirementId,
    required String requirementName,
    RequirementStatus status,
    DateTime? completedAt,
    String? evidence,
    String? notes,
    int? verifiedById,
    String? verifiedByName,
  }) = _RequirementProgress;

  factory RequirementProgress.fromJson(Map<String, dynamic> json) => _$RequirementProgressFromJson(json);
}

@freezed
class MentorAssignment with _$MentorAssignment {
  const factory MentorAssignment({
    required int id,
    required int mentorId,
    required String mentorName,
    required int discipleId,
    required String discipleName,
    required int journeyId,
    DateTime assignedAt,
    DateTime? endedAt,
    AssignmentStatus status,
    String? notes,
    @Default(7) int meetingFrequencyDays,
    DateTime? lastMeetingAt,
    DateTime? nextMeetingAt,
  }) = _MentorAssignment;

  factory MentorAssignment.fromJson(Map<String, dynamic> json) => _$MentorAssignmentFromJson(json);
}

@freezed
class MentorMeeting with _$MentorMeeting {
  const factory MentorMeeting({
    required int id,
    required int assignmentId,
    required int mentorId,
    required int discipleId,
    required DateTime scheduledAt,
    DateTime? actualAt,
    MeetingStatus status,
    String? notes,
    String? actionItems,
    String? nextSteps,
    int? durationMinutes,
    String? location,
  }) = _MentorMeeting;

  factory MentorMeeting.fromJson(Map<String, dynamic> json) => _$MentorMeetingFromJson(json);
}

@freezed
class DiscipleshipReport with _$DiscipleshipReport {
  const factory DiscipleshipReport({
    required int journeyId,
    required String journeyName,
    required int totalDisciples,
    int activeDisciples,
    int completedDisciples,
    int stalledDisciples,
    double averageCompletion,
    int totalMeetings,
    int completedMeetings,
    Map<String, int> stageDistribution,
    Map<String, int> statusDistribution,
    List<TopMentor> topMentors,
    DateTime generatedAt,
  }) = _DiscipleshipReport;

  factory DiscipleshipReport.fromJson(Map<String, dynamic> json) => _$DiscipleshipReportFromJson(json);
}

@freezed
class TopMentor with _$TopMentor {
  const factory TopMentor({
    required int mentorId,
    required String mentorName,
    int discipleCount,
    int meetingCount,
    double averageCompletion,
  }) = _TopMentor;

  factory TopMentor.fromJson(Map<String, dynamic> json) => _$TopMentorFromJson(json);
}

enum JourneyType {
  @JsonValue('NEW_BELIEVER')
  newBeliever,
  @JsonValue('GROWTH')
  growth,
  @JsonValue('LEADERSHIP')
  leadership,
  @JsonValue('MINISTRY')
  ministry,
  @JsonValue('CUSTOM')
  custom;
}

enum RequirementType {
  @JsonValue('ATTEND_MEETING')
  attendMeeting,
  @JsonValue('COMPLETE_STUDY')
  completeStudy,
  @JsonValue('MEMORIZE_VERSE')
  memorizeVerse,
  @JsonValue('PRACTICE_HABIT')
  practiceHabit,
  @JsonValue('SERVE')
  serve,
  @JsonValue('SHARE_TESTIMONY')
  shareTestimony,
  @JsonValue('READ_BOOK')
  readBook,
  @JsonValue('COMPLETE_COURSE')
  completeCourse,
  @JsonValue('ATTEND_EVENT')
  attendEvent,
  @JsonValue('CUSTOM')
  custom;
}

enum RewardType {
  @JsonValue('BADGE')
  badge,
  @JsonValue('CERTIFICATE')
  certificate,
  @JsonValue('POINTS')
  points,
  @JsonValue('ITEM')
  item,
  @JsonValue('PRIVILEGE')
  privilege,
  @JsonValue('RECOGNITION')
  recognition;
}

enum ProgressStatus {
  @JsonValue('NOT_STARTED')
  notStarted,
  @JsonValue('IN_PROGRESS')
  inProgress,
  @JsonValue('STALLED')
  stalled,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('ABANDONED')
  abandoned;
}

enum RequirementStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('IN_PROGRESS')
  inProgress,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('VERIFIED')
  verified,
  @JsonValue('WAIVED')
  waived;
}

enum MeetingStatus {
  @JsonValue('SCHEDULED')
  scheduled,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('RESCHEDULED')
  rescheduled;
}