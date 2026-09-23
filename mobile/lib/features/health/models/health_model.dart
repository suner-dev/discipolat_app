import 'package:freezed_annotation/freezed_annotation.dart';

part 'health_model.freezed.dart';
part 'health_model.g.dart';

@freezed
class Patient with _$Patient {
  const factory Patient({
    required int id,
    required String firstName,
    required String lastName,
    required DateTime dateOfBirth,
    required String gender,
    String? phone,
    String? email,
    String? address,
    String? emergencyContactName,
    String? emergencyContactPhone,
    String? bloodType,
    List<String>? allergies,
    List<String>? chronicConditions,
    List<String>? currentMedications,
    String? insuranceProvider,
    String? insuranceNumber,
    String? photoUrl,
    required PatientStatus status,
    String? notes,
    required DateTime createdAt,
    DateTime? updatedAt,
    int? familyId,
    String? familyName,
  }) = _Patient;

  factory Patient.fromJson(Map<String, dynamic> json) => _$PatientFromJson(json);

  const Patient._();

  String get fullName => '$firstName $lastName';
  int get age => DateTime.now().year - dateOfBirth.year - (DateTime.now().month < dateOfBirth.month || (DateTime.now().month == dateOfBirth.month && DateTime.now().day < dateOfBirth.day) ? 1 : 0);
}

