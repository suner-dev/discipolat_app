import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:discipolat_mobile/features/messages/models/message_model.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class MessageInput extends ConsumerStatefulWidget {
  final Function(String, {MessageType type, List<String>? mediaUrls, int? replyToMessageId}) onSend;
  final VoidCallback onMedia;
  final VoidCallback onVoice;

  const MessageInput({
    super.key,
    required this.onSend,
    required this.onMedia,
    required this.onVoice,
  });

  @override
  ConsumerState<MessageInput> createState() => _MessageInputState();
}

class _MessageInputState extends ConsumerState<MessageInput> {
  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  MessageType _currentType = MessageType.text;
  int? _replyToMessageId;
  String? _replyPreview;

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardDark,
        border: Border(top: BorderSide(color: AppColors.surface.withOpacity(0.3))),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_replyToMessageId != null) _buildReplyPreview(),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                _buildMediaButton(Icons.photo_library_rounded, 'Galerie', onMedia: widget.onMedia),
                const SizedBox(width: 8),
                _buildMediaButton(Icons.camera_alt_rounded, 'Appareil', onMedia: () {
                  // TODO: Camera
                }),
                const SizedBox(width: 8),
                _buildMediaButton(Icons.mic_rounded, 'Vocal', onMedia: widget.onVoice),
                const SizedBox(width: 8),
                _buildMediaButton(Icons.attach_file_rounded, 'Fichier', onMedia: () {
                  // TODO: File picker
                }),
                const SizedBox(width: 12),
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      color: AppColors.surfaceDark,
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(color: AppColors.surface.withOpacity(0.3)),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _controller,
                            focusNode: _focusNode,
                            maxLines: null,
                            textInputAction: TextInputAction.send,
                            onSubmitted: (_) => _send(),
                            onChanged: (value) {
                              // Handle typing indicator
                            },
                            decoration: InputDecoration(
                              hintText: 'Écrire un message...',
                              hintStyle: TextStyle(color: AppColors.surface.withOpacity(0.5)),
                              border: InputBorder.none,
                              contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                            ),
                            style: const TextStyle(color: Colors.white, fontSize: 14),
                          ),
                        ),
                        IconButton(
                          icon: Icon(
                            Icons.face_rounded,
                            color: AppColors.surface.withOpacity(0.5),
                          ),
                          onPressed: () {
                            // TODO: Emoji picker
                          },
                        ),
                        IconButton(
                          icon: Icon(
                            Icons.gif_rounded,
                            color: AppColors.surface.withOpacity(0.5),
                          ),
                          onPressed: () {
                            // TODO: GIF picker
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(width: 12),
        Container(
            decoration: BoxDecoration(
              color: AppColors.primary,
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: const Icon(Icons.send_rounded, color: Colors.white),
              onPressed: _send,
            ),
          ),
        ],
      ),
    ),
    );
  }

  Widget _buildReplyPreview() {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(12),
        border: Border(
          left: BorderSide(color: AppColors.primary, width: 3),
          top: BorderSide(color: AppColors.primary.withOpacity(0.3)),
          right: BorderSide(color: AppColors.primary.withOpacity(0.3)),
          bottom: BorderSide(color: AppColors.primary.withOpacity(0.3)),
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.reply_rounded, size: 14, color: AppColors.primary),
                    const SizedBox(width: 4),
                    Text(
                      'Réponse à',
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: AppColors.primary),
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  _replyPreview ?? 'Message',
                  style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          IconButton(
            icon: Icon(Icons.close_rounded, size: 18, color: AppColors.surface.withOpacity(0.7)),
            onPressed: () {
              setState(() {
                _replyToMessageId = null;
                _replyPreview = null;
              });
            },
          ),
        ],
      ),
    );
  }

  Widget _buildMediaButton(IconData icon, String label, {required VoidCallback onMedia}) {
    return InkWell(
      onTap: onMedia,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: AppColors.surfaceDark,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.surface.withOpacity(0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 18, color: AppColors.primary),
            const SizedBox(width: 4),
            Text(label, style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.7))),
          ],
        ),
      ),
    );
  }

  void _send() {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    widget.onSend(
      text,
      type: _currentType,
      replyToMessageId: _replyToMessageId,
    );

    _controller.clear();
    setState(() {
      _replyToMessageId = null;
      _replyPreview = null;
    });
  }
}