import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import 'package:discipolat_mobile/features/messages/models/message_model.dart';
import 'package:discipolat_mobile/features/messages/models/typing_indicator.dart';
import 'package:discipolat_mobile/features/messages/services/messages_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';
import 'package:discipolat_mobile/features/messages/widgets/message_bubble.dart';
import 'package:discipolat_mobile/features/messages/widgets/message_input.dart';

class ConversationDetailScreen extends ConsumerStatefulWidget {
  final int conversationId;

  const ConversationDetailScreen({super.key, required this.conversationId});

  @override
  ConsumerState<ConversationDetailScreen> createState() => _ConversationDetailScreenState();
}

class _ConversationDetailScreenState extends ConsumerState<ConversationDetailScreen> {
  final ScrollController _scrollController = ScrollController();
  final TextEditingController _searchController = TextEditingController();
  bool _showSearch = false;
  bool _isLoadingMore = false;

  @override
  void initState() {
    super.initState();
    _loadInitialData();
    _scrollController.addListener(_onScroll);
    // Mark as read when opened
    ref.read(messagesServiceProvider).markAsRead(widget.conversationId, 0);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    ref.read(messagesServiceProvider).connectWebSocket('TODO: token');
  }

  void _onScroll() {
    if (_scrollController.position.pixels <= 200 && !_isLoadingMore) {
      _loadMoreMessages();
    }
  }

