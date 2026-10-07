/// Modèles discipleship — contrat V233 aligné sur DiscipleshipService.java.
///
/// Règle d'identifiants (vérifiée dans le code serveur, pas les termes) :
/// - ids STRUCTURELLES (journey, stage, progress, assignment, meeting,
///   requirement, reward) = BIGSERIAL → `int` côté Dart ;
/// - références PERSONNES (createdById, discipleId, mentorId, verifiedById)
///   = UUID → `String` côté Dart. Le serveur fait `UUID.fromString(...)` sur
///   ces valeurs : un `int` y lèverait une IllegalArgumentException.
///
/// `toJson` n'envoie QUE les clés lues par le serveur aux création
/// (createJourney/createStage/createProgress/createAssignment/scheduleMeeting),
/// aucune clé inventée. Les classes sont plain Dart (plus de freezed :
/// build_runner n'est pas requis pour compiler l'app).
library;

// ---------- parsing tolérant ----------

int? _i(Object? v) => v == null ? null : (v is num ? v.toInt() : int.tryParse(v.toString()));
double? _f(Object? v) => v == null ? null : (v is num ? v.toDouble() : double.tryParse(v.toString()));
bool _b(Object? v, bool def) => v == null ? def : (v is bool ? v : v.toString() == 'true');
String? _s(Object? v) => v?.toString();

/// Date serveur (Instant.toString, ex. "2026-10-07T12:00:00Z") → DateTime.
DateTime? _d(Object? v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}

/// DateTime → format ISO accepté par Instant.parse serveur (suffixe "Z").
String? _iso(DateTime? d) => d?.toUtc().toIso8601String();

// ---------- enums (valeurs = Enum.name serveur, vérifiées entité par entité) ----------

enum JourneyType {
  newBeliever('NEW_BELIEVER', 'Nouveau converti'),
  growth('GROWTH', 'Croissance'),
  leadership('LEADERSHIP', 'Leadership'),
  ministry('MINISTRY', 'Ministère'),
  custom('CUSTOM', 'Personnalisé');

  const JourneyType(this.wire, this.label);
  final String wire;
  final String label;

  static JourneyType fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in JourneyType.values) {
      if (e.wire == v) return e;
    }
    return JourneyType.custom;
  }
}

enum RequirementType {
  attendMeeting('ATTEND_MEETING', 'Assister à une réunion'),
  completeStudy('COMPLETE_STUDY', 'Terminer une étude'),
  memorizeVerse('MEMORIZE_VERSE', 'Mémoriser un verset'),
  practiceHabit('PRACTICE_HABIT', 'Pratiquer une habitude'),
  serve('SERVE', 'Servir'),
  shareTestimony('SHARE_TESTIMONY', 'Partager un témoignage'),
  readBook('READ_BOOK', 'Lire un livre'),
  completeCourse('COMPLETE_COURSE', 'Terminer un cours'),
  attendEvent('ATTEND_EVENT', 'Assister à un événement'),
  custom('CUSTOM', 'Autre');

  const RequirementType(this.wire, this.label);
  final String wire;
  final String label;

  static RequirementType fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in RequirementType.values) {
      if (e.wire == v) return e;
    }
    return RequirementType.custom;
  }
}

enum RewardType {
  badge('BADGE', 'Badge'),
  certificate('CERTIFICATE', 'Certificat'),
  points('POINTS', 'Points'),
  item('ITEM', 'Objet'),
  privilege('PRIVILEGE', 'Privilège'),
  recognition('RECOGNITION', 'Reconnaissance');

  const RewardType(this.wire, this.label);
  final String wire;
  final String label;

  static RewardType fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in RewardType.values) {
      if (e.wire == v) return e;
    }
    return RewardType.badge;
  }
}

enum ProgressStatus {
  notStarted('NOT_STARTED', 'Non démarré'),
  inProgress('IN_PROGRESS', 'En cours'),
  stalled('STALLED', 'En pause'),
  completed('COMPLETED', 'Terminé'),
  abandoned('ABANDONED', 'Abandonné');

