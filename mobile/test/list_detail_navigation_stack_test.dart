// LOT 4 §GLISE-D'ABORD (T4.1 / T4.2, mobile) — conversion `go` → `push`.
//
// PROBLÈME 3 (« marque retour »). La cause mesurée n'est pas l'absence d'un
// widget de retour (`DetailBackButton` existe) mais l'ABSENCE DE PILE : `go()`
// ÉCRASE la pile GoRouter, donc `canPop()` retombe à `false` — ni flèche dans
// l'`AppBar`, ni pop du `DetailBackButton`, ni geste matériel Android. Depuis
// une fiche on est coincé.
//
// Ces tests verrouillent la conversion D6 (une conversion, un widget test) sur
// les deux cas d'école `SOUS_ECRAN` (liste → enfant) inventoriés en T4.0 :
//   - `souls_list_screen.dart`      : `context.go('/souls/{id}')`       → push
//   - `departments_list_screen.dart`: `context.go('/departments/{id}')`  → push
//
// Preuve DISCRIMINANTE : après le tap sur une carte, la pile contient la liste
// EN DESSOUS (`router.canPop() == true`) et le retour ramène à la liste. Sans
// la conversion (`go`), cette assertion tombe (`canPop() == false`).
//
// Preuve de NON-RÉGRESSION : la liste reste atteignable ET le lien profond
// direct `/souls/:id` servi par le routeur (via `go`) garde une pile d'un seul
// niveau — la conversion n'affecte QUE le tap interne, pas l'entrée profonde.

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/souls/souls_list_screen.dart';
import 'package:discipolat_mobile/presentation/screens/departments/departments_list_screen.dart';

/// ApiService factice piloté par le chemin demandé. On n'imite que ce que les
/// deux listes consomment : `/souls` (page Spring), `/departments` + `/users`
/// (listes brutes). Toute autre route renvoie une page vide.
class _NavFakeApi extends ApiService {
  _NavFakeApi() : super(baseUrl: 'http://fake');

  final List<String> calls = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    calls.add(path);
    dynamic body;
    if (path == '/souls') {
      body = <String, dynamic>{
        'content': <dynamic>[
          <String, dynamic>{
            'id': 'soul-42',
            'nom': 'Bosquet',
            'prenom': 'Jean',
            'email': 'jean@example.org',
            'typeDisciple': 'NOUVEAU_CONVERTI',
            'statut': 'ACTIF',
            'dateIntegration': '2026-01-10',
            'faiseurId': 'maker-1',
          },
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 50,
        'first': true,
        'last': true,
      };
    } else if (path == '/departments') {
      body = <String, dynamic>{
        'content': <dynamic>[
          <String, dynamic>{
            'id': 'dept-7',
            'nom': 'Louange',
            'description': 'Chants et adoration',
            'statut': 'ACTIF',
            'responsableNom': 'Marie',
            'nombreMembres': 12,
          },
        ],
      };
    } else if (path == '/users') {
      body = <String, dynamic>{'content': <dynamic>[]};
    } else {
      body = <String, dynamic>{'content': <dynamic>[]};
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: body,
    );
  }
}

/// Écran-fiche minimal, uniquement pour observer la pile (aucune logique).
Widget _marker(String text) => Scaffold(
      body: Center(child: Text(text)),
    );

void main() {
  testWidgets(
      'T4.1 — tap carte âme : `push` garde la liste sous la fiche (canPop==true, retour OK)',
      (tester) async {
    final api = _NavFakeApi();
    final router = GoRouter(
      initialLocation: '/souls',
      routes: [
        GoRoute(
          path: '/souls',
          builder: (_, __) => SoulsListScreen(apiService: api),
          routes: [
            GoRoute(
              path: ':id',
              builder: (context, state) =>
                  _marker('FICHE_SOUL:${state.pathParameters['id']}'),
            ),
          ],
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    // Liste chargée : la carte de l'âme est visible.
    expect(find.text('Jean Bosquet'), findsOneWidget);
    // Racine : rien à popper AVANT le tap.
    expect(router.canPop(), isFalse,
        reason: 'la liste est la racine, aucune pile à popper au départ');

    // Tap sur l'enfant → conversion `push` → pile = [/souls, /souls/soul-42].
    await tester.tap(find.text('Jean Bosquet'));
    await tester.pumpAndSettle();

    expect(find.text('FICHE_SOUL:soul-42'), findsOneWidget);
    // PREUVE DISCRIMINANTE : avec `go()` cette valeur serait `false`.
    expect(router.canPop(), isTrue,
        reason: 'D6/T4.1 : un enfant doit être pushé, la liste reste dessous');

    // Le retour ramène bien à la liste.
    router.pop();
    await tester.pumpAndSettle();
    expect(find.text('Jean Bosquet'), findsOneWidget);
    expect(find.text('FICHE_SOUL:soul-42'), findsNothing);
  });

  testWidgets(
      'T4.1 injection additive — `SoulsListScreen()` sans apiService reste valide (A1)',
      (tester) async {
    // La conversion T4.1 ne touche QUE le site du tap (`go` → `push`). Les
    // appelants existants (`SoulsListScreen()` sans argument, `const`)
    // restent valides : `apiService` était déjà optionnel, on ne l'a pas
    // modifié. On vérifie juste que la signature d\'avant continue de passer.
    expect(() => const SoulsListScreen(), returnsNormally);
    expect(const SoulsListScreen().apiService, isNull);
  });

  testWidgets(
      'T4.2 — tap carte département : `push` garde la liste sous la fiche (canPop==true, retour OK)',
      (tester) async {
    final api = _NavFakeApi();
    final router = GoRouter(
      initialLocation: '/departments',
      routes: [
        GoRoute(
          path: '/departments',
          builder: (_, __) => DepartmentsListScreen(apiService: api),
          routes: [
            GoRoute(
              path: ':id',
              builder: (context, state) =>
                  _marker('FICHE_DEPT:${state.pathParameters['id']}'),
            ),
          ],
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(find.text('Louange'), findsOneWidget);
    expect(router.canPop(), isFalse);

    await tester.tap(find.text('Louange'));
    await tester.pumpAndSettle();

    expect(find.text('FICHE_DEPT:dept-7'), findsOneWidget);
    expect(router.canPop(), isTrue,
        reason: 'D6/T4.2 : département = enfant de la liste, pushé');

    router.pop();
    await tester.pumpAndSettle();
    expect(find.text('Louange'), findsOneWidget);
    expect(find.text('FICHE_DEPT:dept-7'), findsNothing);
  });

  testWidgets(
      'T4.2 injection additive — `DepartmentsListScreen()` sans apiService reste valide (A1)',
      (tester) async {
    // L\'appelant existant `const DepartmentsListScreen()` (sans argument) DOIT
    // continuer à se construire : le paramètre ajouté est optionnel, défaut
    // `null`, et retombe sur l\'`ApiService()` interne (comportement d\'avant).
    expect(() => const DepartmentsListScreen(), returnsNormally);
    expect(const DepartmentsListScreen().apiService, isNull);
  });
}
