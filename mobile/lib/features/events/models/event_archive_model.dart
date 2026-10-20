// Modèles du parcours « Archives » (V272) — SANS codegen.
//
// Contrat serveur vérifié (backend head, oct. 2026) :
//  - GET  /church-events/archives/search  -> PageResponse<ChurchEventArchiveSummary>
//      { content, page, size, totalElements, totalPages }
//      résumé = { id, eventId, eventTitle, eventStartAt, eventYear, eventMonth,
//                  spaceId, version, attendanceCount, archivedAt, archivedBy }
//      (PAS de snapshotJson : c'est tout l'intérêt du endpoint né de V272.)
//  - GET  /church-events/archives/facets  -> [{ eventYear, months[], archiveCount }]
//  - GET  /church-events/archives/{id}    -> ChurchEventArchive complet,
//      avec `snapshotJson` (Map jsonb) et `tenantId`.
//  - POST /church-events/{eventId}/archive -> ChurchEventArchive (la vN créée).
//
// L'`id` serveur est un UUID (Jackson l'écrit en chaîne) : ici tout reste
// String, jamais de cast `int` (le piège qui vidait l'écran Santé, cf.
// test/contract/health_model_contract_test.dart). Le décodage est tolérant :
// une clé absente devient null/vide, jamais une exception — un payload
// partiel ne doit pas faire échouer TOUTE la liste.

/// Résumé d'archive (ligne de liste / page de recherche).
class EventArchiveSummary {
  const EventArchiveSummary({
    required this.id,
    required this.eventId,
    required this.eventTitle,
    this.eventStartAt,
    this.eventYear,
    this.eventMonth,
    this.spaceId,
    this.version,
    this.attendanceCount,
    this.archivedAt,
    this.archivedBy,
  });

  final String id;
  final String eventId;
  final String eventTitle;
  final DateTime? eventStartAt;
  final int? eventYear;
  final int? eventMonth;
  final String? spaceId;
  final int? version;
  final int? attendanceCount;
  final DateTime? archivedAt;
  final String? archivedBy;

  factory EventArchiveSummary.fromJson(Map<String, dynamic> json) {
    return EventArchiveSummary(
      id: _str(json['id']) ?? '',
      eventId: _str(json['eventId']) ?? '',
      eventTitle: _str(json['eventTitle']) ?? '',
      eventStartAt: _date(json['eventStartAt']),
      eventYear: _int(json['eventYear']),
      eventMonth: _int(json['eventMonth']),
      spaceId: _str(json['spaceId']),
      version: _int(json['version']),
      attendanceCount: _int(json['attendanceCount']),
      archivedAt: _date(json['archivedAt']),
      archivedBy: _str(json['archivedBy']),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'eventId': eventId,
        'eventTitle': eventTitle,
        'eventStartAt': eventStartAt?.toIso8601String(),
        'eventYear': eventYear,
        'eventMonth': eventMonth,
        'spaceId': spaceId,
        'version': version,
        'attendanceCount': attendanceCount,
        'archivedAt': archivedAt?.toIso8601String(),
        'archivedBy': archivedBy,
      };
}

/// Facette année/mois produite par GET /archives/facets : l'agrégation
/// serveur qui remplace le téléchargement intégral du legacy `/archives`.
class EventArchiveFacet {
  const EventArchiveFacet({
    required this.eventYear,
    required this.months,
    required this.archiveCount,
  });

  final int eventYear;
  final List<int> months;
  final int archiveCount;