  const ProgressStatus(this.wire, this.label);
  final String wire;
  final String label;

  static ProgressStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in ProgressStatus.values) {
      if (e.wire == v) return e;
    }
    return ProgressStatus.notStarted;
  }
}

enum AssignmentStatus {
  pending('PENDING', 'En attente'),
  active('ACTIVE', 'Actif'),
  ended('ENDED', 'Terminé'),
  cancelled('CANCELLED', 'Annulé');

  const AssignmentStatus(this.wire, this.label);
  final String wire;
  final String label;

  static AssignmentStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in AssignmentStatus.values) {
      if (e.wire == v) return e;
    }
    return AssignmentStatus.pending;
  }
}

enum RequirementStatus {
  pending('PENDING', 'En attente'),
  inProgress('IN_PROGRESS', 'En cours'),
  completed('COMPLETED', 'Terminé'),
  verified('VERIFIED', 'Vérifié'),
  waived('WAIVED', 'Dispensé');

  const RequirementStatus(this.wire, this.label);
  final String wire;
  final String label;

  static RequirementStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in RequirementStatus.values) {
      if (e.wire == v) return e;
    }
    return RequirementStatus.pending;
  }
}

enum MeetingStatus {
  scheduled('SCHEDULED', 'Planifiée'),
  completed('COMPLETED', 'Terminée'),
  cancelled('CANCELLED', 'Annulée'),
  rescheduled('RESCHEDULED', 'Replanifiée');

  const MeetingStatus(this.wire, this.label);
  final String wire;
  final String label;

  static MeetingStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in MeetingStatus.values) {
      if (e.wire == v) return e;
    }
    return MeetingStatus.scheduled;
  }
}

// ---------- Journey (journeyView) ----------

class DiscipleshipJourney {
  const DiscipleshipJourney({
    required this.id,
    required this.name,
    this.description,
    this.type = JourneyType.custom,
    this.totalStages = 0,
    required this.startDate,
    this.endDate,
    this.createdById,
    this.isActive = true,
    required this.createdAt,
    this.updatedAt,
  });

  factory DiscipleshipJourney.fromJson(Map<String, dynamic> json) => DiscipleshipJourney(
        id: _i(json['id']) ?? 0,
        name: _s(json['name']) ?? '',
        description: _s(json['description']),
        type: JourneyType.fromWire(json['type']),
        totalStages: _i(json['totalStages']) ?? 0,
        startDate: _d(json['startDate']) ?? DateTime.now(),
        endDate: _d(json['endDate']),
        // createdById = UUID (j.getCreatedBy()) → String.
        // createdByName n'est PAS rendu par journeyView : champ supprimé.
        createdById: _s(json['createdById']),
        isActive: _b(json['isActive'], true),
        createdAt: _d(json['createdAt']) ?? DateTime.now(),
        updatedAt: _d(json['updatedAt']),
      );

  final int id;
  final String name;
  final String? description;
  final JourneyType type;
  final int totalStages;
  final DateTime startDate;
  final DateTime? endDate;
  final String? createdById;
  final bool isActive;
  final DateTime createdAt;
  final DateTime? updatedAt;

  /// Corps createJourney — clés lues par le serveur uniquement.
  Map<String, dynamic> toJson() => <String, dynamic>{
        'name': name,
        'description': description,
        'type': type.wire,
        'totalStages': totalStages,
        'startDate': _iso(startDate),
        'endDate': _iso(endDate),
      };
}

// ---------- Stage (stageView) ----------

class DiscipleshipStage {
  const DiscipleshipStage({
    required this.id,
    required this.journeyId,
    this.order = 0,
    required this.name,
    this.description,
    this.color,
    this.icon,
    this.durationDays,
    this.isOptional = false,
    this.requirements = const [],
    this.rewards = const [],
    required this.createdAt,
    this.updatedAt,
  });

