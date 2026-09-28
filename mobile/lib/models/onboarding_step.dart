// B7 — Modèle du wizard d'onboarding (contrat §3.1 du plan).
//
// Parsing STRICT : un champ manquant ou mal typé lève une FormatException.
// Le contrat est figé (R2) : on ne devine jamais un nom de champ, on échoue
// visiblement plutôt que d'afficher un écran faux.

enum OnboardingStepType {
  churchIdentity,
  memberImport,
  structure,
  roles,
  branding,
  modules,
  firstEvent;

  static OnboardingStepType fromJson(String raw) {
    switch (raw) {
      case 'CHURCH_IDENTITY':
        return OnboardingStepType.churchIdentity;
      case 'MEMBER_IMPORT':
        return OnboardingStepType.memberImport;
      case 'STRUCTURE':
        return OnboardingStepType.structure;
      case 'ROLES':
        return OnboardingStepType.roles;
      case 'BRANDING':
        return OnboardingStepType.branding;
      case 'MODULES':
        return OnboardingStepType.modules;
      case 'FIRST_EVENT':
        return OnboardingStepType.firstEvent;
      default:
        throw FormatException('Type d\'étape inconnu : $raw');
    }
  }
}

enum OnboardingStepStatus {
  pending,
  inProgress,
  completed,
  skipped;

  static OnboardingStepStatus fromJson(String raw) {
    switch (raw) {
      case 'PENDING':
        return OnboardingStepStatus.pending;
      case 'IN_PROGRESS':
        return OnboardingStepStatus.inProgress;
      case 'COMPLETED':
        return OnboardingStepStatus.completed;
      case 'SKIPPED':
        return OnboardingStepStatus.skipped;
      default:
        throw FormatException('Statut d\'étape inconnu : $raw');
    }
  }
}

class OnboardingStep {
  const OnboardingStep({
    required this.id,
    required this.stepType,
    required this.stepOrder,
    required this.title,
    required this.description,
    required this.status,
    required this.isCompleted,
    required this.isSkippable,
    required this.skipRequiresReason,
    required this.startedAt,
    required this.completedAt,
    required this.completedData,
  });

  final String id;
  final OnboardingStepType stepType;
  final int stepOrder;
  final String title;
  final String? description;
  final OnboardingStepStatus status;
  final bool isCompleted;
  final bool isSkippable;
  final bool skipRequiresReason;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final Map<String, dynamic>? completedData;

  static DateTime? _date(dynamic v) =>
      v == null ? null : DateTime.tryParse(v as String);

  static OnboardingStep fromJson(Map<String, dynamic> json) {
    T required<T>(String key) {
      if (!json.containsKey(key) || json[key] == null) {
        throw FormatException('Champ manquant : $key');
      }
      return json[key] as T;
    }

    final rawData = json['completedData'];
    return OnboardingStep(
      id: required<String>('id'),
      stepType: OnboardingStepType.fromJson(required<String>('stepType')),
      stepOrder: required<int>('stepOrder'),
      title: required<String>('title'),
      description: json['description'] as String?,
      status: OnboardingStepStatus.fromJson(required<String>('status')),
      isCompleted: required<bool>('isCompleted'),
      isSkippable: required<bool>('isSkippable'),
      skipRequiresReason: required<bool>('skipRequiresReason'),
      startedAt: _date(json['startedAt']),
      completedAt: _date(json['completedAt']),
      completedData: rawData is Map<String, dynamic> ? rawData : null,
    );
  }
}

class OnboardingProgress {
  const OnboardingProgress({
    required this.totalSteps,
    required this.completedSteps,
    required this.skippedSteps,
    required this.percentage,
    required this.isComplete,
    required this.steps,
  });

  final int totalSteps;
  final int completedSteps;
  final int skippedSteps;
  final int percentage;
  final bool isComplete;
  final List<OnboardingStep> steps;

  static OnboardingProgress fromJson(Map<String, dynamic> json) {
    final steps = (json['steps'] as List<dynamic>? ?? const [])
        .cast<Map<String, dynamic>>()
        .map(OnboardingStep.fromJson)
        .toList();
    return OnboardingProgress(
      totalSteps: (json['totalSteps'] as num?)?.toInt() ?? steps.length,
      completedSteps: (json['completedSteps'] as num?)?.toInt() ?? 0,
      skippedSteps: (json['skippedSteps'] as num?)?.toInt() ?? 0,
      percentage: (json['percentage'] as num?)?.toInt() ?? 0,
      isComplete: json['isComplete'] as bool? ?? false,
      steps: steps,
    );
  }
}

class OnboardingStatus {
  const OnboardingStatus({
    required this.completed,
    required this.completedAt,
    required this.completedBy,
    required this.totalSteps,
    required this.completedSteps,
    required this.skippedSteps,
    required this.percentage,
  });

  final bool completed;
  final DateTime? completedAt;
  final String? completedBy;
  final int totalSteps;
  final int completedSteps;
  final int skippedSteps;
  final int percentage;

  static OnboardingStatus fromJson(Map<String, dynamic> json) => OnboardingStatus(
        completed: json['completed'] as bool? ?? false,
        completedAt: json['completedAt'] == null
            ? null
            : DateTime.tryParse(json['completedAt'] as String),
        completedBy: json['completedBy'] as String?,
        totalSteps: (json['totalSteps'] as num?)?.toInt() ?? 0,
        completedSteps: (json['completedSteps'] as num?)?.toInt() ?? 0,
        skippedSteps: (json['skippedSteps'] as num?)?.toInt() ?? 0,
        percentage: (json['percentage'] as num?)?.toInt() ?? 0,
      );
}
