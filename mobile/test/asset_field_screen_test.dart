import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/realtime_bus_service.dart';
import 'package:discipolat_mobile/presentation/screens/asset_field/asset_field_screen.dart';

import 'helpers/pump_localized.dart';

/// §G5.6 — Fake « Inventaire terrain » : mêmes routes et mêmes formes que le
/// backend réel (résolution QR, checkout, retour, photo de dommage, QR code).
class _FakeApiService extends ApiService {
  _FakeApiService({this.notFound = false, this.checkedOut = false})
      : super(baseUrl: 'http://fake');
  final bool notFound;

  /// §G5.8 — mutable : un autre appareil peut avoir sorti l'objet entre-temps.
  bool checkedOut;

  final List<String> postedPaths = [];
  final List<Map<String, dynamic>> postedBodies = [];
  final List<String> multipartPaths = [];
  final List<String> getPaths = [];

  static const String itemId = '9a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d';
  static const String soulId = '3f6f1a34-6f1e-4b6f-9b1c-2c2f9a1d0b11';

  DioException _err(String path, int status) => DioException(
      requestOptions: RequestOptions(path: path),
      response:
          Response(requestOptions: RequestOptions(path: path), statusCode: status));

  Response _ok(String path, dynamic data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

  Map<String, dynamic> _summary() => {
        'itemId': itemId,
        'nom': 'Sono MB',
        'categorie': 'TECHNIQUE',
        'statut': checkedOut ? 'AFFECTE' : 'DISPONIBLE',
        'quantite': 1,
        'quantiteDisponible': checkedOut ? 0 : 1,
        'qrToken': 'tok-1',
        if (checkedOut) ...{
          'checkedOut': true,
          'activeCheckoutId': 'c1',
          'borrowedByMemberId': soulId,
        },
      };

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    if (path == '/inventory/qr/resolve') {
      if (notFound) throw _err(path, 404);
      return _ok(path, _summary());
    }
    if (path == '/inventory/$itemId/qr-code') {
      return _ok(path, {
        'itemId': itemId,
        'content': 'discipolat:asset:$itemId:tok-1',
        // PNG 1×1 valide → Image.memory du bas-sheet se décode réellement.
        'qrPngDataUrl':
            'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
      });
    }
    return _ok(path, {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postedPaths.add(path);
    postedBodies.add(Map<String, dynamic>.from((data ?? {}) as Map));
    return _ok(path, {'id': 'c1', 'status': 'CHECKED_OUT'});
  }

  @override
  Future<Response> postMultipart(
    String path, {
    required String fieldName,
    required Uint8List fileBytes,
    required String filename,
    Map<String, dynamic>? data,
    DioMediaType? mimeType,
  }) async {
    multipartPaths.add(path);
    return _ok(path, {'photoPath': 'asset-damage/t/$filename'});
  }
}

Future<_FakeApiService> resolveAsset(WidgetTester tester,
    {_FakeApiService? api,
    String code = 'tok-1',
    Stream<RealtimeEvent>? realtimeEvents}) async {
  final service = api ?? _FakeApiService();
  await pumpLocalized(tester,
      AssetFieldScreen(apiService: service, realtimeEvents: realtimeEvents));
  await tester.pumpAndSettle();
  await tester.enterText(find.byType(TextField).first, code);
  await tester.tap(find.text('Retrouver l\u2019actif'));
  await tester.pumpAndSettle();
  return service;
}

void main() {
  group('AssetFieldScreen — résolution QR (G5.6)', () {
    testWidgets('manual token resolves to the real item card', (tester) async {
      await resolveAsset(tester);
      expect(find.text('Sono MB'), findsOneWidget);
      expect(find.text('État : DISPONIBLE'), findsOneWidget);
      // objet non sorti → carte de sortie avec destinataire
      expect(find.text('Remis à (membre)'), findsOneWidget);
    });

    testWidgets('unknown token shows not-found banner', (tester) async {
      await resolveAsset(tester, api: _FakeApiService(notFound: true));
      expect(find.text('Actif introuvable'), findsOneWidget);
    });

    testWidgets('checked-out item shows borrower and return card', (tester) async {
      await resolveAsset(tester, api: _FakeApiService(checkedOut: true));
      expect(find.textContaining('Emprunté par'), findsOneWidget);
      expect(find.text('Retour de matériel'), findsOneWidget);
      expect(find.text('Photo du dommage'), findsOneWidget);
    });

    testWidgets('asset QR code sheet renders the signed PNG', (tester) async {
      await resolveAsset(tester);
      await tester.tap(find.text('Afficher le QR de cet actif'));
      await tester.pumpAndSettle();
      expect(find.byType(Image), findsOneWidget);
    });
  });

  group('AssetFieldScreen — checkout / retour (G5.6)', () {
    testWidgets('checkout posts real endpoint with pasted soul code', (tester) async {
      final api = await resolveAsset(tester);
      expect(find.text('Sortie de matériel'), findsOneWidget);

      await tester.enterText(find.byKey(const Key('assignee-code-field')),
          'discipolat:soul:${_FakeApiService.soulId}');
      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.logout));
      await tester.pumpAndSettle();

      expect(api.postedPaths.first, '/assets/${_FakeApiService.itemId}/checkout');
      expect(api.postedBodies.first['memberId'], _FakeApiService.soulId);
      await tester.dragUntilVisible(
          find.text('Sortie enregistrée'),
          find.byType(Scrollable).first, const Offset(0, -200));
      expect(find.text('Sortie enregistrée'), findsOneWidget);
    });

    testWidgets('checkout stays disabled without an assignee', (tester) async {
      await resolveAsset(tester);
      final button = tester.widget<ButtonStyleButton>(find.ancestor(
          of: find.byIcon(Icons.logout),
          matching: find.byWidgetPredicate((w) => w is ButtonStyleButton)).first);
      expect(button.onPressed, isNull);
    });

    testWidgets('return posts condition DAMAGED after choosing it', (tester) async {
      final api =
          await resolveAsset(tester, api: _FakeApiService(checkedOut: true));

      await tester.tap(find.text('Bon état'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Endommagé').last);
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.login));
      await tester.pumpAndSettle();

      expect(api.postedPaths, contains('/assets/${_FakeApiService.itemId}/return'));
      final body = api.postedBodies.last;
      expect(body['condition'], 'DAMAGED');
      expect(find.text('Retour enregistré'), findsOneWidget);
    });
  });

