/// Model for ScheduledAnnouncement entity (backend AnnouncementController /api/v1/announcements).
class ScheduledAnnouncement {
  final String id;
  final String title;
  final String? content;
  final String target;
  final String status;
  final DateTime? scheduledAt;
  final DateTime? publishedAt;
  final bool pinToTop;
  final DateTime createdAt;

  ScheduledAnnouncement({
    required this.id,
    required this.title,
    this.content,
    required this.target,
    required this.status,
    this.scheduledAt,
    this.publishedAt,
    this.pinToTop = false,
    required this.createdAt,
  });

  factory ScheduledAnnouncement.fromJson(Map<String, dynamic> json) => ScheduledAnnouncement(
        id: (json['id'] ?? '').toString(),
        title: (json['title'] ?? '').toString(),
        content: json['content']?.toString(),
        target: (json['target'] ?? 'ALL').toString(),
        status: (json['status'] ?? 'DRAFT').toString(),
        scheduledAt: DateTime.tryParse(json['scheduledAt']?.toString() ?? ''),
        publishedAt: DateTime.tryParse(json['publishedAt']?.toString() ?? ''),
        pinToTop: json['pinToTop'] is bool
            ? json['pinToTop'] as bool
            : (json['pinToTop']?.toString().toLowerCase() == 'true'),
        createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? '') ?? DateTime.now(),
      );
}
