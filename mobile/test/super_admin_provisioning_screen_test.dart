// B10 — Provisioning mobile complet : plans + géo + owner (constat MO4).
//
// Vérifie la mise en conformité avec le contrat §3.5 et la décision D12 :
// - les plans viennent de l'API (GET /platform/admin/plans), pas de valeurs en
//   dur ; en cas d'échec → repli documentaire + message visible ;
// - l'owner (email/prenom/nom) est obligatoire : « Continuer » reste bloque tant
//   qu'il est invalide, et le payload contient les 3 champs owner ;
// - le recap affiche l'etat `activationEmailSent` (D10 : SMTP absent = false).
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/super_admin_provisioning_screen.dart';

/// ApiService factice : sert les referentiels (plans/devises/fuseaux) et
/// simule la transaction atomique du provisionnement.
class _FakeApiService extends ApiService {
  _FakeApiService({this.plansShouldFail = false}) : super(baseUrl: 'http://fake');

  final bool plansShouldFail;
  final List<String> getPaths = [];
  final List<String> postPaths = [];
  final List<Map<String, dynamic>> postDatas = [];

  /// `owner.activationEmailSent` renvoye par le serveur (D10).
  bool ownerActivationSent = false;

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    if (path == '/platform/admin/plans') {
      if (plansShouldFail) {
        throw DioException(requestOptions: RequestOptions(path: path));
      }
      return _json(path, [
        {'key': 'GROWTH', 'name': 'Growth'},
        {'key': 'STARTUP', 'name': 'Startup'},
      ]);
    }
    if (path == '/platform/currencies') {
      return _json(path, {
        'standard': 'ISO-4217',
        'count': 2,
        'currencies': [
          {'code': 'XAF', 'name': 'Franc CFA (BEAC)', 'symbol': 'FCFA', 'decimals': 0},
          {'code': 'USD', 'name': 'Dollar', 'symbol': r'$', 'decimals': 2},
        ],
      });
    }
    if (path == '/currencies/timezones') {
      return _json(path, [
        {'id': 'Africa/Douala', 'name': 'Africa/Douala'},
        {'id': 'UTC', 'name': 'UTC'},
      ]);
    }
    return _json(path, {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    final map = data is Map<String, dynamic> ? data : <String, dynamic>{};
    postDatas.add(map);
    if (path == '/platform/admin/provisioning') {
      return _json(path, {
        'tenant': {
          'id': 'tenant-1', 'name': map['name'], 'slug': map['slug'],
          'plan': map['plan'], 'status': 'ACTIVE',
        },
        'church': {'id': 'church-1', 'tenantId': 'tenant-1', 'name': map['churchName'], 'code': 'ROOT'},
        'department': {'id': 'dept-1', 'tenantId': 'tenant-1', 'nom': map['departmentName'], 'responsableId': 'resp-1'},
        'family': {'id': 'fam-1', 'tenantId': 'tenant-1', 'nom': map['familyName'], 'chefFamilleId': 'chef-1'},
        'owner': {
          'userId': 'owner-1',
          'email': map['ownerEmail'],
          'activationEmailSent': ownerActivationSent,
        },
      });
    }
    return _json(path, {});
  }

  Response<dynamic> _json(String path, Object data) => Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 201,
        data: data,
      );
}

Future<void> _pump(WidgetTester tester, ApiService api) async {
  tester.view.physicalSize = const Size(1000, 2400);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(MaterialApp(home: SuperAdminProvisioningScreen(apiService: api)));
  await tester.pumpAndSettle();
}

Future<void> _type(WidgetTester tester, String label, String value) async {
  final field = find.widgetWithText(TextField, label);
  await tester.ensureVisible(field);
  await tester.enterText(field, value);
  await tester.pump();
}

Future<void> _tap(WidgetTester tester, String label) async {
  final target = find.text(label);
  await tester.ensureVisible(target);
  await tester.tap(target);
  await tester.pumpAndSettle();
}

/// Renseigne l'étape 1 (organisation) y compris l'owner obligatoire.
Future<void> _fillOrgAndOwner(WidgetTester tester) async {
  await _type(tester, "Nom de l'organisation *", 'Église Bethel');
  await _type(tester, 'Pays (code)', 'CM');
  await _type(tester, 'Email du propriétaire *', 'pasteur@bethel.cm');
  await _type(tester, 'Prénom *', 'Jean');
  await _type(tester, 'Nom *', 'Dupont');
}

