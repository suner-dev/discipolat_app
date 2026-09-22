import 'package:flutter/material.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

part 'event_model.freezed.dart';
part 'event_model.g.dart';

@freezed
class Event with _$Event {
  const factory Event({
    required int id,
    required String title,
    String? description,
    required EventType type,
    required DateTime startAt,
    required DateTime endAt,
    String? location,
    double? latitude,
    double? longitude,
    int? maxAttendees,
    @Default(0) int currentAttendees,
    required EventStatus status,
    String? spaceId,
    String? spaceName,
    String? dressCodeId,
    String? dressCodeName,
    String? dressCodeDescription,
    @Default(false) bool isPublic,
    @Default(false) bool requiresRegistration,
    @Default(false) bool hasCheckIn,
    @Default(false) bool hasGeofencing,
    @Default(false) bool hasFaceCheckIn,
    String? checkInQrCode,
    String? thumbnailUrl,
    List<String>? tags,
    required DateTime createdAt,
    DateTime? updatedAt,
    @Default(false) bool isRegistered,
    @Default(false) bool isCheckedIn,
    @Default(false) bool isOrganizer,
    @Default(false) bool isTeamMember,
  }) = _Event;

  factory Event.fromJson(Map<String, dynamic> json) => _$EventFromJson(json);
}

@freezed
class EventRegistration with _$EventRegistration {
  const factory EventRegistration({
    required int id,
    required int eventId,
    required int personId,
    required RegistrationStatus status,
    DateTime? checkedInAt,
    String? checkInMethod,
    @Default(false) bool hasGuest,
    int? guestCount,
    DateTime? registeredAt,
    DateTime? cancelledAt,
  }) = _EventRegistration;

  factory EventRegistration.fromJson(Map<String, dynamic> json) => _$EventRegistrationFromJson(json);
}

@freezed
class EventTeamMember with _$EventTeamMember {
  const factory EventTeamMember({
    required int id,
    required int eventId,
    required int personId,
    required String role,
    required TeamRole teamRole,
    String? responsibilities,
    DateTime? assignedAt,
  }) = _EventTeamMember;

  factory EventTeamMember.fromJson(Map<String, dynamic> json) => _$EventTeamMemberFromJson(json);
}

@freezed
class EventChecklist with _$EventChecklist {
  const factory EventChecklist({
    required int id,
    required int eventId,
    required String title,
    String? description,
    @Default(false) bool isRequired,
    int? assignedToId,
    String? assignedToName,
    @Default(false) bool isCompleted,
    DateTime? completedAt,
    int? completedById,
    int? order,
  }) = _EventChecklist;

  factory EventChecklist.fromJson(Map<String, dynamic> json) => _$EventChecklistFromJson(json);
}

@freezed
class DressCode with _$DressCode {
  const factory DressCode({
    required int id,
    required String name,
    String? description,
    String? colorCode,
    String? iconUrl,
    List<DressCodeItem>? items,
    @Default(false) bool isRequired,
  }) = _DressCode;

  factory DressCode.fromJson(Map<String, dynamic> json) => _$DressCodeFromJson(json);
}

@freezed
class DressCodeItem with _$DressCodeItem {
  const factory DressCodeItem({
    required int id,
    required int dressCodeId,
    required String name,
    String? description,
    String? color,
    String? icon,
    @Default(true) bool isRequired,
    @Default(false) bool isAlternative,
  }) = _DressCodeItem;

  factory DressCodeItem.fromJson(Map<String, dynamic> json) => _$DressCodeItemFromJson(json);
}

enum EventType {
  @JsonValue('CULTE')
  culte,
  @JsonValue('REUNION')
  reunion,
  @JsonValue('EVANGELISATION')
  evangelisation,
  @JsonValue('FORMATION')
  formation,
  @JsonValue('EVENEMENT_SPECIAL')
  evenementSpecial,
  @JsonValue('REPAS')
  repas,
  @JsonValue('RETRAITE')
  retraite,
  @JsonValue('CONFERENCE')
  conference,
  @JsonValue('AUTRE')
  autre;

  String get displayName {
    switch (this) {
      case EventType.culte:
        return 'Culte';
      case EventType.reunion:
        return 'Réunion';
      case EventType.evangelisation:
        return 'Évangélisation';
      case EventType.formation:
        return 'Formation';
      case EventType.evenementSpecial:
        return 'Événement spécial';
      case EventType.repas:
        return 'Repas';
      case EventType.retraite:
        return 'Retraite';
      case EventType.conference:
        return 'Conférence';
      case EventType.autre:
        return 'Autre';
    }
  }

  String getColorHex() {
    switch (this) {
      case EventType.culte:
        return '#7C3AED';
      case EventType.reunion:
        return '#2563EB';
      case EventType.evangelisation:
        return '#DC2626';
      case EventType.formation:
        return '#059669';
      case EventType.evenementSpecial:
        return '#F59E0B';
      case EventType.repas:
        return '#EC4899';
      case EventType.retraite:
        return '#6366F1';
      case EventType.conference:
        return '#14B8A6';
      case EventType.autre:
        return '#6B7280';
    }
  }
}

enum EventStatus {
  @JsonValue('DRAFT')
  draft,
  @JsonValue('PUBLISHED')
  published,
  @JsonValue('LIVE')
  live,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled;

  String get displayName {
    switch (this) {
      case EventStatus.draft:
        return 'Brouillon';
      case EventStatus.published:
        return 'Publié';
      case EventStatus.live:
        return 'En cours';
      case EventStatus.completed:
        return 'Terminé';
      case EventStatus.cancelled:
        return 'Annulé';
    }
  }
}

enum RegistrationStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('CONFIRMED')
  confirmed,
  @JsonValue('WAITLIST')
  waitlist,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('ATTENDED')
  attended;

  String get displayName {
    switch (this) {
      case RegistrationStatus.pending:
        return 'En attente';
      case RegistrationStatus.confirmed:
        return 'Confirmé';
      case RegistrationStatus.waitlist:
        return 'Liste d\'attente';
      case RegistrationStatus.cancelled:
        return 'Annulé';
      case RegistrationStatus.attended:
        return 'Présent';
    }
  }
}

enum TeamRole {
  @JsonValue('ORGANIZER')
  organizer,
  @JsonValue('COORDINATOR')
  coordinator,
  @JsonValue('WELCOME')
  welcome,
  @JsonValue('TECH')
  tech,
  @JsonValue('WORSHIP')
  worship,
  @JsonValue('CHILDREN')
  children,
  @JsonValue('SECURITY')
  security,
  @JsonValue('LOGISTICS')
  logistics,
  @JsonValue('FOLLOW_UP')
  followUp,
}

extension HexColor on String {
  Color toColor() {
    final hex = replaceAll('#', '');
    if (hex.length == 6) {
      return Color(int.parse('FF$hex', radix: 16));
    } else if (hex.length == 8) {
      return Color(int.parse(hex, radix: 16));
    }
    return Colors.grey;
  }
}