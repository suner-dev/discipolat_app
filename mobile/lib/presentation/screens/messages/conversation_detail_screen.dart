import 'dart:async';

import 'package:flutter/material.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// Détail d'une conversation : fil de messages, envoi et marquage « lu ».
/// Endpoints réels (MessageController) :
///   GET   /messages/conversations/{id}/messages
///   POST  /messages/conversations/{id}/messages   {content}
///   PATCH /messages/conversations/{id}/read
class ConversationDetailScreen extends StatefulWidget {
  const ConversationDetailScreen({
    super.key,
    required this.conversationId,
    required this.title,
    this.apiService,
  });

  final String conversationId;
  final String title;

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<ConversationDetailScreen> createState() => _ConversationDetailScreenState();
}

class _ConversationDetailScreenState extends State<ConversationDetailScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final _messageCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();

  List<dynamic> _messages = [];
  bool _isLoading = true;
  bool _isSending = false;
  String? _error;

  String? get _myUserId => AuthState().userId;

  @override
  void initState() {
    super.initState();
    _loadMessages();
  }

  @override
  void dispose() {
    _messageCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadMessages() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/messages/conversations/${widget.conversationId}/messages');
      // Marquage lu côté serveur (le compteur de non-lus baisse à la prochaine recharge).
      // Erreur avalée : le marquage lu ne doit jamais faire échouer le chargement.
      unawaited(_markAsRead());
      if (mounted) {
        setState(() {
          _messages = (res.data as List?) ?? [];
          _isLoading = false;
          _error = null;
        });
        _scrollToBottom();
      }
    } catch (_) {
      if (mounted) setState(() { _isLoading = false; _error = 'Impossible de charger la conversation'; });
    }
  }

  Future<void> _markAsRead() async {
    try {
      await _apiService.patch('/messages/conversations/${widget.conversationId}/read');
    } catch (_) {
      // Ignoré : le marquage lu est une optimisation, pas une obligation.
    }
  }

  Future<void> _send() async {
    final content = _messageCtrl.text.trim();
    if (content.isEmpty || _isSending) return;
    setState(() => _isSending = true);
    try {
      await _apiService.post('/messages/conversations/${widget.conversationId}/messages', data: {'content': content});
      _messageCtrl.clear();
      await _loadMessages();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Erreur lors de l\'envoi')));
      }
    } finally {
      if (mounted) setState(() => _isSending = false);
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollCtrl.hasClients) {
        _scrollCtrl.animateTo(
          _scrollCtrl.position.maxScrollExtent,
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeOut,
        );
      }
    });
  }

  String _time(String? iso) {
    if (iso == null || iso.length < 16) return '';
    final datePart = iso.substring(0, 10);
    final timePart = iso.length >= 16 ? iso.substring(11, 16) : '';
    return '$datePart $timePart';
  }

  @override
  Widget build(BuildContext context) {
    final isGroup = false; // Le backend ne renvoie que des conversations 1-1 (otherUser*).
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            CircleAvatar(
              radius: 16,
              backgroundColor: Colors.teal.withValues(alpha: 0.2),
              child: Text(
                initialsFromName(widget.title),
                style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold, fontSize: 12),
              ),
            ),
            const SizedBox(width: 10),
            Flexible(
              child: Text(widget.title, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16)),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: _isLoading
                ? const ShimmerLoading(itemCount: 5)
                : _error != null && _messages.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.error_outline, size: 44, color: Colors.white.withValues(alpha: 0.3)),
                            const SizedBox(height: 10),
                            Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                            const SizedBox(height: 10),
                            OutlinedButton(onPressed: _loadMessages, child: const Text('Réessayer')),
                          ],
                        ),
                      )
                    : _messages.isEmpty
                        ? Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.chat_bubble_outline, size: 44, color: Colors.white.withValues(alpha: 0.3)),
                                const SizedBox(height: 10),
                                Text('Aucun message — dites bonjour !',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                              ],
                            ),
                          )
                        : ListView.builder(
                            controller: _scrollCtrl,
                            padding: const EdgeInsets.all(14),
                            itemCount: _messages.length,
                            itemBuilder: (context, index) {
                              final m = _messages[index] as Map<String, dynamic>;
                              final mine = m['senderId'] == _myUserId;
                              return _messageBubble(m, mine, isGroup);
                            },
                          ),
          ),
          _buildComposer(),
        ],
      ),
    );
  }

  Widget _messageBubble(Map<String, dynamic> m, bool mine, bool isGroup) {
    final content = m['content'] ?? '';
    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.78),
        decoration: BoxDecoration(
          gradient: mine
              ? LinearGradient(colors: [AppColors.primary, AppColors.primary.withValues(alpha: 0.85)])
              : LinearGradient(colors: [Colors.white.withValues(alpha: 0.07), Colors.white.withValues(alpha: 0.04)]),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(14),
            topRight: const Radius.circular(14),
            bottomLeft: Radius.circular(mine ? 14 : 4),
            bottomRight: Radius.circular(mine ? 4 : 14),
          ),
          border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (!mine && !isGroup) ...[
              Text('${m['senderName'] ?? ''}',
                  style: TextStyle(color: AppColors.primaryLight, fontSize: 10, fontWeight: FontWeight.w600)),
              const SizedBox(height: 2),
            ],
            Text('$content',
                style: TextStyle(
                  color: mine ? Colors.white : Colors.white.withValues(alpha: 0.85),
                  fontSize: 13,
                  height: 1.3,
                )),
            const SizedBox(height: 3),
            Text(_time(m['createdAt']?.toString()),
                style: TextStyle(
                  color: mine ? Colors.white.withValues(alpha: 0.6) : Colors.white.withValues(alpha: 0.3),
                  fontSize: 9,
                )),
          ],
        ),
      ),
    );
  }

  Widget _buildComposer() {
    return Container(
      padding: EdgeInsets.only(
        left: 14, right: 8, top: 8, bottom: MediaQuery.of(context).viewInsets.bottom + 8,
      ),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.03),
        border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.06))),
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _messageCtrl,
              style: const TextStyle(color: Colors.white, fontSize: 13),
              minLines: 1,
              maxLines: 4,
              textInputAction: TextInputAction.send,
              onChanged: (_) => setState(() {}),
              onSubmitted: (_) => _send(),
              decoration: InputDecoration(
                hintText: 'Message...',
                hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13),
                filled: true,
                fillColor: Colors.white.withValues(alpha: 0.06),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(22), borderSide: BorderSide.none),
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              ),
            ),
          ),
          const SizedBox(width: 6),
          IconButton(
            onPressed: (_isSending || _messageCtrl.text.trim().isEmpty) ? null : _send,
            style: IconButton.styleFrom(
              backgroundColor: AppColors.primary.withValues(alpha: _isSending ? 0.6 : 1),
            ),
            icon: _isSending
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.send, color: Colors.white, size: 18),
          ),
        ],
      ),
    );
  }

}


