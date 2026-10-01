import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../../tenant_config.dart';
import 'api_config.dart';
import 'auth_service.dart';

/// §G5.8 — Bus temps réel MOBILE : abonné au firehose du tenant
/// `/topic/tenant:{id}/events` (push outbox immédiat, web ↔ mobile < 5 s).
///
/// Ce que le web modifie arrive ici et réciproquement (les mutations mobiles
/// passent par l'API ou la file §G5.7 → outbox → firehose). Les écrans actifs
/// écoutent [events] et re-rendent ; un TROU de delta ( eventId non consécutif
/// après coupure ) déclenche [RealtimeEvent.fullRefresh] = rafraîchissement
/// total, jamais de donnée périmée silencieuse.
class RealtimeEvent {
  final int eventId;
  final String eventType;
  final String? aggregateId;
  final String? spaceId;
  final Map<String, dynamic> payload;

  /// True quand cet événement signale un trou de delta : l'écran doit
  /// tout recharger (pas seulement sa section).
  final bool fullRefresh;

  RealtimeEvent({
    required this.eventId,
    required this.eventType,
    this.aggregateId,
    this.spaceId,
    this.payload = const {},
    this.fullRefresh = false,
  });

  factory RealtimeEvent.fromJson(Map<String, dynamic> j) => RealtimeEvent(
        eventId: int.tryParse('${j['eventId']}') ?? -1,
        eventType: '${j['eventType'] ?? ''}',
        aggregateId: j['aggregateId']?.toString(),
        spaceId: j['spaceId']?.toString(),
        payload: j['payload'] is Map
            ? Map<String, dynamic>.from(j['payload'] as Map)
            : const {},
      );
}

/// Garde de delta pure (testable sans réseau) : déduplique les rejeux et
/// signale les trous.
class RealtimeDeltaGuard {
  int lastEventId = -1;

  /// Retourne null si l'événement est un doublon (déjà vu) ; sinon renvoie
  /// l'événement, marqué `fullRefresh` si un trou de delta est détecté.
  RealtimeEvent? accept(RealtimeEvent e) {
    if (e.eventId >= 0 && e.eventId <= lastEventId) return null; // rejeu
    final gap = lastEventId >= 0 &&
        e.eventId >= 0 &&
        e.eventId > lastEventId + 1; // trous 11-13 perdus…
    if (e.eventId >= 0) lastEventId = e.eventId;
    if (!gap) return e;
    return RealtimeEvent(
      eventId: e.eventId,
      eventType: e.eventType,
      aggregateId: e.aggregateId,
      spaceId: e.spaceId,
      payload: e.payload,
      fullRefresh: true,
    );
  }
}

class RealtimeBus {
  RealtimeBus({StompClient Function(StompConfig)? clientFactory})
      : _clientFactory = clientFactory;

  StompClient? _client;
  final RealtimeDeltaGuard _guard = RealtimeDeltaGuard();
  final StreamController<RealtimeEvent> _controller =
      StreamController<RealtimeEvent>.broadcast();
  String? _tenantId;
  bool _isDisposed = false;

  /// Émissions : événements dédupliqués (marqués fullRefresh sur trou).
  Stream<RealtimeEvent> get events => _controller.stream;
  bool get isConnected => _client != null && _connected;
  bool _connected = false;

  /// Usine de client injectable (tests) — par défaut stomp_dart_client.
  final StompClient? Function(StompConfig config)? _clientFactory;

  /// Connexion au firehose. Idempotente : re-call avec un autre tenant
  /// (changement d'église) → réabonnement sur le nouveau canal.
  Future<void> connect({
    required String token,
    required String tenantId,
  }) async {
    if (_isDisposed) return;
    if (_tenantId == tenantId && _connected) return;
    _teardownClient();
    _tenantId = tenantId;

    final config = StompConfig.sockJS(
      url: '${_wsBase()}/ws-church',
      onConnect: (frame) {
        _connected = true;
        live.value = true;
        _client?.subscribe(
          destination: '/topic/tenant:$tenantId/events',
          callback: _onFrame,
        );
      },
      onDisconnect: (frame) {
        _connected = false;
        live.value = false;
      },
      onStompError: (frame) => _connected = false,
      onWebSocketError: (e) => _connected = false,
      stompConnectHeaders: {
        'Authorization': 'Bearer $token',
        'token': token,
      },
      connectionTimeout: const Duration(seconds: 5),
      heartbeatIncoming: const Duration(seconds: 10),
      heartbeatOutgoing: const Duration(seconds: 10),
      reconnectDelay: const Duration(seconds: 3),
    );

    _client = _clientFactory?.call(config) ?? StompClient(config: config);
    _client!.activate();
  }

  /// Traitement d'une frame du firehose (exposé pour tests : même chemin que
  /// le callback STOMP réel).
  void handleRawFrame(String? body) {
    if (body == null || body.isEmpty) return;
    try {
      final json = jsonDecode(body);
      if (json is! Map) return;
      final evt = RealtimeEvent.fromJson(Map<String, dynamic>.from(json));
      final accepted = _guard.accept(evt);
      if (accepted != null && !_controller.isClosed) {
        _controller.add(accepted);
      }
    } catch (e) {
      debugPrint('[RealtimeBus] frame ignorée: $e');
    }
  }

  void _onFrame(StompFrame frame) => handleRawFrame(frame.body);

  void _teardownClient() {
    _connected = false;
    try {
      _client?.deactivate();
    } catch (_) {}
    _client = null;
  }

  Future<void> dispose() async {
    _isDisposed = true;
    _teardownClient();
    await _controller.close();
  }

  /// Base ws(s):// dérivée de la config API (comme StompService).
  static String _wsBase() {
    final apiBase = ApiConfig.baseUrl;
    final wsBase = apiBase
        .replaceFirst(RegExp(r'^https?://'), '')
        .replaceFirst(RegExp(r'/api/v1$'), '');
    return apiBase.startsWith('https') ? 'wss://$wsBase' : 'ws://$wsBase';
  }

  // ========== Intégration app ==========

  static final RealtimeBus instance = RealtimeBus();

  /// Indicateur de fraîcheur global (point « Direct » des écrans branchés).
  static final ValueNotifier<bool> live = ValueNotifier<bool>(false);

  /// Branche le bus global (après login / changement de tenant). Le tenant
  /// courant vient de TenantConfig (même source que les headers REST).
  static Future<void> startFor() async {
    try {
      final token = await AuthService().token;
      if (token == null || token.isEmpty) return;
      final tenantId =
          TenantConfig.currentOrgId ?? await TenantConfig.resolveOrgId();
      if (tenantId == null || tenantId.isEmpty) {
        debugPrint('[RealtimeBus] tenant inconnu — bus non démarré');
        return;
      }
      await instance.connect(token: token, tenantId: tenantId);
      live.value = instance.isConnected;
    } catch (e) {
      // Login test / storage indisponible : le temps réel est best-effort,
      // les écrans restent fonctionnels via REST + file §G5.7.
      debugPrint('[RealtimeBus] démarrage ignoré: $e');
    }
  }

  /// Coupure propre (logout) : plus de subscription ni de callbacks.
  static void stop() {
    instance._teardownClient();
    live.value = false;
  }
}
