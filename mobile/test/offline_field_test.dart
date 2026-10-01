import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/local/offline_sync_manager.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/checkin/QrCheckinScreen.dart';
import 'package:discipolat_mobile/presentation/screens/asset_field/asset_field_screen.dart';

import 'helpers/pump_localized.dart';

/// §G5.7 — Les écrans terrain, en VRAIE panne réseau (aucune réponse HTTP),
/// mettent l'opération en file idempotente et le disent à l'utilisateur ;
/// en mode tenant LECTURE, ils refusent explicitement au lieu de simuler.

DioException _offlineOn(String path) => DioException(
    requestOptions: RequestOptions(path: path),
    type: DioExceptionType.connectionError);

class _FakeApi extends ApiService {
  _FakeApi({this.checkedOut = false}) : super(baseUrl: 'http://fake');
  final bool checkedOut;
  final List<String> postedPaths = [];
  static const itemId = '9a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d';
  static const soulId = '3f6f1a34-6f1e-4b6f-9b1c-2c2f9a1d0b11';

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    if (path == '/inventory/qr/resolve') {
      return Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 200,
          data: {
            'itemId': itemId,
            'nom': 'Sono MB',
            'statut': checkedOut ? 'AFFECTE' : 'DISPONIBLE',
            'qrToken': 'tok-1',
            if (checkedOut) ...{
              'checkedOut': true,
              'activeCheckoutId': 'c1',
              'borrowedByMemberId': soulId,
            },
          });
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postedPaths.add(path);
    // Terrain sans réseau : TOUTE écriture échoue sans réponse HTTP.
    throw _offlineOn(path);
  }
}

class _FakeSync extends Fake implements OfflineSyncManager {
  _FakeSync({this.accepts = true});
  final bool accepts;
  final List<String> checkinCodes = [];
  final List<String> checkouts = [];
  final List<String> returns = [];

  @override
  Future<bool> queueQrCheckin(String code) async {
    if (accepts) checkinCodes.add(code);
    return accepts;
  }

  @override
  Future<bool> queueAssetCheckout(
      {required String itemId, required String memberId, String condition = 'GOOD', String? notes}) async {
    if (accepts) checkouts.add(itemId);
    return accepts;
  }

  @override
  Future<bool> queueAssetReturn(
      {required String itemId, String condition = 'GOOD', String? notes}) async {
    if (accepts) returns.add(itemId);
    return accepts;
  }
}

/// Amène l'écran inventaire sur un actif résolu.
Future<_FakeApi> _pumpAsset(WidgetTester tester, OfflineSyncManager sync,
    {bool checkedOut = false}) async {
  final api = _FakeApi(checkedOut: checkedOut);
  await pumpLocalized(
      tester, AssetFieldScreen(apiService: api, syncManager: sync));
  await tester.pumpAndSettle();
  await tester.enterText(find.byType(TextField).first, 'tok-1');
  await tester.pumpAndSettle();
  await tester.tap(find.text('Retrouver l’actif'));
  await tester.pumpAndSettle();
  return api;
}

void main() {
  group('QrCheckinScreen hors-ligne', () {
    testWidgets('panne réseau → pointage fiché, message explicite',
        (tester) async {
      final api = _FakeApi();
      final sync = _FakeSync();
      await pumpLocalized(
          tester, QrCheckinScreen(apiService: api, syncManager: sync));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).first, 'discipolat:soul:abc');
      await tester.pumpAndSettle();
      await tester.tap(find.text('Pointer'));
      await tester.pumpAndSettle();

      expect(api.postedPaths, ['/members/qr-checkin']); // a bien tenté le réel
      expect(sync.checkinCodes, ['discipolat:soul:abc']);
      expect(find.textContaining('Enregistré hors-ligne'), findsOneWidget);
    });

    testWidgets('tenant en LECTURE → refus explicite, rien n\'est simulé',
        (tester) async {
      final sync = _FakeSync(accepts: false);
      await pumpLocalized(
          tester, QrCheckinScreen(apiService: _FakeApi(), syncManager: sync));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).first, 'code-1');
      await tester.pumpAndSettle();
      await tester.tap(find.text('Pointer'));
      await tester.pumpAndSettle();

      expect(sync.checkinCodes, isEmpty);
      expect(find.textContaining('lecture seule'), findsOneWidget);
    });
  });

  group('AssetFieldScreen hors-ligne', () {
    testWidgets('sortie de matériel → ASSET_CHECKOUT en file idempotente',
        (tester) async {
      final api = await _pumpAsset(tester, _FakeSync());
      await tester.enterText(find.byKey(const Key('assignee-code-field')),
          'discipolat:soul:${_FakeApi.soulId}');
      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pumpAndSettle();
      await tester.tap(find.byIcon(Icons.logout));
      await tester.pumpAndSettle();

      expect(api.postedPaths, ['/assets/${_FakeApi.itemId}/checkout']);
      await tester.dragUntilVisible(
          find.textContaining('Enregistré hors-ligne'),
          find.byType(Scrollable).first,
          const Offset(0, -200));
      expect(find.textContaining('Enregistré hors-ligne'), findsOneWidget);
    });

    testWidgets('retour → ASSET_RETURN en file, jamais perdu', (tester) async {
      final sync = _FakeSync();
      final api = await _pumpAsset(tester, sync, checkedOut: true);
      await tester.dragUntilVisible(find.byIcon(Icons.login),
          find.byType(Scrollable).first, const Offset(0, -200));
      await tester.tap(find.byIcon(Icons.login));
      await tester.pumpAndSettle();

      expect(api.postedPaths, ['/assets/${_FakeApi.itemId}/return']);
      expect(sync.returns, [_FakeApi.itemId]);
      expect(find.textContaining('Enregistré hors-ligne'), findsOneWidget);
    });
  });
}
