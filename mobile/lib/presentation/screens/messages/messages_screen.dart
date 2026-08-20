import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import 'conversation_detail_screen.dart';

class MessagesScreen extends StatefulWidget {
  const MessagesScreen({super.key});

  @override
  State<MessagesScreen> createState() => _MessagesScreenState();
}

class _MessagesScreenState extends State<MessagesScreen> {
  final _apiService = ApiService();
  List<dynamic> _conversations = [];
  int _unreadCount = 0;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final convRes = await _apiService.get('/messages/conversations');
      final unreadRes = await _apiService.get('/messages/conversations/unread-total');
      if (mounted) {
        setState(() {
          _conversations = convRes.data as List<dynamic>? ?? [];
          _unreadCount = (unreadRes.data is Map) ? (unreadRes.data as Map)['total'] as int? ?? 0 : 0;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openConversation(Map<String, dynamic> conv) async {
    final title = (conv['otherUserName'] ?? conv['nom'] ?? 'Conversation') as String;
    final id = (conv['id'] as String?) ?? '';
    if (id.isEmpty) return;
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ConversationDetailScreen(conversationId: id, title: title, apiService: _apiService),
      ),
    );
    _loadData(); // reload unread count after reading
  }

  void _startNewConversation() {
    _showStartConversationSheet();
  }

  void _showStartConversationSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF1A202C),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => _NewConversationSheet(
        apiService: _apiService,
        onStarted: (id, title) {
          Navigator.of(ctx).pop();
          _loadData();
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (_) => ConversationDetailScreen(conversationId: id, title: title, apiService: _apiService),
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            const Text('Messagerie'),
            if (_unreadCount > 0) ...[
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(color: Colors.red, borderRadius: BorderRadius.circular(12)),
                child: Text('$_unreadCount', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
            ],
          ],
        ),
        actions: [
          IconButton(icon: const Icon(Icons.add_comment_outlined), onPressed: _startNewConversation, tooltip: 'Nouvelle conversation'),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: _conversations.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.chat_bubble_outline, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucune conversation', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      itemCount: _conversations.length,
                      itemBuilder: (context, index) {
                        final conv = _conversations[index] as Map<String, dynamic>;
                        final title = conv['nom'] ?? conv['title'] ?? 'Conversation';
                        final lastMsg = conv['dernierMessage'] ?? conv['lastMessage'] ?? '';
                        final unread = conv['nonLus'] ?? conv['unread'] ?? 0;
                        final isGroup = conv['type'] == 'GROUPE' || conv['isGroup'] == true;
                        return Container(
                          margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 3),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: unread > 0 ? 0.06 : 0.03),
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                          ),
                          child: ListTile(
                            contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                            leading: CircleAvatar(
                              radius: 22,
                              backgroundColor: isGroup ? Colors.blue.withValues(alpha: 0.2) : Colors.teal.withValues(alpha: 0.2),
                              child: Text(
                                _initials(title),
                                style: TextStyle(color: isGroup ? Colors.blue : Colors.teal, fontWeight: FontWeight.bold, fontSize: 14),
                              ),
                            ),
                            title: Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    title,
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontWeight: unread > 0 ? FontWeight.w600 : FontWeight.normal,
                                      fontSize: 14,
                                    ),
                                  ),
                                ),
                                if (conv['dateDernierMessage'] != null)
                                  Text(
                                    conv['dateDernierMessage'].toString().substring(0, 16).replaceAll('T', ' ').substring(11, 16),
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10),
                                  ),
                              ],
                            ),
                            subtitle: Text(
                              lastMsg,
                              style: TextStyle(
                                color: Colors.white.withValues(alpha: unread > 0 ? 0.7 : 0.4),
                                fontSize: 12,
                                fontWeight: unread > 0 ? FontWeight.w500 : FontWeight.normal,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            trailing: unread > 0
                                ? Container(
                                    width: 20, height: 20,
                                    decoration: const BoxDecoration(color: Colors.blue, shape: BoxShape.circle),
                                    child: Center(child: Text('$unread', style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold))),
                                  )
                                : null,
                            onTap: () => _openConversation(conv),
                          ),
                        );
                      },
                    ),
            ),
    );
  }

  static String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty) return '?';
    if (parts.length == 1) return parts[0][0].toUpperCase();
    return '${parts[0][0]}${parts.last[0]}'.toUpperCase();
  }
}

/// Bottom sheet to start a new conversation by selecting a user.
class _NewConversationSheet extends StatefulWidget {
  final ApiService apiService;
  final void Function(String id, String title) onStarted;

  const _NewConversationSheet({required this.apiService, required this.onStarted});

  @override
  State<_NewConversationSheet> createState() => _NewConversationSheetState();
}

class _NewConversationSheetState extends State<_NewConversationSheet> {
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
      final res = await widget.apiService.get('/users', params: {'size': 200});
      final data = res.data;
      if (data is Map && data['content'] is List) {
        _users = data['content'] as List;
      } else if (data is List) {
        _users = data;
      }
    } catch (_) {}
    if (mounted) setState(() => _isLoading = false);
  }

  Future<void> _startConversation(Map<String, dynamic> user) async {
    try {
      final userId = user['id']?.toString() ?? '';
      final name = '${user['prenom'] ?? ''} ${user['nom'] ?? ''}'.trim();
      if (userId.isEmpty) return;
      final res = await widget.apiService.post('/messages/conversations', data: {
        'participantIds': [userId],
      });
      final convId = (res.data is Map) ? res.data['id']?.toString() ?? '' : '';
      if (convId.isNotEmpty) {
        widget.onStarted(convId, name);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Erreur: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _users.where((u) {
      if (_search.isEmpty) return true;
      final name = '${u['prenom'] ?? ''} ${u['nom'] ?? ''}'.toLowerCase();
      return name.contains(_search.toLowerCase());
    }).toList();

    return DraggableScrollableSheet(
      initialChildSize: 0.7,
      minChildSize: 0.4,
      maxChildSize: 0.95,
      expand: false,
      builder: (ctx, scrollCtrl) {
        return Column(
          children: [
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.3),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Nouvelle conversation',
                  style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: TextField(
                onChanged: (v) => setState(() => _search = v),
                decoration: InputDecoration(
                  hintText: 'Rechercher un utilisateur...',
                  prefixIcon: const Icon(Icons.search),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.1),
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
                        final name = '${user['prenom'] ?? ''} ${user['nom'] ?? ''}'.trim();
                        final email = user['email']?.toString() ?? '';
                        return ListTile(
                          leading: CircleAvatar(
                            backgroundColor: Colors.teal.withValues(alpha: 0.2),
                            child: Text(
                              _initials(name),
                              style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold),
                            ),
                          ),
                          title: Text(name, style: const TextStyle(color: Colors.white)),
                          subtitle: Text(email, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                          onTap: () => _startConversation(user),
                        );
                      },
                    ),
            ),
          ],
        );
      },
    );
  }

  static String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty) return '?';
    if (parts.length == 1) return parts[0][0].toUpperCase();
    return '${parts[0][0]}${parts.last[0]}'.toUpperCase();
  }
}
