/// Modèles de messagerie — alignés sur les DTO serveur réels (vérifiés) :
/// - `ConversationResponse` (api/ConversationResponse.java) → [Conversation]
/// - `MessageResponse` (api/MessageResponse.java) → [Message]
/// - `GroupConversationResponse` (api/GroupConversationResponse.java) → [GroupConversation]
///
/// IDENTIFIANTS : toutes les entités messages sont `@GeneratedValue(UUID)`
/// (Conversation.java, ConversationMessage.java, GroupConversation.java) →
/// ids et refs personnes sont des `String` ici, comme le serveur.
///
/// Pas de freezed : classes immuables plain Dart, fromJson tolérants,
/// toJson = corps exacts des requêtes serveur (SendEnhancedMessageRequest,
/// CreateGroupRequest, ToggleReactionRequest…).
library;

// ── Helpers de parsing tolérants ───────────────────────────────────────────

int _i(Object? v, [int fallback = 0]) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  if (v is String) return int.tryParse(v) ?? fallback;
  return fallback;
}

String? _s(Object? v) => v?.toString();

String _str(Object? v, [String fallback = '']) => v == null ? fallback : v.toString();

/// LocalDateTime serveur (ISO sans offset, ex. 2026-10-07T03:15:22.123456).
/// Sans offset → interprété comme heure locale, cohérent avec l'affichage.
DateTime? _dt(Object? v) {
  if (v is DateTime) return v;
  if (v is! String) return null;
  return DateTime.tryParse(v);
}

Map<String, int> _counts(Object? v) {
  if (v is! Map) return const {};
  return v.map((key, value) => MapEntry(key.toString(), _i(value)));
}

// ── Enums wire (le serveur manipule des String, pas des enums Java) ───────

/// Types de message réellement produits par ConversationMessage.messageType
/// (TEXT, VOICE, IMAGE, FILE, SYSTEM — javadoc de l'entité) + valeurs
/// historiques tolérées en lecture.
enum MessageType {
  text('TEXT'),
  voice('VOICE'),
  image('IMAGE'),
  file('FILE'),
  system('SYSTEM');

  const MessageType(this.wire);
  final String wire;

  static MessageType fromWire(Object? v) {
    final name = v?.toString().trim().toUpperCase();
    return MessageType.values.firstWhere(
      (e) => e.wire == name,
      orElse: () => MessageType.text,
    );
  }
}

/// Classification côté client uniquement : le serveur n'expose pas de champ
/// `type` sur les conversations — 1:1 vient de /messages/conversations,
/// groupes de /messages/groups.
enum ConversationKind {
  direct,
  group;

  String get label => this == ConversationKind.direct ? 'Privé' : 'Groupe';
}

// ── Conversation 1:1 (ConversationResponse) ────────────────────────────────

class Conversation {
  const Conversation({
    required this.id,
    required this.otherUserId,
    required this.otherUserName,
    this.otherUserRole,
    this.lastMessage,
    this.lastMessageSenderId,
    this.lastMessageAt,
    this.unreadCount = 0,
    this.createdAt,
  });

  final String id;
  final String otherUserId;
  final String otherUserName;
  final String? otherUserRole;
  final String? lastMessage;
  final String? lastMessageSenderId;
  final DateTime? lastMessageAt;
  final int unreadCount;
  final DateTime? createdAt;

  factory Conversation.fromJson(Map<String, dynamic> json) => Conversation(
        id: _str(json['id']),
        otherUserId: _str(json['otherUserId']),
        otherUserName: _str(json['otherUserName'], 'Utilisateur'),
        otherUserRole: _s(json['otherUserRole']),
        lastMessage: _s(json['lastMessage']),
        lastMessageSenderId: _s(json['lastMessageSenderId']),
        lastMessageAt: _dt(json['lastMessageAt']),
        unreadCount: _i(json['unreadCount']),
        createdAt: _dt(json['createdAt']),
      );

  /// Corps exact de StartConversationRequest (@NotNull otherUserId UUID).
  Map<String, dynamic> startBody() => {'otherUserId': otherUserId};
}

// ── Message (MessageResponse) ──────────────────────────────────────────────

class Message {
  const Message({
    required this.id,
    this.conversationId,
    this.groupId,
    required this.senderId,
    required this.senderName,
    this.content,
    this.messageType = MessageType.text,
    this.mediaUrl,
    this.mediaDuration,
    this.replyToId,
    this.replyToSenderName,
    this.replyToContent,
    this.readAt,
    required this.createdAt,
    this.reactionCounts = const {},
    this.userReaction,
  });

