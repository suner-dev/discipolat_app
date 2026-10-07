/// Modèle streaming aligné sur le contrat serveur V240.
///
/// Source de vérité : com.discipolat.modules.streaming —
/// - `LiveStream` : id **BIGSERIAL (Long)** — c'est le contrat structurel du
///   module (précédent task_model V234 : un `int id` légitime, pas de la
///   dette). `tenantId`/`createdBy` sont désormais UUID côté serveur mais
///   FORCEMENT serveur (JWT) : ils ne figurent pas dans les corps clients.
/// - `StreamChatMessage` : id UUID → String Dart ; `senderId` UUID → String.
///   Le serveur ne renvoie que {id, streamId, senderId, senderName, content,
///   messageType, emoji, createdAt} — rien d'inventé (pas de totalViews,
///   pas d'isSystem/isOwn sur le fil : isOwn se dérive localement).
library;

class StreamModel {
  final int id;
  final String title;
  final String? description;
  final StreamStatus status;
  final String? streamUrl;
  final String? thumbnailUrl;
  final String? recordingUrl;
  final DateTime? scheduledAt;
  final DateTime? startedAt;
  final DateTime? endedAt;
  final int viewerCount;
  final DateTime? createdAt;

  const StreamModel({
    required this.id,
    required this.title,
    this.description,
    this.status = StreamStatus.scheduled,
    this.streamUrl,
    this.thumbnailUrl,
    this.recordingUrl,
    this.scheduledAt,
    this.startedAt,
    this.endedAt,
    this.viewerCount = 0,
    this.createdAt,
  });

  factory StreamModel.fromJson(Map<String, dynamic> json) => StreamModel(
        id: (json['id'] as num?)?.toInt() ?? 0,
        title: json['title']?.toString() ?? 'Sans titre',
        description: json['description']?.toString(),
        status: (json['status'] as String?)?.toStreamStatus() ?? StreamStatus.scheduled,
        streamUrl: json['streamUrl']?.toString(),
        thumbnailUrl: json['thumbnailUrl']?.toString(),
        recordingUrl: json['recordingUrl']?.toString(),
        scheduledAt: _parseDate(json['scheduledAt']),
        startedAt: _parseDate(json['startedAt']),
        endedAt: _parseDate(json['endedAt']),
        viewerCount: (json['viewerCount'] as num?)?.toInt() ?? 0,
        createdAt: _parseDate(json['createdAt']),
      );

  /// Corps attendu par POST/PUT /api/v1/streams (LiveStream désérialisé
  /// serveur) : uniquement les champs éditables. tenantId/createdBy/status
  /// sont ignorés du corps — forcés/transitionnés serveur.
  Map<String, dynamic> editBody() => {
        'title': title,
        if (description != null && description!.isNotEmpty) 'description': description,
        if (streamUrl != null) 'streamUrl': streamUrl,
        if (thumbnailUrl != null) 'thumbnailUrl': thumbnailUrl,
        if (scheduledAt != null) 'scheduledAt': _wireDate(scheduledAt!),
      };
}

class StreamChatMessage {
  final String id;
  final int streamId;
  final String? senderId;
  final String senderName;
  final String content;
  final String messageType; // TEXT | REACTION (valeurs serveur)
  final String? emoji;
  final DateTime createdAt;

  const StreamChatMessage({
    required this.id,
    required this.streamId,
    this.senderId,
    required this.senderName,
    required this.content,
    this.messageType = 'TEXT',
    this.emoji,
    required this.createdAt,
  });

  factory StreamChatMessage.fromJson(Map<String, dynamic> json) => StreamChatMessage(
        id: json['id']?.toString() ?? '',
        streamId: (json['streamId'] as num?)?.toInt() ?? 0,
        senderId: json['senderId']?.toString(),
        senderName: json['senderName']?.toString() ?? 'Utilisateur',
        content: json['content']?.toString() ?? '',
        messageType: json['messageType']?.toString() ?? 'TEXT',
        emoji: json['emoji']?.toString(),
        createdAt: _parseDate(json['createdAt']) ?? DateTime.now(),
      );

  bool get isReaction => messageType == 'REACTION';
}

enum StreamStatus {
  scheduled,
  live,
  ended,
  cancelled;

  /// Valeur wire exacte de LiveStream.StreamStatus (enum Java).
  String get wire => name.toUpperCase();

  String get displayName {
    switch (this) {
      case StreamStatus.scheduled:
        return 'Planifié';
      case StreamStatus.live:
        return 'En direct';
      case StreamStatus.ended:
        return 'Terminé';
      case StreamStatus.cancelled:
        return 'Annulé';
    }
  }

  String get colorHex {
    switch (this) {
      case StreamStatus.scheduled:
        return '#3B82F6';
      case StreamStatus.live:
        return '#EF4444';
      case StreamStatus.ended:
        return '#6B7280';
      case StreamStatus.cancelled:
        return '#9CA3AF';
    }
  }
}

extension StreamStatusExtension on String {
  StreamStatus toStreamStatus() {
    switch (toUpperCase()) {
      case 'SCHEDULED':
        return StreamStatus.scheduled;
      case 'LIVE':
        return StreamStatus.live;
      case 'ENDED':
        return StreamStatus.ended;
      case 'CANCELLED':
        return StreamStatus.cancelled;
      default:
        return StreamStatus.scheduled;
    }
  }
}

/// Les dates serveur sont des LocalDateTime Jackson (« 2026-10-07T20:00:00 »,
/// sans fuseau) ou null — tolérant, jamais un cast dur.
DateTime? _parseDate(Object? v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}

/// Format wire attendu par LocalDateTime serveur (sans décalage).
String _wireDate(DateTime d) {
  final local = d.toLocal();
  String two(int n) => n.toString().padLeft(2, '0');
  return '${local.year}-${two(local.month)}-${two(local.day)}'
      'T${two(local.hour)}:${two(local.minute)}:${two(local.second)}';
}