  factory DiscipleshipStage.fromJson(Map<String, dynamic> json) => DiscipleshipStage(
        id: _i(json['id']) ?? 0,
        journeyId: _i(json['journeyId']) ?? 0,
        order: _i(json['order']) ?? 0,
        name: _s(json['name']) ?? '',
        description: _s(json['description']),
        color: _s(json['color']),
        icon: _s(json['icon']),
        durationDays: _i(json['durationDays']),
        isOptional: _b(json['isOptional'], false),
        requirements: (json['requirements'] as List?)
                ?.map((e) => StageRequirement.fromJson(Map<String, dynamic>.from(e as Map)))
                .toList() ??
            const [],
        rewards: (json['rewards'] as List?)
                ?.map((e) => StageReward.fromJson(Map<String, dynamic>.from(e as Map)))
                .toList() ??
            const [],
        createdAt: _d(json['createdAt']) ?? DateTime.now(),
        updatedAt: _d(json['updatedAt']),
      );

  final int id;
  final int journeyId;
  final int order;
  final String name;
  final String? description;
  final String? color;
  final String? icon;
  final int? durationDays;
  final bool isOptional;
  final List<StageRequirement> requirements;
  final List<StageReward> rewards;
  final DateTime createdAt;
  final DateTime? updatedAt;

  /// Corps createStage — clés lues par le serveur uniquement.
  Map<String, dynamic> toJson() => <String, dynamic>{
        'name': name,
        'journeyId': journeyId,
        'order': order,
        'description': description,
        'color': color,
        'icon': icon,
        'durationDays': durationDays,
        'isOptional': isOptional,
      };
}

class StageRequirement {
  const StageRequirement({
    required this.id,
    required this.stageId,
    this.type = RequirementType.custom,
    this.description,
    this.referenceId,
    this.referenceName,
    this.isRequired = true,
    this.order = 0,
  });

  factory StageRequirement.fromJson(Map<String, dynamic> json) => StageRequirement(
        id: _i(json['id']) ?? 0,
        stageId: _i(json['stageId']) ?? 0,
        type: RequirementType.fromWire(json['type']),
        description: _s(json['description']),
        // referenceId = VARCHAR(100) côté serveur (StageRequirement.java) → String.
        referenceId: _s(json['referenceId']),
        referenceName: _s(json['referenceName']),
        isRequired: _b(json['isRequired'], true),
        order: _i(json['order']) ?? 0,
      );

  final int id;
  final int stageId;
  final RequirementType type;
  final String? description;
  final String? referenceId;
  final String? referenceName;
  final bool isRequired;
  final int order;
}

class StageReward {
  const StageReward({
    required this.id,
    required this.stageId,
    this.type = RewardType.badge,
    this.name,
    this.description,
    this.icon,
    this.imageUrl,
    this.points,
    this.badgeId,
    this.badgeName,
  });

  factory StageReward.fromJson(Map<String, dynamic> json) => StageReward(
        id: _i(json['id']) ?? 0,
        stageId: _i(json['stageId']) ?? 0,
        type: RewardType.fromWire(json['type']),
        name: _s(json['name']),
        description: _s(json['description']),
        icon: _s(json['icon']),
        imageUrl: _s(json['imageUrl']),
        points: _i(json['points']),
        // badgeId = VARCHAR côté serveur (StageReward.java) → String.
        badgeId: _s(json['badgeId']),
        badgeName: _s(json['badgeName']),
      );

  final int id;
  final int stageId;
  final RewardType type;
  final String? name;
  final String? description;
  final String? icon;
  final String? imageUrl;
  final int? points;
  final String? badgeId;
  final String? badgeName;
}

// ---------- Progress (progressView) ----------

class DiscipleProgress {
  const DiscipleProgress({
    required this.id,
    required this.discipleId,
    required this.discipleName,
    required this.journeyId,
    required this.journeyName,
    this.currentStageId = 0,
    this.currentStageName = '',
    this.currentStageOrder = 0,
    this.completedStages = 0,
    this.totalStages = 0,
    this.completedRequirements = 0,
    this.totalRequirements = 0,
    this.startedAt,
    this.lastActivityAt,
    this.completedAt,
    this.status = ProgressStatus.notStarted,
    this.requirementProgress,
    this.nextMilestoneDate,
    this.nextMilestoneName,
    required this.createdAt,
    this.updatedAt,
  });

