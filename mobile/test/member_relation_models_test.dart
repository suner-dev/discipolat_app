import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/models/member_relation.dart';
import 'package:discipolat_mobile/l10n/app_localizations.dart';
import 'package:discipolat_mobile/presentation/widgets/hierarchy_card.dart';

/// V231 — « Mon encadrement » (mobile) : modèles et rendu de l'arbre.
///
/// Le parsing doit être DÉFENSIF : une réponse partielle (brique dégradée,
/// champ renommé, code de relation inconnu) ne doit jamais faire crasher
/// l'écran de profil ni la fiche utilisateur.
Widget wrap(Widget child) => MaterialApp(
      locale: const Locale('fr'),
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );

final Map<String, dynamic> aggregate = {
  'ascendants': [
    {
      'id': 'u-pasteur',
      'nom': 'Jean Maka',
      'via': 'ORGANISATION',
      'noeud': 'Église mère'
    },
    {
      'id': 'u-mentor',
      'nom': 'Paul Beye',
      'via': 'DECLARATIF',
      'typeLabel': 'Mon mentor'
    },
  ],
  'branches': [
    {
      'noeud': {'id': 'n1', 'nom': 'Campus Nord', 'type': 'CAMPUS'},
      'origine': 'ASSIGNATION_V3',
      'chaine': [
        {
          'id': 'n1',
          'nom': 'Campus Nord',
          'responsable': {'id': 'u-1', 'nom': 'Paul Beye'}
        },
        {
          'id': 'n0',
          'nom': 'Église mère',
          'responsable': {'id': 'u-pasteur', 'nom': 'Jean Maka'}
        },
      ],
    },
  ],
  'suivi': {
    'faiseur': {'id': 'u-pasteur', 'nom': 'Jean Maka'},
    'chefDeFamille': null,
    'departementsDiriges': [
      {'id': 'd1', 'nom': 'Louange'}
    ],
    'noeudsDiriges': <Map<String, dynamic>>[],
    'amesSuiviesTotal': 0,
  },
  'resume': {'membresRattaches': 1},
};

