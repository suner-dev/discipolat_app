// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$EventImpl _$$EventImplFromJson(Map<String, dynamic> json) => _$EventImpl(
  id: json['id'] as String,
  title: json['title'] as String,
  description: json['description'] as String?,
  type: $enumDecode(_$EventTypeEnumMap, json['type']),
  startAt: DateTime.parse(json['startAt'] as String),
  endAt: DateTime.parse(json['endAt'] as String),
  location: json['location'] as String?,
  latitude: (json['latitude'] as num?)?.toDouble(),
  longitude: (json['longitude'] as num?)?.toDouble(),
  maxAttendees: (json['maxAttendees'] as num?)?.toInt(),
  currentAttendees: (json['currentAttendees'] as num?)?.toInt() ?? 0,
  status: $enumDecode(_$EventStatusEnumMap, json['status']),
  spaceId: json['spaceId'] as String?,
  spaceName: json['spaceName'] as String?,
  dressCodeId: json['dressCodeId'] as String?,
  dressCodeName: json['dressCodeName'] as String?,
  dressCodeDescription: json['dressCodeDescription'] as String?,
  isPublic: json['isPublic'] as bool? ?? false,
  requiresRegistration: json['requiresRegistration'] as bool? ?? false,
  hasCheckIn: json['hasCheckIn'] as bool? ?? false,
  hasGeofencing: json['hasGeofencing'] as bool? ?? false,
  hasFaceCheckIn: json['hasFaceCheckIn'] as bool? ?? false,
  checkInQrCode: json['checkInQrCode'] as String?,
  streamUrl: json['streamUrl'] as String?,
  thumbnailUrl: json['thumbnailUrl'] as String?,
  tags: (json['tags'] as List<dynamic>?)?.map((e) => e as String).toList(),
  createdAt: DateTime.parse(json['createdAt'] as String),
  updatedAt: json['updatedAt'] == null
      ? null
      : DateTime.parse(json['updatedAt'] as String),
  isRegistered: json['isRegistered'] as bool? ?? false,
  isCheckedIn: json['isCheckedIn'] as bool? ?? false,
  isOrganizer: json['isOrganizer'] as bool? ?? false,
  isTeamMember: json['isTeamMember'] as bool? ?? false,
);

Map<String, dynamic> _$$EventImplToJson(_$EventImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'description': instance.description,
      'type': _$EventTypeEnumMap[instance.type]!,
      'startAt': instance.startAt.toIso8601String(),
      'endAt': instance.endAt.toIso8601String(),
      'location': instance.location,
      'latitude': instance.latitude,
      'longitude': instance.longitude,
      'maxAttendees': instance.maxAttendees,
      'currentAttendees': instance.currentAttendees,
      'status': _$EventStatusEnumMap[instance.status]!,
      'spaceId': instance.spaceId,
      'spaceName': instance.spaceName,
      'dressCodeId': instance.dressCodeId,
      'dressCodeName': instance.dressCodeName,
      'dressCodeDescription': instance.dressCodeDescription,
      'isPublic': instance.isPublic,
      'requiresRegistration': instance.requiresRegistration,
      'hasCheckIn': instance.hasCheckIn,
      'hasGeofencing': instance.hasGeofencing,
      'hasFaceCheckIn': instance.hasFaceCheckIn,
      'checkInQrCode': instance.checkInQrCode,
      'streamUrl': instance.streamUrl,
      'thumbnailUrl': instance.thumbnailUrl,
      'tags': instance.tags,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
      'isRegistered': instance.isRegistered,
      'isCheckedIn': instance.isCheckedIn,
      'isOrganizer': instance.isOrganizer,
      'isTeamMember': instance.isTeamMember,
    };

const _$EventTypeEnumMap = {
  EventType.culte: 'CULTE',
  EventType.reunion: 'REUNION',
  EventType.evangelisation: 'EVANGELISATION',
  EventType.formation: 'FORMATION',
  EventType.evenementSpecial: 'EVENEMENT_SPECIAL',
  EventType.repas: 'REPAS',
  EventType.retraite: 'RETRAITE',
  EventType.conference: 'CONFERENCE',
  EventType.autre: 'AUTRE',
};

const _$EventStatusEnumMap = {
  EventStatus.published: 'PLANIFIE',
  EventStatus.live: 'EN_COURS',
  EventStatus.completed: 'TERMINE',
  EventStatus.cancelled: 'ANNULE',
  EventStatus.draft: 'BROUILLON',
  EventStatus.unknown: 'UNKNOWN',
};

_$EventRegistrationImpl _$$EventRegistrationImplFromJson(
  Map<String, dynamic> json,
) => _$EventRegistrationImpl(
  id: json['id'] as String,
  eventId: json['eventId'] as String,
  personId: json['personId'] as String,
  status: $enumDecode(_$RegistrationStatusEnumMap, json['status']),
  checkedInAt: json['checkedInAt'] == null
      ? null
      : DateTime.parse(json['checkedInAt'] as String),
  checkInMethod: json['checkInMethod'] as String?,
  hasGuest: json['hasGuest'] as bool? ?? false,
  guestCount: (json['guestCount'] as num?)?.toInt(),
  registeredAt: json['registeredAt'] == null
      ? null
      : DateTime.parse(json['registeredAt'] as String),
  cancelledAt: json['cancelledAt'] == null
      ? null
      : DateTime.parse(json['cancelledAt'] as String),
);

