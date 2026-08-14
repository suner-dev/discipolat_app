import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/departments/department_management_screen.dart';

/// ApiService factice : renvoie le management du département, les membres,
/// les événements du département et les résultats de la recherche globale.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> postPaths = [];
  final List<String> getPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    getPaths.add('$path?$params');
    if (path.endsWith('/management')) {
      return _json(path, {
        'org': {'equipesActives': 2, 'postesActifs': 3, 'membresAffectes': 4},
        'teams': [
          {
            'id': 'team-1',
            'nom': 'Chorale',
            'type': 'EQUIPE_PERMANENTE',
            'statut': 'ACTIVE',
            'parentId': null,
            'nbMembres': 3,
          },
          {
            'id': 'team-2',
            'nom': 'Sono Convention',
            'type': 'EQUIPE_TEMPORAIRE',
            'statut': 'ACTIVE',
            'parentId': null,
            'nbMembres': 2,
            'eventId': 'evt-1',
            'eventTitre': 'Convention départementale',
          },
        ],
        'positions': [],
        'assignments': [],
        'taskStats': {'enCours': 1, 'aFaire': 2, 'enRetard': 1, 'terminees': 3},
        'activity': [],
      });
    }
    if (path.endsWith('/members')) {
      return _json(path, {
        'content': [
          {'id': 'member-1', 'nom': 'Jean Dupont', 'statut': 'ACTIF'},
        ],
      });
    }
    if (path.endsWith('/events/department/dept-1')) {
      return _json(path, {
        'content': [
          {
            'id': 'evt-1',
            'titre': 'Convention départementale',
            'typeEvenement': 'CONFERENCE',
            'lieu': 'Temple',
            'dateDebut': '2026-09-01T09:00:00',
            'statut': 'PLANIFIE',
          },
          {
            'id': 'evt-2',
            'titre': 'Culte des familles',
            'typeEvenement': 'REUNION',
            'lieu': 'Salle 2',
            'dateDebut': '2026-08-01T10:00:00',
            'statut': 'TERMINE',
          },
        ],
      });
    }
    if (path.endsWith('/search')) {
      final q = params?['q'] ?? '';
      final membres = q.toLowerCase().contains('jean')
          ? [
              {
                'id': 'member-1',
                'nomComplet': 'Jean Dupont',
                'statut': 'ACTIF',
                'telephone': '691111111',
              },
            ]
          : <dynamic>[];
      return _json(path, {
        'membres': membres,
        'equipes': q.toLowerCase().contains('chor') ? [{'id': 'team-1', 'nom': 'Chorale'}] : <dynamic>[],
        'postes': <dynamic>[],
        'taches': <dynamic>[],
        'evenements': q.toLowerCase().contains('convention')
            ? [{'id': 'evt-1', 'titre': 'Convention départementale', 'dateDebut': '2026-09-01T09:00:00'}]
            : <dynamic>[],
        'total': membres.length +
            (q.toLowerCase().contains('chor') ? 1 : 0) +
            (q.toLowerCase().contains('convention') ? 1 : 0),
      });
    }
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> delete(String path) async {
    return _json(path, <String, dynamic>{});
  }

  Response<dynamic> _json(String path, Object data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);
}

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  testWidgets('affiche les événements du département (à venir + passés)', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentManagementScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    // L'onglet Événements est accessible depuis la barre d'onglets
    await tester.tap(find.text('Événements'));
    await tester.pumpAndSettle();

    expect(find.text('Convention départementale'), findsOneWidget);
    expect(find.text('À venir (1)'), findsOneWidget);
    expect(find.text('Passés (1)'), findsOneWidget);
    expect(find.text('Culte des familles'), findsOneWidget);
  });

  testWidgets('crée un événement de département → POST /events avec departmentId', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentManagementScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Événements'));
    await tester.pumpAndSettle();

    // Ouvre le dialogue de création
    await tester.tap(find.byIcon(Icons.add_circle));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Titre *'), 'Sortie d’évangélisation');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/events'));
  });

  testWidgets('affiche le titre de l’événement lié sur une équipe temporaire', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentManagementScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Organisation'));
    await tester.pumpAndSettle();

    expect(find.text('Chorale'), findsOneWidget);
    expect(find.text('Sono Convention'), findsOneWidget);
    expect(find.textContaining('Événement : Convention départementale'), findsOneWidget);
  });

  testWidgets('recherche globale : résultats par catégorie à partir de 2 caractères', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentManagementScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    final searchField = find.byType(TextField).first;
    await tester.enterText(searchField, 'jean');
    await tester.pumpAndSettle();

    // Le panneau de résultats remplace les onglets
    expect(find.textContaining('1 résultat(s) pour « jean »'), findsOneWidget);
    expect(find.text('Membres (1)'), findsOneWidget);
    expect(find.text('Jean Dupont'), findsOneWidget);
  });
}
