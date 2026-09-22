import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/messages/models/message_model.dart';
import 'package:discipolat_mobile/features/messages/services/messages_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

class ConversationsScreen extends ConsumerStatefulWidget {
  const ConversationsScreen({super.key});

  @override
  ConsumerState<ConversationsScreen> createState() => _ConversationsScreenState();
}

class _ConversationsScreenState extends ConsumerState<ConversationsScreen> {
  ConversationType? _filterType;

  @override
  Widget build(BuildContext context) {
    final conversationsAsync = ref.watch(_conversationsProvider(_filterType));

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Messages'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        actions: [
          PopupMenuButton<ConversationType?>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() => _filterType = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous')),
              const PopupMenuItem(value: ConversationType.direct, child: Text('Discussions privées')),
              const PopupMenuItem(value: ConversationType.group, child: Text('Groupes')),
              const PopupMenuItem(value: ConversationType.channel, child: Text('Canaux')),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => _showNewConversationDialog(),
            tooltip: 'Nouvelle conversation',
          ),
        ],
      ),
      body: conversationsAsync.when(
        data: (conversations) {
          if (conversations.isEmpty) {
            return _buildEmptyState();
          }
          return RefreshIndicator(
            onRefresh: () => ref.refresh(_conversationsProvider(_filterType).future),
            child: ListView.builder(
              padding: const EdgeInsets.all(8),
              itemCount: conversations.length,
              itemBuilder: (context, index) {
                final conv = conversations[index];
                return _buildConversationTile(conv);
              },
            ),
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
                onPressed: () => ref.refresh(_conversationsProvider(_filterType)),
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Réessayer'),
              ),
            ],
          ),
        ),
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
            'Aucune conversation',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
              color: AppColors.surface.withOpacity(0.7),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Commencez une nouvelle discussion',
            style: TextStyle(color: AppColors.surface.withOpacity(0.5)),
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: _showNewConversationDialog,
            icon: const Icon(Icons.add_rounded),
            label: const Text('Nouvelle conversation'),
          ),
        ],
      ),
    );
  }

  Widget _buildConversationTile(Conversation conv) {
    final hasUnread = conv.unreadCount > 0;
    final isGroup = conv.type != ConversationType.direct;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ListTile(
        leading: Stack(
          children: [
            CircleAvatar(
              radius: 24,
              backgroundColor: AppColors.primary.withOpacity(0.2),
              backgroundImage: conv.avatarUrl != null ? NetworkImage(conv.avatarUrl!) : null,
              child: conv.avatarUrl == null
                  ? Icon(
                      isGroup ? Icons.group_rounded : Icons.person_rounded,
                      color: AppColors.primary,
                    )
                  : null,
            ),
            if (hasUnread)
              Positioned(
                right: 0,
                bottom: 0,
                child: Container(
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    shape: BoxShape.circle,
                  ),
                  constraints: const BoxConstraints(minWidth: 18, minHeight: 18),
                  child: Text(
                    conv.unreadCount > 9 ? '9+' : conv.unreadCount.toString(),
                    style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            if (conv.isPinned)
              Positioned(
                left: 0,
                top: 0,
                child: Icon(Icons.push_pin_rounded, size: 14, color: AppColors.primary),
              ),
          ],
        ),
        title: Text(
          conv.title,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
            fontWeight: conv.unreadCount > 0 ? FontWeight.w600 : FontWeight.normal,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: conv.lastMessage != null
            ? Row(
                children: [
                  if (conv.lastMessage!.type != MessageType.text) ...[
                    Icon(_getMessageTypeIcon(conv.lastMessage!.type), size: 12, color: AppColors.surface.withOpacity(0.7)),
                    const SizedBox(width: 4),
                  ],
                  if (conv.lastMessage!.isSystem)
                    Text(
                      conv.lastMessage!.content,
                      style: TextStyle(fontSize: 12, color: AppColors.surface.withOpacity(0.5), fontStyle: FontStyle.italic),
                    )
                  else
                    Flexible(
                      child: Text(
                        conv.lastMessage!.senderId == _getCurrentUserId() ? 'Vous: ${conv.lastMessage!.content}' : '${conv.lastMessage!.senderName}: ${conv.lastMessage!.content}',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.surface.withOpacity(0.7),
                          fontWeight: conv.unreadCount > 0 ? FontWeight.w500 : FontWeight.normal,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                ],
              )
            : null,
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            if (conv.updatedAt != null)
              Text(
                _formatTime(conv.updatedAt!),
                style: TextStyle(fontSize: 10, color: AppColors.surface.withOpacity(0.5)),
              ),
            if (conv.isMuted)
              Icon(Icons.notifications_off_rounded, size: 16, color: AppColors.surface.withOpacity(0.5)),
          ],
        ),
        onTap: () => context.push('/conversation/${conv.id}'),
        onLongPress: () => _showConversationOptions(conv),
      ),
    );
  }

  IconData _getMessageTypeIcon(MessageType type) {
    switch (type) {
      case MessageType.image:
        return Icons.image_rounded;
      case MessageType.video:
        return Icons.videocam_rounded;
      case MessageType.audio:
        return Icons.mic_rounded;
      case MessageType.file:
        return Icons.attach_file_rounded;
      case MessageType.location:
        return Icons.location_on_rounded;
      case MessageType.contact:
        return Icons.person_rounded;
      case MessageType.reply:
        return Icons.reply_rounded;
      case MessageType.forward:
        return Icons.forward_rounded;
      default:
        return Icons.chat_rounded;
    }
  }

  String _formatTime(DateTime dateTime) {
    final now = DateTime.now();
    final difference = now.difference(dateTime);

    if (difference.inMinutes < 1) return 'À l\'instant';
    if (difference.inMinutes < 60) return '${difference.inMinutes}min';
    if (difference.inHours < 24) return '${difference.inHours}h';
    if (difference.inDays < 7) return '${difference.inDays}j';
    return DateFormat('dd/MM/yyyy').format(dateTime);
  }

  int _getCurrentUserId() {
    // TODO: Get from auth state
    return 1;
  }

  void _showNewConversationDialog() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Nouvelle conversation',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 24),
            ListTile(
              leading: CircleAvatar(
                backgroundColor: AppColors.primary,
                child: const Icon(Icons.person_add_rounded, color: Colors.white),
              ),
              title: const Text('Discussion privée'),
              subtitle: const Text('Discuter avec une personne'),
              onTap: () {
                Navigator.pop(context);
                context.push('/messages/new-direct');
              },
            ),
            ListTile(
              leading: CircleAvatar(
                backgroundColor: AppColors.success,
                child: const Icon(Icons.group_add_rounded, color: Colors.white),
              ),
              title: const Text('Groupe'),
              subtitle: const Text('Créer un groupe de discussion'),
              onTap: () {
                Navigator.pop(context);
                context.push('/messages/new-group');
              },
            ),
            ListTile(
              leading: CircleAvatar(
                backgroundColor: AppColors.accent,
                child: const Icon(Icons.campaign_rounded, color: Colors.white),
              ),
              title: const Text('Canal'),
              subtitle: const Text('Créer un canal de diffusion'),
              onTap: () {
                Navigator.pop(context);
                context.push('/messages/new-channel');
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showConversationOptions(Conversation conv) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: Icon(conv.isPinned ? Icons.push_pin_rounded : Icons.push_pin_outlined_rounded, color: AppColors.primary),
            title: Text(conv.isPinned ? 'Désépingler' : 'Épingler'),
            onTap: () {
              ref.read(messagesServiceProvider).updateConversation(conv.id, isPinned: !conv.isPinned);
              Navigator.pop(context);
            },
          ),
          ListTile(
            leading: Icon(conv.isMuted ? Icons.notifications_off_rounded : Icons.notifications_none_rounded, color: Colors.orange),
            title: Text(conv.isMuted ? 'Activer notifications' : 'Couper notifications'),
            onTap: () {
              ref.read(messagesServiceProvider).updateConversation(conv.id, isMuted: !conv.isMuted);
              Navigator.pop(context);
            },
          ),
          ListTile(
            leading: Icon(conv.isArchived ? Icons.unarchive_rounded : Icons.archive_rounded, color: AppColors.surface.withOpacity(0.7)),
            title: Text(conv.isArchived ? 'Désarchiver' : 'Archiver'),
            onTap: () {
              ref.read(messagesServiceProvider).updateConversation(conv.id, isArchived: !conv.isArchived);
              Navigator.pop(context);
            },
          ),
          if (conv.type == ConversationType.group)
            ListTile(
              leading: const Icon(Icons.group_add_rounded, color: AppColors.success),
              title: const Text('Gérer les membres'),
              onTap: () {
                Navigator.pop(context);
                context.push('/conversation/${conv.id}/participants');
              },
            ),
          ListTile(
            leading: Icon(Icons.delete_rounded, color: Colors.red),
            title: const Text('Supprimer', style: TextStyle(color: Colors.red)),
            onTap: () async {
              final confirm = await showDialog<bool>(
                context: context,
                builder: (context) => AlertDialog(
                  backgroundColor: AppColors.cardDark,
                  title: const Text('Supprimer la conversation'),
                  content: const Text('Cette action est irréversible. Confirmer la suppression ?'),
                  actions: [
                    TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
                    FilledButton(onPressed: () => Navigator.pop(context, true), style: FilledButton.styleFrom(backgroundColor: Colors.red), child: const Text('Supprimer')),
                  ],
                ),
              );
              if (confirm == true) {
                await ref.read(messagesServiceProvider).deleteConversation(conv.id);
                ref.invalidate(_conversationsProvider(_filterType));
              }
              Navigator.pop(context);
            },
          ),
        ],
      ),
    );
  }
}

// Providers
final _conversationsProvider = FutureProvider.family<List<Conversation>, ConversationType?>((ref, type) async {
  final service = ref.watch(messagesServiceProvider);
  return service.getConversations(type: type?.name);
});