@freezed
class Consultation with _$Consultation {
  const factory Consultation({
    required int id,
    required int patientId,
    required String patientName,
    required int doctorId,
    required String doctorName,
    required DateTime dateTime,
    required ConsultationType type,
    String? chiefComplaint,
    String? diagnosis,
    String? treatmentPlan,
    String? notes,
    List<VitalSigns>? vitalSigns,
    List<Prescription>? prescriptions,
    required ConsultationStatus status,
    double? fee,
    String? paymentStatus,
    DateTime? nextAppointment,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _Consultation;

  factory Consultation.fromJson(Map<String, dynamic> json) => _$ConsultationFromJson(json);
}

@freezed
class VitalSigns with _$VitalSigns {
  const factory VitalSigns({
    double? temperature,
    int? heartRate,
    int? systolicBP,
    int? diastolicBP,
    int? respiratoryRate,
    double? oxygenSaturation,
    double? weight,
    double? height,
    double? bmi,
  }) = _VitalSigns;

  factory VitalSigns.fromJson(Map<String, dynamic> json) => _$VitalSignsFromJson(json);
}

@freezed
class Prescription with _$Prescription {
  const factory Prescription({
    required int id,
    required int consultationId,
    required int medicationId,
    required String medicationName,
    required String dosage,
    required String frequency,
    required String duration,
    String? instructions,
    required DateTime startDate,
    DateTime? endDate,
    @Default(false) bool isActive,
  }) = _Prescription;

  factory Prescription.fromJson(Map<String, dynamic> json) => _$PrescriptionFromJson(json);
}

@freezed
class Medication with _$Medication {
  const factory Medication({
    required int id,
    required String name,
    String? genericName,
    String? brandName,
    String? form,
    String? strength,
    String? manufacturer,
    String? category,
    String? description,
    double? unitPrice,
    String? unit,
    @Default(true) bool requiresPrescription,
    @Default(true) bool isActive,
  }) = _Medication;

  factory Medication.fromJson(Map<String, dynamic> json) => _$MedicationFromJson(json);
}

@freezed
class PharmacyStock with _$PharmacyStock {
  const factory PharmacyStock({
    required int id,
    required int medicationId,
    required String medicationName,
    required int quantity,
    required int minStockLevel,
    required int maxStockLevel,
    required String location,
    required DateTime expiryDate,
    String? batchNumber,
    double? unitCost,
    double? sellingPrice,
    @Default(0) int reservedQuantity,
    @Default(true) bool isActive,
  }) = _PharmacyStock;

  factory PharmacyStock.fromJson(Map<String, dynamic> json) => _$PharmacyStockFromJson(json);

  const PharmacyStock._();

  bool get isLowStock => quantity <= minStockLevel;
  bool get isExpired => DateTime.now().isAfter(expiryDate);
  int get availableQuantity => quantity - reservedQuantity;
}

@freezed
class HealthCampaign with _$HealthCampaign {
  const factory HealthCampaign({
    required int id,
    required String name,
    String? description,
    required CampaignType type,
    required DateTime startDate,
    required DateTime endDate,
    String? location,
    int? targetPopulation,
    @Default(0) int registeredCount,
    @Default(0) int attendedCount,
    required CampaignStatus status,
    int? coordinatorId,
    String? coordinatorName,
    List<String>? targetGroups,
    List<String>? servicesOffered,
    String? notes,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _HealthCampaign;

  factory HealthCampaign.fromJson(Map<String, dynamic> json) => _$HealthCampaignFromJson(json);

  const HealthCampaign._();

  bool get isActive => status == CampaignStatus.active && DateTime.now().isBefore(endDate) && DateTime.now().isAfter(startDate);
  bool get isUpcoming => status == CampaignStatus.planned && DateTime.now().isBefore(startDate);
  bool get isCompleted => status == CampaignStatus.completed || DateTime.now().isAfter(endDate);
}

@freezed
class CampaignParticipant with _$CampaignParticipant {
  const factory CampaignParticipant({
    required int id,
    required int campaignId,
    required int patientId,
    required String patientName,
    required DateTime registrationDate,
    DateTime? attendanceDate,
    required ParticipationStatus status,
    String? notes,
  }) = _CampaignParticipant;

  factory CampaignParticipant.fromJson(Map<String, dynamic> json) => _$CampaignParticipantFromJson(json);
}

@freezed
class MedicalKit with _$MedicalKit {
  const factory MedicalKit({
    required int id,
    required String name,
    String? description,
    required KitType type,
    required List<KitItem> items,
    String? location,
    int? assignedToId,
    String? assignedToName,
    DateTime? lastInspectionDate,
    DateTime? nextInspectionDate,
    required KitStatus status,
    String? notes,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _MedicalKit;

  factory MedicalKit.fromJson(Map<String, dynamic> json) => _$MedicalKitFromJson(json);

  const MedicalKit._();

  bool get needsInspection => nextInspectionDate != null && DateTime.now().isAfter(nextInspectionDate!);
  bool get isComplete => items.every((item) => item.currentQuantity >= item.requiredQuantity);
}

@freezed
class KitItem with _$KitItem {
  const factory KitItem({
    required int id,
    required int kitId,
    required int medicationId,
    required String medicationName,
    required int requiredQuantity,
    required int currentQuantity,
    String? notes,
  }) = _KitItem;

  factory KitItem.fromJson(Map<String, dynamic> json) => _$KitItemFromJson(json);

  const KitItem._();

  bool get isSufficient => currentQuantity >= requiredQuantity;
}

@freezed
class StaffDuty with _$StaffDuty {
  const factory StaffDuty({
    required int id,
    required int staffId,
    required String staffName,
    required String role,
    required DateTime startTime,
    required DateTime endTime,
    required DutyStatus status,
    String? location,
    String? notes,
    DateTime? actualStartTime,
    DateTime? actualEndTime,
  }) = _StaffDuty;

  factory StaffDuty.fromJson(Map<String, dynamic> json) => _$StaffDutyFromJson(json);
}

enum PatientStatus {
  @JsonValue('ACTIVE')
  active,
  @JsonValue('INACTIVE')
  inactive,
  @JsonValue('DISCHARGED')
  discharged,
  @JsonValue('DECEASED')
  deceased;

  String get displayName {
    switch (this) {
      case PatientStatus.active:
        return 'Actif';
      case PatientStatus.inactive:
        return 'Inactif';
      case PatientStatus.discharged:
        return 'Sorti';
      case PatientStatus.deceased:
        return 'Décédé';
    }
  }
}

enum ConsultationType {
  @JsonValue('GENERAL')
  general,
  @JsonValue('SPECIALIST')
  specialist,
  @JsonValue('EMERGENCY')
  emergency,
  @JsonValue('FOLLOW_UP')
  followUp,
  @JsonValue('PREVENTIVE')
  preventive,
  @JsonValue('PRENATAL')
  prenatal,
  @JsonValue('VACCINATION')
  vaccination;

  String get displayName {
    switch (this) {
      case ConsultationType.general:
        return 'Générale';
      case ConsultationType.specialist:
        return 'Spécialiste';
      case ConsultationType.emergency:
        return 'Urgence';
      case ConsultationType.followUp:
        return 'Suivi';
      case ConsultationType.preventive:
        return 'Préventive';
      case ConsultationType.prenatal:
        return 'Prénatale';
      case ConsultationType.vaccination:
        return 'Vaccination';
    }
  }
}

enum ConsultationStatus {
  @JsonValue('SCHEDULED')
  scheduled,
  @JsonValue('IN_PROGRESS')
  inProgress,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('NO_SHOW')
  noShow;

  String get displayName {
    switch (this) {
      case ConsultationStatus.scheduled:
        return 'Planifiée';
      case ConsultationStatus.inProgress:
        return 'En cours';
      case ConsultationStatus.completed:
        return 'Terminée';
      case ConsultationStatus.cancelled:
        return 'Annulée';
      case ConsultationStatus.noShow:
        return 'Absent';
    }
  }
}

enum CampaignType {
  @JsonValue('VACCINATION')
  vaccination,
  @JsonValue('SCREENING')
  screening,
  @JsonValue('AWARENESS')
  awareness,
  @JsonValue('BLOOD_DONATION')
  bloodDonation,
  @JsonValue('HEALTH_CHECK')
  healthCheck,
  @JsonValue('NUTRITION')
  nutrition,
  @JsonValue('MATERNAL_CHILD')
  maternalChild;

  String get displayName {
    switch (this) {
      case CampaignType.vaccination:
        return 'Vaccination';
      case CampaignType.screening:
        return 'Dépistage';
      case CampaignType.awareness:
        return 'Sensibilisation';
      case CampaignType.bloodDonation:
        return 'Don de sang';
      case CampaignType.healthCheck:
        return 'Bilan de santé';
      case CampaignType.nutrition:
        return 'Nutrition';
      case CampaignType.maternalChild:
        return 'Mère-enfant';
    }
  }
}

enum CampaignStatus {
  @JsonValue('PLANNED')
  planned,
  @JsonValue('ACTIVE')
  active,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled;

  String get displayName {
    switch (this) {
      case CampaignStatus.planned:
        return 'Planifiée';
      case CampaignStatus.active:
        return 'Active';
      case CampaignStatus.completed:
        return 'Terminée';
      case CampaignStatus.cancelled:
        return 'Annulée';
    }
  }
}

enum ParticipationStatus {
  @JsonValue('REGISTERED')
  registered,
  @JsonValue('ATTENDED')
  attended,
  @JsonValue('ABSENT')
  absent,
  @JsonValue('CANCELLED')
  cancelled,
}

enum KitType {
  @JsonValue('FIRST_AID')
  firstAid,
  @JsonValue('EMERGENCY')
  emergency,
  @JsonValue('TRAUMA')
  trauma,
  @JsonValue('DELIVERY')
  delivery,
  @JsonValue('CUSTOM')
  custom,
}

enum KitStatus {
  @JsonValue('READY')
  ready,
  @JsonValue('IN_USE')
  inUse,
  @JsonValue('NEEDS_RESTOCK')
  needsRestock,
  @JsonValue('EXPIRED')
  expired,
}

enum DutyStatus {
  @JsonValue('SCHEDULED')
  scheduled,
  @JsonValue('IN_PROGRESS')
  inProgress,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('MISSED')
  missed,
}