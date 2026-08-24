/// P10 — Modèle message de groupe (mobile).
///
/// Correspond au payload JSON de `GroupMessageController` (entity
/// `com.discipolat.modules.groupMessages.domain.GroupMessage`).
class GroupMessage {
  final String id;
  final String groupId;
  final String groupType;
  final String senderId;
  final String content;
  final String messageType;
  final int reactionCount;
  final bool isDeleted;
  final DateTime createdAt;

  GroupMessage({
    required this.id,
    required this.groupId,
    required this.groupType,
    required this.senderId,
    required this.content,
    required this.messageType,
    required this.reactionCount,
    required this.isDeleted,
    required this.createdAt,
  });

  factory GroupMessage.fromJson(Map<String, dynamic> json) => GroupMessage(
        id: (json['id'] ?? json['Id'])?.toString() ?? '',
        groupId: (json['groupId'] ?? json['groupId']?.toString()) ?? '',
        groupType: json['groupType']?.toString() ?? 'DEPARTMENT',
        senderId: json['senderId']?.toString() ?? '',
        content: json['content']?.toString() ?? '',
        messageType: json['messageType']?.toString() ?? 'TEXT',
        reactionCount: json['reactionCount'] is int
            ? json['reactionCount'] as int
            : int.tryParse(json['reactionCount']?.toString() ?? '0') ?? 0,
        isDeleted: json['isDeleted'] is bool
            ? json['isDeleted'] as bool
            : (json['isDeleted']?.toString().toLowerCase() == 'true'),
        createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
            DateTime.now(),
      );
}
