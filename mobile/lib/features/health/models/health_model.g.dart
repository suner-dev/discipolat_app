// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'health_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$PatientImpl _$$PatientImplFromJson(Map<String, dynamic> json) =>
    _$PatientImpl(
      id: (json['id'] as num).toInt(),
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String,
      dateOfBirth: DateTime.parse(json['dateOfBirth'] as String),
      gender: json['gender'] as String,
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      address: json['address'] as String?,
      emergencyContactName: json['emergencyContactName'] as String?,
      emergencyContactPhone: json['emergencyContactPhone'] as String?,
      bloodType: json['bloodType'] as String?,
      allergies: (json['allergies'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      chronicConditions: (json['chronicConditions'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      currentMedications: (json['currentMedications'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      insuranceProvider: json['insuranceProvider'] as String?,
      insuranceNumber: json['insuranceNumber'] as String?,
      photoUrl: json['photoUrl'] as String?,
      status: $enumDecode(_$PatientStatusEnumMap, json['status']),
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
      familyId: (json['familyId'] as num?)?.toInt(),
      familyName: json['familyName'] as String?,
    );

Map<String, dynamic> _$$PatientImplToJson(_$PatientImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'firstName': instance.firstName,
      'lastName': instance.lastName,
      'dateOfBirth': instance.dateOfBirth.toIso8601String(),
      'gender': instance.gender,
      'phone': instance.phone,
      'email': instance.email,
      'address': instance.address,
      'emergencyContactName': instance.emergencyContactName,
      'emergencyContactPhone': instance.emergencyContactPhone,
      'bloodType': instance.bloodType,
      'allergies': instance.allergies,
      'chronicConditions': instance.chronicConditions,
      'currentMedications': instance.currentMedications,
      'insuranceProvider': instance.insuranceProvider,
      'insuranceNumber': instance.insuranceNumber,
      'photoUrl': instance.photoUrl,
      'status': _$PatientStatusEnumMap[instance.status]!,
      'notes': instance.notes,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
      'familyId': instance.familyId,
      'familyName': instance.familyName,
    };

const _$PatientStatusEnumMap = {
  PatientStatus.active: 'ACTIVE',
  PatientStatus.inactive: 'INACTIVE',
  PatientStatus.discharged: 'DISCHARGED',
  PatientStatus.deceased: 'DECEASED',
};

_$ConsultationImpl _$$ConsultationImplFromJson(Map<String, dynamic> json) =>
    _$ConsultationImpl(
      id: (json['id'] as num).toInt(),
      patientId: (json['patientId'] as num).toInt(),
      patientName: json['patientName'] as String,
      doctorId: (json['doctorId'] as num).toInt(),
      doctorName: json['doctorName'] as String,
      dateTime: DateTime.parse(json['dateTime'] as String),
      type: $enumDecode(_$ConsultationTypeEnumMap, json['type']),
      chiefComplaint: json['chiefComplaint'] as String?,
      diagnosis: json['diagnosis'] as String?,
      treatmentPlan: json['treatmentPlan'] as String?,
      notes: json['notes'] as String?,
      vitalSigns: (json['vitalSigns'] as List<dynamic>?)
          ?.map((e) => VitalSigns.fromJson(e as Map<String, dynamic>))
          .toList(),
      prescriptions: (json['prescriptions'] as List<dynamic>?)
          ?.map((e) => Prescription.fromJson(e as Map<String, dynamic>))
          .toList(),
      status: $enumDecode(_$ConsultationStatusEnumMap, json['status']),
      fee: (json['fee'] as num?)?.toDouble(),
      paymentStatus: json['paymentStatus'] as String?,
      nextAppointment: json['nextAppointment'] == null
          ? null
          : DateTime.parse(json['nextAppointment'] as String),
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$ConsultationImplToJson(_$ConsultationImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'patientId': instance.patientId,
      'patientName': instance.patientName,
      'doctorId': instance.doctorId,
      'doctorName': instance.doctorName,
      'dateTime': instance.dateTime.toIso8601String(),
      'type': _$ConsultationTypeEnumMap[instance.type]!,
      'chiefComplaint': instance.chiefComplaint,
      'diagnosis': instance.diagnosis,
      'treatmentPlan': instance.treatmentPlan,
      'notes': instance.notes,
      'vitalSigns': instance.vitalSigns,
      'prescriptions': instance.prescriptions,
      'status': _$ConsultationStatusEnumMap[instance.status]!,
      'fee': instance.fee,
      'paymentStatus': instance.paymentStatus,
      'nextAppointment': instance.nextAppointment?.toIso8601String(),
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$ConsultationTypeEnumMap = {
  ConsultationType.general: 'GENERAL',
  ConsultationType.specialist: 'SPECIALIST',
  ConsultationType.emergency: 'EMERGENCY',
  ConsultationType.followUp: 'FOLLOW_UP',
  ConsultationType.preventive: 'PREVENTIVE',
  ConsultationType.prenatal: 'PRENATAL',
  ConsultationType.vaccination: 'VACCINATION',
};

const _$ConsultationStatusEnumMap = {
  ConsultationStatus.scheduled: 'SCHEDULED',
  ConsultationStatus.inProgress: 'IN_PROGRESS',
  ConsultationStatus.completed: 'COMPLETED',
  ConsultationStatus.cancelled: 'CANCELLED',
  ConsultationStatus.noShow: 'NO_SHOW',
};

_$VitalSignsImpl _$$VitalSignsImplFromJson(Map<String, dynamic> json) =>
    _$VitalSignsImpl(
      temperature: (json['temperature'] as num?)?.toDouble(),
      heartRate: (json['heartRate'] as num?)?.toInt(),
      systolicBP: (json['systolicBP'] as num?)?.toInt(),
      diastolicBP: (json['diastolicBP'] as num?)?.toInt(),
      respiratoryRate: (json['respiratoryRate'] as num?)?.toInt(),
      oxygenSaturation: (json['oxygenSaturation'] as num?)?.toDouble(),
      weight: (json['weight'] as num?)?.toDouble(),
      height: (json['height'] as num?)?.toDouble(),
      bmi: (json['bmi'] as num?)?.toDouble(),
    );

Map<String, dynamic> _$$VitalSignsImplToJson(_$VitalSignsImpl instance) =>
    <String, dynamic>{
      'temperature': instance.temperature,
      'heartRate': instance.heartRate,
      'systolicBP': instance.systolicBP,
      'diastolicBP': instance.diastolicBP,
      'respiratoryRate': instance.respiratoryRate,
      'oxygenSaturation': instance.oxygenSaturation,
      'weight': instance.weight,
      'height': instance.height,
      'bmi': instance.bmi,
    };

_$PrescriptionImpl _$$PrescriptionImplFromJson(Map<String, dynamic> json) =>
    _$PrescriptionImpl(
      id: (json['id'] as num).toInt(),
      consultationId: (json['consultationId'] as num).toInt(),
      medicationId: (json['medicationId'] as num).toInt(),
      medicationName: json['medicationName'] as String,
      dosage: json['dosage'] as String,
      frequency: json['frequency'] as String,
      duration: json['duration'] as String,
      instructions: json['instructions'] as String?,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: json['endDate'] == null
          ? null
          : DateTime.parse(json['endDate'] as String),
      isActive: json['isActive'] as bool? ?? false,
    );

Map<String, dynamic> _$$PrescriptionImplToJson(_$PrescriptionImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'consultationId': instance.consultationId,
      'medicationId': instance.medicationId,
      'medicationName': instance.medicationName,
      'dosage': instance.dosage,
      'frequency': instance.frequency,
      'duration': instance.duration,
      'instructions': instance.instructions,
      'startDate': instance.startDate.toIso8601String(),
      'endDate': instance.endDate?.toIso8601String(),
      'isActive': instance.isActive,
    };

_$MedicationImpl _$$MedicationImplFromJson(Map<String, dynamic> json) =>
    _$MedicationImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      genericName: json['genericName'] as String?,
      brandName: json['brandName'] as String?,
      form: json['form'] as String?,
      strength: json['strength'] as String?,
      manufacturer: json['manufacturer'] as String?,
      category: json['category'] as String?,
      description: json['description'] as String?,
      unitPrice: (json['unitPrice'] as num?)?.toDouble(),
      unit: json['unit'] as String?,
      requiresPrescription: json['requiresPrescription'] as bool? ?? true,
      isActive: json['isActive'] as bool? ?? true,
    );

Map<String, dynamic> _$$MedicationImplToJson(_$MedicationImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'genericName': instance.genericName,
      'brandName': instance.brandName,
      'form': instance.form,
      'strength': instance.strength,
      'manufacturer': instance.manufacturer,
      'category': instance.category,
      'description': instance.description,
      'unitPrice': instance.unitPrice,
      'unit': instance.unit,
      'requiresPrescription': instance.requiresPrescription,
      'isActive': instance.isActive,
    };

_$PharmacyStockImpl _$$PharmacyStockImplFromJson(Map<String, dynamic> json) =>
    _$PharmacyStockImpl(
      id: (json['id'] as num).toInt(),
      medicationId: (json['medicationId'] as num).toInt(),
      medicationName: json['medicationName'] as String,
      quantity: (json['quantity'] as num).toInt(),
      minStockLevel: (json['minStockLevel'] as num).toInt(),
      maxStockLevel: (json['maxStockLevel'] as num).toInt(),
      location: json['location'] as String,
      expiryDate: DateTime.parse(json['expiryDate'] as String),
      batchNumber: json['batchNumber'] as String?,
      unitCost: (json['unitCost'] as num?)?.toDouble(),
      sellingPrice: (json['sellingPrice'] as num?)?.toDouble(),
      reservedQuantity: (json['reservedQuantity'] as num?)?.toInt() ?? 0,
      isActive: json['isActive'] as bool? ?? true,
    );

Map<String, dynamic> _$$PharmacyStockImplToJson(_$PharmacyStockImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'medicationId': instance.medicationId,
      'medicationName': instance.medicationName,
      'quantity': instance.quantity,
      'minStockLevel': instance.minStockLevel,
      'maxStockLevel': instance.maxStockLevel,
      'location': instance.location,
      'expiryDate': instance.expiryDate.toIso8601String(),
      'batchNumber': instance.batchNumber,
      'unitCost': instance.unitCost,
      'sellingPrice': instance.sellingPrice,
      'reservedQuantity': instance.reservedQuantity,
      'isActive': instance.isActive,
    };

_$HealthCampaignImpl _$$HealthCampaignImplFromJson(Map<String, dynamic> json) =>
    _$HealthCampaignImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      type: $enumDecode(_$CampaignTypeEnumMap, json['type']),
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      location: json['location'] as String?,
      targetPopulation: (json['targetPopulation'] as num?)?.toInt(),
      registeredCount: (json['registeredCount'] as num?)?.toInt() ?? 0,
      attendedCount: (json['attendedCount'] as num?)?.toInt() ?? 0,
      status: $enumDecode(_$CampaignStatusEnumMap, json['status']),
      coordinatorId: (json['coordinatorId'] as num?)?.toInt(),
      coordinatorName: json['coordinatorName'] as String?,
      targetGroups: (json['targetGroups'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      servicesOffered: (json['servicesOffered'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$HealthCampaignImplToJson(
        _$HealthCampaignImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'type': _$CampaignTypeEnumMap[instance.type]!,
      'startDate': instance.startDate.toIso8601String(),
      'endDate': instance.endDate.toIso8601String(),
      'location': instance.location,
      'targetPopulation': instance.targetPopulation,
      'registeredCount': instance.registeredCount,
      'attendedCount': instance.attendedCount,
      'status': _$CampaignStatusEnumMap[instance.status]!,
      'coordinatorId': instance.coordinatorId,
      'coordinatorName': instance.coordinatorName,
      'targetGroups': instance.targetGroups,
      'servicesOffered': instance.servicesOffered,
      'notes': instance.notes,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$CampaignTypeEnumMap = {
  CampaignType.vaccination: 'VACCINATION',
  CampaignType.screening: 'SCREENING',
  CampaignType.awareness: 'AWARENESS',
  CampaignType.bloodDonation: 'BLOOD_DONATION',
  CampaignType.healthCheck: 'HEALTH_CHECK',
  CampaignType.nutrition: 'NUTRITION',
  CampaignType.maternalChild: 'MATERNAL_CHILD',
};

const _$CampaignStatusEnumMap = {
  CampaignStatus.planned: 'PLANNED',
  CampaignStatus.active: 'ACTIVE',
  CampaignStatus.completed: 'COMPLETED',
  CampaignStatus.cancelled: 'CANCELLED',
};

_$CampaignParticipantImpl _$$CampaignParticipantImplFromJson(
        Map<String, dynamic> json) =>
    _$CampaignParticipantImpl(
      id: (json['id'] as num).toInt(),
      campaignId: (json['campaignId'] as num).toInt(),
      patientId: (json['patientId'] as num).toInt(),
      patientName: json['patientName'] as String,
      registrationDate: DateTime.parse(json['registrationDate'] as String),
      attendanceDate: json['attendanceDate'] == null
          ? null
          : DateTime.parse(json['attendanceDate'] as String),
      status: $enumDecode(_$ParticipationStatusEnumMap, json['status']),
      notes: json['notes'] as String?,
    );

Map<String, dynamic> _$$CampaignParticipantImplToJson(
        _$CampaignParticipantImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'campaignId': instance.campaignId,
      'patientId': instance.patientId,
      'patientName': instance.patientName,
      'registrationDate': instance.registrationDate.toIso8601String(),
      'attendanceDate': instance.attendanceDate?.toIso8601String(),
      'status': _$ParticipationStatusEnumMap[instance.status]!,
      'notes': instance.notes,
    };

const _$ParticipationStatusEnumMap = {
  ParticipationStatus.registered: 'REGISTERED',
  ParticipationStatus.attended: 'ATTENDED',
  ParticipationStatus.absent: 'ABSENT',
  ParticipationStatus.cancelled: 'CANCELLED',
};

_$MedicalKitImpl _$$MedicalKitImplFromJson(Map<String, dynamic> json) =>
    _$MedicalKitImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      type: $enumDecode(_$KitTypeEnumMap, json['type']),
      items: (json['items'] as List<dynamic>)
          .map((e) => KitItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      location: json['location'] as String?,
      assignedToId: (json['assignedToId'] as num?)?.toInt(),
      assignedToName: json['assignedToName'] as String?,
      lastInspectionDate: json['lastInspectionDate'] == null
          ? null
          : DateTime.parse(json['lastInspectionDate'] as String),
      nextInspectionDate: json['nextInspectionDate'] == null
          ? null
          : DateTime.parse(json['nextInspectionDate'] as String),
      status: $enumDecode(_$KitStatusEnumMap, json['status']),
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$MedicalKitImplToJson(_$MedicalKitImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'type': _$KitTypeEnumMap[instance.type]!,
      'items': instance.items,
      'location': instance.location,
      'assignedToId': instance.assignedToId,
      'assignedToName': instance.assignedToName,
      'lastInspectionDate': instance.lastInspectionDate?.toIso8601String(),
      'nextInspectionDate': instance.nextInspectionDate?.toIso8601String(),
      'status': _$KitStatusEnumMap[instance.status]!,
      'notes': instance.notes,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$KitTypeEnumMap = {
  KitType.firstAid: 'FIRST_AID',
  KitType.emergency: 'EMERGENCY',
  KitType.trauma: 'TRAUMA',
  KitType.delivery: 'DELIVERY',
  KitType.custom: 'CUSTOM',
};

const _$KitStatusEnumMap = {
  KitStatus.ready: 'READY',
  KitStatus.inUse: 'IN_USE',
  KitStatus.needsRestock: 'NEEDS_RESTOCK',
  KitStatus.expired: 'EXPIRED',
};

_$KitItemImpl _$$KitItemImplFromJson(Map<String, dynamic> json) =>
    _$KitItemImpl(
      id: (json['id'] as num).toInt(),
      kitId: (json['kitId'] as num).toInt(),
      medicationId: (json['medicationId'] as num).toInt(),
      medicationName: json['medicationName'] as String,
      requiredQuantity: (json['requiredQuantity'] as num).toInt(),
      currentQuantity: (json['currentQuantity'] as num).toInt(),
      notes: json['notes'] as String?,
    );

Map<String, dynamic> _$$KitItemImplToJson(_$KitItemImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'kitId': instance.kitId,
      'medicationId': instance.medicationId,
      'medicationName': instance.medicationName,
      'requiredQuantity': instance.requiredQuantity,
      'currentQuantity': instance.currentQuantity,
      'notes': instance.notes,
    };

_$StaffDutyImpl _$$StaffDutyImplFromJson(Map<String, dynamic> json) =>
    _$StaffDutyImpl(
      id: (json['id'] as num).toInt(),
      staffId: (json['staffId'] as num).toInt(),
      staffName: json['staffName'] as String,
      role: json['role'] as String,
      startTime: DateTime.parse(json['startTime'] as String),
      endTime: DateTime.parse(json['endTime'] as String),
      status: $enumDecode(_$DutyStatusEnumMap, json['status']),
      location: json['location'] as String?,
      notes: json['notes'] as String?,
      actualStartTime: json['actualStartTime'] == null
          ? null
          : DateTime.parse(json['actualStartTime'] as String),
      actualEndTime: json['actualEndTime'] == null
          ? null
          : DateTime.parse(json['actualEndTime'] as String),
    );

Map<String, dynamic> _$$StaffDutyImplToJson(_$StaffDutyImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'staffId': instance.staffId,
      'staffName': instance.staffName,
      'role': instance.role,
      'startTime': instance.startTime.toIso8601String(),
      'endTime': instance.endTime.toIso8601String(),
      'status': _$DutyStatusEnumMap[instance.status]!,
      'location': instance.location,
      'notes': instance.notes,
      'actualStartTime': instance.actualStartTime?.toIso8601String(),
      'actualEndTime': instance.actualEndTime?.toIso8601String(),
    };

const _$DutyStatusEnumMap = {
  DutyStatus.scheduled: 'SCHEDULED',
  DutyStatus.inProgress: 'IN_PROGRESS',
  DutyStatus.completed: 'COMPLETED',
  DutyStatus.missed: 'MISSED',
};
