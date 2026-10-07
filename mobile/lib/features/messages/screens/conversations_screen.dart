import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/features/messages/models/message_model.dart';
import 'package:discipolat_mobile/features/messages/services/messages_service.dart';
import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Liste des conversations — 1:1 (GET /messages/conversations) et groupes
/// (GET /messages/groups), fusionnées côté client comme le web
/// (MessagesPage.tsx). Le serveur n'expose ni épinglage, ni mute, ni
/// archivage, ni suppression de conversation : aucune de ces actions n'est
/// proposée ici.

/// Vue unifiée d'une ligne de conversation (1:1 ou groupe).
class _Thread {
  const _Thread({
    required this.id,
    required this.title,
    required this.kind,
    this.lastMessage,
    this.lastMessageAt,
    this.unreadCount = 0,
    this.avatarUrl,
    this.subtitle,
  });

  final String id;
  final String title;
  final ConversationKind kind;
  final String? lastMessage;
  final DateTime? lastMessageAt;
  final int unreadCount;
  final String? avatarUrl;
  final String? subtitle;

  factory _Thread.fromDirect(Conversation c) => _Thread(
        id: c.id,
        title: c.otherUserName,
        kind: ConversationKind.direct,
        lastMessage: c.lastMessage,
        lastMessageAt: c.lastMessageAt,
        unreadCount: c.unreadCount,
      );

  factory _Thread.fromGroup(GroupConversation g) => _Thread(
        id: g.id,
        title: g.name,
        kind: ConversationKind.group,
        lastMessage: g.lastMessage,
        lastMessageAt: g.lastMessageAt,
        unreadCount: g.unreadCount,
        avatarUrl: g.avatarUrl,
        subtitle: '${g.memberCount} membres',
      );
}

class ConversationsScreen extends ConsumerStatefulWidget {
  const ConversationsScreen({super.key});

  @override
  ConsumerState<ConversationsScreen> createState() =>
      _ConversationsScreenState();
}

class _ConversationsScreenState extends ConsumerState<ConversationsScreen> {
  ConversationKind? _filterKind;

