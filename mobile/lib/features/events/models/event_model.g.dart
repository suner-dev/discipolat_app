// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$EventImpl _$$EventImplFromJson(Map<String, dynamic> json) => _$EventImpl(
      id: (json['id'] as num).toInt(),
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
  EventStatus.draft: 'DRAFT',
  EventStatus.published: 'PUBLISHED',
  EventStatus.live: 'LIVE',
  EventStatus.completed: 'COMPLETED',
  EventStatus.cancelled: 'CANCELLED',
};

_$EventRegistrationImpl _$$EventRegistrationImplFromJson(
        Map<String, dynamic> json) =>
    _$EventRegistrationImpl(
      id: (json['id'] as num).toInt(),
      eventId: (json['eventId'] as num).toInt(),
      personId: (json['personId'] as num).toInt(),
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
        _$EventRegistrationImpl instance) =>
    <String, dynamic>{
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

_$EventTeamMemberImpl _$$EventTeamMemberImplFromJson(
        Map<String, dynamic> json) =>
    _$EventTeamMemberImpl(
      id: (json['id'] as num).toInt(),
      eventId: (json['eventId'] as num).toInt(),
      personId: (json['personId'] as num).toInt(),
      role: json['role'] as String,
      teamRole: $enumDecode(_$TeamRoleEnumMap, json['teamRole']),
      responsibilities: json['responsibilities'] as String?,
      assignedAt: json['assignedAt'] == null
          ? null
          : DateTime.parse(json['assignedAt'] as String),
    );

Map<String, dynamic> _$$EventTeamMemberImplToJson(
        _$EventTeamMemberImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'eventId': instance.eventId,
      'personId': instance.personId,
      'role': instance.role,
      'teamRole': _$TeamRoleEnumMap[instance.teamRole]!,
      'responsibilities': instance.responsibilities,
      'assignedAt': instance.assignedAt?.toIso8601String(),
    };

const _$TeamRoleEnumMap = {
  TeamRole.organizer: 'ORGANIZER',
  TeamRole.coordinator: 'COORDINATOR',
  TeamRole.welcome: 'WELCOME',
  TeamRole.tech: 'TECH',
  TeamRole.worship: 'WORSHIP',
  TeamRole.children: 'CHILDREN',
  TeamRole.security: 'SECURITY',
  TeamRole.logistics: 'LOGISTICS',
  TeamRole.followUp: 'FOLLOW_UP',
};

_$EventChecklistImpl _$$EventChecklistImplFromJson(Map<String, dynamic> json) =>
    _$EventChecklistImpl(
      id: (json['id'] as num).toInt(),
      eventId: (json['eventId'] as num).toInt(),
      title: json['title'] as String,
      description: json['description'] as String?,
      isRequired: json['isRequired'] as bool? ?? false,
      assignedToId: (json['assignedToId'] as num?)?.toInt(),
      assignedToName: json['assignedToName'] as String?,
      isCompleted: json['isCompleted'] as bool? ?? false,
      completedAt: json['completedAt'] == null
          ? null
          : DateTime.parse(json['completedAt'] as String),
      completedById: (json['completedById'] as num?)?.toInt(),
      order: (json['order'] as num?)?.toInt(),
    );

Map<String, dynamic> _$$EventChecklistImplToJson(
        _$EventChecklistImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'eventId': instance.eventId,
      'title': instance.title,
      'description': instance.description,
      'isRequired': instance.isRequired,
      'assignedToId': instance.assignedToId,
      'assignedToName': instance.assignedToName,
      'isCompleted': instance.isCompleted,
      'completedAt': instance.completedAt?.toIso8601String(),
      'completedById': instance.completedById,
      'order': instance.order,
    };

_$DressCodeImpl _$$DressCodeImplFromJson(Map<String, dynamic> json) =>
    _$DressCodeImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      colorCode: json['colorCode'] as String?,
      iconUrl: json['iconUrl'] as String?,
      items: (json['items'] as List<dynamic>?)
          ?.map((e) => DressCodeItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      isRequired: json['isRequired'] as bool? ?? false,
    );

Map<String, dynamic> _$$DressCodeImplToJson(_$DressCodeImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'colorCode': instance.colorCode,
      'iconUrl': instance.iconUrl,
      'items': instance.items,
      'isRequired': instance.isRequired,
    };

_$DressCodeItemImpl _$$DressCodeItemImplFromJson(Map<String, dynamic> json) =>
    _$DressCodeItemImpl(
      id: (json['id'] as num).toInt(),
      dressCodeId: (json['dressCodeId'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String?,
      color: json['color'] as String?,
      icon: json['icon'] as String?,
      isRequired: json['isRequired'] as bool? ?? true,
      isAlternative: json['isAlternative'] as bool? ?? false,
    );

Map<String, dynamic> _$$DressCodeItemImplToJson(_$DressCodeItemImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'dressCodeId': instance.dressCodeId,
      'name': instance.name,
      'description': instance.description,
      'color': instance.color,
      'icon': instance.icon,
      'isRequired': instance.isRequired,
      'isAlternative': instance.isAlternative,
    };
