import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../data/services/api_service.dart';

/// AI Pastoral Copilot — mobile (équivalent d'AiAssistantPage web).
/// Branché sur POST /api/v1/ai/chat, GET/DELETE /api/v1/ai/chat/history.
/// Accessible à TOUS les rôles authentifiés.
class AiAssistantScreen extends StatefulWidget {
  const AiAssistantScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<AiAssistantScreen> createState() => _AiAssistantScreenState();
}

class _AiAssistantScreenState extends State<AiAssistantScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final List<_CovMessage> _messages = [];
  bool _isLoading = false;
  bool _loadingHistory = true;
  String? _copiedId;
  bool _healthChecking = false;

  static const _suggestions = [
    'Quelles personnes nécessitent un suivi cette semaine ?',
    'Donne-moi les familles sans contact récent',
    'Résume les demandes de prière ouvertes',
    'Propose les actions prioritaires',
    'Quelles familles sont à risque ?',
    'Résumé des présences cette semaine',
  ];

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _loadHistory() async {
    try {
      final res = await _apiService.get('/ai/chat/history');
      if (!mounted) return;
      final d = res.data;
      if (d is List) {
        final history = d.reversed.map((m) {
          final mm = m as Map<String, dynamic>;
          final role = (mm['role'] ?? 'assistant').toString();
          return _CovMessage(
            id: mm['id']?.toString() ??
                DateTime.now().millisecondsSinceEpoch.toString(),
            role: role == 'user' ? 'user' : 'assistant',
            content: mm['content']?.toString() ?? '',
            timestamp: DateTime.tryParse(mm['timestamp']?.toString() ?? ''),
          );
        }).toList();
        if (history.isNotEmpty) {
          setState(() => _messages.addAll(history));
        }
      }
    } catch (_) {
      // historique indisponible : on démarre vide
    }
    if (mounted) setState(() => _loadingHistory = false);
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _sendMessage(String text) async {
    if (text.trim().isEmpty || _isLoading) return;

    final userMsg = _CovMessage(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      role: 'user',
      content: text.trim(),
      timestamp: DateTime.now(),
    );
    setState(() {
      _messages.add(userMsg);
      _isLoading = true;
    });
    _controller.clear();
    _scrollToBottom();

    try {
      final res =
          await _apiService.post('/ai/chat', data: {'message': text.trim()});
      final data = res.data;
      final reply = data is Map
          ? (data['reply'] ?? data['response'] ?? 'Pas de réponse.')
          : 'Pas de réponse.';
      final sources = data is Map
          ? (data['sources'] is List ? data['sources'] as List : <dynamic>[])
          : <dynamic>[];

      final assistantMsg = _CovMessage(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        role: 'assistant',
        content: reply.toString(),
        timestamp: DateTime.now(),
        sources: sources.map((s) => s.toString()).toList(),
      );
      if (mounted) {
        setState(() {
          _messages.add(assistantMsg);
          _isLoading = false;
        });
        _scrollToBottom();
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _messages.add(_CovMessage(
          id: DateTime.now().millisecondsSinceEpoch.toString(),
          role: 'assistant',
          content:
              "Désolé, je ne peux pas répondre pour le moment. Vérifiez que l'assistant IA est configuré.",
          timestamp: DateTime.now(),
        ));
        _isLoading = false;
      });
    }
  }

  Future<void> _checkHealth() async {
    setState(() => _healthChecking = true);
    bool ok = false;
    String message = 'Vérification...';
    try {
      final res = await _apiService.get('/ai/health');
      final d = res.data;
      if (d is Map) {
        ok = d['ollama'] == true;
        message = ok
            ? 'IA stable (${d['model']})'
            : "Ollama indisponible — réponses contextuelles basées sur vos données.";
      }
    } catch (_) {
      message = 'Impossible de joindre le backend IA.';
    }
    if (!mounted) return;
    setState(() => _healthChecking = false);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(ok ? '✅ $message' : 'ℹ️ $message'),
        backgroundColor: ok ? const Color(0xFF059669) : const Color(0xFF1E293B),
        duration: const Duration(seconds: 4),
      ),
    );
  }

  Future<void> _clearHistory() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E293B),
        title: const Text('Effacer l\'historique ?',
            style: TextStyle(color: Colors.white)),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Annuler',
                  style: TextStyle(color: Colors.white54))),
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('Effacer',
                  style: TextStyle(color: Colors.redAccent))),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _apiService.delete('/ai/chat/history');
      if (mounted) setState(() => _messages.clear());
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('Impossible d\'effacer l\'historique')),
        );
      }
    }
  }

  void _copyMessage(String id, String text) {
    Clipboard.setData(ClipboardData(text: text));
    setState(() => _copiedId = id);
    Future.delayed(const Duration(seconds: 2), () {
      if (mounted) setState(() => _copiedId = null);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1E293B),
        elevation: 0,
        title: const Row(
          children: [
            SizedBox(
              width: 32,
              height: 32,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                      colors: [Color(0xFF8B5CF6), Color(0xFF7C3AED)]),
                  borderRadius: BorderRadius.all(Radius.circular(8)),
                ),
                child: Icon(Icons.auto_awesome,
                    color: Colors.white, size: 16),
              ),
            ),
            SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Copilot IA Pastoral',
                    style:
                        TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                Text('IA locale • Vos données',
                    style:
                        TextStyle(fontSize: 10, color: Colors.white54)),
              ],
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'État de l\'IA',
            onPressed: _healthChecking ? null : _checkHealth,
            icon: _healthChecking
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.memory, color: Colors.white54, size: 20),
          ),
          IconButton(
            tooltip: 'Effacer l\'historique',
            onPressed:
                _messages.isEmpty ? null : _clearHistory,
            icon: const Icon(Icons.delete_sweep_outlined,
                color: Colors.white54, size: 20),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _loadingHistory
                ? const Center(
                    child: CircularProgressIndicator(color: Colors.white38))
                : _messages.isEmpty
                    ? _buildEmptyState()
                    : ListView.builder(
                        controller: _scrollController,
                        padding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 12),
                        itemCount: _messages.length + (_isLoading ? 1 : 0),
                        itemBuilder: (context, index) {
                          if (index == _messages.length) {
                            return _buildTypingIndicator();
                          }
                          return _buildMessageBubble(_messages[index]);
                        },
                      ),
          ),
          _buildInputBar(),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF8B5CF6), Color(0xFF7C3AED)],
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: const Icon(Icons.auto_awesome,
                  color: Colors.white, size: 28),
            ),
            const SizedBox(height: 16),
            const Text(
              'Posez une question sur votre église',
              style: TextStyle(
                color: Colors.white,
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              "L'IA connaît les données de votre église",
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.5),
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 20),
            ..._suggestions.map((s) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () {
                        _controller.text = s;
                        _sendMessage(s);
                      },
                      borderRadius: BorderRadius.circular(10),
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          border: Border.all(
                              color: Colors.white.withValues(alpha: 0.1)),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          s,
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.6),
                            fontSize: 13,
                          ),
                        ),
                      ),
                    ),
                  ),
                )),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageBubble(_CovMessage msg) {
    final isUser = msg.role == 'user';
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment:
            isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isUser)
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                    colors: [Color(0xFF8B5CF6), Color(0xFF7C3AED)]),
                borderRadius: BorderRadius.circular(7),
              ),
              child: const Icon(Icons.auto_awesome,
                  color: Colors.white, size: 14),
            ),
          if (!isUser) const SizedBox(width: 8),
          Flexible(
            child: Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: isUser
                    ? const Color(0xFF7C3AED)
                    : const Color(0xFF1E293B),
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(12),
                  topRight: const Radius.circular(12),
                  bottomLeft: Radius.circular(isUser ? 12 : 4),
                  bottomRight: Radius.circular(isUser ? 4 : 12),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    msg.content,
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: isUser ? 1.0 : 0.9),
                      fontSize: 14,
                      height: 1.4,
                    ),
                  ),
                  if (msg.sources.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: msg.sources.map((s) => Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: const Color(0xFF8B5CF6)
                                  .withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              'source: $s',
                              style: const TextStyle(
                                  color: Color(0xFFC4B5FD), fontSize: 10),
                            ),
                          )).toList(),
                    ),
                  ],
                  const SizedBox(height: 6),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        msg.timestamp != null
                            ? '${msg.timestamp!.hour.toString().padLeft(2, '0')}:${msg.timestamp!.minute.toString().padLeft(2, '0')}'
                            : '',
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.3),
                          fontSize: 10,
                        ),
                      ),
                      if (!isUser) ...[
                        const SizedBox(width: 8),
                        GestureDetector(
                          onTap: () => _copyMessage(msg.id, msg.content),
                          child: Icon(
                            _copiedId == msg.id ? Icons.check : Icons.copy,
                            size: 12,
                            color: Colors.white.withValues(alpha: 0.3),
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ),
          if (isUser) const SizedBox(width: 8),
        ],
      ),
    );
  }

  Widget _buildTypingIndicator() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 28,
            height: 28,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                  colors: [Color(0xFF8B5CF6), Color(0xFF7C3AED)]),
              borderRadius: BorderRadius.circular(7),
            ),
            child: const Icon(Icons.auto_awesome,
                color: Colors.white, size: 14),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: const Color(0xFF1E293B),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white.withValues(alpha: 0.5),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  "L'IA réfléchit...",
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.5),
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: const BoxDecoration(
        color: Color(0xFF1E293B),
        border: Border(top: BorderSide(color: Color(0xFF334155))),
      ),
      child: SafeArea(
        top: false,
        child: Row(
          children: [
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: const Color(0xFF0F172A),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFF334155)),
                ),
                child: TextField(
                  controller: _controller,
                  style: const TextStyle(color: Colors.white, fontSize: 14),
                  maxLines: null,
                  textInputAction: TextInputAction.send,
                  onSubmitted: _sendMessage,
                  decoration: InputDecoration(
                    hintText: 'Question sur votre église...',
                    hintStyle: TextStyle(
                        color: Colors.white.withValues(alpha: 0.3)),
                    border: InputBorder.none,
                    contentPadding: const EdgeInsets.symmetric(
                        horizontal: 14, vertical: 10),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            GestureDetector(
              onTap: _isLoading ? null : () => _sendMessage(_controller.text),
              child: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  gradient: _isLoading
                      ? null
                      : const LinearGradient(
                          colors: [Color(0xFF8B5CF6), Color(0xFF7C3AED)]),
                  color: _isLoading ? Colors.grey : null,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  _isLoading ? Icons.hourglass_empty : Icons.send,
                  color: Colors.white,
                  size: 18,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CovMessage {
  final String id;
  final String role;
  final String content;
  final DateTime? timestamp;
  final List<String> sources;

  _CovMessage({
    required this.id,
    required this.role,
    required this.content,
    this.timestamp,
    this.sources = const [],
  });
}