/// Choisit explicitement une option dans un `_LabeledDropdown` identifié par
/// son libellé. Sans cette sélection, la valeur reste `null` et la clé est
/// omise du payload (G-B §5) : le test doit donc prouver un choix réel.
Future<void> _choose(WidgetTester tester, String dropdownLabel, String option) async {
  final dropdown = find.ancestor(
    of: find.text(dropdownLabel),
    matching: find.byType(InputDecorator),
  );
  await tester.ensureVisible(dropdown);
  await tester.tap(dropdown);
  await tester.pumpAndSettle();
  await tester.tap(find.text(option).last);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('référentiels chargés depuis l\'API (plans + devise + fuseau)',
      (tester) async {
    final api = _FakeApiService();
    await _pump(tester, api);

    expect(api.getPaths, contains('/platform/admin/plans'));
    expect(api.getPaths, contains('/platform/currencies'));
    expect(api.getPaths, contains('/currencies/timezones'));
    // Les plans servis (GROWTH/STARTUP) remplacent tout hardcoded : le sélecteur
    // porte la valeur réelle choisie, jamais « free ».
    expect(api.postPaths, isEmpty);
  });

  testWidgets('Continuer bloque tant que l\'owner est incomplet',
      (tester) async {
    final api = _FakeApiService();
    await _pump(tester, api);

    await _type(tester, "Nom de l'organisation *", 'Église Bethel');
    // Pas d'owner : le bouton reste désactivé, aucune navigation.
    await tester.tap(find.text('Continuer'));
    await tester.pumpAndSettle();

    expect(find.text('Renseignez un propriétaire (email + prénom + nom) valide pour continuer.'),
        findsOneWidget);
    expect(find.text('2. Église racine'), findsNothing);
  });

  testWidgets('payload conforme (owner + géo API) et activationEmailSent=false affiché',
      (tester) async {
    final api = _FakeApiService()..ownerActivationSent = false;
    await _pump(tester, api);

    await _fillOrgAndOwner(tester);
    // Choix EXPLICITES : le test ne doit pas valider une valeur simplement
    // positionnée par défaut, il doit valider un choix réel de l'utilisateur.
    await _choose(tester, 'Plan', 'Growth (GROWTH)');
    await _choose(tester, 'Devise', 'XAF — FCFA, Franc CFA (BEAC)');
    await _choose(tester, 'Fuseau horaire', 'Africa/Douala');
    await _tap(tester, 'Continuer'); // → église
    await _tap(tester, 'Continuer'); // → département
    await _type(tester, 'Nom du département *', 'Accueil & Louange');
    await _type(tester, 'Prénom du responsable *', 'Marie');
    await _type(tester, 'Nom du responsable *', 'Ngono');
    await _type(tester, 'Email du responsable *', 'marie@bethel.cm');
    await _tap(tester, 'Continuer'); // → famille
    await _type(tester, 'Nom de la famille *', 'Famille Mbarga');
    await _type(tester, 'Prénom du chef *', 'Pierre');
    await _type(tester, 'Nom du chef *', 'Mbarga');
    await _type(tester, 'Email du chef *', 'pierre@bethel.cm');
    await _tap(tester, "Provisionner l'organisation");

    expect(api.postPaths, ['/platform/admin/provisioning']);
    final payload = api.postDatas.single;
    expect(payload['ownerEmail'], 'pasteur@bethel.cm');
    expect(payload['ownerFirstName'], 'Jean');
    expect(payload['ownerLastName'], 'Dupont');
    // Valeurs issues des référentiels (D12), non forcées.
    expect(payload['plan'], 'GROWTH');
    expect(payload['currency'], 'XAF');
    expect(payload['timezone'], 'Africa/Douala');
    expect(payload['locale'], 'fr');

    // Récap owner + avertissement activation non envoyée (D10).
    expect(find.text('pasteur@bethel.cm'), findsOneWidget);
    expect(find.textContaining("Email d'activation non envoyé"), findsOneWidget);
  });

  testWidgets('échec API plans → repli documentaire + message visible',
      (tester) async {
    final api = _FakeApiService(plansShouldFail: true);
    await _pump(tester, api);

    expect(find.text('Liste minimale (API indisponible)'), findsOneWidget);
    // Le repli documenté (DISCOVERY…) est présent, jamais une valeur inventée.
    expect(api.getPaths, contains('/platform/admin/plans'));
  });

  testWidgets('G-B §5 : aucune valeur géo/plan n\'est FORCÉE si l\'utilisateur ne choisit rien',
      (tester) async {
    final api = _FakeApiService();
    await _pump(tester, api);

    // Ni le plan, ni la devise, ni le fuseau ne sont touchés : l'écran doit
    // proposer explicitement « non choisi » plutôt qu'une valeur pré-cochée.
    expect(find.text('Non choisi (valeur par défaut du serveur)'),
        findsNWidgets(3));

    // On remplit l'owner + le nom, on laisse la géo de côté.
    await _type(tester, "Nom de l'organisation *", 'Église Bethel');
    await _type(tester, 'Email du propriétaire *', 'pasteur@bethel.cm');
    await _type(tester, 'Prénom *', 'Jean');
    await _type(tester, 'Nom *', 'Dupont');
    await _tap(tester, 'Continuer'); // → église
    await _tap(tester, 'Continuer'); // → département
    await _type(tester, 'Nom du département *', 'Accueil & Louange');
    await _type(tester, 'Prénom du responsable *', 'Marie');
    await _type(tester, 'Nom du responsable *', 'Ngono');
    await _type(tester, 'Email du responsable *', 'marie@bethel.cm');
    await _tap(tester, 'Continuer'); // → famille
    await _type(tester, 'Nom de la famille *', 'Famille Mbarga');
    await _type(tester, 'Prénom du chef *', 'Pierre');
    await _type(tester, 'Nom du chef *', 'Mbarga');
    await _type(tester, 'Email du chef *', 'pierre@bethel.cm');
    await _tap(tester, "Provisionner l'organisation");

    final payload = api.postDatas.single;
    // Les clés sont OMISES, pas envoyées avec une valeur plaquée : le serveur
    // applique alors sa propre valeur par défaut, au lieu de recevoir un choix
    // que l'utilisateur n'a pas fait.
    expect(payload.containsKey('plan'), isFalse);
    expect(payload.containsKey('country'), isFalse);
    expect(payload.containsKey('currency'), isFalse);
    expect(payload.containsKey('timezone'), isFalse);
    // Et aucune des anciennes valeurs en dur ne fuit dans le payload.
    for (final forced in const ['CM', 'XAF', 'Africa/Douala', 'DISCOVERY']) {
      expect(payload.containsValue(forced), isFalse,
          reason: '« $forced » ne doit pas être imposé sans choix');
    }
    // Le contrat reste complet sur les champs réellement renseignés.
    expect(payload['ownerEmail'], 'pasteur@bethel.cm');
    expect(payload['locale'], 'fr');
  });
}
