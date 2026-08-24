import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/services/group_message_service.dart';
import '../../../data/services/providers.dart';
import '../../../models/group_message.dart';
import '../../../models/group_thread.dart';

/// P10 — Messagerie de groupe (mobile) — liee aux APIs reelles.
///
/// APIs backend Spring Boot :
///   * liste des groupes : GET /api/v1/departments
///   * historique : GET /api/v1/group-messages/group/{id}
///   * envoi : POST /api/v1/group-messages
///   * reaccion : POST /api/v1/group-messages/{id}/reaction
///   * P10 — recherche : GET /api/v1/group-messages/search?groupId=&q=
///
/// Repli offline sans faute : les erreurs reseau affichent un etat d'erreur
/// et n'interrompent jamais le lancement de l'application.
class GroupMessagesScreen extends ConsumerStatefulWidget {
  const GroupMessagesScreen({super.key});

  @override
  ConsumerState<GroupMessagesScreen> createState() => _GroupMessagesScreenState();
}

class _GroupMessagesScreenState extends ConsumerState<GroupMessagesScreen> {
  GroupThread? _selected;
  late Future<List<GroupMessage>> _messagesFuture;
  final _searchCtrl = TextEditingController();
  String _searchQ = '';
  late Future<List<GroupMessage>> _searchFuture;

  @override
  void initState() {
    super.initState();
    _searchFuture = Future.value(const []);
    _messagesFuture = Future.value(const []);
  }

  GroupMessageService get _svc => ref.read(groupMessageServiceProvider);

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  void _select(GroupThread t) {
    setState(() {
      _selected = t;
      _searchCtrl.clear();
      _searchQ = '';
      _messagesFuture = _svc.fetchMessages(t.id);
      _searchFuture = Future.value(const []);
    });
  }

  Future<void> _reload() {
    setState(() {
      _messagesFuture = _selected != null
          ? _svc.fetchMessages(_selected!.id)
          : Future.value(const []);
    });
    return _messagesFuture;
  }

  Future<void> _send(String content) async {
    if (_selected == null || content.trim().isEmpty) return;
    try {
      await _svc.sendMessage(
        groupId: _selected!.id,
        senderId: 'current',
        groupType: _selected!.groupType,
        content: content.trim(),
      );
      _reload();
      _searchCtrl.clear();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Envoi impossible : ${e.toString().split(':').first}'),
          ),
        );
      }
    }
  }

  void _doSearch(String q) {
    setState(() {
      _searchQ = q;
      _searchFuture = q.isNotEmpty
          ? _svc.search(_selected!.id, q)
          : Future.value(const []);
    });
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Messagerie Groupe'),
        backgroundColor: Colors.blue.shade600,
        foregroundColor: Colors.white,
        bottom: _selected == null
            ? null
            : PreferredSize(
                preferredSize: const Size.fromHeight(56),
                child: _searchBar(isDark),
              ),
      ),
      body: _selected == null ? _groupList() : _conversation(),
      floatingActionButton: _selected == null
          ? null
          : FloatingActionButton(
              onPressed: _reload,
              backgroundColor: Colors.blue.shade600,
              child: const Icon(Icons.refresh, color: Colors.white),
            ),
    );
  }

  Widget _searchBar(bool isDark) => Container(
        color: isDark ? Colors.grey.shade900 : Colors.grey.shade100,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: TextField(
          controller: _searchCtrl,
          decoration: InputDecoration(
            hintText: 'Rechercher dans le groupe...',
            prefixIcon: const Icon(Icons.search, size: 18),
          ),
          onChanged: _doSearch,
        ),
      );

  Widget _groupList() {
    return FutureBuilder<List<GroupThread>>(
      future: ref.watch(groupMessageServiceProvider).fetchGroups(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        final groups = snapshot.data;
        if (groups == null || groups.isEmpty) {
          return _emptyState(Icons.group, 'Aucun groupe pour le moment.');
        }
        return RefreshIndicator(
          onRefresh: () => ref.refresh(groupMessageServiceProvider).fetchGroups(),
          child: ListView.separated(
            itemCount: groups.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, i) {
              final g = groups[i];
              return ListTile(
                leading: CircleAvatar(
                  backgroundColor: Colors.primaries[i % Colors.primaries.length].withOpacity(0.15),
                  child: const Icon(Icons.groups_2, color: Colors.blueGrey, size: 18),
                ),
                title: Text(g.nom, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text(g.groupType),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _select(g),
              );
            },
          ),
        );
      },
        );
  }

  Widget _conversation() {
    final msgs = _searchQ.isNotEmpty ? _searchFuture : _messagesFuture;
    return Column(
      children: [
        Container(
          width: double.infinity,
          color: Colors.blue.shade50,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          child: Text('${_selected!.nom} • ${_selected!.groupType}',
              style: TextStyle(color: Colors.blue.shade700, fontSize: 12)),
        ),
        Expanded(
          child: FutureBuilder<List<GroupMessage>>(
            future: msgs,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              final list = snapshot.data;
              if (list == null || list.isEmpty) {
                final suffix = _searchQ.isNotEmpty ? " résultat pour '$_searchQ'" : '';
                return _emptyState(Icons.forum, "Aucun message$suffix.");
              }
              return ListView.builder(
                padding: const EdgeInsets.all(12),
                reverse: true,
                itemCount: list.length,
                itemBuilder: (context, i) {
                  final m = list[list.length - 1 - i];
                  return _messageBubble(m);
                },
              );
            },
          ),
        ),
        Row(
          children: [
            IconButton(icon: const Icon(Icons.refresh), onPressed: _reload, tooltip: 'Actualiser'),
            Expanded(
              child: TextField(
                controller: _searchCtrl,
                decoration: const InputDecoration(hintText: 'Tapez un message...'),
                onSubmitted: _send,
              ),
            ),
            IconButton(
              icon: const Icon(Icons.send, color: Colors.blue),
              onPressed: () => _send(_searchCtrl.text),
              tooltip: 'Envoyer',
            ),
          ],
        ),
      ],
    );
  }

  Widget _messageBubble(GroupMessage m) => Card(
        margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 4),
        color: m.senderId == 'current' ? Colors.blue.shade50 : Colors.grey.shade50,
        child: ListTile(
                    leading: CircleAvatar(
            backgroundColor: Colors.primaries[
                    m.senderId.isEmpty ? 0 : m.senderId.codeUnitAt(0) % Colors.primaries.length].withOpacity(0.2),
            child: Text(
              m.senderId.isNotEmpty ? m.senderId[0].toUpperCase() : '?',
              style: const TextStyle(fontSize: 12),
            ),
          ),
          title: Text(m.content),
          subtitle: Text(
            '${m.createdAt.hour.toString().padLeft(2, '0')}:${m.createdAt.minute.toString().padLeft(2, '0')} • ${m.messageType}',
            style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
          ),
          trailing: m.reactionCount > 0
              ? Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.orange.shade100,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text('🔥 ${m.reactionCount}', style: const TextStyle(fontSize: 11)),
                )
              : const Icon(Icons.more_horiz),
          onTap: () => _svc.react(m.id),
        ),
      );

  Widget _emptyState(IconData icon, String msg) => Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(icon, size: 48, color: Colors.grey.shade400),
          const SizedBox(height: 12),
          Text(msg, style: TextStyle(color: Colors.grey.shade600)),
        ]),
      );
}