  Future<void> _loadMoreMessages() async {
    setState(() => _isLoadingMore = true);
    try {
      final messagesAsync = ref.read(_messagesProvider(widget.conversationId));
      await messagesAsync;
    } catch (e) {
      // Ignore
    } finally {
      if (mounted) setState(() => _isLoadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final conversationAsync = ref.watch(_conversationProvider(widget.conversationId));
    final messagesAsync = ref.watch(_messagesProvider(widget.conversationId));
    final typingAsync = ref.watch(_typingProvider(widget.conversationId));

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: _buildAppBar(conversationAsync),
      body: Column(
        children: [
          if (_showSearch) _buildSearchBar(),
          Expanded(
            child: Stack(
              children: [
                messagesAsync.when(
                  data: (messages) {
                    if (messages.isEmpty) {
                      return _buildEmptyState();
                    }
                    return ListView.builder(
                      controller: _scrollController,
                      reverse: true,
                      padding: const EdgeInsets.all(16),
                      itemCount: messages.length + (_isLoadingMore ? 1 : 0),
                      itemBuilder: (context, index) {
                        if (index == messages.length) {
                          return const Center(child: Padding(
                            padding: EdgeInsets.all(16),
                            child: CircularProgressIndicator(),
                          ));
                        }
                        final message = messages[index];
                        final isFirst = index == messages.length - 1;
                        final showDate = isFirst || _shouldShowDate(messages, index);
                        return Column(
                          children: [
                            if (showDate) _buildDateHeader(messages[index].createdAt),
                            MessageBubble(
                              message: messages[index],
                              onLongPress: () => _showMessageOptions(messages[index]),
                              onReply: () => _replyToMessage(messages[index]),
                              onReaction: (emoji) => _toggleReaction(messages[index], emoji),
                            ),
                          ],
                        );
                      },
                    );
                  },
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (error, _) => Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.error_outline_rounded, size: 64, color: Colors.red),
                        const SizedBox(height: 16),
                        Text('Erreur: $error'),
                        const SizedBox(height: 16),
                        FilledButton.icon(
                          onPressed: () => ref.refresh(_messagesProvider(widget.conversationId)),
                          icon: const Icon(Icons.refresh_rounded),
                          label: const Text('Réessayer'),
                        ),
                      ],
                    ),
                  ),
                ),
                // Typing indicator
                typingAsync.when(
                  data: (typing) {
                    if (typing != null && typing.isTyping) {
                      return Positioned(
                        bottom: 80,
                        left: 16,
                        right: 16,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          decoration: BoxDecoration(
                            color: AppColors.cardDark,
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: AppColors.surface.withOpacity(0.3)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              SizedBox(
                                width: 16,
                                height: 16,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(
                                '${typing?.userName ?? ''} écrit...',
                                style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.7)),
                              ),
                            ],
                          ),
                        ),
                      );
                    }
                    return const SizedBox.shrink();
                  },
                  loading: () => const SizedBox.shrink(),
                  error: (_, __) => const SizedBox.shrink(),
                ),
              ],
            ),
          ),
          MessageInput(
            onSend: _sendMessage,
            onMedia: _sendMedia,
            onVoice: _sendVoice,
          ),
        ],
      ),
    );
  }

  PreferredSizeWidget _buildAppBar(AsyncValue<Conversation> conversationAsync) {
    return AppBar(
      backgroundColor: AppColors.cardDark,
      elevation: 0,
      leading: IconButton(
        icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
        onPressed: () => context.pop(),
      ),
      title: conversationAsync.when(
        data: (conv) => Row(
          children: [
            CircleAvatar(
              radius: 18,
              backgroundColor: AppColors.primary.withOpacity(0.2),
              backgroundImage: conv.avatarUrl != null ? NetworkImage(conv.avatarUrl!) : null,
              child: conv.avatarUrl == null
                  ? Icon(conv.type == ConversationType.direct ? Icons.person_rounded : Icons.group_rounded, color: AppColors.primary, size: 18)
                  : null,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    conv.title,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (conv.type != ConversationType.direct)
                    Text(
                      '${conv.participantIds.length} membres',
                      style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.7)),
                    )
                  else
                    Text(
                      'En ligne',
                      style: TextStyle(fontSize: 11, color: AppColors.success),
                    ),
                ],
              ),
            ),
          ],
        ),
        loading: () => const Text('Chargement...'),
        error: (_, __) => const Text('Conversation'),
      ),
      actions: [
        IconButton(
          icon: const Icon(Icons.search_rounded, color: Colors.white),
          onPressed: () => setState(() => _showSearch = !_showSearch),
        ),
        PopupMenuButton<String>(
          icon: const Icon(Icons.more_vert_rounded, color: Colors.white),
          onSelected: (value) => _handleAction(value),
          itemBuilder: (context) => [
            const PopupMenuItem(value: 'participants', child: Row(children: [Icon(Icons.people_rounded), SizedBox(width: 8), Text('Participants')])),
            const PopupMenuItem(value: 'media', child: Row(children: [Icon(Icons.photo_library_rounded), SizedBox(width: 8), Text('Médias partagés')])),
            const PopupMenuItem(value: 'links', child: Row(children: [Icon(Icons.link_rounded), SizedBox(width: 8), Text('Liens partagés')])),
            const PopupMenuItem(value: 'files', child: Row(children: [Icon(Icons.folder_rounded), SizedBox(width: 8), Text('Fichiers partagés')])),
            const PopupMenuItem(value: 'mute', child: Row(children: [Icon(Icons.notifications_off_rounded), SizedBox(width: 8), Text('Couper notifications')])),
            const PopupMenuItem(value: 'clear', child: Row(children: [Icon(Icons.delete_sweep_rounded, color: Colors.red), SizedBox(width: 8), Text('Vider la conversation', style: TextStyle(color: Colors.red))])),
          ],
        ),
      ],
    );
  }

  Widget _buildSearchBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.cardDark,
        border: Border(bottom: BorderSide(color: AppColors.surface.withOpacity(0.3))),
      ),
      child: TextField(
        controller: _searchController,
        decoration: InputDecoration(
          hintText: 'Rechercher dans la conversation...',
          hintStyle: TextStyle(color: AppColors.surface.withOpacity(0.5)),
          prefixIcon: Icon(Icons.search_rounded, color: AppColors.surface.withOpacity(0.5)),
          suffixIcon: IconButton(
            icon: Icon(Icons.close_rounded, color: AppColors.surface.withOpacity(0.5)),
            onPressed: () {
              _searchController.clear();
              setState(() => _showSearch = false);
            },
          ),
          filled: true,
          fillColor: AppColors.surfaceDark,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(24),
            borderSide: BorderSide.none,
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        ),
        style: const TextStyle(color: Colors.white),
        onChanged: (value) => _searchMessages(value),
        autofocus: true,
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.chat_bubble_outline_rounded, size: 64, color: AppColors.surface.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(
            'Aucun message',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
              color: AppColors.surface.withOpacity(0.7),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Commencez la conversation',
            style: TextStyle(color: AppColors.surface.withOpacity(0.5)),
          ),
        ],
      ),
    );
  }

  Widget _buildDateHeader(DateTime dateTime) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(const Duration(days: 1));
    final messageDate = DateTime(dateTime.year, dateTime.month, dateTime.day);

    String label;
    if (messageDate == today) {
      label = 'Aujourd\'hui';
    } else if (messageDate == yesterday) {
      label = 'Hier';
    } else {
      label = DateFormat('dd MMMM yyyy', 'fr_FR').format(dateTime);
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 16),
      child: Center(
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          decoration: BoxDecoration(
            color: AppColors.surfaceDark,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Text(
            label,
            style: TextStyle(fontSize: 11, color: AppColors.surface.withOpacity(0.5), fontWeight: FontWeight.w500),
          ),
        ),
      ),
    );
  }

  bool _shouldShowDate(List<Message> messages, int index) {
    if (index == 0) return true;
    final current = messages[index].createdAt;
    final previous = messages[index - 1].createdAt;
    return current.day != previous.day || current.month != previous.month || current.year != previous.year;
  }

  void _sendMessage(String content, {MessageType type = MessageType.text, List<String>? mediaUrls, int? replyToMessageId}) async {
    try {
      await ref.read(messagesServiceProvider).sendMessage(widget.conversationId, content, type: type, mediaUrls: mediaUrls, replyToMessageId: replyToMessageId);
      ref.invalidate(_messagesProvider(widget.conversationId));
      _scrollToBottom();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  void _sendMedia() async {
    final picker = ImagePicker();
    final media = await showModalBottomSheet<ImageSource>(
      context: context,
      backgroundColor: AppColors.cardDark,
      builder: (context) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: Icon(Icons.camera_alt_rounded, color: AppColors.primary),
            title: const Text('Prendre une photo'),
            onTap: () => Navigator.pop(context, ImageSource.camera),
          ),
          ListTile(
            leading: Icon(Icons.photo_library_rounded, color: AppColors.success),
            title: const Text('Choisir dans la galerie'),
            onTap: () => Navigator.pop(context, ImageSource.gallery),
          ),
        ],
      ),
    );

    if (media != null) {
      final file = await picker.pickImage(source: media, maxWidth: 1920, maxHeight: 1080, imageQuality: 85);
      if (file != null) {
        // TODO: Upload and send media message
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Envoi de média bientôt disponible')));
      }
    }
  }

  void _sendVoice() {
    // TODO: Voice message recording
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Messages vocaux bientôt disponibles')));
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          0,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _showMessageOptions(Message message) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (!message.isSystem) ...[
            ListTile(
              leading: Icon(Icons.reply_rounded, color: AppColors.primary),
              title: const Text('Répondre'),
              onTap: () {
                Navigator.pop(context);
                _replyToMessage(message);
              },
            ),
            ListTile(
              leading: Icon(Icons.forward_rounded, color: AppColors.success),
              title: const Text('Transférer'),
              onTap: () {
                Navigator.pop(context);
                _forwardMessage(message);
              },
            ),
            if (message.isOwn) ...[
              ListTile(
                leading: Icon(Icons.edit_rounded, color: AppColors.accent),
                title: const Text('Modifier'),
                onTap: () {
                  Navigator.pop(context);
                  _editMessage(message);
                },
              ),
              ListTile(
                leading: Icon(Icons.delete_rounded, color: Colors.red),
                title: const Text('Supprimer', style: TextStyle(color: Colors.red)),
                onTap: () {
                  Navigator.pop(context);
                  _deleteMessage(message);
                },
              ),
            ],
            ListTile(
              leading: Icon(Icons.copy_rounded, color: AppColors.surface.withOpacity(0.7)),
              title: const Text('Copier'),
              onTap: () {
                Navigator.pop(context);
                // TODO: Copy to clipboard
              },
            ),
          ],
        ],
      ),
    );
  }

  void _replyToMessage(Message message) {
    // TODO: Show reply preview in input
  }

  void _forwardMessage(Message message) {
    // TODO: Show forward dialog
  }

  void _editMessage(Message message) {
    // TODO: Show edit dialog
  }

  void _deleteMessage(Message message) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Supprimer le message'),
        content: const Text('Supprimer pour tout le monde ou juste pour vous ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Pour moi')),
          FilledButton(onPressed: () => Navigator.pop(context, true), style: FilledButton.styleFrom(backgroundColor: Colors.red), child: const Text('Pour tous')),
        ],
      ),
    );
    if (confirm != null && mounted) {
      await ref.read(messagesServiceProvider).deleteMessage(message.id, forEveryone: confirm);
      ref.invalidate(_messagesProvider(widget.conversationId));
    }
  }

  void _toggleReaction(Message message, String emoji) async {
    try {
      final hasReaction = message.reactions[emoji]?.contains(_getCurrentUserId()) ?? false;
      if (hasReaction) {
        await ref.read(messagesServiceProvider).removeReaction(message.id, emoji);
      } else {
        await ref.read(messagesServiceProvider).addReaction(message.id, emoji);
      }
      ref.invalidate(_messagesProvider(widget.conversationId));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  void _handleAction(String action) {
    switch (action) {
      case 'participants':
        context.push('/conversation/${widget.conversationId}/participants');
        break;
      case 'media':
        // TODO: Show shared media gallery
        break;
      case 'links':
        // TODO: Show shared links
        break;
      case 'files':
        // TODO: Show shared files
        break;
      case 'mute':
        // TODO: Toggle mute
        break;
      case 'clear':
        _clearConversation();
        break;
    }
  }

  void _clearConversation() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Vider la conversation'),
        content: const Text('Tous les messages seront supprimés pour vous. Cette action est irréversible.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(context, true), style: FilledButton.styleFrom(backgroundColor: Colors.red), child: const Text('Vider')),
        ],
      ),
    );
    if (confirm == true && mounted) {
      // TODO: Implement clear conversation
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Fonctionnalité bientôt disponible')));
    }
  }

  void _searchMessages(String query) {
    if (query.isEmpty) return;
    // TODO: Implement search
  }

  int _getCurrentUserId() {
    // TODO: Get from auth state
    return 1;
  }
}

// Providers
final _conversationProvider = FutureProvider.family<Conversation, int>((ref, id) async {
  final service = ref.watch(messagesServiceProvider);
  return service.getConversation(id);
});

final _messagesProvider = FutureProvider.family<List<Message>, int>((ref, id) async {
  final service = ref.watch(messagesServiceProvider);
  return service.getMessages(id);
});

final _typingProvider = StreamProvider.family<TypingIndicator?, int>((ref, conversationId) {
  final service = ref.watch(messagesServiceProvider);
  return service.onTyping.where((t) => t.conversationId == conversationId);
});