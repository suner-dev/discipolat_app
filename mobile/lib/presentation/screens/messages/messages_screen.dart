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
    _loadData(); // recalcule le compteur de non-lus après la lecture
  }

  void _startNewConversation() {
    showStartConversationSheet(
      context,
      _apiService,
      onStarted: (id, title) {
        _loadData();
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => ConversationDetailScreen(conversationId: id, title: title, apiService: _apiService),
          ),
        );
      },
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
                                isGroup ? '${_conversations.length}' : initialsFromName(title),
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
}
