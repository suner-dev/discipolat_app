import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/finances/finance_screen.dart';

/// ApiService factice : renvoie les transactions / stats et enregistre les POST / DELETE.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postDatas.add(data is Map<String, dynamic> ? data : null);
    return _json(path, {});
  }

  @override
  Future<Response> delete(String path) async {
    deletePaths.add(path);
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

final _stats = {
  'annee': 2026,
  'totalRecettes': 1500,
  'totalDepenses': 600,
  'solde': 900,
  'parMois': <dynamic>[],
  'recettesParCategorie': <dynamic>[],
  'depensesParCategorie': <dynamic>[],
};

final _transactions = [
  {
    'id': 'tx-1',
    'type': 'RECETTE',
    'categorie': 'DIME',
    'montant': 1500,
    'description': 'Dîmes du dimanche',
    'dateTransaction': '2026-08-17',
  },
  {
    'id': 'tx-2',
    'type': 'DEPENSE',
    'categorie': 'LOYER',
    'montant': 600,
    'description': 'Loyer du mois',
    'dateTransaction': '2026-08-05',
  },
];

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: FinanceScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche les KPIs et la liste des transactions', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path.startsWith('/finances/transactions')) return _json(path, _transactions);
      return _json(path, _stats);
    });
    await pumpScreen(tester, api);

    expect(find.text('Finances'), findsOneWidget);
    // KPIs calculés sur les données réelles.
    // Les montants apparaissent dans les KPI ET dans la liste des transactions.
    expect(find.text('1500 FCFA'), findsWidgets);
    expect(find.text('600 FCFA'), findsWidgets);
    expect(find.text('900 FCFA'), findsOneWidget); // solde : KPI uniquement
    // Transactions.
    expect(find.text('DIME'), findsOneWidget);
    expect(find.text('LOYER'), findsOneWidget);
  });

  testWidgets('filtre par type → GET /finances/transactions?type=DEPENSE', (tester) async {
    final paths = <String>[];
    final api = _FakeApiService((path, params) {
      paths.add(path);
      if (path.startsWith('/finances/transactions')) return _json(path, _transactions);
      return _json(path, _stats);
    });
    await pumpScreen(tester, api);

    // « Dépenses » apparaît dans le KPI ET dans le chip de filtre → prendre le chip (dernier).
    await tester.tap(find.text('Dépenses').last);
    await tester.pumpAndSettle();

    expect(paths, contains('/finances/transactions?type=DEPENSE'));
  });

  testWidgets('supprime une transaction avec confirmation → DELETE', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path.startsWith('/finances/transactions')) return _json(path, _transactions);
      return _json(path, _stats);
    });
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.delete_rounded).first);
    await tester.pumpAndSettle();

    expect(find.text('Supprimer la transaction ?'), findsOneWidget);
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/finances/transactions/tx-1'));
    expect(find.text('Transaction supprimée'), findsOneWidget);
  });
}