  group('AssetFieldScreen — synchro temps réel (G5.8)', () {
    int resolvesOf(_FakeApiService api) =>
        api.getPaths.where((p) => p == '/inventory/qr/resolve').length;

    testWidgets('AssetCheckedOut distant re-charge la fiche sans action',
        (tester) async {
      final bus = StreamController<RealtimeEvent>();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await resolveAsset(tester, api: api, realtimeEvents: bus.stream);
      expect(find.text('État : DISPONIBLE'), findsOneWidget);
      final before = resolvesOf(api);

      // Un autre appareil (web) sort l'objet → outbox → firehose du tenant.
      api.checkedOut = true;
      bus.add(RealtimeEvent(
        eventId: 1,
        eventType: 'AssetCheckedOut',
        aggregateId: _FakeApiService.itemId,
      ));
      await tester.pumpAndSettle();

      expect(resolvesOf(api), greaterThan(before));
      expect(find.text('État : AFFECTE'), findsOneWidget);
    });

    testWidgets('événement d\u2019un autre actif ne touche pas la fiche',
        (tester) async {
      final bus = StreamController<RealtimeEvent>();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await resolveAsset(tester, api: api, realtimeEvents: bus.stream);
      final before = resolvesOf(api);

      bus.add(RealtimeEvent(
        eventId: 2,
        eventType: 'AssetCheckedOut',
        aggregateId: '00000000-0000-0000-0000-000000000000',
      ));
      await tester.pumpAndSettle();

      expect(resolvesOf(api), before);
      expect(find.text('État : DISPONIBLE'), findsOneWidget);
    });

    testWidgets('trou de delta (fullRefresh) → rechargement total de la fiche',
        (tester) async {
      final bus = StreamController<RealtimeEvent>();
      addTearDown(bus.close);
      final api = _FakeApiService();
      await resolveAsset(tester, api: api, realtimeEvents: bus.stream);
      final before = resolvesOf(api);

      // Après coupure, le bus signale un trou : type non suivi d\u2019un mouvement
      // d\u2019inventaire connu, mais fullRefresh impose le rechargement.
      api.checkedOut = true;
      bus.add(RealtimeEvent(
        eventId: 99,
        eventType: 'SomeFutureEvent',
        fullRefresh: true,
      ));
      await tester.pumpAndSettle();

      expect(resolvesOf(api), greaterThan(before));
      expect(find.text('État : AFFECTE'), findsOneWidget);
    });
  });
}
