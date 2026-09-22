import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/events/models/event_model.dart';
import 'package:discipolat_mobile/features/events/services/events_service.dart';

class EventChatOverlay extends ConsumerStatefulWidget {
  final int eventId;
  final VoidCallback onClose;

  const EventChatOverlay({
    super.key,
    required this.eventId,
    required this.onClose,
  });

  @override
  ConsumerState<EventChatOverlay> createState() => _EventChatOverlayState();
}

class _EventChatOverlayState extends ConsumerState<EventChatOverlay> {
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
    final messagesAsync = ref.watch(_messagesProvider(widget.eventId));

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
                  'Chat de l\'événement',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
                const Spacer(),
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
                          'Aucun message pour cet événement',
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
      // TODO: Implement send message via events service
      // await ref.read(eventsServiceProvider).sendChatMessage(widget.eventId, text);
      ref.invalidate(_messagesProvider(widget.eventId));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }
}

// Providers
final _messagesProvider = FutureProvider.family<List<StreamChatMessage>, int>((ref, eventId) async {
  final service = ref.watch(eventsServiceProvider);
  // TODO: Implement getChatMessages in events service
  return <StreamChatMessage>[];
});

@freezed
class StreamChatMessage with _$StreamChatMessage {
  const factory StreamChatMessage({
    required int id,
    required String senderName,
    required String content,
    required DateTime createdAt,
    @Default(false) bool isOwn,
  }) = _StreamChatMessage;

  factory StreamChatMessage.fromJson(Map<String, dynamic> json) => _$StreamChatMessageFromJson(json);
}