import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';

/// Real-time conversation detail with WebSocket for live messaging.
class ConversationDetailScreen extends StatefulWidget {
  const ConversationDetailScreen({
    super.key,
    required this.conversationId,
    required this.title,
    this.apiService,
  });

  final String conversationId;
  final String title;
  final ApiService? apiService;

  @override
  State<ConversationDetailScreen> createState() =>
      _ConversationDetailScreenState();
}

class _ConversationDetailScreenState extends State<ConversationDetailScreen>
    with WidgetsBindingObserver {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final _messageCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  final _focusNode = FocusNode();

  List<dynamic> _messages = [];
  bool _isLoading = true;
  bool _isSending = false;
  String? _error;
  final Set<String> _typingUsers = {};

  WebSocketChannel? _wsChannel;
  bool _wsConnected = false;
  Timer? _typingDebounce;
  Timer? _reconnectTimer;
  DateTime? _lastTypingSent;
  bool _isTyping = false;
  bool _disposed = false;

  String? get _myUserId => AuthState().userId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadMessages();
    _connectWebSocket();
    _focusNode.addListener(_onFocusChange);
  }

  @override
  void dispose() {
    // Marquer d'abord l'état : empêche tout callback WebSocket (en particulier
    // le onDone de reconnexion) de créer un nouveau timer après le dispose.
    _disposed = true;
    WidgetsBinding.instance.removeObserver(this);
    _messageCtrl.dispose();
    _scrollCtrl.dispose();
    _focusNode.dispose();
    _typingDebounce?.cancel();
    _disconnectWebSocket();
    _reconnectTimer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _loadMessages();
      if (!_wsConnected) _connectWebSocket();
    }
  }

  void _onFocusChange() {
    if (_focusNode.hasFocus) {
      _sendTypingIndicator(true);
    }
  }

  // ======================== WebSocket ========================

  Future<void> _connectWebSocket() async {
    try {
      final dio = _apiService.dio;
      final baseUrl = dio.options.baseUrl;
      final wsUrl = baseUrl.replaceFirst('http', 'ws') + '/ws';
      _wsChannel = WebSocketChannel.connect(Uri.parse(wsUrl));

      _wsChannel!.sink.add(
          'CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\x00');

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
          if (_disposed) return; // aucun nouveau timer après le dispose
          _reconnectTimer?.cancel();
          _reconnectTimer = Timer(const Duration(seconds: 3), () {
            if (mounted) _connectWebSocket();
          });
        },
        onError: (_) {
          _wsConnected = false;
        },
      );
    } catch (_) {
      _wsConnected = false;
    }
  }

  void _subscribeToConversation() {
    if (!_wsConnected || _wsChannel == null) return;
    final dest = '/topic/conversations/${widget.conversationId}';
    final typingDest = '/topic/conversations/${widget.conversationId}/typing';
    _wsChannel!.sink.add(
        'SUBSCRIBE\nid:sub-${widget.conversationId}\ndestination:$dest\n\n\x00');
    _wsChannel!.sink.add(
        'SUBSCRIBE\nid:typing-${widget.conversationId}\ndestination:$typingDest\n\n\x00');
  }

  void _handleWsFrame(String frame) {
    if (!frame.startsWith('MESSAGE') && !frame.startsWith('message')) return;
    final bodyStart = frame.indexOf('\n\n');
    if (bodyStart == -1) return;
    final body =
        frame.substring(bodyStart + 2).replaceAll('\x00', '').trim();
    if (body.isEmpty) return;

    try {
      final data = jsonDecode(body) as Map<String, dynamic>;
      if (data.containsKey('typing')) {
        final userId = data['userId']?.toString() ?? '';
        if (userId != _myUserId) {
          setState(() {
            if (data['typing'] == true) {
              _typingUsers.add(userId);
            } else {
              _typingUsers.remove(userId);
            }
          });
        }
        return;
      }
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
    _wsChannel!.sink.add(
        'SEND\ndestination:/app/conversations/${widget.conversationId}/send\ncontent-type:application/json\n\n$payload\x00');
  }

  void _sendTypingIndicator(bool typing) {
    if (!_wsConnected || _wsChannel == null) return;
    final now = DateTime.now();
    if (typing && _lastTypingSent != null && now.difference(_lastTypingSent!).inSeconds < 2) {
      return;
    }
    _lastTypingSent = now;
    final payload = jsonEncode({'typing': typing});
    _wsChannel!.sink.add(
        'SEND\ndestination:/app/conversations/${widget.conversationId}/typing\ncontent-type:application/json\n\n$payload\x00');
  }

  void _disconnectWebSocket() {
    _wsChannel?.sink.close();
    _wsChannel = null;
    _wsConnected = false;
  }

  // ======================== REST ========================

  Future<void> _loadMessages() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService
          .get('/messages/conversations/${widget.conversationId}/messages');
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
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Impossible de charger la conversation';
        });
      }
    }
  }

  Future<void> _markAsRead() async {
    try {
      await _apiService
          .patch('/messages/conversations/${widget.conversationId}/read');
    } catch (_) {}
  }

  Future<void> _send() async {
    final content = _messageCtrl.text.trim();
    if (content.isEmpty || _isSending) return;
    setState(() => _isSending = true);
    _sendTypingIndicator(false);

    try {
      if (_wsConnected) {
        _sendWsMessage(content);
        setState(() {
          _messages.add({
            'id': DateTime.now().millisecondsSinceEpoch.toString(),
            'senderId': _myUserId,
            'content': content,
            'sentAt': DateTime.now().toIso8601String(),
          });
        });
        _scrollToBottom();
      } else {
        await _apiService.post(
          '/messages/conversations/${widget.conversationId}/messages',
          data: {'content': content},
        );
        await _loadMessages();
      }
      _messageCtrl.clear();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text("Erreur lors de l'envoi")));
      }
    } finally {
      if (mounted) setState(() => _isSending = false);
    }
  }

  // ======================== UI Helpers ========================

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
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            CircleAvatar(
              radius: 16,
              backgroundColor: Colors.teal.withValues(alpha: 0.2),
              child: Text(
                _initials(widget.title),
                style: const TextStyle(
                    color: Colors.teal,
                    fontWeight: FontWeight.bold,
                    fontSize: 12),
              ),
            ),
            const SizedBox(width: 10),
            Flexible(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(widget.title,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 16)),
                  if (_typingUsers.isNotEmpty)
                    Text(
                      'ecrit...',
                      style: TextStyle(
                          fontSize: 12,
                          color: Colors.teal.withValues(alpha: 0.7),
                          fontStyle: FontStyle.italic),
                    )
                  else
                    Text(
                      _wsConnected ? 'En ligne' : 'Hors ligne',
                      style: TextStyle(
                          fontSize: 11,
                          color: _wsConnected
                              ? Colors.green.withValues(alpha: 0.7)
                              : Colors.orange.withValues(alpha: 0.7)),
                    ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Icon(
              _wsConnected ? Icons.wifi : Icons.wifi_off,
              size: 18,
              color: _wsConnected ? Colors.green : Colors.orange,
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _error != null && _messages.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.error_outline,
                                size: 44,
                                color: Colors.white.withValues(alpha: 0.3)),
                            const SizedBox(height: 10),
                            Text(_error!,
                                style: TextStyle(
                                    color:
                                        Colors.white.withValues(alpha: 0.5))),
                            const SizedBox(height: 10),
                            OutlinedButton(
                                onPressed: _loadMessages,
                                child: const Text('Réessayer')),
                          ],
                        ),
                      )
                    : _messages.isEmpty
                        ? Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.chat_bubble_outline,
                                    size: 44,
                                    color:
                                        Colors.white.withValues(alpha: 0.3)),
                                const SizedBox(height: 10),
                                Text('Aucun message — dites bonjour !',
                                    style: TextStyle(
                                        color: Colors.white
                                            .withValues(alpha: 0.5))),
                              ],
                            ),
                          )
                        : ListView.builder(
                            controller: _scrollCtrl,
                            padding: const EdgeInsets.all(14),
                            itemCount: _messages.length,
                            itemBuilder: (context, index) {
                              final m =
                                  _messages[index] as Map<String, dynamic>;
                              final mine =
                                  m['senderId'].toString() == _myUserId;
                              return _messageBubble(m, mine);
                            },
                          ),
          ),
          if (_typingUsers.isNotEmpty)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              alignment: Alignment.centerLeft,
              child: Row(
                children: [
                  SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.teal.withValues(alpha: 0.5)),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    '${_typingUsers.length} personne(s) ecrivent...',
                    style: TextStyle(
                        fontSize: 12,
                        color: Colors.teal.withValues(alpha: 0.7),
                        fontStyle: FontStyle.italic),
                  ),
                ],
              ),
            ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: Theme.of(context).scaffoldBackgroundColor,
              boxShadow: [
                BoxShadow(
                    color: Colors.black.withValues(alpha: 0.05),
                    blurRadius: 8,
                    offset: const Offset(0, -2)),
              ],
            ),
            child: SafeArea(
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageCtrl,
                      focusNode: _focusNode,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _send(),
                      onChanged: (text) {
                        if (text.isNotEmpty && !_isTyping) {
                          _isTyping = true;
                          _sendTypingIndicator(true);
                        }
                        _typingDebounce?.cancel();
                        _typingDebounce = Timer(
                            const Duration(seconds: 2), () {
                          if (_isTyping) {
                            _isTyping = false;
                            _sendTypingIndicator(false);
                          }
                        });
                      },
                      decoration: InputDecoration(
                        hintText: 'Ecrire un message...',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide.none,
                        ),
                        filled: true,
                        fillColor: Colors.grey.withValues(alpha: 0.1),
                        contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 10),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  CircleAvatar(
                    radius: 22,
                    backgroundColor: const Color(0xFF2B6CB0),
                    child: _isSending
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white))
                        : IconButton(
                            icon: const Icon(Icons.send,
                                color: Colors.white, size: 20),
                            onPressed: _send,
                          ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _messageBubble(Map<String, dynamic> m, bool mine) {
    final content = m['content']?.toString() ?? '';
    final sentAt = m['sentAt']?.toString() ?? m['createdAt']?.toString();

    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        child: Column(
          crossAxisAlignment:
              mine ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: mine
                    ? const Color(0xFF2B6CB0)
                    : Colors.grey.withValues(alpha: 0.15),
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(18),
                  topRight: const Radius.circular(18),
                  bottomLeft:
                      mine ? const Radius.circular(18) : Radius.zero,
                  bottomRight:
                      mine ? Radius.zero : const Radius.circular(18),
                ),
              ),
              child: Text(
                content,
                style: TextStyle(
                  color: mine ? Colors.white : Colors.black87,
                  fontSize: 15,
                ),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              _time(sentAt),
              style: TextStyle(
                  fontSize: 10, color: Colors.white.withValues(alpha: 0.4)),
            ),
          ],
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
