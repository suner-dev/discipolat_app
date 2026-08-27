import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/users/user_detail_screen.dart';

/// ApiService factice pour la fiche utilisateur : renvoie le détail et
/// enregistre les PUT d'évaluation.
class _FakeApiService extends ApiService {
  _FakeApiService(this._detail) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> _detail;
  final List<String> putPaths = [];
  final List<Map<String, dynamic>?> putDatas = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/users/u1/detail') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: _detail);
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    putDatas.add(data is Map<String, dynamic> ? data : null);
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {});
  }
}

Map<String, dynamic> _detailWithoutEval() => {
  'id': 'u1',
  'firstName': 'Jean',
  'lastName': 'Dupont',
  'email': 'jean@discipolat.test',
  'role': 'FAISEUR',
  'statut': 'ACTIVE',
  'dateCreation': '2024-01-15',
  'monEvaluation': <dynamic>[],
  'evaluations': <String, dynamic>{
    'RESPONSABLE': {'moyenne': 4.0, 'total': 2},
  },
  'amesSuivies': [
    {'id': 's1', 'nom': 'Paul Robert', 'statut': 'ACTIF', 'familleNom': 'Famille A'},
    {'id': 's2', 'nom': 'Anna Blanc', 'statut': 'EN_VEILLE'},
  ],
  'sorties': [
    {'motif': 'Déménagement', 'dateSortie': '2024-06-01'},
  ],
  'departements': [
    {'id': 'd1', 'nom': 'Jeunesse', 'membres': [{'nomComplet': 'Luc Bernard', 'statut': 'ACTIF'}]},
  ],
  'familleGeree': {
    'nom': 'Famille A',
    'membres': [{'nomComplet': 'Marie Martin', 'statut': 'ACTIF'}],
  },
  'dossier': [
    {
      'departmentId': 'd1',
      'departmentNom': 'Jeunesse',
      'objectifs': [
        {'id': 'o1', 'titre': 'Mener 2 études bibliques', 'statut': 'EN_COURS', 'avancement': 50, 'echeance': '2026-09-01', 'enRetard': false},
      ],
      'rapportsResponsable': [
        {'id': 'r1', 'type': 'PROGRESSION', 'contenu': 'Bonne progression', 'auteurNom': 'Paul Robert', 'createdAt': '2026-07-01'},
      ],
      'notes': [
        {'id': 'n1', 'contenu': 'A rencontré son faiseur', 'auteurNom': 'Paul Robert', 'createdAt': '2026-07-02'},
      ],
    },
  ],
  'dossierDocuments': [
    {'id': 'doc1', 'nom': 'Compte rendu de visite', 'url': 'https://example.com/cr.pdf'},
  ],
};

Map<String, dynamic> _detailWithEval() => {
  ..._detailWithoutEval(),
  'monEvaluation': [
    {'note': 4, 'commentaire': 'Bon travail'},
  ],
};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.binding.setSurfaceSize(const Size(800, 2000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: UserDetailScreen(userId: 'u1', apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche la fiche complète : identité, âme, âmes suivies, départements, famille', (tester) async {
    final api = _FakeApiService(_detailWithoutEval());
    await pumpScreen(tester, api);

    expect(find.text('Fiche utilisateur'), findsOneWidget);
    expect(find.text('Jean Dupont'), findsOneWidget);
    expect(find.text('jean@discipolat.test'), findsOneWidget);
    expect(find.text('Faiseur de disciples'), findsOneWidget);
    // Âmes suivies
    expect(find.text('Âmes suivies (2)'), findsOneWidget);
    expect(find.text('Paul Robert'), findsOneWidget);
    expect(find.text('Anna Blanc'), findsOneWidget);
    // Sorties
    expect(find.text('Sorties de suivi (1)'), findsOneWidget);
    expect(find.text('Déménagement'), findsOneWidget);
    // Département dirigé (+ même nom dans le dossier du membre)
    expect(find.text('Départements dirigés (1)'), findsOneWidget);
    expect(find.text('Jeunesse'), findsNWidgets(2));
    // Famille gérée
    expect(find.text('Famille gérée : Famille A (1)'), findsOneWidget);
    // Évaluation reçue (statistiques)
    expect(find.text('Évaluations reçues (anonymes)'), findsOneWidget);
    expect(find.textContaining('4.0/5 (2)'), findsOneWidget);
  });

  testWidgets('affiche le dossier du membre : objectifs, rapports, notes, documents', (tester) async {
    final api = _FakeApiService(_detailWithoutEval());
    await pumpScreen(tester, api);

    // La section dossier est en bas de la fiche → on y défile.
    await tester.dragUntilVisible(
      find.textContaining('Dossier du membre'),
      find.byType(ListView).first,
      const Offset(0, -300),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('Dossier du membre'), findsAtLeastNWidgets(1));
    expect(find.text('Jeunesse'), findsNWidgets(2)); // section départements + dossier
    expect(find.text('Objectifs (1)'), findsWidgets);
    expect(find.text('Mener 2 études bibliques'), findsOneWidget);
    expect(find.text('En Cours'), findsOneWidget);
    expect(find.text('Rapports (1)'), findsWidgets);
    expect(find.text('Bonne progression'), findsOneWidget);
    expect(find.text('Notes (1)'), findsWidgets);
    expect(find.text('A rencontré son faiseur'), findsOneWidget);
    expect(find.textContaining('Documents du dossier'), findsAtLeastNWidgets(1));
    expect(find.text('Compte rendu de visite'), findsOneWidget);
  });

  testWidgets('sans évaluation de ma part → bouton « Donner l’évaluation » → PUT', (tester) async {
    final api = _FakeApiService(_detailWithoutEval());
    await pumpScreen(tester, api);

    expect(find.text('Pas encore évalué'), findsOneWidget);
    expect(find.text("Donner l'évaluation"), findsOneWidget);

    // Choisir 5 étoiles puis enregistrer.
    await tester.tap(find.byIcon(Icons.star_border_rounded).last);
    await tester.pump();
    await tester.ensureVisible(find.text("Donner l'évaluation"));
    await tester.tap(find.text("Donner l'évaluation"));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/evaluations/u1'));
    expect(api.putDatas.last?['note'], 5);
    expect(find.text('Évaluation enregistrée'), findsOneWidget);
  });

  testWidgets('avec mon évaluation → pré-remplie et bouton « Modifier l’évaluation » → PUT', (tester) async {
    final api = _FakeApiService(_detailWithEval());
    await pumpScreen(tester, api);

    expect(find.text('Vous avez évalué'), findsOneWidget);
    expect(find.text("Modifier l'évaluation"), findsOneWidget);

    await tester.ensureVisible(find.text("Modifier l'évaluation"));
    await tester.tap(find.text("Modifier l'évaluation"));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/evaluations/u1'));
    expect(api.putDatas.last?['note'], 4); // pré-remplie depuis monEvaluation
  });
}