/// Lance une conversation avec un autre utilisateur : récupère la liste des
/// membres, affiche un sélecteur, puis POST /messages/conversations
/// (retourne la conversation créée — l'écran l'ouvre ensuite).
Future<void> showStartConversationSheet(
  BuildContext context,
  ApiService apiService, {
  required void Function(String conversationId, String title) onStarted,
}) async {  List<dynamic> users = [];
  try {
    final res = await apiService.get('/users', params: {'size': '100'});
    users = (res.data is Map ? (res.data as Map)['content'] : res.data) as List? ?? [];
  } catch (_) {}
  if (!context.mounted) return;

  final myId = AuthState().userId;
  final candidates = users
      .where((u) => u is Map && u['id'] != myId)
      .whereType<Map<String, dynamic>>()
      .toList();

  // État partagé entre les rebuilds du StatefulBuilder : déclaré AVANT le
  // builder, sinon chaque setSheetState réinitialiserait la recherche et
  // l'état « démarrage en cours ».
  String query = '';
  bool starting = false;

  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (ctx) => StatefulBuilder(
      builder: (ctx, setSheetState) {
        return Container(
          decoration: BoxDecoration(
            color: const Color(0xFF111827),
            borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
            border: Border(top: BorderSide(color: Colors.white.withValues(alpha: 0.06))),
          ),
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 32, height: 4,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text('Nouvelle conversation',
                      style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  TextField(
                    style: const TextStyle(color: Colors.white),
                    onChanged: (v) => setSheetState(() => query = v),
                    decoration: InputDecoration(
                      hintText: 'Rechercher un membre...',
                      hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13),
                      prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 20),
                      filled: true,
                      fillColor: Colors.white.withValues(alpha: 0.05),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Flexible(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxHeight: 320),
                      child: candidates.isEmpty
                              ? Padding(
                                  padding: const EdgeInsets.all(24),
                                  child: Center(
                                    child: Text('Aucun membre disponible',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
                                  ),
                                )
                              : ListView(
                                  shrinkWrap: true,
                                  children: [
                                    for (final u in candidates.where((u) {
                                      final q = query.trim().toLowerCase();
                                      if (q.isEmpty) return true;
                                      return ('${u['firstName'] ?? ''} ${u['lastName'] ?? ''}')
                                          .toLowerCase()
                                          .contains(q);
                                    })) ...[
                                      ListTile(
                                        dense: true,
                                        leading: CircleAvatar(
                                          radius: 18,
                                          backgroundColor: AppColors.primary.withValues(alpha: 0.15),
                                          child: Text(
                                            initialsFromUser(u),
                                            style: TextStyle(color: AppColors.primaryLight, fontSize: 12, fontWeight: FontWeight.bold),
                                          ),
                                        ),
                                        title: Text(
                                          '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}'.trim().isEmpty
                                              ? '${u['email'] ?? '—'}'
                                              : '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}',
                                          style: const TextStyle(color: Colors.white, fontSize: 14),
                                        ),
                                        subtitle: Text(
                                          '${u['role'] ?? u['activeRole'] ?? ''}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                        ),
                                        onTap: starting
                                            ? null
                                            : () async {
                                                setSheetState(() => starting = true);
                                                try {
                                                  final res = await apiService.post('/messages/conversations',
                                                      data: {'otherUserId': u['id']});
                                                  if (ctx.mounted) Navigator.pop(ctx);
                                                  final conv = res.data as Map<String, dynamic>? ?? {};
                                                  onStarted(
                                                    (conv['id'] ?? u['id']) as String,
                                                    '${u['firstName'] ?? ''} ${u['lastName'] ?? ''}'.trim(),
                                                  );
                                                } catch (_) {
                                                  if (ctx.mounted) {
                                                    ScaffoldMessenger.of(ctx).showSnackBar(
                                                      const SnackBar(content: Text('Impossible de démarrer la conversation')),
                                                    );
                                                  }
                                                  setSheetState(() => starting = false);
                                                }
                                              },
                                      ),
                                      const Divider(height: 1, color: Colors.white10),
                                    ],
                                  ],
                                ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    ),
  );
}


