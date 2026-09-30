import 'package:flutter/material.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

part 'event_model.freezed.dart';
part 'event_model.g.dart';

/// Un événement, sur le contrat RÉEL du backend.
///
/// Source de vérité : `com.discipolat.modules.events.api.EventResponse` (lecture)
/// et `CreateEventRequest` / `UpdateEventRequest` (écriture). Les trois portent
/// les mêmes noms de champs — `titre`, `dateDebut`, `lieu`, `typeEvenement`… —
/// donc un seul modèle sert dans les deux sens.
///
/// AVANT : 31 champs anglicisés (`title`, `startAt`, `location`, `type` enum,
/// `isPublic`, `hasCheckIn`, `dressCode*`, `streamUrl`…). Aucun ne correspondait
/// au nom de champ du serveur : `Event.fromJson` échouait sur le premier
/// `null as String` et le module ne pouvait lire aucun événement.
///
/// Le statut est un enum à décodage tolérant, parce que le serveur le stocke en
/// texte libre : une valeur atypique ne doit pas faire échouer une liste.
@freezed
class Event with _$Event {
  const factory Event({
    // --- EventResponse ---
    required String id,
    String? organisateurId,
    String? familleId,
    String? departmentId,
    String? typeEvenement,
    required String titre,
    String? description,
    String? lieu,
    required DateTime dateDebut,
    DateTime? dateFin,
    int? limitePlaces,
    @Default(0) int nbInscrits,
    required EventStatus statut,
    String? compteRendu,
    DateTime? createdAt,
    @Default(<EventPieceJointe>[]) List<EventPieceJointe> piecesJointes,

    // --- État DÉRIVÉ côté client ---
    // Jamais lu dans la réponse du serveur, jamais renvoyé : calculé à partir
    // de la session et des inscriptions. Les declaring ici évite de le
    // transporter dans le payload, ce qui serait un mensonge de contrat.
    @Default(false) @JsonKey(includeFromJson: false, includeToJson: false)
    bool isRegistered,
    @Default(false) @JsonKey(includeFromJson: false, includeToJson: false)
    bool isCheckedIn,
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
      case EventType.culte:
        return 'Culte';
      case EventType.reunion:
        return 'Réunion';
      case EventType.evangelisation:
        return 'Évangélisation';
      case EventType.formation:
        return 'Formation';
      case EventType.retraite:
        return 'Retraite';
      case EventType.conference:
        return 'Conférence';
      case EventType.sortie:
        return 'Sortie';
      case EventType.visite:
        return 'Visite';
      case EventType.anniversaire:
        return 'Anniversaire';
      case EventType.etudeBiblique:
        return 'Étude biblique';
      case EventType.veillee:
        return 'Veillée';
      case EventType.priere:
        return 'Prière';
      case EventType.autre:
        return 'Autre';
      case ChecklistStatus.pending:
        return 'À faire';
      case ChecklistStatus.done:
        return 'Terminé';
      case ChecklistStatus.skipped:
        return 'Ignoré';
    }
  }
}

/// Logique métier de l'événement. Freezed n'accepte pas de membre concret dans
/// le corps d'une classe générée : tout passe par une extension.
extension EventDerived on Event {
  /// Type d'événement, lu de façon tolérante.
  ///
  /// Le champ transporté reste `typeEvenement` (une chaîne, comme le serveur) :
  /// l'enum n'est qu'une lecture. Le serveur accepte une valeur libre, donc
  /// `AUTRE` est le repli plutôt qu'une exception.
  EventType get type => EventTypeWire.decode(typeEvenement);

  /// L'utilisateur connecté est-il l'organisateur de cet événement ?
  bool isOrganizedBy(String? userId) =>
  userId != null && userId.isNotEmpty && organisateurId == userId;

  /// Places restantes, ou `null` si l'événement n'est pas borné.
  int? get placesRestantes {
  final max = limitePlaces;
  if (max == null || max <= 0) return null;
  final restant = max - nbInscrits;
  return restant < 0 ? 0 : restant;
  }

  bool get estComplet => statut == EventStatus.completed || statut == EventStatus.cancelled;
}

/// Pièce jointe d'un événement — `EntityAttachmentService.AttachmentItem`
/// (backend) : `id`, `fileId`, `nom`, `url`.
@freezed
class EventPieceJointe with _$EventPieceJointe {
  const factory EventPieceJointe({
    required String id,
    required String fileId,
    required String nom,
    required String url,
  }) = _EventPieceJointe;

  factory EventPieceJointe.fromJson(Map<String, dynamic> json) => _$EventPieceJointeFromJson(json);
}

/// Décodage tolérant du type d'événement.
extension EventTypeWire on EventType {
  static const Map<String, EventType> _byWire = {
    'SORTIE': EventType.sortie,
    'RETRAITE': EventType.retraite,
    'EVANGELISATION': EventType.evangelisation,
    'REUNION': EventType.reunion,
    'VISITE': EventType.visite,
    'CONFERENCE': EventType.conference,
    'FORMATION': EventType.formation,
    'ANNIVERSAIRE': EventType.anniversaire,
    'CULTE': EventType.culte,
    'ETUDE_BIBLIQUE': EventType.etudeBiblique,
    'VEILLEE': EventType.veillee,
    'PRIERE': EventType.priere,
    'AUTRE': EventType.autre,
  };

