import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../services/api_service.dart';

/// Real-time WebSocket service for messaging.
/// Connects via STOMP over SockJS and manages subscriptions.
class WebSocketService {
  WebSocketChannel? _channel;
  final Map<String, StreamSubscription> _subscriptions = {};
  final StreamController<Map<String, dynamic>> _messageController =
      StreamController<Map<String, dynamic>>.broadcast();

  bool _isConnected = false;

  Stream<Map<String, dynamic>> get messageStream => _messageController.stream;
  bool get isConnected => _isConnected;

  /// Connect to WebSocket endpoint
  Future<void> connect() async {
    if (_isConnected) return;

    try {
      final apiService = ApiService();
      final baseUrl = apiService.dio.options.baseUrl;
      final wsUrl = '${baseUrl.replaceFirst('http', 'ws')}/ws';

      _channel = WebSocketChannel.connect(Uri.parse(wsUrl));

      // Send STOMP CONNECT frame
      _channel!.sink.add(
          'CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\x00');

      _channel!.stream.listen(
        (data) {
          _handleStompFrame(data.toString());
        },
        onDone: () {
          _isConnected = false;
          _reconnect();
        },
        onError: (error) {
          _isConnected = false;
        },
      );
    } catch (e) {
      _isConnected = false;
    }
  }

  void _handleStompFrame(String frame) {
    if (frame.contains('CONNECTED')) {
      _isConnected = true;
      return;
    }

    if (frame.startsWith('MESSAGE') || frame.startsWith('message')) {
      // Extract body from STOMP frame
      final bodyStart = frame.indexOf('\n\n');
      if (bodyStart != -1) {
        final body = frame.substring(bodyStart + 2).replaceAll('\x00', '').trim();
        if (body.isNotEmpty) {
          try {
            final data = jsonDecode(body) as Map<String, dynamic>;
            _messageController.add(data);
          } catch (e) {
            // Not JSON, skip
          }
        }
      }
    }
  }

  /// Subscribe to a conversation
  void subscribeToConversation(String conversationId) {
    if (!_isConnected || _channel == null) return;

    final destination = '/topic/conversations/$conversationId';
    final subId = 'sub-$conversationId';

    _channel!.sink.add(
        'SUBSCRIBE\nid:$subId\ndestination:$destination\n\n\x00');

    // Subscribe to typing indicator
    final typingDestination = '/topic/conversations/$conversationId/typing';
    final typingSubId = 'typing-$conversationId';
    _channel!.sink.add(
        'SUBSCRIBE\nid:$typingSubId\ndestination:$typingDestination\n\n\x00');
  }

  /// Unsubscribe from a conversation
  void unsubscribeFromConversation(String conversationId) {
    if (!_isConnected || _channel == null) return;

    _channel!.sink.add('UNSUBSCRIBE\nid:sub-$conversationId\n\n\x00');
    _channel!.sink.add('UNSUBSCRIBE\nid:typing-$conversationId\n\n\x00');
  }

  /// Send a message to a conversation
  void sendMessage(String conversationId, String content, {String? replyToId}) {
    if (!_isConnected || _channel == null) return;

    final payload = jsonEncode({
      'content': content,
      if (replyToId != null) 'replyToId': replyToId,
    });

    _channel!.sink.add(
        'SEND\ndestination:/app/conversations/$conversationId/send\ncontent-type:application/json\n\n$payload\x00');
  }

  /// Send typing indicator
  void sendTypingIndicator(String conversationId, bool isTyping) {
    if (!_isConnected || _channel == null) return;

    final payload = jsonEncode({'typing': isTyping});

    _channel!.sink.add(
        'SEND\ndestination:/app/conversations/$conversationId/typing\ncontent-type:application/json\n\n$payload\x00');
  }

  /// Send read receipt
  void sendReadReceipt(String conversationId, String lastMessageId) {
    if (!_isConnected || _channel == null) return;

    final payload = jsonEncode({'lastReadMessageId': lastMessageId});

    _channel!.sink.add(
        'SEND\ndestination:/app/conversations/$conversationId/read\ncontent-type:application/json\n\n$payload\x00');
  }

  void _reconnect() {
    Future.delayed(const Duration(seconds: 3), () {
      if (!_isConnected) {
        connect();
      }
    });
  }

  /// Disconnect from WebSocket
  void disconnect() {
    for (final sub in _subscriptions.values) {
      sub.cancel();
    }
    _subscriptions.clear();

    _channel?.sink.close();
    _channel = null;
    _isConnected = false;
  }

  void dispose() {
    disconnect();
    _messageController.close();
  }
}
