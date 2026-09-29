import 'package:flutter/material.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

part 'event_model.freezed.dart';
part 'event_model.g.dart';

@freezed
class Event with _$Event {
  const factory Event({
    required String id,
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
    String? streamUrl,
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
    required String id,
    required String eventId,
    required String personId,
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

/// Contrat réel : `EventTeam` (backend), exposée par
/// `GET/POST /api/v1/church-events/{eventId}/teams`.
///
/// ATTENTION — l'ancien modèle mobile était un « membre d'équipe » avec un
/// rôle (`personId`, `teamRole`, `responsibilities`) : **ce concept n'existe
/// pas côté backend**. Le serveur connaît une *équipe* avec un *responsable*
/// (`leadPersonId`). On réaligne sur le serveur, qui est la source de vérité.
@freezed
class EventTeam with _$EventTeam {
  const factory EventTeam({
    required String id,
    String? tenantId,
    String? churchEventId,
    String? spaceId,
    required String name,
    String? description,
    String? leadPersonId,
    String? color,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) = _EventTeam;

  factory EventTeam.fromJson(Map<String, dynamic> json) => _$EventTeamFromJson(json);
}

@freezed
class EventChecklist with _$EventChecklist {
  /// Contrat réel : `EventChecklistItem` (backend) — `id`, `tenantId`,
  /// `eventId`, `title`, `description`, `status`, `assignedTo`, `orderIndex`,
  /// `createdAt`. Les anciens champs `isRequired`, `isCompleted`,
  /// `assignedToId`, `order` n'existaient pas côté serveur.
  const factory EventChecklist({
    required String id,
    String? tenantId,
    required String eventId,
    required String title,
    String? description,
    @Default(ChecklistStatus.pending) ChecklistStatus status,
    String? assignedTo,
    @Default(0) int orderIndex,
    DateTime? createdAt,
  }) = _EventChecklist;

  factory EventChecklist.fromJson(Map<String, dynamic> json) => _$EventChecklistFromJson(json);
}

@freezed
class DressCode with _$DressCode {
  /// Contrat réel : `DressCodeResponse` (backend) — `id`, `spaceId`,
  /// `eventId`, `serviceName`, `title`, `beginsAt`, `endsAt`, `status`,
  /// `archived`, plus `rules` (`DressCodeDetailResponse`).
  const factory DressCode({
    required String id,
    String? spaceId,
    String? eventId,
    String? serviceName,
    required String title,
    DateTime? beginsAt,
    DateTime? endsAt,
    @Default('ACTIVE') String status,
    @Default(false) bool archived,
    @Default(<DressCodeRule>[]) List<DressCodeRule> rules,
  }) = _DressCode;

  factory DressCode.fromJson(Map<String, dynamic> json) => _$DressCodeFromJson(json);
}

/// `DressCodeRuleRequest` (backend) : `groupName`, `description`, `imageUrl`.
@freezed
class DressCodeRule with _$DressCodeRule {
  const factory DressCodeRule({
    required String groupName,
    String? description,
    String? imageUrl,
  }) = _DressCodeRule;

  factory DressCodeRule.fromJson(Map<String, dynamic> json) => _$DressCodeRuleFromJson(json);
}

/// Statuts d'un élément de checklist (`EventChecklistItem.Status`).
enum ChecklistStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('DONE')
  done,
  @JsonValue('SKIPPED')
  skipped;

  String get displayName {
    switch (this) {
      case ChecklistStatus.pending:
        return 'À faire';
      case ChecklistStatus.done:
        return 'Terminé';
      case ChecklistStatus.skipped:
        return 'Ignoré';
    }
  }
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
  /// Vocabulaire du BACKEND. Reference : `StatutEvenement` du web
  /// ('PLANIFIE' | 'EN_COURS' | 'TERMINE' | 'ANNULE'), `Event.statut`
  /// (String, defaut "PLANIFIE") et le filtre `findByStatutAndDeletedFalse("PLANIFIE")`.
  /// Avant : DRAFT/PUBLISHED/LIVE/COMPLETED/CANCELLED — un vocabulaire anglais
  /// que le backend ne produit ni ne filtre : la liste d'evenements ne pouvait
  /// pas etre lue, et une annulation aurait ecrit une valeur invisible.
  @JsonValue('PLANIFIE')
  published,
  @JsonValue('EN_COURS')
  live,
  @JsonValue('TERMINE')
  completed,
  @JsonValue('ANNULE')
  cancelled,
  @JsonValue('BROUILLON')
  draft,

  /// Valeur de repli : le backend est un texte libre, donc une valeur
  /// inconnue ne doit JAMAIS faire echouer le chargement de l'ecran.
  @JsonValue('UNKNOWN')
  unknown;

  String get displayName {
    switch (this) {
      case EventStatus.draft:
        return 'Brouillon';
      case EventStatus.published:
        return 'Planifié';
      case EventStatus.live:
        return 'En cours';
      case EventStatus.completed:
        return 'Terminé';
      case EventStatus.cancelled:
        return 'Annulé';
      case EventStatus.unknown:
        return 'Statut inconnu';
    }
  }
}

/// Décodage tolérant du statut.
///
/// Le backend expose `statut` comme une chaîne libre. Un `enumDecode` strict
/// lève une exception sur une valeur inattendue, ce qui ferait échouer TOUTE la
/// liste d'événements pour un seul enregistrement. On tolère donc les deux
/// vocabulaires (celui du backend et l'ancien vocabulaire anglais du mobile),
/// et une valeur totalement inconnue retombe sur [EventStatus.unknown] — jamais
/// sur une exception, jamais sur une valeur inventée.
extension EventStatusWire on EventStatus {
  static const Map<String, EventStatus> _byWire = {
    'PLANIFIE': EventStatus.published,
    'EN_COURS': EventStatus.live,
    'TERMINE': EventStatus.completed,
    'ANNULE': EventStatus.cancelled,
    'BROUILLON': EventStatus.draft,
    // Ancien vocabulaire mobile, accepte en lecture pour ne pas casser des
    // caches ou des enregistrements deja stockes cote client.
    'PUBLISHED': EventStatus.published,
    'LIVE': EventStatus.live,
    'COMPLETED': EventStatus.completed,
    'CANCELLED': EventStatus.cancelled,
    'DRAFT': EventStatus.draft,
  };

  static EventStatus decode(Object? raw) {
    if (raw is EventStatus) return raw;
    if (raw is String) {
      return _byWire[raw.trim().toUpperCase()] ?? EventStatus.unknown;
    }
    return EventStatus.unknown;
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