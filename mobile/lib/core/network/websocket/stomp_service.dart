import 'dart:async';
import 'dart:convert';
import 'package:stomp_dart_client/stomp.dart';
import 'package:stomp_dart_client/stomp_config.dart';
import 'package:stomp_dart_client/stomp_frame.dart';

/// DTO correspondant au VoiceNotificationDTO du backend.
class VoiceNotificationDTO {
  final String id;
  final String title;
  final String body;
  final String timestamp;
  final String? targetRole;
  final String? targetUserId;
  final String? actionUrl;
  final String priority;
  final String locale;

  VoiceNotificationDTO({
    required this.id,
    required this.title,
    required this.body,
    required this.timestamp,
    this.targetRole,
    this.targetUserId,
    this.actionUrl,
    required this.priority,
    required this.locale,
  });

  factory VoiceNotificationDTO.fromJson(Map<String, dynamic> json) {
    return VoiceNotificationDTO(
      id: json['id'] ?? '',
      title: json['title'] ?? '',
      body: json['body'] ?? '',
      timestamp: json['timestamp'] ?? '',
      targetRole: json['targetRole'],
      targetUserId: json['targetUserId'],
      actionUrl: json['actionUrl'],
      priority: json['priority'] ?? 'NORMAL',
      locale: json['locale'] ?? 'fr-FR',
    );
  }

  String get speakText => '$title. $body';
}

typedef VoiceNotificationCallback = void Function(VoiceNotificationDTO notification);

/// Service STOMP pour la connexion WebSocket aux notifications vocales.
class StompService {
  StompClient? _client;
  final String _token;
  final String _userId;
  final String? _activeRole;

  final List<StreamSubscription> _subscriptions = [];
  final List<VoiceNotificationCallback> _callbacks = [];

  bool _isConnected = false;
  bool _isDisposed = false;

  StompService({
    required String token,
    required String userId,
    String? activeRole,
  })  : _token = token,
        _userId = userId,
        _activeRole = activeRole;

  bool get isConnected => _isConnected;

  void addCallback(VoiceNotificationCallback callback) {
    _callbacks.add(callback);
  }

  void removeCallback(VoiceNotificationCallback callback) {
    _callbacks.remove(callback);
  }

  void connect() {
    if (_isDisposed) return;

    _client = StompClient(
      config: StompConfig.sockJS(
        url: '${_getBaseUrl()}/ws-church',
        onConnect: _onConnect,
        onDisconnect: _onDisconnect,
        onStompError: _onStompError,
        onWebSocketError: _onWebSocketError,
        stompConnectHeaders: {
          'Authorization': 'Bearer $_token',
          'token': _token,
        },
        connectionTimeout: const Duration(seconds: 5),
        heartbeatIncoming: const Duration(seconds: 4),
        heartbeatOutgoing: const Duration(seconds: 4),
        reconnectDelay: const Duration(seconds: 5),
      ),
    );

    _client!.activate();
  }

  void _onConnect(StompFrame frame) {
    if (_isDisposed) return;
    _isConnected = true;

    final allSub = _client!.subscribe(
      destination: '/topic/voice/all',
      callback: (frame) => _handleMessage(frame),
    );
    _subscriptions.add(allSub);

    if (_activeRole != null && _activeRole!.isNotEmpty) {
      final roleSub = _client!.subscribe(
        destination: '/topic/voice/role/$_activeRole',
        callback: (frame) => _handleMessage(frame),
      );
      _subscriptions.add(roleSub);
    }

    final userSub = _client!.subscribe(
      destination: '/topic/voice/user/$_userId',
      callback: (frame) => _handleMessage(frame),
    );
    _subscriptions.add(userSub);
  }

  void _onDisconnect(StompFrame frame) {
    _isConnected = false;
  }

  void _onStompError(StompFrame frame) {
    print('[VoiceWS] Erreur STOMP: ${frame.body}');
    _isConnected = false;
  }

  void _onWebSocketError(dynamic error) {
    print('[VoiceWS] Erreur WebSocket: $error');
    _isConnected = false;
  }

  void _handleMessage(StompFrame frame) {
    if (frame.body == null) return;
    try {
      final json = jsonDecode(frame.body!) as Map<String, dynamic>;
      final notification = VoiceNotificationDTO.fromJson(json);
      for (final callback in _callbacks) {
        callback(notification);
      }
    } catch (e) {
      print('[VoiceWS] Erreur parsing: $e');
    }
  }

  void disconnect() {
    _isDisposed = true;
    for (final sub in _subscriptions) {
      try { sub.cancel(); } catch (_) {}
    }
    _subscriptions.clear();
    _client?.deactivate();
    _client = null;
    _isConnected = false;
  }

  void pause() {}

  void resume() {
    if (!_isConnected && !_isDisposed) {
      connect();
    }
  }

  String _getBaseUrl() {
    return 'http://localhost:8080';
  }
}