  final String id;
  final String? conversationId;
  final String? groupId;
  final String senderId;
  final String senderName;
  final String? content;
  final MessageType messageType;
  final String? mediaUrl;
  final int? mediaDuration;
  final String? replyToId;
  final String? replyToSenderName;
  final String? replyToContent;
  final DateTime? readAt;
  final DateTime createdAt;
  final Map<String, int> reactionCounts;
  final String? userReaction;

  bool get isSystem => messageType == MessageType.system;
  bool get isReply => replyToId != null;
  bool get hasMedia => mediaUrl != null && mediaUrl!.isNotEmpty;

  int get reactionsTotal =>
      reactionCounts.values.fold(0, (sum, n) => sum + n);

  /// Le serveur ne renvoie pas `isOwn` : la comparaison se fait avec
  /// AuthState().userId (UUID String), même domaine que senderId.
  bool isOwnBy(String? currentUserId) =>
      currentUserId != null &&
      currentUserId.isNotEmpty &&
      senderId == currentUserId;

  factory Message.fromJson(Map<String, dynamic> json) => Message(
        id: _str(json['id']),
        conversationId: _s(json['conversationId']),
        groupId: _s(json['groupId']),
        senderId: _str(json['senderId']),
        senderName: _str(json['senderName'], 'Utilisateur'),
        content: _s(json['content']),
        messageType: MessageType.fromWire(json['messageType']),
        mediaUrl: _s(json['mediaUrl']),
        mediaDuration:
            json['mediaDuration'] == null ? null : _i(json['mediaDuration']),
        replyToId: _s(json['replyToId']),
        replyToSenderName: _s(json['replyToSenderName']),
        replyToContent: _s(json['replyToContent']),
        readAt: _dt(json['readAt']),
        createdAt: _dt(json['createdAt']) ?? DateTime.now(),
        reactionCounts: _counts(json['reactionCounts']),
        userReaction: _s(json['userReaction']),
      );
}

// ── Réactions (payload de toggleReaction/getReactions) ─────────────────────

/// Le serveur répond `Map<String,Object>` : {added?, reactionCounts,
/// userReaction, totalReactions?} — pas une liste d'entités.
class ReactionSummary {
  const ReactionSummary({
    this.added,
    this.reactionCounts = const {},
    this.userReaction,
    this.totalReactions,
  });

  final bool? added;
  final Map<String, int> reactionCounts;
  final String? userReaction;
  final int? totalReactions;

  factory ReactionSummary.fromJson(Map<String, dynamic> json) =>
      ReactionSummary(
        added: json['added'] is bool ? json['added'] as bool : null,
        reactionCounts: _counts(json['reactionCounts']),
        userReaction: _s(json['userReaction']),
        totalReactions:
            json['totalReactions'] == null ? null : _i(json['totalReactions']),
      );
}

// ── Conversation de groupe (GroupConversationResponse) ─────────────────────

class GroupConversation {
  const GroupConversation({
    required this.id,
    required this.name,
    this.description,
    this.groupType,
    this.createdBy,
    this.avatarUrl,
    this.memberCount = 0,
    this.unreadCount = 0,
    this.isMember = false,
    this.lastMessage,
    this.lastMessageAt,
    this.createdAt,
  });

  final String id;
  final String name;
  final String? description;
  final String? groupType;
  final String? createdBy;
  final String? avatarUrl;
  final int memberCount;
  final int unreadCount;
  final bool isMember;
  final String? lastMessage;
  final DateTime? lastMessageAt;
  final DateTime? createdAt;

  factory GroupConversation.fromJson(Map<String, dynamic> json) =>
      GroupConversation(
        id: _str(json['id']),
        name: _str(json['name'], 'Groupe'),
        description: _s(json['description']),
        groupType: _s(json['groupType']),
        createdBy: _s(json['createdBy']),
        avatarUrl: _s(json['avatarUrl']),
        memberCount: _i(json['memberCount']),
        unreadCount: _i(json['unreadCount']),
        isMember: json['isMember'] == true,
        lastMessage: _s(json['lastMessage']),
        lastMessageAt: _dt(json['lastMessageAt']),
        createdAt: _dt(json['createdAt']),
      );

  /// Corps exact de CreateGroupRequest (name @NotBlank, description,
  /// groupType, memberIds `List<UUID>`).
  static Map<String, dynamic> createBody({
    required String name,
    String? description,
    String? groupType,
    List<String> memberIds = const [],
  }) =>
      {
        'name': name,
        if (description != null && description.isNotEmpty)
          'description': description,
        if (groupType != null && groupType.isNotEmpty) 'groupType': groupType,
        'memberIds': memberIds,
      };
}
