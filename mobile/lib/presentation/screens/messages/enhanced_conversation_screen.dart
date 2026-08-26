import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/secure_screen.dart';

/// Enhanced real-time conversation with voice messages, reactions, threads, and group support.
class EnhancedConversationScreen extends StatefulWidget {
  final String conversationId;
  final String title;
  final bool isGroup;
  final ApiService? apiService;

  const EnhancedConversationScreen({
    super.key,
    required this.conversationId,
    required this.title,
    this.isGroup = false,
    this.apiService,
  });

  @override
  State<EnhancedConversationScreen> createState() => _EnhancedConversationScreenState();
}

class _EnhancedConversationScreenState extends State<EnhancedConversationScreen>
    with WidgetsBindingObserver {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final _messageCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  Timer? _recordingTimer;

  List<dynamic> _messages = [];
  bool _isLoading = true;
  bool _isSending = false;
  bool _isRecording = false;
  int _recordingSeconds = 0;

  // Reply state
  String? _replyToId;
  String? _replyToContent;
  String? _replyToSender;

  // Thread state
  String? _viewingThread;
  List<dynamic> _threadReplies = [];

  // Emoji reactions
  static const _emojiOptions = ['❤️', '👍', '😊', '🙏', '😂', '😮'];
  String? _showEmojiForMessage;

  WebSocketChannel? _wsChannel;
  bool _wsConnected = false;
  Timer? _reconnectTimer;
  bool _disposed = false;

  String? get _myUserId => AuthState().userId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadMessages();
    _connectWebSocket();
  }

  @override
  void dispose() {
    _disposed = true;
    WidgetsBinding.instance.removeObserver(this);
    _messageCtrl.dispose();
    _scrollCtrl.dispose();
    _recordingTimer?.cancel();
    _disconnectWebSocket();
    _reconnectTimer?.cancel();
    super.dispose();
  }

  // ── WebSocket ──

  Future<void> _connectWebSocket() async {
    try {
      final dio = _apiService.dio;
      final baseUrl = dio.options.baseUrl;
      final wsUrl = '${baseUrl.replaceFirst('http', 'ws')}/ws';
      _wsChannel = WebSocketChannel.connect(Uri.parse(wsUrl));
      _wsChannel!.sink.add('CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\x00');
      _wsChannel!.stream.listen(
        (data) {
          final frame = data.toString();
          if (frame.contains('CONNECTED')) {
            _wsConnected = true;
            _subscribeToConversation();
            return;
          }
          _handleWsFrame(frame);
        },
        onDone: () {
          _wsConnected = false;
          if (!_disposed && mounted) {
            _reconnectTimer?.cancel();
            _reconnectTimer = Timer(const Duration(seconds: 3), () {
              if (mounted && !_disposed) _connectWebSocket();
            });
          }
        },
        onError: (_) { _wsConnected = false; },
      );
    } catch (_) { _wsConnected = false; }
  }

  void _subscribeToConversation() {
    if (!_wsConnected || _wsChannel == null) return;
    final dest = '/topic/conversations/${widget.conversationId}';
    _wsChannel!.sink.add('SUBSCRIBE\nid:sub-${widget.conversationId}\ndestination:$dest\n\n\x00');
  }

  void _handleWsFrame(String frame) {
    if (!frame.startsWith('MESSAGE') && !frame.startsWith('message')) return;
    final bodyStart = frame.indexOf('\n\n');
    if (bodyStart == -1) return;
    final body = frame.substring(bodyStart + 2).replaceAll('\x00', '').trim();
    if (body.isEmpty) return;
    try {
      final data = jsonDecode(body) as Map<String, dynamic>;
      if (mounted) {
        setState(() {
          final id = data['id']?.toString();
          if (id != null && !_messages.any((m) => m['id']?.toString() == id)) {
            _messages.add(data);
          }
        });
        _scrollToBottom();
      }
    } catch (_) {}
  }

  void _sendWsMessage(String content) {
    if (!_wsConnected || _wsChannel == null) return;
    final payload = jsonEncode({'content': content});
    _wsChannel!.sink.add('SEND\ndestination:/app/conversations/${widget.conversationId}/send\ncontent-type:application/json\n\n$payload\x00');
  }

  void _disconnectWebSocket() {
    _wsChannel?.sink.close();
    _wsChannel = null;
    _wsConnected = false;
  }

  // ── REST ──

  Future<void> _loadMessages() async {
    setState(() => _isLoading = true);
    try {
      final endpoint = widget.isGroup
          ? '/messages/groups/${widget.conversationId}/messages'
          : '/messages/conversations/${widget.conversationId}/messages/enhanced';
      final res = await _apiService.get(endpoint);
      if (mounted) {
        setState(() { _messages = (res.data as List?) ?? []; _isLoading = false; });
        _scrollToBottom();
      }
    } catch (_) {
      if (mounted) setState(() { _isLoading = false; });
    }
  }

  Future<void> _send() async {
    final content = _messageCtrl.text.trim();
    if (content.isEmpty || _isSending) return;
    setState(() => _isSending = true);
    try {
      final endpoint = widget.isGroup
          ? '/messages/groups/${widget.conversationId}/messages'
          : '/messages/conversations/${widget.conversationId}/messages/enhanced';
      final body = <String, dynamic>{'content': content};
      if (_replyToId != null) body['replyToId'] = _replyToId;
      if (_wsConnected && !widget.isGroup) {
        _sendWsMessage(content);
        setState(() { _messages.add({'id': DateTime.now().millisecondsSinceEpoch.toString(), 'senderId': _myUserId, 'senderName': 'Moi', 'content': content, 'createdAt': DateTime.now().toIso8601String()}); });
        _scrollToBottom();
      } else {
        await _apiService.post(endpoint, data: body);
        await _loadMessages();
      }
      _messageCtrl.clear();
      setState(() { _replyToId = null; _replyToContent = null; _replyToSender = null; });
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Erreur lors de l'envoi")));
    } finally { if (mounted) setState(() => _isSending = false); }
  }

  // ── Voice Recording (simulated — use platform channel for real) ──

  Future<void> _startRecording() async {
    setState(() { _isRecording = true; _recordingSeconds = 0; });
    _recordingTimer = Timer.periodic(const Duration(seconds: 1), (t) { if (mounted) setState(() => _recordingSeconds++); });
  }

  Future<void> _stopRecordingAndSend() async {
    _recordingTimer?.cancel();
    setState(() => _isRecording = false);
    if (mounted) {
      try {
        final endpoint = widget.isGroup
            ? '/messages/groups/${widget.conversationId}/voice'
            : '/messages/conversations/${widget.conversationId}/voice';
        await _apiService.post(endpoint, data: {'audioUrl': 'voice_recording', 'duration': _recordingSeconds});
        await _loadMessages();
      } catch (_) {}
    }
  }

  // ── Reactions ──

  Future<void> _toggleReaction(String messageId, String emoji) async {
    try {
      await _apiService.post('/messages/messages/$messageId/reactions', data: {'emoji': emoji});
      setState(() => _showEmojiForMessage = null);
      await _loadMessages();
    } catch (_) {}
  }

  // ── Thread ──

  Future<void> _loadReplies(String messageId) async {
    try {
      final res = await _apiService.get('/messages/messages/$messageId/replies');
      setState(() { _viewingThread = messageId; _threadReplies = (res.data as List?) ?? []; });
    } catch (_) {}
  }

  // ── Helpers ──

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollCtrl.hasClients) _scrollCtrl.animateTo(_scrollCtrl.position.maxScrollExtent, duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
    });
  }

  String _time(String? iso) {
    if (iso == null || iso.length < 16) return '';
    return '${iso.substring(0, 10)} ${iso.substring(11, 16)}';
  }

  String _formatDuration(int seconds) {
    final m = seconds ~/ 60;
    final s = seconds % 60;
    return '$m:${s.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'ConversationScreen',
      auditAction: AuditActions.viewMessages,
      child: Scaffold(
        appBar: AppBar(
          title: Row(children: [
            CircleAvatar(radius: 16, backgroundColor: Colors.teal.withAlpha(50), child: Text(widget.title[0].toUpperCase(), style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold, fontSize: 12))),
            const SizedBox(width: 10),
            Flexible(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(widget.title, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16)),
              Text(_wsConnected ? 'En ligne' : 'Hors ligne', style: TextStyle(fontSize: 11, color: _wsConnected ? Colors.green.withAlpha(180) : Colors.orange.withAlpha(180))),
            ])),
          ]),
          actions: [Padding(padding: const EdgeInsets.only(right: 12), child: Icon(_wsConnected ? Icons.wifi : Icons.wifi_off, size: 18, color: _wsConnected ? Colors.green : Colors.orange))],
        ),
        body: Column(children: [
          Expanded(child: _isLoading
              ? const Center(child: CircularProgressIndicator())
              : _messages.isEmpty
                  ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.chat_bubble_outline, size: 44, color: Colors.white.withAlpha(70)), const SizedBox(height: 10), Text('Aucun message — dites bonjour !', style: TextStyle(color: Colors.white.withAlpha(120)))]))
                  : ListView.builder(controller: _scrollCtrl, padding: const EdgeInsets.all(14), itemCount: _messages.length, itemBuilder: (ctx, i) {
                      final m = _messages[i] as Map<String, dynamic>;
                      final mine = m['senderId']?.toString() == _myUserId;
                      return _buildMessage(m, mine);
                    }),
          ),

          // Thread replies panel
          if (_viewingThread != null) _buildThreadPanel(),

          // Reply preview
          if (_replyToId != null) Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: Colors.blue.shade50,
            child: Row(children: [
              Icon(Icons.reply, size: 16, color: Colors.blue.shade600),
              const SizedBox(width: 8),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(_replyToSender ?? '', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.blue.shade700)),
                Text(_replyToContent ?? '', style: const TextStyle(fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
              ])),
              IconButton(icon: const Icon(Icons.close, size: 16), onPressed: () => setState(() { _replyToId = null; _replyToContent = null; _replyToSender = null; })),
            ]),
          ),

          // Recording indicator
          if (_isRecording) Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: Colors.red.shade50,
            child: Row(children: [
              Container(width: 10, height: 10, decoration: const BoxDecoration(color: Colors.red, shape: BoxShape.circle)),
              const SizedBox(width: 8),
              Text('Enregistrement... ${_formatDuration(_recordingSeconds)}', style: TextStyle(color: Colors.red.shade700, fontWeight: FontWeight.w500)),
              const Spacer(),
              IconButton(icon: Icon(Icons.stop_circle, color: Colors.red.shade600), onPressed: _stopRecordingAndSend),
            ]),
          ),

          // Input bar
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(color: Theme.of(context).scaffoldBackgroundColor, boxShadow: [BoxShadow(color: Colors.black.withAlpha(10), blurRadius: 8, offset: const Offset(0, -2))]),
            child: SafeArea(child: Row(children: [
              // Mic button
              IconButton(
                icon: Icon(_isRecording ? Icons.stop : Icons.mic, color: _isRecording ? Colors.red : Colors.grey.shade600),
                onPressed: _isRecording ? _stopRecordingAndSend : _startRecording,
              ),
              Expanded(child: TextField(
                controller: _messageCtrl,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => _send(),
                decoration: InputDecoration(hintText: 'Écrire un message...', border: OutlineInputBorder(borderRadius: BorderRadius.circular(24), borderSide: BorderSide.none), filled: true, fillColor: Colors.grey.withAlpha(25), contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10)),
              )),
              const SizedBox(width: 8),
              CircleAvatar(radius: 22, backgroundColor: const Color(0xFF2B6CB0), child: _isSending
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : IconButton(icon: const Icon(Icons.send, color: Colors.white, size: 20), onPressed: _send)),
            ])),
          ),
        ]),
      ),
    );
  }

  Widget _buildMessage(Map<String, dynamic> m, bool mine) {
    final content = m['content']?.toString() ?? '';
    final messageType = m['messageType']?.toString() ?? 'TEXT';
    final sentAt = m['sentAt']?.toString() ?? m['createdAt']?.toString();
    final replyTo = m['replyToContent']?.toString();
    final replySender = m['replyToSenderName']?.toString();
    final reactions = m['reactionCounts'] as Map<String, dynamic>?;
    final msgId = m['id']?.toString() ?? '';

    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.78),
        child: Column(
          crossAxisAlignment: mine ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          children: [
            // Reply preview
            if (replyTo != null) Container(
              margin: const EdgeInsets.only(bottom: 4),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: Colors.blue.withAlpha(20), borderRadius: BorderRadius.circular(8), border: Border(left: BorderSide(color: Colors.blue.shade300, width: 2))),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(replySender ?? '', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.blue.shade700)),
                Text(replyTo, style: TextStyle(fontSize: 10, color: Colors.grey.shade600), maxLines: 1, overflow: TextOverflow.ellipsis),
              ]),
            ),

            // Message bubble
            GestureDetector(
              onLongPress: () => setState(() => _showEmojiForMessage = _showEmojiForMessage == msgId ? null : msgId),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                decoration: BoxDecoration(
                  color: mine ? const Color(0xFF2B6CB0) : Colors.grey.withAlpha(40),
                  borderRadius: BorderRadius.only(
                    topLeft: const Radius.circular(18), topRight: const Radius.circular(18),
                    bottomLeft: mine ? const Radius.circular(18) : Radius.zero,
                    bottomRight: mine ? Radius.zero : const Radius.circular(18),
                  ),
                ),
                child: messageType == 'VOICE'
                    ? Row(mainAxisSize: MainAxisSize.min, children: [
                        Icon(Icons.play_arrow, color: mine ? Colors.white : Colors.blue, size: 24),
                        Container(width: 100, height: 3, margin: const EdgeInsets.symmetric(horizontal: 8), decoration: BoxDecoration(color: (mine ? Colors.white : Colors.blue).withAlpha(80), borderRadius: BorderRadius.circular(2))),
                        Text(_formatDuration(m['mediaDuration'] ?? 0), style: TextStyle(color: mine ? Colors.white70 : Colors.grey, fontSize: 11)),
                      ])
                    : Text(content, style: TextStyle(color: mine ? Colors.white : Colors.black87, fontSize: 15)),
              ),
            ),

            // Timestamp
            Padding(padding: const EdgeInsets.only(top: 2), child: Text(_time(sentAt), style: TextStyle(fontSize: 10, color: Colors.white.withAlpha(100)))),

            // Reactions display
            if (reactions != null && reactions.isNotEmpty) Wrap(
              spacing: 4, runSpacing: 2,
              children: reactions.entries.map((e) => GestureDetector(
                onTap: () => _toggleReaction(msgId, e.key),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(color: Colors.grey.withAlpha(40), borderRadius: BorderRadius.circular(16), border: Border.all(color: Colors.grey.withAlpha(80))),
                  child: Text('${e.key} ${e.value}', style: const TextStyle(fontSize: 11)),
                ),
              )).toList(),
            ),

            // Emoji picker
            if (_showEmojiForMessage == msgId) Container(
              margin: const EdgeInsets.only(top: 4),
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), boxShadow: [BoxShadow(color: Colors.black.withAlpha(20), blurRadius: 8)]),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                ..._emojiOptions.map((emoji) => GestureDetector(
                  onTap: () => _toggleReaction(msgId, emoji),
                  child: Padding(padding: const EdgeInsets.all(4), child: Text(emoji, style: const TextStyle(fontSize: 20))),
                )),
                const SizedBox(width: 4),
                GestureDetector(
                  onTap: () { _replyToId = msgId; _replyToContent = content; _replyToSender = m['senderName']?.toString(); setState(() {}); _showEmojiForMessage = null; },
                  child: Icon(Icons.reply, size: 20, color: Colors.grey.shade600),
                ),
                GestureDetector(
                  onTap: () { _loadReplies(msgId); _showEmojiForMessage = null; },
                  child: Icon(Icons.forum, size: 20, color: Colors.grey.shade600),
                ),
              ]),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildThreadPanel() {
    return Container(
      height: 200,
      decoration: BoxDecoration(color: Colors.grey.shade50, border: Border(top: BorderSide(color: Colors.grey.shade200))),
      child: Column(children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(children: [
            const Icon(Icons.forum, size: 16, color: Colors.blue),
            const SizedBox(width: 8),
            const Text('Réponses', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
            const Spacer(),
            IconButton(icon: const Icon(Icons.close, size: 16), onPressed: () => setState(() { _viewingThread = null; _threadReplies = []; })),
          ]),
        ),
        Expanded(child: _threadReplies.isEmpty
            ? const Center(child: Text('Aucune réponse', style: TextStyle(color: Colors.grey, fontSize: 12)))
            : ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: _threadReplies.length,
                itemBuilder: (ctx, i) {
                  final r = _threadReplies[i] as Map<String, dynamic>;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.grey.shade200)),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [
                          Text(r['senderName']?.toString() ?? '', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.blue)),
                          const SizedBox(width: 8),
                          Text(_time(r['createdAt']?.toString()), style: TextStyle(fontSize: 9, color: Colors.grey.shade500)),
                        ]),
                        Text(r['content']?.toString() ?? '', style: const TextStyle(fontSize: 12)),
                      ]),
                    ),
                  );
                },
              )),
      ]),
    );
  }
}
