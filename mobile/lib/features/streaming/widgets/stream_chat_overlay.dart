import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';
import 'package:discipolat_mobile/features/streaming/services/streaming_service.dart';

class StreamChatOverlay extends ConsumerStatefulWidget {
  final int streamId;
  final VoidCallback onClose;

  const StreamChatOverlay({
    super.key,
    required this.streamId,
    required this.onClose,
  });

  @override
  ConsumerState<StreamChatOverlay> createState() => _StreamChatOverlayState();
}

class _StreamChatOverlayState extends ConsumerState<StreamChatOverlay> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final messagesAsync = ref.watch(_messagesProvider(widget.streamId));
    final viewersAsync = ref.watch(_viewerCountProvider(widget.streamId));

    return Container(
      decoration: BoxDecoration(
        color: AppColors.cardDark,
        borderRadius: const BorderRadius.horizontal(left: Radius.circular(20)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(-5, 0),
          ),
        ],
      ),
      child: Column(
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: AppColors.surface.withOpacity(0.3))),
            ),
            child: Row(
              children: [
                Icon(Icons.chat_bubble_rounded, color: AppColors.primary, size: 24),
                const SizedBox(width: 8),
                const Text(
                  'Chat en direct',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
                const Spacer(),
                viewersAsync.when(
                  data: (count) => Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.red.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.remove_red_eye, size: 12, color: Colors.red),
                        const SizedBox(width: 4),
                        Text(
                          count.count.toString(),
                          style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.red),
                        ),
                      ],
                    ),
                  ),
                  loading: () => const SizedBox.shrink(),
                  error: (_, __) => const SizedBox.shrink(),
                ),
                IconButton(
                  icon: const Icon(Icons.close_rounded),
                  onPressed: widget.onClose,
                ),
              ],
            ),
          ),
          // Messages
          Expanded(
            child: messagesAsync.when(
              data: (messages) {
                if (messages.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.chat_bubble_outline_rounded, size: 48, color: AppColors.surface.withOpacity(0.5)),
                        const SizedBox(height: 12),
                        Text(
                          'Aucun message pour ce stream',
                          style: TextStyle(color: AppColors.surface.withOpacity(0.7)),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Soyez le premier à écrire !',
                          style: TextStyle(color: AppColors.surface.withOpacity(0.5), fontSize: 12),
                        ),
                      ],
                    ),
                  );
                }

                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (_scrollController.hasClients) {
                    _scrollController.animateTo(
                      _scrollController.position.maxScrollExtent,
                      duration: const Duration(milliseconds: 300),
                      curve: Curves.easeOut,
                    );
                  }
                });

                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(16),
                  itemCount: messages.length,
                  itemBuilder: (context, index) {
                    final msg = messages[index];
                    return _buildMessage(msg);
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(child: Text('Erreur: $error')),
            ),
          ),
          // Quick reactions
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              border: Border(top: BorderSide(color: AppColors.surface.withOpacity(0.3))),
            ),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: ['🙏', '❤️', '🔥', '👏', '✨', '💯', '😭', '😍'].map((emoji) {
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: InkWell(
                      onTap: () => _sendReaction(emoji),
                      borderRadius: BorderRadius.circular(20),
                      child: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Text(emoji, style: const TextStyle(fontSize: 20)),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
          ),
          // Input
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              border: Border(top: BorderSide(color: AppColors.surface.withOpacity(0.3))),
            ),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: InputDecoration(
                      hintText: 'Écrire un message...',
                      hintStyle: TextStyle(color: AppColors.surface.withOpacity(0.5)),
                      filled: true,
                      fillColor: AppColors.surfaceDark,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(24),
                        borderSide: BorderSide(color: AppColors.surface.withOpacity(0.3)),
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    ),
                    onSubmitted: (_) => _sendMessage(),
                    maxLines: null,
                    textInputAction: TextInputAction.send,
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    shape: BoxShape.circle,
                  ),
                  child: IconButton(
                    icon: const Icon(Icons.send_rounded, color: Colors.white),
                    onPressed: _sendMessage,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessage(StreamChatMessage msg) {
    final isOwn = msg.isOwn;
    final isReaction = msg.messageType == 'REACTION';
    final isSystem = msg.isSystem;

    if (isSystem) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Text(
            msg.content,
            style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.5), fontStyle: FontStyle.italic),
          ),
        ),
      );
    }

    if (isReaction) {
      return Align(
        alignment: isOwn ? Alignment.centerRight : Alignment.centerLeft,
        child: Container(
          margin: const EdgeInsets.symmetric(vertical: 4),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Text(
            '${msg.senderName} a réagi ${msg.emoji ?? msg.content}',
            style: TextStyle(fontSize: 11, color: AppColors.primary),
          ),
        ),
      );
    }

    return Align(
      alignment: isOwn ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.6),
        child: Column(
          crossAxisAlignment: isOwn ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          children: [
            if (!isOwn)
              Padding(
                padding: const EdgeInsets.only(left: 12, bottom: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      msg.senderName,
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: AppColors.surface.withOpacity(0.7)),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      DateFormat('HH:mm').format(msg.createdAt.toLocal()),
                      style: TextStyle(fontSize: 9, color: AppColors.surface.withOpacity(0.5)),
                    ),
                  ],
                ),
              ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: isOwn ? AppColors.primary : AppColors.surfaceDark,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(18),
                  topRight: const Radius.circular(18),
                  bottomLeft: Radius.circular(isOwn ? 18 : 4),
                  bottomRight: Radius.circular(isOwn ? 4 : 18),
                ),
              ),
              child: Text(
                msg.content,
                style: TextStyle(
                  color: isOwn ? Colors.white : AppColors.surface,
                  fontSize: 14,
                ),
              ),
            ),
            if (isOwn)
              Padding(
                padding: const EdgeInsets.only(right: 12, top: 4),
                child: Text(
                  DateFormat('HH:mm').format(msg.createdAt.toLocal()),
                  style: TextStyle(fontSize: 9, color: AppColors.surface.withOpacity(0.5)),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _sendMessage() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    _controller.clear();
    try {
      await ref.read(streamingServiceProvider).sendChatMessage(widget.streamId, text);
      ref.invalidate(_messagesProvider(widget.streamId));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  Future<void> _sendReaction(String emoji) async {
    try {
      await ref.read(streamingServiceProvider).sendChatMessage(widget.streamId, emoji, emoji: emoji);
      ref.invalidate(_messagesProvider(widget.streamId));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }
}

// Providers
final _messagesProvider = FutureProvider.family<List<StreamChatMessage>, int>((ref, streamId) async {
  final service = ref.watch(streamingServiceProvider);
  return service.getChatMessages(streamId);
});

final _viewerCountProvider = FutureProvider.family<StreamViewerCount, int>((ref, streamId) async {
  final service = ref.watch(streamingServiceProvider);
  return service.getViewerCount(streamId);
});