  factory DiscipleProgress.fromJson(Map<String, dynamic> json) => DiscipleProgress(
        id: _i(json['id']) ?? 0,
        // discipleId = UUID (progressView met pr.getDiscipleId()) → String.
        discipleId: _s(json['discipleId']) ?? '',
        discipleName: _s(json['discipleName']) ?? '',
        journeyId: _i(json['journeyId']) ?? 0,
        journeyName: _s(json['journeyName']) ?? '',
        // Le serveur rend 0 quand currentStageId est null.
        currentStageId: _i(json['currentStageId']) ?? 0,
        currentStageName: _s(json['currentStageName']) ?? '',
        currentStageOrder: _i(json['currentStageOrder']) ?? 0,
        completedStages: _i(json['completedStages']) ?? 0,
        totalStages: _i(json['totalStages']) ?? 0,
        completedRequirements: _i(json['completedRequirements']) ?? 0,
        totalRequirements: _i(json['totalRequirements']) ?? 0,
        startedAt: _d(json['startedAt']),
        lastActivityAt: _d(json['lastActivityAt']),
        completedAt: _d(json['completedAt']),
        status: ProgressStatus.fromWire(json['status']),
        // requirementProgress = MAP indexée par String.valueOf(requirementId).
        requirementProgress: (json['requirementProgress'] as Map?)?.map(
          (k, v) => MapEntry(k.toString(), RequirementProgress.fromJson(Map<String, dynamic>.from(v as Map))),
        ),
        nextMilestoneDate: _d(json['nextMilestoneDate']),
        nextMilestoneName: _s(json['nextMilestoneName']),
        createdAt: _d(json['createdAt']) ?? DateTime.now(),
        updatedAt: _d(json['updatedAt']),
      );

  final int id;
  final String discipleId;
  final String discipleName;
  final int journeyId;
  final String journeyName;
  final int currentStageId;
  final String currentStageName;
  final int currentStageOrder;
  final int completedStages;
  final int totalStages;
  final int completedRequirements;
  final int totalRequirements;
  final DateTime? startedAt;
  final DateTime? lastActivityAt;
  final DateTime? completedAt;
  final ProgressStatus status;
  final Map<String, RequirementProgress>? requirementProgress;
  final DateTime? nextMilestoneDate;
  final String? nextMilestoneName;
  final DateTime createdAt;
  final DateTime? updatedAt;

  double get completionPercentage => totalStages > 0 ? (completedStages / totalStages) * 100 : 0;
  double get requirementsPercentage =>
      totalRequirements > 0 ? (completedRequirements / totalRequirements) * 100 : 0;

  /// Corps createProgress — clés lues par le serveur uniquement
  /// (le statut est imposé NOT_STARTED côté serveur).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'discipleId': discipleId,
        'journeyId': journeyId,
        'currentStageId': currentStageId == 0 ? null : currentStageId,
        'completedStages': completedStages,
        'totalStages': totalStages,
        'completedRequirements': completedRequirements,
        'totalRequirements': totalRequirements,
      };
}

class RequirementProgress {
  const RequirementProgress({
    required this.requirementId,
    this.requirementName = '',
    this.status = RequirementStatus.pending,
    this.completedAt,
    this.evidence,
    this.notes,
    this.verifiedById,
    this.verifiedByName,
  });

  factory RequirementProgress.fromJson(Map<String, dynamic> json) => RequirementProgress(
        // requirementId = BIGSERIAL (r.getRequirementId(), Long) → int.
        requirementId: _i(json['requirementId']) ?? 0,
        requirementName: _s(json['requirementName']) ?? '',
        status: RequirementStatus.fromWire(json['status']),
        completedAt: _d(json['completedAt']),
        evidence: _s(json['evidence']),
        notes: _s(json['notes']),
        // verifiedById = UUID (r.getVerifiedBy()) → String.
        verifiedById: _s(json['verifiedById']),
        verifiedByName: _s(json['verifiedByName']),
      );

