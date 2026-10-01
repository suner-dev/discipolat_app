import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/checkin/QrCheckinScreen.dart';

import 'helpers/pump_localized.dart';

/// §G5.6 — Fake du pointage QR : mêmes routes/formes que le backend réel
/// (POST /members/qr-checkin, /members/qr-checkin/scan, GET /members/me/qr-code).
class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false}) : super(baseUrl: 'http://fake');
  final bool fail;

  final List<String> postedPaths = [];
  final List<Map<String, dynamic>> postedBodies = [];
  final List<String> scannedPaths = [];

  DioException _err(String path, {int status = 400}) => DioException(
      requestOptions: RequestOptions(path: path),
      response: Response(
          requestOptions: RequestOptions(path: path),
          statusCode: status,
          data: {'error': 'x'}));

  Response _ok(String path, dynamic data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    if (fail) throw _err(path);
    postedPaths.add(path);
    postedBodies.add(Map<String, dynamic>.from((data ?? {}) as Map));
    if (path == '/members/qr-checkin') {
      return _ok(path, {'success': true, 'message': 'Présence enregistrée', 'soulId': 's1'});
    }
    return _ok(path, {'ok': true});
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
    if (fail) throw _err(path);
    scannedPaths.add(path);
    return _ok(path, {'success': true, 'message': 'Présence enregistrée', 'soulId': 's1'});
  }

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    if (fail) throw _err(path, status: 401);
    if (path == '/members/me/qr-code') {
      // PNG 1×1 valide → l'Image.memory du bas-sheet se décode réellement.
      return _ok(path, {
        'soulId': 's1',
        'data': 'discipolat:soul:s1',
        'qrPngDataUrl':
            'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
      });
    }
    return _ok(path, {});
  }
}

void main() {
  group('QrCheckinScreen — pointage QR réel (G5.6)', () {
    testWidgets('renders localized title and manual code field', (tester) async {
      await pumpLocalized(tester, QrCheckinScreen(apiService: _FakeApiService()));
      await tester.pumpAndSettle();
      expect(find.text('Pointage QR'), findsWidgets);
      expect(find.textContaining('Scannez le QR'), findsOneWidget);
      expect(find.text('Prendre une photo du QR'), findsOneWidget);
    });

    testWidgets('submitting a scanned code posts to /members/qr-checkin', (tester) async {
      final api = _FakeApiService();
      await pumpLocalized(tester, QrCheckinScreen(apiService: api));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'discipolat:soul:3f6f1a34-6f1e-4b6f-9b1c-2c2f9a1d0b11');
      await tester.pumpAndSettle();
      await tester.tap(find.text('Pointer'));
      await tester.pumpAndSettle();
      expect(api.postedPaths, ['/members/qr-checkin']);
      expect(api.postedBodies.single['code'],
          'discipolat:soul:3f6f1a34-6f1e-4b6f-9b1c-2c2f9a1d0b11');
      expect(find.text('Présence enregistrée'), findsOneWidget);
    });

    testWidgets('check-in button inert while code is empty', (tester) async {
      final api = _FakeApiService();
      await pumpLocalized(tester, QrCheckinScreen(apiService: api));
      await tester.pumpAndSettle();
      // Bouton désactivé tant que le champ est vide → aucun appel réseau.
      await tester.tap(find.text('Pointer'));
      await tester.pumpAndSettle();
      expect(api.postedPaths, isEmpty);
    });

    testWidgets('API failure shows error banner', (tester) async {
      await pumpLocalized(
          tester, QrCheckinScreen(apiService: _FakeApiService(fail: true)));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField), 'some-code');
      await tester.pumpAndSettle();
      await tester.tap(find.text('Pointer'));
      await tester.pumpAndSettle();

      expect(find.text('Erreur'), findsOneWidget);
    });

    testWidgets('my-QR presentation renders the PNG from /members/me/qr-code',
        (tester) async {
      final api = _FakeApiService();
      await pumpLocalized(tester, QrCheckinScreen(apiService: api));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.qr_code_2));
      await tester.pumpAndSettle();

      expect(find.text('Mon QR à présenter'), findsOneWidget);
      // Image.memory du QR décodé (PNG 1×1 valide injecté par le faux API)
      expect(find.byType(Image), findsOneWidget);
    });
  });
}
