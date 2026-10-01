import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import 'package:discipolat_mobile/data/services/realtime_bus_service.dart';

/// §G5.8 — Contrôles du bus temps réel mobile (logique pure, sans réseau) :
/// déduplication des rejeux, détection de trou de delta → rafraîchissement
/// total, abonnement au firehose scopé tenant avec le JWT.

/// Client STOMP factice : capture la config, expose subscribe/activate.
class _FakeStompClient implements StompClient {
  _FakeStompClient(this.capturedConfig);
  final StompConfig capturedConfig;
  final List<String> subscribedDestinations = [];
  bool activated = false;

  @override
  void activate() {
    activated = true;
  }

  @override
  StompUnsubscribe subscribe({
    required String destination,
    required StompFrameCallback callback,
    Map<String, String>? headers,
  }) {
    subscribedDestinations.add(destination);
    return ({unsubscribeHeaders}) {};
  }

  @override
  void deactivate() {}

  @override
  dynamic noSuchMethod(Invocation invocation) => null;
}

void main() {
  group('RealtimeDeltaGuard', () {
    RealtimeEvent evt(int id, [String type = 'TaskAssigned']) =>
        RealtimeEvent(eventId: id, eventType: type);

    test('accepte une séquence continue', () {
      final g = RealtimeDeltaGuard();
      expect(g.accept(evt(1))?.fullRefresh, isFalse);
      expect(g.accept(evt(2))?.fullRefresh, isFalse);
      expect(g.accept(evt(3))?.fullRefresh, isFalse);
    });

    test('ignore les rejeux (eventId <= dernier reçu)', () {
      final g = RealtimeDeltaGuard();
      g.accept(evt(5));
      expect(g.accept(evt(5)), isNull);
      expect(g.accept(evt(3)), isNull);
    });

    test('trou de delta → événement marqué fullRefresh', () {
      final g = RealtimeDeltaGuard();
      g.accept(evt(10));
      final after = g.accept(evt(14));
      expect(after, isNotNull);
      expect(after!.fullRefresh, isTrue);
      // La séquence reprend normalement ensuite.
      expect(g.accept(evt(15))?.fullRefresh, isFalse);
    });
  });

  group('RealtimeBus', () {
    test('se connecte au firehose scopé tenant avec le JWT', () async {
      _FakeStompClient? fake;
      final bus = RealtimeBus(
        clientFactory: (config) => fake = _FakeStompClient(config),
      );
      await bus.connect(token: 'jwt-1', tenantId: 'tenant-X');
      expect(fake!.activated, isTrue);

      // Simule la réponse CONNECTED du serveur → abonnement.
      fake!.capturedConfig.onConnect(StompFrame(command: 'CONNECTED'));
      expect(fake!.subscribedDestinations, ['/topic/tenant:tenant-X/events']);

      // Headers STOMP : le backend refuse CONNECT sans JWT valide.
      expect(fake!.capturedConfig.stompConnectHeaders?['token'], 'jwt-1');
      await bus.dispose();
    });

    test('handleRawFrame émet, déduplique et signale les trous', () async {
      final bus = RealtimeBus(
        clientFactory: (config) => _FakeStompClient(config),
      );
      final received = <RealtimeEvent>[];
      bus.events.listen(received.add);

      String frame(int id, String type) => jsonEncode({
            'eventId': id,
            'eventType': type,
            'aggregateType': 'TASK',
            'aggregateId': 'a-$id',
            'payload': {'k': 'v'},
          });

      bus.handleRawFrame(frame(1, 'TaskAssigned'));
      bus.handleRawFrame(frame(1, 'TaskAssigned')); // rejeu → rien
      bus.handleRawFrame(frame(4, 'TaskCompleted')); // trou 2-3 → fullRefresh
      bus.handleRawFrame('pas-du-json');
      bus.handleRawFrame(null);
      await Future.delayed(Duration.zero);

      expect(received.length, 2);
      expect(received[0].fullRefresh, isFalse);
      expect(received[0].eventType, 'TaskAssigned');
      expect(received[1].fullRefresh, isTrue);
      expect(received[1].aggregateId, 'a-4');
      await bus.dispose();
    });

    test('reconnexion sur un autre tenant réabonne le nouveau canal',
        () async {
      final configs = <_FakeStompClient>[];
      final bus = RealtimeBus(
        clientFactory: (config) {
          final c = _FakeStompClient(config);
          configs.add(c);
          return c;
        },
      );
      await bus.connect(token: 'jwt', tenantId: 'T1');
      configs.last.capturedConfig.onConnect(StompFrame(command: 'CONNECTED'));
      await bus.connect(token: 'jwt', tenantId: 'T2');
      configs.last.capturedConfig.onConnect(StompFrame(command: 'CONNECTED'));
      await Future.delayed(Duration.zero);

      expect(configs.length, 2);
      expect(configs[0].subscribedDestinations, ['/topic/tenant:T1/events']);
      expect(configs[1].subscribedDestinations, ['/topic/tenant:T2/events']);
      await bus.dispose();
    });
  });
}