  factory EventArchiveFacet.fromJson(Map<String, dynamic> json) {
    final rawMonths = json['months'];
    return EventArchiveFacet(
      eventYear: _int(json['eventYear']) ?? 0,
      // Tolérant : une liste absente ou mal typée devient vide, jamais un cast
      // qui ferait échouer le chargement des filtres.
      months: rawMonths is List
          ? rawMonths
              .map((e) => _int(e) ?? 0)
              .where((m) => m >= 1 && m <= 12)
              .toList()
          : const <int>[],
      archiveCount: _int(json['archiveCount']) ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
        'eventYear': eventYear,
        'months': months,
        'archiveCount': archiveCount,
      };
}

/// Archive complète (detail GET /archives/{id} et réponse de POST /archive).
/// `snapshotJson` est la copie immuable de l'événement clôturé — le serveur
/// stocke ça en jsonb et le renvoie tel quel (Map).
class EventArchive {
  const EventArchive({
    required this.id,
    required this.eventId,
    required this.eventTitle,
    this.tenantId,
    this.eventStartAt,
    this.eventYear,
    this.eventMonth,
    this.spaceId,
    this.version,
    this.attendanceCount,
    this.archivedAt,
    this.archivedBy,
    this.snapshotJson = const <String, dynamic>{},
  });

  final String id;
  final String eventId;
  final String eventTitle;
  final String? tenantId;
  final DateTime? eventStartAt;
  final int? eventYear;
  final int? eventMonth;
  final String? spaceId;
  final int? version;
  final int? attendanceCount;
  final DateTime? archivedAt;
  final String? archivedBy;
  final Map<String, dynamic> snapshotJson;

  factory EventArchive.fromJson(Map<String, dynamic> json) {
    final rawSnapshot = json['snapshotJson'];
    return EventArchive(
      id: _str(json['id']) ?? '',
      eventId: _str(json['eventId']) ?? '',
      eventTitle: _str(json['eventTitle']) ?? '',
      tenantId: _str(json['tenantId']),
      eventStartAt: _date(json['eventStartAt']),
      eventYear: _int(json['eventYear']),
      eventMonth: _int(json['eventMonth']),
      spaceId: _str(json['spaceId']),
      version: _int(json['version']),
      attendanceCount: _int(json['attendanceCount']),
      archivedAt: _date(json['archivedAt']),
      archivedBy: _str(json['archivedBy']),
      snapshotJson:
          rawSnapshot is Map ? rawSnapshot.cast<String, dynamic>() : const {},
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'eventId': eventId,
        'eventTitle': eventTitle,
        'tenantId': tenantId,
        'eventStartAt': eventStartAt?.toIso8601String(),
        'eventYear': eventYear,
        'eventMonth': eventMonth,
        'spaceId': spaceId,
        'version': version,
        'attendanceCount': attendanceCount,
        'archivedAt': archivedAt?.toIso8601String(),
        'archivedBy': archivedBy,
        'snapshotJson': snapshotJson,
      };
}

/// Page normalisée `PageResponse<ChurchEventArchiveSummary>` (record serveur :
/// {content, page, size, totalElements, totalPages}).
class EventArchivePage {
  const EventArchivePage({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<EventArchiveSummary> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  bool get hasPrevious => page > 0;
  bool get hasNext => page + 1 < totalPages;

  factory EventArchivePage.fromJson(Map<String, dynamic> json) {
    final rawContent = json['content'];
    // Liste absente ou mal typée → page vide : l'écran affiche son état vide,
    // ce n'est jamais une exception qui doit tuer la liste.
    final items = rawContent is List
        ? rawContent
            .whereType<Map>()
            .map((e) =>
                EventArchiveSummary.fromJson(e.cast<String, dynamic>()))
            .toList()
        : const <EventArchiveSummary>[];
    return EventArchivePage(
      content: items,
      page: _int(json['page']) ?? 0,
      size: _int(json['size']) ?? items.length,
      totalElements: _int(json['totalElements']) ?? items.length,
      totalPages: _int(json['totalPages']) ?? 1,
    );
  }

  Map<String, dynamic> toJson() => {
        'content': content.map((e) => e.toJson()).toList(),
        'page': page,
        'size': size,
        'totalElements': totalElements,
        'totalPages': totalPages,
      };
}

// ── Parseurs tolérants partagés (mêmes règles que les autres contrats) ──
String? _str(dynamic v) {
  if (v == null) return null;
  final s = v.toString();
  return s.isEmpty ? null : s;
}

int? _int(dynamic v) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  if (v is String) return int.tryParse(v);
  return null;
}

DateTime? _date(dynamic v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}
