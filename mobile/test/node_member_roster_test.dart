import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/models/node_member_roster.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/l10n/app_localizations.dart';
import 'package:discipolat_mobile/presentation/screens/tenant/node_detail_screen.dart';

import 'helpers/pump_localized.dart';

/// MO-3 — roster des membres d'un nœud sur mobile (§7, miroir BE
/// `GET /tenant/organization/nodes/{id}/members`).
///
/// Trois garanties verrouillées, dans cet ordre :
///
/// 1. **Parsing défensif** — un champ absent, `null` ou mal typé ne doit pas
///    faire échouer la lecture de toute la page. La fiche nœud affiche alors
///    une section « membres » vide ; elle ne crashe pas et n'invente pas.
/// 2. **Rien n'est inventé** — une origine absente se lit `null` (et non
///    « ADHESION_NOEUD » par défaut) : afficher une origine supposée serait
///    faire dire au produit ce que le serveur n'a pas dit.
/// 3. **i18n 6 locales** — chaque intitulé de la section est traduit en fr,
///    en, pt, es, sw, ar. La table d'attentes est littérale : `translate()`
///    retombe sur le FR pour une clé absente, donc comparer au FR n'aurait
///    rien prouvé.
void main() {
  group('NodeMembersPage — parsing défensif', () {
    test('miroir du serveur : page complète lue', () {
      final page = NodeMembersPage.fromJson(const <String, dynamic>{
        'nodeId': 'n-1',
        'nodeName': 'Cellule Nord',
        'nodeType': 'GROUP',
        'scope': 'SUBTREE',
        'content': [
          {
            'userId': 'u-1',
            'firstName': 'Awa',
            'lastName': 'Diallo',
            'email': 'awa@exemple.net',
            'direct': true,
            'origine': 'ADHESION_NOEUD',
            'roles': ['Ancien', 'Pasteur'],
          },
        ],
        'page': 0,
        'size': 20,
        'totalElements': 1,
        'totalPages': 1,
        'memberCountDirect': 1,
        'memberCountSousArbre': 7,
      });

      expect(page.nodeId, 'n-1');
      expect(page.nodeName, 'Cellule Nord');
      expect(page.scope, 'SUBTREE');
      expect(page.content.length, 1);
      expect(page.content.single.displayName, 'Awa Diallo');
      expect(page.content.single.direct, isTrue);
      expect(page.content.single.origine, 'ADHESION_NOEUD');
      expect(page.content.single.roles, ['Ancien', 'Pasteur']);
      expect(page.memberCountDirect, 1);
      expect(page.memberCountSousArbre, 7);
      expect(page.hasNext, isFalse);
    });

    test('JSON vide : page cohérente, aucune exception', () {
      // Le serveur peut répondre 200 avec un corps vide (entité sans membre,
      // ou migration en cours). « vide » doit rester lisible, pas planté.
      final page = NodeMembersPage.fromJson(const <String, dynamic>{});

      expect(page.nodeId, '');
      expect(page.nodeName, isNull);
      expect(page.scope, 'SUBTREE');
      expect(page.content, isEmpty);
      expect(page.page, 0);
      expect(page.size, 20);
      expect(page.totalElements, 0);
      expect(page.totalPages, 0);
      expect(page.memberCountDirect, 0);
      expect(page.memberCountSousArbre, 0);
      // Pas de page fantôme : sans `totalPages` servi, « Charger plus » ne
      // doit pas s'afficher et renvoyer la requête dans une boucle.
      expect(page.hasNext, isFalse);
    });

    test('champs mal typés : repli, pas de crash', () {
      // Une liste devenue objet, un entier devenu chaîne, un booléen devenu
      // texte : la page entière ne doit pas tomber pour un champ abîmé.
      final page = NodeMembersPage.fromJson(<String, dynamic>{
        'nodeId': 42,
        'nodeName': null,
        'content': 'oops',
        'page': '1',
        'size': 20.0,
        'totalPages': null,
        'memberCountDirect': '3',
        'memberCountSousArbre': 5.0,
      });

      expect(page.nodeId, '42');
      expect(page.nodeName, isNull);
      expect(page.content, isEmpty);
      expect(page.page, 1);
      expect(page.size, 20);
      expect(page.totalPages, 0);
      expect(page.memberCountDirect, 3);
      expect(page.memberCountSousArbre, 5);
    });

    test('content mixte : les lignes valides survivent', () {
      // Une ligne abîmée ne doit pas effacer les autres (le serveur peut
      // evolve en cours de déploiement ; l écran doit rester utilisable).
      final page = NodeMembersPage.fromJson(const <String, dynamic>{
        'nodeId': 'n-1',
        'content': [
          {'userId': 'u-1', 'firstName': 'Awa'},
          'chaque-part',
          null,
          {'userId': 'u-2', 'firstName': 'Yao'},
        ],
      });

      expect(page.content.map((r) => r.userId), ['u-1', 'u-2']);
    });

    test('roles absent ou mal typé : liste vide', () {
      final absent = NodeMemberRow.fromJson(const {'userId': 'u-1'});
      final malType = NodeMemberRow.fromJson(
          const {'userId': 'u-1', 'roles': {'0': 'Ancien'}});

      expect(absent.roles, isEmpty);
      expect(malType.roles, isEmpty);
    });

    test('origine absente : jamais inventée', () {
      // Le défaut précédent (`?? 'ADHESION_NOEUD'`) faisait afficher une
      // adhésion au nœud là où le serveur n'a rien dit.
      final row = NodeMemberRow.fromJson(const {'userId': 'u-1'});
      expect(row.origine, isNull);

      final nullRow = NodeMemberRow.fromJson(
          const <String, dynamic>{'userId': 'u-1', 'origine': null});
      expect(nullRow.origine, isNull);
    });

    test('origine inconnue est conservée telle quelle', () {
      // Chaîne technique non prévue côté client : on ne la remplace pas par
      // une valeur « connue » — le libellé retombe sur le code, visible.
      final row = NodeMemberRow.fromJson(
          const {'userId': 'u-1', 'origine': 'ASSIGNATION_FUTURE'});
      expect(row.origine, 'ASSIGNATION_FUTURE');
    });

    test('direct strictement booléen : tout le reste vaut false', () {
      // « direct » pilote un badge de rattachement : une chaîne « true » ou
      // un 1 ne doit pas le faire passer pour un rattachement ici.
      expect(NodeMemberRow.fromJson(const {'userId': 'u', 'direct': 'true'}).direct, isFalse);
      expect(NodeMemberRow.fromJson(const {'userId': 'u', 'direct': 1}).direct, isFalse);
      expect(NodeMemberRow.fromJson(const {'userId': 'u', 'direct': null}).direct, isFalse);
      expect(NodeMemberRow.fromJson(const {'userId': 'u', 'direct': true}).direct, isTrue);
    });

    test("displayName : nom complet, sinon l'identifiant", () {
      expect(NodeMemberRow.fromJson(const {'userId': 'u-1', 'firstName': 'Awa'}).displayName, 'Awa');
      expect(
          NodeMemberRow.fromJson(
                  const {'userId': 'u-1', 'firstName': 'Awa', 'lastName': 'Diallo'})
              .displayName,
          'Awa Diallo');
      // Ni prénom ni nom : on montre l'identifiant plutôt que de fabriquer
      // un libellé (« Membre », « Inconnu ») qui masquerait une donnée vide.
      expect(NodeMemberRow.fromJson(const {'userId': 'u-1'}).displayName, 'u-1');
    });

    test('hasNext suit la pagination serveur', () {
      NodeMembersPage pg(int page, int totalPages) => NodeMembersPage(
            nodeId: 'n',
            scope: 'SUBTREE',
            content: const [],
            page: page,
            size: 20,
            totalElements: 0,
            totalPages: totalPages,
            memberCountDirect: 0,
            memberCountSousArbre: 0,
          );

      expect(pg(0, 3).hasNext, isTrue);
      expect(pg(1, 3).hasNext, isTrue);
      expect(pg(2, 3).hasNext, isFalse);
      expect(pg(0, 1).hasNext, isFalse);
    });

    test('empty() : section vide lisible pour le même nœud', () {
      final page = NodeMembersPage.empty('n-9');
      expect(page.nodeId, 'n-9');
      expect(page.content, isEmpty);
      expect(page.hasNext, isFalse);
    });
  });

  group('AppLocalizations — section membres, 6 locales', () {
    // Table littérale : `translate()` retombe sur le FR quand une clé manque,
    // donc une comparaison « locale vs FR » ne détectait pas l'absence.
    const expected = <String, Map<String, String>>{
      'fr': {
        'nodeMembersTitle': 'Membres',
        'nodeMembersCounters': 'Directs {direct} • Total {total}',
        'nodeMembersScopeDirect': 'Ici',
        'nodeMembersScopeSubtree': 'Avec sous-unités',
        'nodeMembersSearch': 'Rechercher un membre',
        'nodeMembersEmpty': 'Aucun membre pour cette sélection.',
        'nodeMembersVia': 'via {name}',
        'nodeMembersLoadMore': 'Charger plus',
        'hierarchyOriginAdhesionTenant': 'adhésion (entité)',
      },
      'en': {
        'nodeMembersTitle': 'Members',
        'nodeMembersCounters': 'Direct {direct} • Total {total}',
        'nodeMembersScopeDirect': 'Here',
        'nodeMembersScopeSubtree': 'With sub-units',
        'nodeMembersSearch': 'Search a member',
        'nodeMembersEmpty': 'No member for this selection.',
        'nodeMembersVia': 'via {name}',
        'nodeMembersLoadMore': 'Load more',
        'hierarchyOriginAdhesionTenant': 'membership (entity)',
      },
      'pt': {
        'nodeMembersTitle': 'Membros',
        'nodeMembersCounters': 'Diretos {direct} • Total {total}',
        'nodeMembersScopeDirect': 'Aqui',
        'nodeMembersScopeSubtree': 'Com subunidades',
        'nodeMembersSearch': 'Pesquisar um membro',
        'nodeMembersEmpty': 'Nenhum membro para esta seleção.',
        'nodeMembersVia': 'via {name}',
        'nodeMembersLoadMore': 'Carregar mais',
        'hierarchyOriginAdhesionTenant': 'adesão (entidade)',
      },
      'es': {
        'nodeMembersTitle': 'Miembros',
        'nodeMembersCounters': 'Directos {direct} • Total {total}',
        'nodeMembersScopeDirect': 'Aquí',
        'nodeMembersScopeSubtree': 'Con subunidades',
        'nodeMembersSearch': 'Buscar un miembro',
        'nodeMembersEmpty': 'Ningún miembro para esta selección.',
        'nodeMembersVia': 'vía {name}',
        'nodeMembersLoadMore': 'Cargar más',
        'hierarchyOriginAdhesionTenant': 'adhesión (entidad)',
      },
      'sw': {
        'nodeMembersTitle': 'Wanachama',
        'nodeMembersCounters': 'Moja kwa moja {direct} • Jumla {total}',
        'nodeMembersScopeDirect': 'Hapa',
        'nodeMembersScopeSubtree': 'Kwa subuniti',
        'nodeMembersSearch': 'Tafuta mwanachama',
        'nodeMembersEmpty': 'Hakuna mwanachama kwa chaguo hili.',
        'nodeMembersVia': 'kupitia {name}',
        'nodeMembersLoadMore': 'Pakia zaidi',
        'hierarchyOriginAdhesionTenant': 'ujiungaji (taasisi)',
      },
      'ar': {
        'nodeMembersTitle': 'الأعضاء',
        'nodeMembersCounters': 'مباشر {direct} • الإجمالي {total}',
        'nodeMembersScopeDirect': 'هنا',
        'nodeMembersScopeSubtree': 'مع الكيانات الفرعية',
        'nodeMembersSearch': 'ابحث عن عضو',
        'nodeMembersEmpty': 'لا يوجد أعضاء لهذا التحديد.',
        'nodeMembersVia': 'عبر {name}',
        'nodeMembersLoadMore': 'تحميل المزيد',
        'hierarchyOriginAdhesionTenant': 'انضمام (الكيان)',
      },
    };

    test('les 9 clés sont présentes et justes dans les 6 locales', () {
      expected.forEach((code, table) {
        final l10n = AppLocalizations(Locale(code));
        table.forEach((key, value) {
          expect(l10n.translate(key), value,
              reason: '$key attendue « $value » en $code');
        });
      });
    });

    test('les getters du roster suivent les clés', () {
      expected.forEach((code, table) {
        final l10n = AppLocalizations(Locale(code));
        expect(l10n.nodeMembersTitle, table['nodeMembersTitle'], reason: code);
        expect(l10n.nodeMembersScopeDirect, table['nodeMembersScopeDirect'],
            reason: code);
        expect(l10n.nodeMembersScopeSubtree, table['nodeMembersScopeSubtree'],
            reason: code);
        expect(l10n.nodeMembersSearch, table['nodeMembersSearch'],
            reason: code);
        expect(l10n.nodeMembersEmpty, table['nodeMembersEmpty'], reason: code);
        expect(l10n.nodeMembersLoadMore, table['nodeMembersLoadMore'],
            reason: code);
        expect(l10n.hierarchyOriginAdhesionTenant,
            table['hierarchyOriginAdhesionTenant'],
            reason: code);
      });
    });

    test("aucun gabarit {direct}/{total}/{name} ne subsiste à l'écran", () {
      expected.forEach((code, _) {
        final l10n = AppLocalizations(Locale(code));
        expect(l10n.nodeMembersCounters(3, 12).contains('{'), isFalse,
            reason: 'gabarit non remplacé en $code');
        expect(l10n.nodeMembersVia('Groupe Bethel').contains('{'), isFalse,
            reason: 'gabarit non remplacé en $code');
      });
      expect(AppLocalizations(const Locale('fr')).nodeMembersCounters(1, 2),
          'Directs 1 • Total 2');
      expect(AppLocalizations(const Locale('en')).nodeMembersCounters(1, 2),
          'Direct 1 • Total 2');
      expect(AppLocalizations(const Locale('fr')).nodeMembersVia('Groupe Bethel'),
          'via Groupe Bethel');
      expect(AppLocalizations(const Locale('sw')).nodeMembersVia('Kikundi'),
          'kupitia Kikundi');
    });

    test('locale inconnu : repli sur le français', () {
      final l10n = AppLocalizations(const Locale('de'));
      expect(l10n.nodeMembersTitle, expected['fr']!['nodeMembersTitle']);
      expect(l10n.nodeMembersEmpty, expected['fr']!['nodeMembersEmpty']);
    });

    test('supportedLocales couvre les 6 langues', () {
      expect(
          AppLocalizations.supportedLocales.map((l) => l.languageCode).toList(),
          containsAll(<String>['fr', 'en', 'pt', 'es', 'sw', 'ar']));
    });
  });

  group('NodeDetailScreen — section membres localisée', () {
    /// Roster serveur : un membre rattaché ici, un membre « via » une
    /// sous-structure, une page unique (pas de « Charger plus »).
    const rosterJson = <String, dynamic>{
      'nodeId': 'n-1',
      'nodeName': 'Cellule Nord',
      'nodeType': 'GROUP',
      'scope': 'SUBTREE',
      'content': [
        {
          'userId': 'u-1',
          'firstName': 'Awa',
          'lastName': 'Diallo',
          'direct': true,
          'origine': 'ADHESION_NOEUD',
          'roles': ['Ancien'],
        },
        {
          'userId': 'u-2',
          'firstName': 'Yao',
          'lastName': 'Koffi',
          'direct': false,
          'attachedNodeName': 'Groupe Bethel',
          'origine': 'ASSIGNATION_V3',
        },
      ],
      'page': 0,
      'size': 20,
      'totalElements': 2,
      'totalPages': 1,
      'memberCountDirect': 1,
      'memberCountSousArbre': 2,
    };

    final asked = <String, Map<String, dynamic>?>{};
    final api = _RosterApi(rosterJson, asked);

    Future<void> pumpNodeScreen(WidgetTester tester, Locale locale) async {
      // Surface haute : la section membres suit les compteurs et l'équipe
      // dans la ListView ; sans hauteur suffisante elle reste non construite.
      tester.view.physicalSize = const Size(1200, 6000);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await pumpLocalized(
        tester,
        ProviderScope(
          child: NodeDetailScreen(
            nodeId: 'n-1',
            nodeName: 'Cellule Nord',
            apiService: api,
          ),
        ),
        locale: locale,
      );
      await tester.pumpAndSettle();
    }

    testWidgets('fr : intitulés français et données nominatives',
        (tester) async {
      await pumpNodeScreen(tester, const Locale('fr'));

      expect(find.text('Membres'), findsWidgets);
      expect(find.text('Directs 1 • Total 2'), findsOneWidget);
      expect(find.text('Avec sous-unités'), findsOneWidget);
      expect(find.text('Rechercher un membre'), findsOneWidget);
      expect(find.text('Awa Diallo'), findsOneWidget);
      expect(find.text('Yao Koffi'), findsOneWidget);
      expect(find.text('via Groupe Bethel'), findsOneWidget);
      expect(find.text('Ancien'), findsOneWidget);
      // Pagination : une seule page servie ⇒ pas de bouton fantôme.
      expect(find.text('Charger plus'), findsNothing);
    });

    testWidgets(
        'en : la section est traduite, le français codé en dur a disparu',
        (tester) async {
      await pumpNodeScreen(tester, const Locale('en'));

      expect(find.text('Members'), findsWidgets);
      expect(find.text('Direct 1 • Total 2'), findsOneWidget);
      expect(find.text('With sub-units'), findsOneWidget);
      expect(find.text('Search a member'), findsOneWidget);
      expect(find.text('via Groupe Bethel'), findsOneWidget);
      // Preuve que la section n'est plus en français figé.
      expect(find.text('Rechercher un membre'), findsNothing);
      expect(find.text('Avec sous-unités'), findsNothing);
      // Une clé non résolue s'afficherait telle quelle à l'écran.
      expect(find.textContaining('nodeMembers'), findsNothing);
    });

    testWidgets('ar : la section est traduite', (tester) async {
      await pumpNodeScreen(tester, const Locale('ar'));

      expect(find.text('الأعضاء'), findsWidgets);
      expect(find.text('مباشر 1 • الإجمالي 2'), findsOneWidget);
      expect(find.text('ابحث عن عضو'), findsOneWidget);
      expect(find.text('Rechercher un membre'), findsNothing);
      expect(find.textContaining('nodeMembers'), findsNothing);
    });

    testWidgets('changement de portée : le scope demandé repart au serveur',
        (tester) async {
      await pumpNodeScreen(tester, const Locale('fr'));
      asked.clear();

      await tester.tap(find.text('Ici'));
      await tester.pumpAndSettle();

      final membersCall = asked['/tenant/organization/nodes/n-1/members'];
      expect(membersCall, isNotNull);
      expect(membersCall!['scope'], 'DIRECT');
    });

    testWidgets(
        'roster vide : message explicite, jamais une liste muette',
        (tester) async {
      final emptyApi = _RosterApi(const <String, dynamic>{
        'nodeId': 'n-1',
        'content': <dynamic>[],
        'totalPages': 0,
      }, <String, Map<String, dynamic>?>{});

      tester.view.physicalSize = const Size(1200, 6000);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await pumpLocalized(
        tester,
        ProviderScope(
          child: NodeDetailScreen(
              nodeId: 'n-1', nodeName: 'Cellule Nord', apiService: emptyApi),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Aucun membre pour cette sélection.'), findsOneWidget);
    });
  });
}

/// Fake `ApiService` : la fiche nœud lit quatre endpoints avant le roster ;
/// on ne sert du contenu que pour `/members`, le reste en vide cohérent.
class _RosterApi extends ApiService {
  _RosterApi(this._rosterJson, this._asked) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> _rosterJson;
  final Map<String, Map<String, dynamic>?> _asked;

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    _asked[path] = params;
    final data = path.endsWith('/members')
        ? _rosterJson
        : (path.endsWith('/aggregate')
            ? <String, dynamic>{'nodeId': 'n-1'}
            : <dynamic>[]);
    return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data);
  }
}