  final int requirementId;
  final String requirementName;
  final RequirementStatus status;
  final DateTime? completedAt;
  final String? evidence;
  final String? notes;
  final String? verifiedById;
  final String? verifiedByName;
}

// ---------- Assignment (assignmentView) ----------

class MentorAssignment {
  const MentorAssignment({
    required this.id,
    required this.mentorId,
    required this.mentorName,
    required this.discipleId,
    required this.discipleName,
    required this.journeyId,
    required this.assignedAt,
    this.endedAt,
    this.status = AssignmentStatus.active,
    this.notes,
    this.meetingFrequencyDays = 7,
    this.lastMeetingAt,
    this.nextMeetingAt,
  });

  factory MentorAssignment.fromJson(Map<String, dynamic> json) => MentorAssignment(
        id: _i(json['id']) ?? 0,
        // mentorId/discipleId = UUID → String (assignation vue serveur R7).
        mentorId: _s(json['mentorId']) ?? '',
        mentorName: _s(json['mentorName']) ?? '',
        discipleId: _s(json['discipleId']) ?? '',
        discipleName: _s(json['discipleName']) ?? '',
        journeyId: _i(json['journeyId']) ?? 0,
        assignedAt: _d(json['assignedAt']) ?? DateTime.now(),
        endedAt: _d(json['endedAt']),
        status: AssignmentStatus.fromWire(json['status']),
        notes: _s(json['notes']),
        meetingFrequencyDays: _i(json['meetingFrequencyDays']) ?? 7,
        lastMeetingAt: _d(json['lastMeetingAt']),
        nextMeetingAt: _d(json['nextMeetingAt']),
      );

  final int id;
  final String mentorId;
  final String mentorName;
  final String discipleId;
  final String discipleName;
  final int journeyId;
  final DateTime assignedAt;
  final DateTime? endedAt;
  final AssignmentStatus status;
  final String? notes;
  final int meetingFrequencyDays;
  final DateTime? lastMeetingAt;
  final DateTime? nextMeetingAt;

  /// Corps createAssignment — clés lues par le serveur uniquement
  /// (le statut est imposé ACTIVE côté serveur).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'mentorId': mentorId,
        'discipleId': discipleId,
        'journeyId': journeyId,
        'meetingFrequencyDays': meetingFrequencyDays,
        'notes': notes,
      };
}

// ---------- Meeting (meetingView) ----------

class MentorMeeting {
  const MentorMeeting({
    required this.id,
    required this.assignmentId,
    required this.mentorId,
    required this.discipleId,
    required this.scheduledAt,
    this.actualAt,
    this.status = MeetingStatus.scheduled,
    this.notes,
    this.actionItems,
    this.nextSteps,
    this.durationMinutes,
    this.location,
    this.mentorName,
    this.discipleName,
    this.isGroup = false,
    this.createdAt,
    this.updatedAt,
  });

  factory MentorMeeting.fromJson(Map<String, dynamic> json) => MentorMeeting(
        id: _i(json['id']) ?? 0,
        assignmentId: _i(json['assignmentId']) ?? 0,
        // mentorId/discipleId = UUID → String.
        mentorId: _s(json['mentorId']) ?? '',
        discipleId: _s(json['discipleId']) ?? '',
        scheduledAt: _d(json['scheduledAt']) ?? DateTime.now(),
        actualAt: _d(json['actualAt']),
        status: MeetingStatus.fromWire(json['status']),
        notes: _s(json['notes']),
        actionItems: _s(json['actionItems']),
        nextSteps: _s(json['nextSteps']),
        durationMinutes: _i(json['durationMinutes']),
        location: _s(json['location']),
        mentorName: _s(json['mentorName']),
        discipleName: _s(json['discipleName']),
        isGroup: _b(json['isGroup'], false),
        createdAt: _d(json['createdAt']),
        updatedAt: _d(json['updatedAt']),
      );