void main() {
  group('MemberRelation.fromJson — parsing défensif', () {
    test('payload complet', () {
      final r = MemberRelation.fromJson({
        'id': 'rel-1',
        'fromUserId': 'u-a',
        'fromNom': 'Awa Diallo',
        'toUserId': 'u-b',
        'toNom': 'Jean Maka',
        'otherUserId': 'u-b',
        'otherNom': 'Jean Maka',
        'relationType': 'PASTEUR',
        'typeLabel': 'Mon pasteur',
        'statut': 'ACTIVE',
        'note': 'depuis 2019',
        'createdAt': '2026-01-01T10:00:00Z',
        'revocable': true,
      });
      expect(r.id, 'rel-1');
      expect(r.fromNom, 'Awa Diallo');
      expect(r.toNom, 'Jean Maka');
      expect(r.typeLabel, 'Mon pasteur');
      expect(r.isActive, isTrue);
      expect(r.revocable, isTrue);
    });

    test('payload vide : aucun crash, valeurs par défaut', () {
      final r = MemberRelation.fromJson(const {});
      expect(r.id, '');
      expect(r.otherNom, '—');
      expect(r.statut, 'ACTIVE');
      expect(r.isActive, isTrue);
      expect(r.revocable, isTrue, reason: 'par défaut on ne masque pas l’action');
    });

    test('typeLabel absent : repli sur le code de relation', () {
      expect(MemberRelation.fromJson({'relationType': 'MENTOR'}).typeLabel,
          'MENTOR');
    });

    test('otherNom vide : jamais une chaîne vide affichée', () {
      expect(MemberRelation.fromJson({'otherNom': ''}).otherNom, '—');
    });

    test('revocable absent ou non-booléen : défaut true', () {
      expect(MemberRelation.fromJson({'revocable': 'oui'}).revocable, isTrue);
      expect(MemberRelation.fromJson({'revocable': false}).revocable, isFalse);
    });
  });

  group('RelationsSummary', () {
    test('extrait sortantes et entrantes', () {
      final s = RelationsSummary.fromJson({
        'sortantes': [
          {'id': 'a', 'otherUserId': 'x', 'otherNom': 'X'}
        ],
        'entrantes': [
          {'id': 'b', 'otherUserId': 'y', 'otherNom': 'Y'}
        ],
      });
      expect(s.sortantes, hasLength(1));
      expect(s.entrantes, hasLength(1));
      expect(s.entrantes.single.otherNom, 'Y');
    });

    test('clés absentes : listes vides, pas d exception', () {
      final s = RelationsSummary.fromJson(const {});
      expect(s.sortantes, isEmpty);
      expect(s.entrantes, isEmpty);
    });

    test('payload non conforme : ignoré silencieusement', () {
      final s =
          RelationsSummary.fromJson({'sortantes': 'oups', 'entrantes': 42});
      expect(s.sortantes, isEmpty);
      expect(s.entrantes, isEmpty);
    });
  });

  group('MemberRelationPage — pagination', () {
    test('lit les métadonnées Spring Page', () {
      final p = MemberRelationPage.fromJson({
        'content': [
          {'id': 'a', 'otherUserId': 'x', 'otherNom': 'X'}
        ],
        'totalElements': 130,
        'totalPages': 6,
        'number': 2,
      });
      expect(p.content, hasLength(1));
      expect(p.totalElements, 130);
      expect(p.totalPages, 6);
      expect(p.number, 2);
      expect(p.hasPrevious, isTrue);
      expect(p.hasNext, isTrue);
    });

    test('première page : pas de précédent', () {
      final p = MemberRelationPage.fromJson({
        'content': [],
        'totalElements': 3,
        'totalPages': 1,
        'number': 0,
      });
      expect(p.hasPrevious, isFalse);
      expect(p.hasNext, isFalse);
    });

    test('dernière page : pas de suivant', () {
      final p = MemberRelationPage.fromJson({
        'content': [],
        'totalElements': 130,
        'totalPages': 6,
        'number': 5,
      });
      expect(p.hasNext, isFalse);
    });

    test('réponse dégradée : page vide mais non cassée', () {
      final p = MemberRelationPage.fromJson(const {});
      expect(p.content, isEmpty);
      expect(p.totalElements, 0);
      expect(p.totalPages, 0);
      expect(p.hasNext, isFalse);
    });
  });

  group('HierarchyBranch / HierarchyStep / Ascendant', () {
    test('branche complète avec chaîne et responsable', () {
      final b = HierarchyBranch.fromJson({
        'noeud': {'id': 'n1', 'nom': 'Campus Nord', 'type': 'CAMPUS'},
        'origine': 'ASSIGNATION_V3',
        'chaine': [
          {
            'id': 'n1',
            'nom': 'Campus Nord',
            'responsable': {'id': 'u-1', 'nom': 'Paul Beye'}
          },
          {'id': 'n0', 'nom': 'Église mère', 'responsable': null}
        ],
      });
      expect(b.nodeNom, 'Campus Nord');
      expect(b.origine, 'ASSIGNATION_V3');
      expect(b.steps, hasLength(2));
      expect(b.steps.first.responsableNom, 'Paul Beye');
      expect(b.steps.last.responsableId, isNull);
    });

    test('branche sans noeud : pas de crash', () {
      final b = HierarchyBranch.fromJson(const {});
      expect(b.nodeNom, '—');
      expect(b.steps, isEmpty);
    });

    test('ascendant : distingue organisation et déclaratif', () {
      final org = Ascendant.fromJson(
          {'id': 'u1', 'nom': 'Jean', 'via': 'ORGANISATION', 'noeud': 'Église mère'});
      final dec = Ascendant.fromJson(
          {'id': 'u2', 'nom': 'Paul', 'via': 'DECLARATIF', 'typeLabel': 'Mon mentor'});
      expect(org.isDeclared, isFalse);
      expect(dec.isDeclared, isTrue);
      expect(dec.typeLabel, 'Mon mentor');
    });
  });

  group('HierarchyCard — rendu de l’arbre', () {
    testWidgets('racine, chaîne d’ascendance et responsables',
        (WidgetTester tester) async {
      await tester.pumpWidget(wrap(HierarchyCard(hierarchy: aggregate)));
      await tester.pumpAndSettle();

      expect(find.text('Hiérarchie et encadrement'), findsOneWidget);
      expect(find.text('Campus Nord'), findsWidgets);
      expect(find.text('Paul Beye'), findsWidgets);
      expect(find.text('Église mère'), findsWidgets);
      // Origine de la branche affichée, LOCALISÉE (plus le code brut ASSIGNATION_V3).
      expect(find.textContaining('assignation'), findsOneWidget);
    });

    testWidgets('ascendants : organisation et déclaratif distingués',
        (WidgetTester tester) async {
      await tester.pumpWidget(wrap(HierarchyCard(hierarchy: aggregate)));
      await tester.pumpAndSettle();

      expect(find.text('Ses responsables'), findsOneWidget);
      expect(find.text('Église mère'), findsWidgets);
      expect(find.text('Mon mentor'), findsWidgets);
    });

    testWidgets('encadrement pastoral affiché quand il existe',
        (WidgetTester tester) async {
      await tester.pumpWidget(wrap(HierarchyCard(hierarchy: aggregate)));
      await tester.pumpAndSettle();

      expect(find.text('Encadrement pastoral'), findsOneWidget);
      expect(find.textContaining('Pasteur (fait par)'), findsOneWidget);
      expect(find.textContaining('Départements dirigés'), findsOneWidget);
      expect(find.text('Louange'), findsOneWidget);
    });

    testWidgets('cliquer un responsable appelle onOpenProfile',
        (WidgetTester tester) async {
      final opened = <String>[];
      await tester.pumpWidget(
          wrap(HierarchyCard(hierarchy: aggregate, onOpenProfile: opened.add)));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Paul Beye').first);
      await tester.pumpAndSettle();

      expect(opened, isNotEmpty);
    });

    testWidgets(
        'aucune branche mais un encadrement existe : message explicite, pas de vide',
        (WidgetTester tester) async {
      // Cas réel : un membre déclare son pasteur mais n'est rattaché à aucun
      // nœud de l'organisation. La carte doit le dire, pas rester muette.
      await tester.pumpWidget(wrap(HierarchyCard(
        hierarchy: const {
          'ascendants': <Map<String, dynamic>>[],
          'branches': <Map<String, dynamic>>[],
        },
        relations: const {
          'sortantes': [
            {
              'id': 'rel-1',
              'otherUserId': 'u-pasteur',
              'otherNom': 'Jean Maka',
              'typeLabel': 'Mon pasteur'
            }
          ],
        },
      )));
      await tester.pumpAndSettle();

      expect(find.text('Jean Maka'), findsOneWidget);
      expect(
          find.textContaining("Aucune branche d'organisation"), findsOneWidget);
    });

    testWidgets('agrégat vide : rien ne s’affiche', (WidgetTester tester) async {
      await tester
          .pumpWidget(wrap(HierarchyCard(hierarchy: const {})));
      await tester.pumpAndSettle();

      expect(find.text('Hiérarchie et encadrement'), findsNothing);
    });

    testWidgets('brique dégradée : message d’indisponibilité explicite',
        (WidgetTester tester) async {
      await tester.pumpWidget(wrap(const HierarchyUnavailableCard()));
      await tester.pumpAndSettle();

      expect(find.textContaining('Hiérarchie indisponible'), findsOneWidget);
    });
  });
}
