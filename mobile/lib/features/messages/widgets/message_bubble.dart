import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/features/messages/models/message_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class MessageBubble extends StatelessWidget {
  final Message message;
  final VoidCallback? onLongPress;
  final VoidCallback? onReply;
  final Function(String)? onReaction;

  const MessageBubble({
    super.key,
    required this.message,
    this.onLongPress,
    this.onReply,
    this.onReaction,
  });

  @override
  Widget build(BuildContext context) {
    final isOwn = message.isOwn;
    final isSystem = message.isSystem;
    final isReply = message.replyToMessageId != null;

    if (isSystem) {
      return _buildSystemMessage();
    }

    return GestureDetector(
      onLongPress: onLongPress,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(
          mainAxisAlignment:
              isOwn ? MainAxisAlignment.end : MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            if (!isOwn) ...[
              CircleAvatar(
                radius: 14,
                backgroundColor: AppColors.primary.withOpacity(0.2),
                child: Text(
                  message.senderName.isNotEmpty
                      ? message.senderName[0].toUpperCase()
                      : '?',
                  style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: AppColors.primary),
                ),
              ),
              const SizedBox(width: 8),
            ],
            Flexible(
              child: Column(
                crossAxisAlignment:
                    isOwn ? CrossAxisAlignment.end : CrossAxisAlignment.start,
                children: [
                  if (isReply) _buildReplyPreview(),
                  if (!isOwn && !isSystem)
                    Padding(
                      padding: const EdgeInsets.only(left: 4, bottom: 2),
                      child: Text(
                        message.senderName,
                        style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w500,
                            color: AppColors.surface.withOpacity(0.7)),
                      ),
                    ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      if (!isOwn) const SizedBox(width: 8),
                      Flexible(
                        child: Column(
                          crossAxisAlignment: isOwn
                              ? CrossAxisAlignment.end
                              : CrossAxisAlignment.start,
                          children: [
                            _buildMessageContent(),
                            const SizedBox(height: 4),
                            _buildMessageFooter(),
                          ],
                        ),
                      ),
                      if (isOwn) const SizedBox(width: 8),
                    ],
                  ),
                  if (message.reactions.isNotEmpty) _buildReactions(),
                ],
              ),
            ),
            if (isOwn) ...[
              const SizedBox(width: 8),
              CircleAvatar(
                radius: 14,
                backgroundColor: Colors.green.withOpacity(0.2),
                child: const Icon(Icons.check_circle_rounded,
                    color: Colors.green, size: 14),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSystemMessage() {
    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: AppColors.cardDark,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.surface.withOpacity(0.3)),
        ),
        child: Text(
          message.content,
          style: TextStyle(
              fontSize: 11,
              color: AppColors.surface.withOpacity(0.5),
              fontStyle: FontStyle.italic),
          textAlign: TextAlign.center,
        ),
      ),
    );
  }

  Widget _buildReplyPreview() {
    return Container(
      margin: const EdgeInsets.only(bottom: 4),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(8),
        border: Border(
          left: BorderSide(color: AppColors.primary, width: 3),
          top: BorderSide(color: AppColors.primary.withOpacity(0.3)),
          right: BorderSide(color: AppColors.primary.withOpacity(0.3)),
          bottom: BorderSide(color: AppColors.primary.withOpacity(0.3)),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.reply_rounded, size: 12, color: AppColors.primary),
          const SizedBox(width: 4),
          Flexible(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Réponse',
                  style: TextStyle(
                      fontSize: 9,
                      fontWeight: FontWeight.w500,
                      color: AppColors.primary),
                ),
                Text(
                  message.content.length > 30
                      ? '${message.content.substring(0, 30)}...'
                      : message.content,
                  style: TextStyle(
                      fontSize: 10, color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageContent() {
    if (message.type != MessageType.text && message.mediaUrls.isNotEmpty) {
      return _buildMediaContent();
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: message.isOwn ? AppColors.primary : AppColors.cardDark,
        borderRadius: BorderRadius.only(
          topLeft: const Radius.circular(18),
          topRight: const Radius.circular(18),
          bottomLeft: Radius.circular(message.isOwn ? 18 : 4),
          bottomRight: Radius.circular(message.isOwn ? 4 : 18),
        ),
        boxShadow: message.isOwn
            ? [
                BoxShadow(
                    color: AppColors.primary.withOpacity(0.3),
                    blurRadius: 8,
                    offset: const Offset(0, 2))
              ]
            : null,
      ),
      child: SelectableText(
        message.content,
        style: TextStyle(
          color: message.isOwn ? Colors.white : AppColors.surface,
          fontSize: 14,
          height: 1.4,
        ),
      ),
    );
  }

  Widget _buildMediaContent() {
    final mediaUrl = message.mediaUrls.first;
    final isImage = message.type == MessageType.image;

    return Container(
      constraints: const BoxConstraints(maxWidth: 250, maxHeight: 300),
      decoration: BoxDecoration(
        color: message.isOwn
            ? AppColors.primary.withOpacity(0.9)
            : AppColors.cardDark,
        borderRadius: BorderRadius.only(
          topLeft: const Radius.circular(18),
          topRight: const Radius.circular(18),
          bottomLeft: Radius.circular(message.isOwn ? 18 : 4),
          bottomRight: Radius.circular(message.isOwn ? 4 : 18),
        ),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.only(
          topLeft: const Radius.circular(18),
          topRight: const Radius.circular(18),
          bottomLeft: Radius.circular(message.isOwn ? 18 : 4),
          bottomRight: Radius.circular(message.isOwn ? 4 : 18),
        ),
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (isImage)
              Image.network(
                mediaUrl,
                fit: BoxFit.cover,
                errorBuilder: (_, __, ___) => Container(
                  color: Colors.grey[800],
                  child: const Center(
                      child: Icon(Icons.broken_image_rounded,
                          color: Colors.white54)),
                ),
              )
            else
              Container(
                color: Colors.grey[800],
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                          message.type == MessageType.video
                              ? Icons.videocam_rounded
                              : Icons.audiotrack_rounded,
                          size: 48,
                          color: Colors.white54),
                      const SizedBox(height: 8),
                      Text(
                        message.content.isEmpty
                            ? message.type.name
                            : message.content,
                        style: TextStyle(color: Colors.white70, fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ),
            if (message.content.isNotEmpty)
              Positioned(
                bottom: 0,
                left: 0,
                right: 0,
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.bottomCenter,
                      end: Alignment.topCenter,
                      colors: [
                        Colors.black.withOpacity(0.8),
                        Colors.transparent
                      ],
                    ),
                  ),
                  child: Text(
                    message.content,
                    style: const TextStyle(color: Colors.white, fontSize: 12),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageFooter() {
    return Padding(
      padding: const EdgeInsets.only(left: 4, right: 4),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment:
            message.isOwn ? MainAxisAlignment.end : MainAxisAlignment.start,
        children: [
          Text(
            DateFormat('HH:mm').format(message.createdAt.toLocal()),
            style: TextStyle(
                fontSize: 10,
                color: message.isOwn
                    ? Colors.white70
                    : AppColors.surface.withOpacity(0.5)),
          ),
          if (message.isOwn) ...[
            const SizedBox(width: 4),
            Icon(
              Icons.done_all_rounded,
              size: 12,
              color: message.reactions.isNotEmpty
                  ? AppColors.success
                  : Colors.white70,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildReactions() {
    return Padding(
      padding: const EdgeInsets.only(top: 4, left: 4, right: 4),
      child: Wrap(
        spacing: 4,
        runSpacing: 2,
        children: message.reactions.entries.map((entry) {
          final emoji = entry.key;
          final users = entry.value;
          final hasReacted = users.contains(_getCurrentUserId());

          return InkWell(
            onTap: () => onReaction?.call(emoji),
            borderRadius: BorderRadius.circular(12),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                color: hasReacted
                    ? AppColors.primary.withOpacity(0.2)
                    : AppColors.surfaceDark,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                    color: hasReacted
                        ? AppColors.primary
                        : AppColors.surface.withOpacity(0.3)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(emoji, style: const TextStyle(fontSize: 12)),
                  if (users.length > 1) ...[
                    const SizedBox(width: 4),
                    Text(
                      users.length.toString(),
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: hasReacted
                            ? AppColors.primary
                            : AppColors.surface.withOpacity(0.7),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  int _getCurrentUserId() {
    // TODO: Get from auth state
    return 1;
  }
}