  final int id;
  final int assignmentId;
  final String mentorId;
  final String discipleId;
  final DateTime scheduledAt;
  final DateTime? actualAt;
  final MeetingStatus status;
  final String? notes;
  final String? actionItems;
  final String? nextSteps;
  final int? durationMinutes;
  final String? location;
  final String? mentorName;
  final String? discipleName;
  final bool isGroup;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  /// Corps scheduleMeeting — clés lues par le serveur uniquement
  /// (le statut est imposé SCHEDULED côté serveur).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'assignmentId': assignmentId,
        'mentorId': mentorId.isEmpty ? null : mentorId,
        'discipleId': discipleId.isEmpty ? null : discipleId,
        'scheduledAt': _iso(scheduledAt),
        'location': location,
        'isGroup': isGroup,
      };
}

// ---------- Report (getReport) ----------

class DiscipleshipReport {
  const DiscipleshipReport({
    required this.journeyId,
    required this.journeyName,
    this.totalDisciples = 0,
    this.activeDisciples = 0,
    this.completedDisciples = 0,
    this.stalledDisciples = 0,
    this.averageCompletion = 0,
    this.totalMeetings = 0,
    this.completedMeetings = 0,
    this.stageDistribution = const {},
    this.statusDistribution = const {},
    this.topMentors = const [],
    required this.generatedAt,
  });

  factory DiscipleshipReport.fromJson(Map<String, dynamic> json) => DiscipleshipReport(
        journeyId: _i(json['journeyId']) ?? 0,
        journeyName: _s(json['journeyName']) ?? '',
        totalDisciples: _i(json['totalDisciples']) ?? 0,
        activeDisciples: _i(json['activeDisciples']) ?? 0,
        completedDisciples: _i(json['completedDisciples']) ?? 0,
        stalledDisciples: _i(json['stalledDisciples']) ?? 0,
        averageCompletion: _f(json['averageCompletion']) ?? 0,
        totalMeetings: _i(json['totalMeetings']) ?? 0,
        completedMeetings: _i(json['completedMeetings']) ?? 0,
        stageDistribution: (json['stageDistribution'] as Map?)?.map(
              (k, v) => MapEntry(k.toString(), _i(v) ?? 0),
            ) ??
            const {},
        statusDistribution: (json['statusDistribution'] as Map?)?.map(
              (k, v) => MapEntry(k.toString(), _i(v) ?? 0),
            ) ??
            const {},
        topMentors: (json['topMentors'] as List?)
                ?.map((e) => TopMentor.fromJson(Map<String, dynamic>.from(e as Map)))
                .toList() ??
            const [],
        generatedAt: _d(json['generatedAt']) ?? DateTime.now(),
      );

  final int journeyId;
  final String journeyName;
  final int totalDisciples;
  final int activeDisciples;
  final int completedDisciples;
  final int stalledDisciples;
  final double averageCompletion;
  final int totalMeetings;
  final int completedMeetings;
  final Map<String, int> stageDistribution;
  final Map<String, int> statusDistribution;
  final List<TopMentor> topMentors;
  final DateTime generatedAt;
}

/// getTopMentors agrège mentorId (UUID → String), mentorName, discipleCount,
/// meetingCount. `averageCompletion` avait été retiré : le serveur ne le
/// renvoie PAS dans cette vue (vérifié DiscipleshipService.getTopMentors).
class TopMentor {
  const TopMentor({
    required this.mentorId,
    required this.mentorName,
    this.discipleCount = 0,
    this.meetingCount = 0,
  });

  factory TopMentor.fromJson(Map<String, dynamic> json) => TopMentor(
        mentorId: _s(json['mentorId']) ?? '',
        mentorName: _s(json['mentorName']) ?? '',
        discipleCount: _i(json['discipleCount']) ?? 0,
        meetingCount: _i(json['meetingCount']) ?? 0,
      );

  final String mentorId;
  final String mentorName;
  final int discipleCount;
  final int meetingCount;
}
