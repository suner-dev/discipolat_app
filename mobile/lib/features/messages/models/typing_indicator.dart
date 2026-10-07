/// Indicateur « écrit… » — payload réel du broker STOMP serveur
/// (MessageWebSocketController.handleTyping) :
/// `{userId: String(UUID), typing: bool, timestamp: String}` broadcasté sur
/// `/topic/conversations/{id}/typing`. Le conversationId vient de la
/// destination d'abonnement, pas du corps du message.
library;

class TypingIndicator {
  const TypingIndicator({
    required this.conversationId,
    required this.userId,
    required this.isTyping,
    this.userName,
    this.timestamp,
  });

  final String conversationId;
  final String userId;
  final bool isTyping;

  /// Le serveur n'envoie pas de nom — rempli côté client si connu.
  final String? userName;
  final DateTime? timestamp;

  factory TypingIndicator.fromJson(String conversationId, Map<String, dynamic> json) =>
      TypingIndicator(
        conversationId: conversationId,
        userId: json['userId']?.toString() ?? '',
        isTyping: json['typing'] == true,
        userName: json['userName']?.toString(),
        timestamp: DateTime.tryParse(json['timestamp']?.toString() ?? ''),
      );
}