Map<String, dynamic> _$$EventRegistrationImplToJson(
  _$EventRegistrationImpl instance,
) => <String, dynamic>{
  'id': instance.id,
  'eventId': instance.eventId,
  'personId': instance.personId,
  'status': _$RegistrationStatusEnumMap[instance.status]!,
  'checkedInAt': instance.checkedInAt?.toIso8601String(),
  'checkInMethod': instance.checkInMethod,
  'hasGuest': instance.hasGuest,
  'guestCount': instance.guestCount,
  'registeredAt': instance.registeredAt?.toIso8601String(),
  'cancelledAt': instance.cancelledAt?.toIso8601String(),
};

const _$RegistrationStatusEnumMap = {
  RegistrationStatus.pending: 'PENDING',
  RegistrationStatus.confirmed: 'CONFIRMED',
  RegistrationStatus.waitlist: 'WAITLIST',
  RegistrationStatus.cancelled: 'CANCELLED',
  RegistrationStatus.attended: 'ATTENDED',
};

_$EventTeamImpl _$$EventTeamImplFromJson(Map<String, dynamic> json) =>
    _$EventTeamImpl(
      id: json['id'] as String,
      tenantId: json['tenantId'] as String?,
      churchEventId: json['churchEventId'] as String?,
      spaceId: json['spaceId'] as String?,
      name: json['name'] as String,
      description: json['description'] as String?,
      leadPersonId: json['leadPersonId'] as String?,
      color: json['color'] as String?,
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$EventTeamImplToJson(_$EventTeamImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'tenantId': instance.tenantId,
      'churchEventId': instance.churchEventId,
      'spaceId': instance.spaceId,
      'name': instance.name,
      'description': instance.description,
      'leadPersonId': instance.leadPersonId,
      'color': instance.color,
      'createdAt': instance.createdAt?.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

_$EventChecklistImpl _$$EventChecklistImplFromJson(Map<String, dynamic> json) =>
    _$EventChecklistImpl(
      id: json['id'] as String,
      tenantId: json['tenantId'] as String?,
      eventId: json['eventId'] as String,
      title: json['title'] as String,
      description: json['description'] as String?,
      status:
          $enumDecodeNullable(_$ChecklistStatusEnumMap, json['status']) ??
          ChecklistStatus.pending,
      assignedTo: json['assignedTo'] as String?,
      orderIndex: (json['orderIndex'] as num?)?.toInt() ?? 0,
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$EventChecklistImplToJson(
  _$EventChecklistImpl instance,
) => <String, dynamic>{
  'id': instance.id,
  'tenantId': instance.tenantId,
  'eventId': instance.eventId,
  'title': instance.title,
  'description': instance.description,
  'status': _$ChecklistStatusEnumMap[instance.status]!,
  'assignedTo': instance.assignedTo,
  'orderIndex': instance.orderIndex,
  'createdAt': instance.createdAt?.toIso8601String(),
};

const _$ChecklistStatusEnumMap = {
  ChecklistStatus.pending: 'PENDING',
  ChecklistStatus.done: 'DONE',
  ChecklistStatus.skipped: 'SKIPPED',
};

_$DressCodeImpl _$$DressCodeImplFromJson(Map<String, dynamic> json) =>
    _$DressCodeImpl(
      id: json['id'] as String,
      spaceId: json['spaceId'] as String?,
      eventId: json['eventId'] as String?,
      serviceName: json['serviceName'] as String?,
      title: json['title'] as String,
      beginsAt: json['beginsAt'] == null
          ? null
          : DateTime.parse(json['beginsAt'] as String),
      endsAt: json['endsAt'] == null
          ? null
          : DateTime.parse(json['endsAt'] as String),
      status: json['status'] as String? ?? 'ACTIVE',
      archived: json['archived'] as bool? ?? false,
      rules:
          (json['rules'] as List<dynamic>?)
              ?.map((e) => DressCodeRule.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const <DressCodeRule>[],
    );

Map<String, dynamic> _$$DressCodeImplToJson(_$DressCodeImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'spaceId': instance.spaceId,
      'eventId': instance.eventId,
      'serviceName': instance.serviceName,
      'title': instance.title,
      'beginsAt': instance.beginsAt?.toIso8601String(),
      'endsAt': instance.endsAt?.toIso8601String(),
      'status': instance.status,
      'archived': instance.archived,
      'rules': instance.rules,
    };

_$DressCodeRuleImpl _$$DressCodeRuleImplFromJson(Map<String, dynamic> json) =>
    _$DressCodeRuleImpl(
      groupName: json['groupName'] as String,
      description: json['description'] as String?,
      imageUrl: json['imageUrl'] as String?,
    );

Map<String, dynamic> _$$DressCodeRuleImplToJson(_$DressCodeRuleImpl instance) =>
    <String, dynamic>{
      'groupName': instance.groupName,
      'description': instance.description,
      'imageUrl': instance.imageUrl,
    };