  static EventType decode(Object? raw) {
    if (raw is EventType) return raw;
    if (raw is String) return _byWire[raw.trim().toUpperCase()] ?? EventType.autre;
    return EventType.autre;
  }
}

enum EventType {
  // Vocabulaire aligne sur le web (`TypeEvenement`, 13 valeurs) : c'est le
  // seul client deja alimente par cette API. `EVENEMENT_SPECIAL` et `REPAS`
  // n'existaient que dans le mobile : le serveur ne les produit pas.
  @JsonValue('SORTIE')
  sortie,
  @JsonValue('RETRAITE')
  retraite,
  @JsonValue('EVANGELISATION')
  evangelisation,
  @JsonValue('REUNION')
  reunion,
  @JsonValue('VISITE')
  visite,
  @JsonValue('CONFERENCE')
  conference,
  @JsonValue('FORMATION')
  formation,
  @JsonValue('ANNIVERSAIRE')
  anniversaire,
  @JsonValue('CULTE')
  culte,
  @JsonValue('ETUDE_BIBLIQUE')
  etudeBiblique,
  @JsonValue('VEILLEE')
  veillee,
  @JsonValue('PRIERE')
  priere,
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
        return 'Événement spécial';
        return 'Repas';
      case EventType.retraite:
        return 'Retraite';
      case EventType.conference:
        return 'Conférence';
      case EventType.autre:
        return 'Autre';
      case EventType.sortie:
        return 'Sortie';
      case EventType.visite:
        return 'Visite';
      case EventType.anniversaire:
        return 'Anniversaire';
      case EventType.etudeBiblique:
        return 'Étude biblique';
      case EventType.veillee:
        return 'Veillée';
      case EventType.priere:
        return 'Prière';
      case EventType.culte:
        return 'Culte';
      case EventType.reunion:
        return 'Réunion';
      case EventType.evangelisation:
        return 'Évangélisation';
      case EventType.formation:
        return 'Formation';
        return 'Événement spécial';
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
        return '#F59E0B';
        return '#EC4899';
      case EventType.retraite:
        return '#6366F1';
      case EventType.conference:
        return '#14B8A6';
      case EventType.autre:
        return '#6B7280';
      case EventType.sortie:
        return '#16A34A';
      case EventType.visite:
        return '#0D9488';
      case EventType.anniversaire:
        return '#DB2777';
      case EventType.etudeBiblique:
        return '#4F46E5';
      case EventType.veillee:
        return '#334155';
      case EventType.priere:
        return '#9333EA';
      case EventType.culte:
        return '#7C3AED';
      case EventType.reunion:
        return '#2563EB';
      case EventType.evangelisation:
        return '#DC2626';
      case EventType.formation:
        return '#059669';
        return '#F59E0B';
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
      case EventType.culte:
        return 'Culte';
      case EventType.reunion:
        return 'Réunion';
      case EventType.evangelisation:
        return 'Évangélisation';
      case EventType.formation:
        return 'Formation';
      case EventType.retraite:
        return 'Retraite';
      case EventType.conference:
        return 'Conférence';
      case EventType.sortie:
        return 'Sortie';
      case EventType.visite:
        return 'Visite';
      case EventType.anniversaire:
        return 'Anniversaire';
      case EventType.etudeBiblique:
        return 'Étude biblique';
      case EventType.veillee:
        return 'Veillée';
      case EventType.priere:
        return 'Prière';
      case EventType.autre:
        return 'Autre';
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

  /// Valeur TRANSPORTÉE d'un statut.
  ///
  /// Indispensable : le décodeur généré attend la valeur sérialisée
  /// (`PLANIFIE`), pas le nom Dart (`published`). Confondre les deux fait
  /// échouer la lecture — c'est exactement ce que ce getter évite.
  static String wire(EventStatus status) => switch (status) {
        EventStatus.published => 'PLANIFIE',
        EventStatus.live => 'EN_COURS',
        EventStatus.completed => 'TERMINE',
        EventStatus.cancelled => 'ANNULE',
        EventStatus.draft => 'BROUILLON',
        EventStatus.unknown => 'UNKNOWN',
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
      case EventType.culte:
        return 'Culte';
      case EventType.reunion:
        return 'Réunion';
      case EventType.evangelisation:
        return 'Évangélisation';
      case EventType.formation:
        return 'Formation';
      case EventType.retraite:
        return 'Retraite';
      case EventType.conference:
        return 'Conférence';
      case EventType.sortie:
        return 'Sortie';
      case EventType.visite:
        return 'Visite';
      case EventType.anniversaire:
        return 'Anniversaire';
      case EventType.etudeBiblique:
        return 'Étude biblique';
      case EventType.veillee:
        return 'Veillée';
      case EventType.priere:
        return 'Prière';
      case EventType.autre:
        return 'Autre';
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