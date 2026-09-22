import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/status.dart' as status;
import 'dart:convert';
import 'dart:async';
import '../../../data/services/api_service.dart';
import '../../../tenant_config.dart';
import '../../../../l10n/app_localizations.dart';

/// Streaming Chat — mobile WebSocket version matching frontend StreamingChat.
/// Uses STOMP over WebSocket for real-time messaging.
class StreamingChatScreen extends StatefulWidget {
  final int streamId;
  const StreamingChatScreen({super.key, required this.streamId, this.apiService});
  final ApiService? apiService;

  @override
  State<StreamingChatScreen> createState() => _StreamingChatScreenState();
}

class _StreamingChatScreenState extends State<StreamingChatScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final List<_ChatMessage> _messages = [];
  bool _isLoading = true;
  bool _isConnected = false;
  String? _copiedId;
  WebSocketChannel? _channel;
  StreamSubscription? _subscription;

  static const _reactions = ['🙏', '❤️', '🔥', '👏', '✨', '💯'];

  @override
  void initState() {
    super.initState();
    _loadHistory();
    _connectWebSocket();
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    _subscription?.cancel();
    _channel?.sink.close(status.goingAway);
    super.dispose();
  }

  Future<void> _loadHistory() async {
    try {
      final orgId = await TenantConfig.resolveOrgId();
      final tenantId = int.tryParse(orgId ?? '');
      final params = tenantId == null ? null : {'tenantId': tenantId};
      final res = await _apiService.get('/stream-chat/${widget.streamId}', params: params);
      if (!mounted) return;
      final d = res.data;
      if (d is List) {
        final history = d.reversed.map((m) {
          final mm = m as Map<String, dynamic>;
          final role = (mm['senderName'] ?? '').toString() == 'Vous' ? 'user' : 'assistant';
          return _ChatMessage(
            id: mm['id']?.toString() ?? DateTime.now().millisecondsSinceEpoch.toString(),
            senderName: mm['senderName']?.toString() ?? '',
            content: mm['content']?.toString() ?? '',
            messageType: mm['messageType']?.toString() ?? 'TEXT',
            emoji: mm['emoji']?.toString(),
            timestamp: DateTime.tryParse(mm['createdAt']?.toString() ?? ''),
            isSystem: mm['isSystem'] == true,
          );
        }).toList();
        if (history.isNotEmpty) {
          setState(() => _messages.addAll(history));
        }
      }
    } catch (_) {
      // ignore
    }
    if (mounted) setState(() => _isLoading = false);
  }

  void _connectWebSocket() async {
    try {
      final token = await TenantConfig.getAccessToken();
      if (token == null) return;
      
      _channel = WebSocketChannel.connect(
        Uri.parse('ws://${TenantConfig.apiBaseUrl.replaceFirst('http', 'ws')}/ws?access_token=$token'),
      );
      
      _subscription = _channel!.stream.listen(
        (data) {
          try {
            final decoded = json.decode(data);
            if (decoded is Map) {
              if (decoded['command'] == 'CONNECTED') {
                _sendStompFrame({
                  'command': 'SUBSCRIBE',
                  'headers': {
                    'destination': '/topic/streams/${widget.streamId}/chat',
                    'id': 'sub-chat-${widget.streamId}',
                  },
                });
                _sendStompFrame({
                  'command': 'SUBSCRIBE',
                  'headers': {
                    'destination': '/topic/streams/${widget.streamId}/presence',
                    'id': 'sub-presence-${widget.streamId}',
                  },
                });
                _sendStompFrame({
                  'command': 'SEND',
                  'headers': {
                    'destination': '/app/streams/${widget.streamId}/join',
                  },
                  'body': json.encode({'senderName': 'Vous'}),
                });
              } else if (decoded['command'] == 'MESSAGE') {
                final destination = decoded['headers']?['destination'] ?? '';
                final body = json.decode(decoded['body'] ?? '{}');
                
                if (destination.contains('/chat')) {
                  final msg = _ChatMessage.fromJson(body);
                  if (mounted) {
                    setState(() => _messages.add(msg));
                    _scrollToBottom();
                  }
                } else if (destination.contains('/presence')) {
                  final type = body['type'] ?? '';
                  final displayName = body['displayName'] ?? '';
                  if (mounted) {
                    setState(() {
                      _messages.add(_ChatMessage(
                        id: 'sys-${DateTime.now().millisecondsSinceEpoch}',
                        senderName: 'Système',
                        content: type == 'JOIN' ? '$displayName a rejoint le chat' : '$displayName a quitté le chat',
                        messageType: 'SYSTEM',
                        timestamp: DateTime.tryParse(body['timestamp'] ?? ''),
                        isSystem: true,
                      ));
                    });
                    _scrollToBottom();
                  }
                }
              }
            }
          } catch (_) {
          }
        },
        onError: (_) {
          if (mounted) setState(() => _isConnected = false);
        },
        onDone: () {
          if (mounted) setState(() => _isConnected = false);
        },
      );
      
      _sendStompFrame({
        'command': 'CONNECT',
        'headers': {
          'Authorization': 'Bearer $token',
          'accept-version': '1.1,1.0',
          'heart-beat': '10000,10000',
        },
      });
      
      if (mounted) setState(() => _isConnected = true);
    } catch (_) {
      if (mounted) setState(() => _isConnected = false);
    }
  }

  void _sendStompFrame(Map<String, dynamic> frame) {
    _channel?.sink.add(json.encode(frame));
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
    if (text.trim().isEmpty || !_isConnected) return;
    
    _sendStompFrame({
      'command': 'SEND',
      'headers': {
        'destination': '/app/streams/${widget.streamId}/chat',
      },
      'body': json.encode({
        'content': text.trim(),
        'senderName': 'Vous',
      }),
    });
    _controller.clear();
  }

  void _quickReact(String emoji) {
    if (!_isConnected) return;
    _sendStompFrame({
      'command': 'SEND',
      'headers': {
        'destination': '/app/streams/${widget.streamId}/chat',
      },
      'body': json.encode({
        'content': emoji,
        'emoji': emoji,
        'senderName': 'Vous',
      }),
    });
  }

  void _copyMessage(String id, String text) {
    Clipboard.setData(ClipboardData(text: text));
    setState(() => _copiedId = id);
    Future.delayed(const Duration(seconds: 2), () {
      if (mounted) setState(() => _copiedId = null);
    });
  }

  String _formatTime(DateTime? dt) {
    if (dt == null) return '';
    return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')};
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A),
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).streamingChatTitle),
        backgroundColor: const Color(0xFF1E293B),
        foregroundColor: Colors.white,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Center(
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: _isConnected ? Colors.green.withOpacity(0.2) : Colors.red.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      _isConnected ? Icons.wifi : Icons.wifi_off,
                      color: _isConnected ? Colors.green : Colors.red,
                      size: 14,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      _isConnected ? 'En direct' : 'Déconnecté',
                      style: TextStyle(
                        color: _isConnected ? Colors.green : Colors.red,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Expanded(
                  child: ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(12),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final msg = _messages[index];
                      final isMe = msg.senderName == 'Vous';
                      final isReaction = msg.messageType == 'REACTION';
                      final isSystem = msg.isSystem == true;
                      
                      if (isSystem) {
                        return Center(
                          child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 8),
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            decoration: BoxDecoration(
                              color: Colors.amber.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Text(
                              msg.content,
                              style: const TextStyle(
                                color: Colors.amber,
                                fontSize: 12,
                              ),
                            ),
                          ),
                        );
                      }
                      
                      if (isReaction) {
                        return Center(
                          child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 4),
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            decoration: BoxDecoration(
                              color: Colors.purple.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Text(
                              '${msg.senderName} a réagi ${msg.emoji ?? msg.content}',
                              style: const TextStyle(
                                color: Colors.purple,
                                fontSize: 12,
                              ),
                            ),
                          ),
                        );
                      }
                      
                      return Align(
                        alignment: isMe ? Alignment.centerRight : Alignment.centerLeft,
                        child: Container(
                          margin: const EdgeInsets.symmetric(vertical: 4),
                          constraints: BoxConstraints(
                            maxWidth: MediaQuery.of(context).size.width * 0.75,
                          ),
                          child: Column(
                            crossAxisAlignment: isMe ? CrossAxisAlignment.end : CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  if (!isMe) ...[
                                    CircleAvatar(
                                      radius: 12,
                                      backgroundColor: Colors.pink.withOpacity(0.2),
                                      child: const Icon(Icons.person, size: 14, color: Colors.pink),
                                    ),
                                    const SizedBox(width: 6),
                                  ],
                                  Text(
                                    msg.senderName,
                                    style: TextStyle(
                                      color: Colors.grey.shade400,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    _formatTime(msg.timestamp ?? DateTime.now()),
                                    style: TextStyle(
                                      color: Colors.grey.shade600,
                                      fontSize: 9,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              GestureDetector(
                                onLongPress: () => _copyMessage(msg.id, msg.content),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                  decoration: BoxDecoration(
                                    color: isMe ? const Color(0xFFEC4899) : const Color(0xFF1E293B),
                                    borderRadius: BorderRadius.only(
                                      topLeft: const Radius.circular(16),
                                      topRight: const Radius.circular(16),
                                      bottomLeft: Radius.circular(isMe ? 16 : 4),
                                      bottomRight: Radius.circular(isMe ? 4 : 16),
                                    ),
                                    border: isMe ? null : Border.all(color: const Color(0xFF334155)),
                                  ),
                                  child: Text(
                                    msg.content,
                                    style: TextStyle(
                                      color: isMe ? Colors.white : Colors.white.withOpacity(0.9),
                                      fontSize: 14,
                                    ),
                                  ),
                                ),
                              ),
                              if (_copiedId == msg.id)
                                Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Text(
                                    'Copié !',
                                    style: TextStyle(color: Colors.green.shade400, fontSize: 10),
                                  ),
                                ),
                            ],
                          ),
                        );
                    },
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: const BoxDecoration(
                    color: Color(0xFF1E293B),
                    border: Border(top: BorderSide(color: Color(0xFF334155))),
                  ),
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: _reactions.map((emoji) {
                        return Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: GestureDetector(
                            onTap: () => _quickReact(emoji),
                            child: Container(
                              width: 36,
                              height: 36,
                              decoration: BoxDecoration(
                                color: const Color(0xFF0F172A),
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: const Color(0xFF334155)),
                              ),
                              child: Center(
                                child: Text(emoji, style: const TextStyle(fontSize: 16)),
                              ),
                            ),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
                ),
                Container(
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
                                hintText: 'Message...',
                                hintStyle: TextStyle(color: Colors.white.withOpacity(0.3)),
                                border: InputBorder.none,
                                contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        GestureDetector(
                          onTap: _isConnected && _controller.text.trim().isNotEmpty ? () => _sendMessage(_controller.text) : null,
                          child: Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              gradient: _isConnected && _controller.text.trim().isNotEmpty
                                  ? const LinearGradient(colors: [Color(0xFFEC4899), Color(0xFF8B5CF6)])
                                  : null,
                              color: (!_isConnected || _controller.text.trim().isEmpty) ? Colors.grey : null,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Icon(
                              _controller.text.trim().isEmpty ? Icons.hourglass_empty : Icons.send,
                              color: Colors.white,
                              size: 18,
                            ),
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
}

class _ChatMessage {
  final String id;
  final String senderName;
  final String content;
  final String messageType;
  final String? emoji;
  final DateTime? timestamp;
  final bool isSystem;

  _ChatMessage({
    required this.id,
    required this.senderName,
    required this.content,
    required this.messageType,
    this.emoji,
    this.timestamp,
    this.isSystem = false,
  });

  factory _ChatMessage.fromJson(Map<String, dynamic> json) {
    return _ChatMessage(
      id: json['id']?.toString() ?? '',
      senderName: json['senderName']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      messageType: json['messageType']?.toString() ?? 'TEXT',
      emoji: json['emoji']?.toString(),
      timestamp: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
      isSystem: json['isSystem'] == true,
    );
  }
}