  @override
  Widget build(BuildContext context) {
    final threadsAsync = ref.watch(_threadsProvider);

    return Scaffold(
      backgroundColor: AppColors.surfaceDark,
      appBar: AppBar(
        title: const Text('Messages'),
        backgroundColor: AppColors.cardDark,
        elevation: 0,
        actions: [
          PopupMenuButton<ConversationKind?>(
            icon: const Icon(Icons.filter_list_rounded),
            onSelected: (value) => setState(() => _filterKind = value),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('Tous')),
              const PopupMenuItem(
                  value: ConversationKind.direct,
                  child: Text('Discussions privées')),
              const PopupMenuItem(
                  value: ConversationKind.group, child: Text('Groupes')),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: _showNewConversationSheet,
            tooltip: 'Nouvelle conversation',
          ),
        ],
      ),
      body: threadsAsync.when(
        data: (threads) {
          final visible = _filterKind == null
              ? threads
              : threads.where((t) => t.kind == _filterKind).toList();
          if (visible.isEmpty) {
            return _buildEmptyState();
          }
          return RefreshIndicator(
            onRefresh: () async =>
                ref.invalidate(_threadsProvider),
            child: ListView.builder(
              padding: const EdgeInsets.all(8),
              itemCount: visible.length,
              itemBuilder: (context, index) =>
                  _buildThreadTile(visible[index]),
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline_rounded,
                  size: 64, color: Colors.red),
              const SizedBox(height: 16),
              Flexible(child: Text('Erreur: $error')),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () => ref.invalidate(_threadsProvider),
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
          Icon(Icons.chat_bubble_outline_rounded,
              size: 64, color: AppColors.surface.withOpacity(0.5)),
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
            onPressed: _showNewConversationSheet,
            icon: const Icon(Icons.add_rounded),
            label: const Text('Nouvelle conversation'),
          ),
        ],
      ),
    );
  }

  Widget _buildThreadTile(_Thread thread) {
    final hasUnread = thread.unreadCount > 0;
    final isGroup = thread.kind == ConversationKind.group;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
      color: AppColors.cardDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ListTile(
        leading: CircleAvatar(
          radius: 24,
          backgroundColor: AppColors.primary.withOpacity(0.2),
          backgroundImage:
              thread.avatarUrl != null ? NetworkImage(thread.avatarUrl!) : null,
          child: thread.avatarUrl == null
              ? Icon(
                  isGroup ? Icons.group_rounded : Icons.person_rounded,
                  color: AppColors.primary,
                )
              : null,
        ),
        title: Text(
          thread.title,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: hasUnread ? FontWeight.w600 : FontWeight.normal,
              ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Text(
          thread.lastMessage ?? thread.subtitle ?? 'Aucun message',
          style: TextStyle(
            fontSize: 12,
            color: AppColors.surface.withOpacity(
                hasUnread ? 0.8 : 0.5),
            fontWeight: hasUnread ? FontWeight.w500 : FontWeight.normal,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            if (thread.lastMessageAt != null)
              Text(
                _formatTime(thread.lastMessageAt!),
                style: TextStyle(
                    fontSize: 10,
                    color: AppColors.surface.withOpacity(0.5)),
              ),
            if (hasUnread)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6),
                constraints:
                    const BoxConstraints(minWidth: 18, minHeight: 18),
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                ),
                child: Text(
                  thread.unreadCount > 9 ? '9+' : '${thread.unreadCount}',
                  style: const TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
              ),
          ],
        ),
        // Routes vivantes : 1:1 → ConversationDetailScreen (presentation),
        // groupe → EnhancedConversationScreen avec isGroup=1 (app.dart).
        onTap: () => isGroup
            ? context.push('/conversation/enhanced/${thread.id}'
                '?title=${Uri.encodeComponent(thread.title)}&isGroup=1')
            : context.push('/conversation/${thread.id}'
                '?title=${Uri.encodeComponent(thread.title)}'),
      ),
    );
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

  void _showNewConversationSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
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
                child:
                    const Icon(Icons.person_add_rounded, color: Colors.white),
              ),
              title: const Text('Discussion privée'),
              subtitle: const Text('Discuter avec une personne'),
              onTap: () {
                Navigator.pop(context);
                _showUserPicker();
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
                _showCreateGroupDialog();
              },
            ),
          ],
        ),
      ),
    );
  }

  /// Choix d'un utilisateur puis POST /messages/conversations {otherUserId}.
  void _showUserPicker() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.cardDark,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) => _UserPickerSheet(
        onPicked: (userId, name) async {
          Navigator.pop(sheetContext);
          try {
            final conv = await ref
                .read(messagesServiceProvider)
                .startConversation(userId);
            // Après l'await : uniquement le contexte de l'État, gardé par
            // sa propre vérification mounted (la feuille est déjà fermée).
            if (!mounted) return;
            ref.invalidate(_threadsProvider);
            context.push('/conversation/${conv.id}'
                '?title=${Uri.encodeComponent(name)}');
          } catch (e) {
            if (!mounted) return;
            ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Erreur: $e')));
          }
        },
      ),
    );
  }

  /// Création d'un groupe — corps exact CreateGroupRequest ; le serveur
  /// ajoute automatiquement le créateur comme ADMIN.
  void _showCreateGroupDialog() {
    final nameCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: AppColors.cardDark,
        title: const Text('Nouveau groupe'),
        content: TextField(
          controller: nameCtrl,
          autofocus: true,
          maxLength: 100,
          decoration: const InputDecoration(labelText: 'Nom du groupe'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Annuler'),
          ),
          FilledButton(
            onPressed: () async {
              final name = nameCtrl.text.trim();
              if (name.isEmpty) return;
              Navigator.pop(dialogContext);
              try {
                await ref.read(messagesServiceProvider).createGroup(name: name);
                ref.invalidate(_threadsProvider);
              } catch (e) {
                if (!mounted) return;
                ScaffoldMessenger.of(context)
                    .showSnackBar(SnackBar(content: Text('Erreur: $e')));
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}

/// Sélecteur d'utilisateur — mêmes champs que la feuille existante du
/// chemin vivant (GET /users : id UUID, prenom, nom).
class _UserPickerSheet extends StatefulWidget {
  final void Function(String userId, String name) onPicked;

  const _UserPickerSheet({required this.onPicked});

  @override
  State<_UserPickerSheet> createState() => _UserPickerSheetState();
}

class _UserPickerSheetState extends State<_UserPickerSheet> {
  final _api = ApiService();
  List<dynamic> _users = [];
  bool _isLoading = true;
  String _search = '';

  @override
  void initState() {
    super.initState();
    _loadUsers();
  }

  Future<void> _loadUsers() async {
    try {
      final res = await _api.get('/users', params: {'size': 200});
      final data = res.data;
      if (data is Map && data['content'] is List) {
        _users = data['content'] as List;
      } else if (data is List) {
        _users = data;
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Impossible de charger les utilisateurs')));
      }
    }
    if (mounted) setState(() => _isLoading = false);
  }

  @override
  Widget build(BuildContext context) {
    final me = AuthState().userId;
    final filtered = _users.where((u) {
      final id = u['id']?.toString() ?? '';
      if (id.isEmpty || id == me) return false;
      if (_search.isEmpty) return true;
      final name = '${u['prenom'] ?? ''} ${u['nom'] ?? ''}'
          .toLowerCase();
      return name.contains(_search.toLowerCase());
    }).toList();

    return DraggableScrollableSheet(
      initialChildSize: 0.7,
      minChildSize: 0.4,
      maxChildSize: 0.95,
      expand: false,
      builder: (ctx, scrollCtrl) => Column(
        children: [
          const Padding(
            padding: EdgeInsets.all(16),
            child: Text('Nouvelle discussion privée',
                style:
                    TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: TextField(
              onChanged: (v) => setState(() => _search = v),
              decoration: const InputDecoration(
                hintText: 'Rechercher un utilisateur...',
                prefixIcon: Icon(Icons.search),
              ),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : ListView.builder(
                    controller: scrollCtrl,
                    itemCount: filtered.length,
                    itemBuilder: (ctx, index) {
                      final user = filtered[index] as Map<String, dynamic>;
                      final name =
                          '${user['prenom'] ?? ''} ${user['nom'] ?? ''}'.trim();
                      return ListTile(
                        leading: CircleAvatar(
                          backgroundColor:
                              AppColors.primary.withOpacity(0.2),
                          child: Text(
                            name.isNotEmpty ? name[0].toUpperCase() : '?',
                            style: TextStyle(color: AppColors.primary),
                          ),
                        ),
                        title: Text(name,
                            style: const TextStyle(color: Colors.white)),
                        onTap: () => widget
                            .onPicked(user['id'].toString(), name),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}

/// Conversations 1:1 + groupes, triés par activité récente (nulls first,
/// comme le serveur pour son propre tri).
final _threadsProvider = FutureProvider<List<_Thread>>((ref) async {
  final service = ref.watch(messagesServiceProvider);
  // Deux requêtes indépendantes lancées en parallèle puis attendues —
  // Future.wait sur des types différents perd l'inférence.
  final directsFuture = service.getConversations();
  final groupsFuture = service.getGroups();
  final directs = await directsFuture;
  final groups = await groupsFuture;
  final threads = <_Thread>[
    ...directs.map(_Thread.fromDirect),
    ...groups.map(_Thread.fromGroup),
  ]..sort((a, b) {
      final at = a.lastMessageAt;
      final bt = b.lastMessageAt;
      if (at == null && bt == null) return 0;
      if (at == null) return 1;
      if (bt == null) return -1;
      return bt.compareTo(at);
    });
  return threads;
});
