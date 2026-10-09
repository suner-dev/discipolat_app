// B9 — Test widget de l'écran de gestion des invitations (constat MO3).
//
// Couvre les 5 états (§5.0.2) et les issues métier : liste, vide, erreur +
// retry, création avec `emailSent = false` → proposition de copie du lien,
// renvoi. Un service factice remplace l'appel réseau (même contrat).
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/invitation_admin_service.dart';
import 'package:discipolat_mobile/presentation/screens/invitations/invitation_management_screen.dart';

class _FakeInvitationService extends InvitationAdminService {
  _FakeInvitationService();

  InvitationListResult listResult = InvitationListResult(items: <InvitationItem>[]);
  bool shouldThrowList = false;
  CreateInvitationResult createResult = CreateInvitationResult(success: true);
  ResendInvitationResult resendResult = ResendInvitationResult(emailSent: true);
  final List<String> cancelled = <String>[];

  /// Référentiels d'API — surchargés pour qu'aucun appel réseau réel ne parte
  /// depuis un test. Sans ces deux surcharges, la feuille de création
  /// afficherait un `LinearProgressIndicator` indéfini et `pumpAndSettle`
  /// expirerait.
  List<AssignableRole> rolesResult = const <AssignableRole>[
    AssignableRole(key: 'PASTEUR', label: 'Pasteur'),
    AssignableRole(key: 'MEMBRE', label: 'Membre'),
  ];
  List<OrganizationNodeRef> nodesResult = const <OrganizationNodeRef>[
    OrganizationNodeRef(id: 'node-a', name: 'Campus Nord', type: 'CAMPUS', level: 1),
  ];

  /// Trace les paramètres de création : c'est ce qui prouve que le scope
  /// ORGANIZATION transmet bien `organizationNodeId`.
  final List<Map<String, String?>> created = <Map<String, String?>>[];

  @override
  Future<InvitationListResult> list({
    int? page,
    int? size,
    String? status,
    String? q,
  }) async {
    if (shouldThrowList) throw Exception('réseau indisponible');
    return listResult;
  }

  @override
  Future<List<AssignableRole>> listAssignableRoles() async => rolesResult;

  @override
  Future<List<OrganizationNodeRef>> listOrganizationNodes() async => nodesResult;

  @override
  Future<CreateInvitationResult> create({
    required String email,
    required String role,
    String scopeType = 'TENANT',
    String? scopeId,
    String? organizationNodeId,
  }) async {
    created.add(<String, String?>{
      'email': email,
      'role': role,
      'scopeType': scopeType,
      'scopeId': scopeId,
      'organizationNodeId': organizationNodeId,
    });
    return createResult;
  }

  @override
  Future<ResendInvitationResult> resend(String id) async => resendResult;

  @override
  Future<void> cancel(String id) async => cancelled.add(id);
}

InvitationItem _item({
  String id = 'i-1',
  String email = 'pasteur@eglise.com',
  String status = 'PENDING',
}) =>
    InvitationItem(
      id: id,
      email: email,
      role: 'PASTEUR',
      status: status,
      scopeType: 'TENANT',
      expiresAt: DateTime.utc(2026, 10, 5),
    );

Future<void> _pump(WidgetTester tester, InvitationAdminService service) async {
  await tester.pumpWidget(
    MaterialApp(home: InvitationManagementScreen(service: service)),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('affiche la liste des invitations avec statut et actions',
      (tester) async {
    final service = _FakeInvitationService()
      ..listResult = InvitationListResult(items: <InvitationItem>[_item()]);

    await _pump(tester, service);

    expect(find.text('pasteur@eglise.com'), findsOneWidget);
    expect(find.text('Statut : En attente'), findsOneWidget);
    // Une invitation en attente offre renvoi + annulation.
    expect(find.byTooltip('Renvoyer'), findsOneWidget);
    expect(find.byTooltip('Annuler'), findsOneWidget);
  });

  testWidgets('affiche l\'état vide', (tester) async {
    final service = _FakeInvitationService()
      ..listResult = InvitationListResult(items: <InvitationItem>[]);

    await _pump(tester, service);

    expect(find.text('Aucune invitation'), findsOneWidget);
  });

  testWidgets('affiche l\'état d\'erreur avec un retry', (tester) async {
    final service = _FakeInvitationService()..shouldThrowList = true;

    await _pump(tester, service);

    expect(find.text('Impossible de charger les invitations.'), findsOneWidget);
    expect(find.text('Réessayer'), findsOneWidget);

    // Le retry rejoue le chargement : rétabli, la liste s'affiche.
    service.shouldThrowList = false;
    service.listResult = InvitationListResult(items: <InvitationItem>[_item()]);
    await tester.tap(find.text('Réessayer'));
    await tester.pumpAndSettle();
    expect(find.text('pasteur@eglise.com'), findsOneWidget);
  });

  testWidgets('création avec email non envoyé → propose de copier le lien',
      (tester) async {
    final service = _FakeInvitationService()
      ..createResult = CreateInvitationResult(
        success: true,
        invitationLink: 'https://app.discipolat.com/accept-invitation?token=abc',
        emailSent: false,
      );

    await _pump(tester, service);

    await tester.tap(find.text('Inviter'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const ValueKey<String>('invitation-email-field')), 'nouveau@eglise.com');
    // Le bouton se réactive sur le contenu du champ : il faut une frame avant
    // de pouvoir le taper.
    await tester.pumpAndSettle();
    await tester.tap(find.text('Envoyer l’invitation'));
    await tester.pumpAndSettle();

    expect(find.text('Copier le lien'), findsOneWidget);
  });

  testWidgets('renvoi d\'une invitation en attente → copier le lien si non envoyé',
      (tester) async {
    final service = _FakeInvitationService()
      ..listResult = InvitationListResult(items: <InvitationItem>[_item()])
      ..resendResult = ResendInvitationResult(
        emailSent: false,
        invitationLink: 'https://app.discipolat.com/accept-invitation?token=new',
      );

    await _pump(tester, service);

    await tester.tap(find.byTooltip('Renvoyer'));
    await tester.pumpAndSettle();

    expect(find.text('Copier le lien'), findsOneWidget);
  });

  testWidgets('annulation confirmée appelle le service', (tester) async {
    final service = _FakeInvitationService()
      ..listResult = InvitationListResult(items: <InvitationItem>[_item(id: 'inv-42')]);

    await _pump(tester, service);

    await tester.tap(find.byTooltip('Annuler'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Annuler l’invitation'));
    await tester.pumpAndSettle();

    expect(service.cancelled, contains('inv-42'));
  });

  // ── Non-régression B9 : le scope ORGANIZATION doit transmettre le nœud ──────
  //
  // Régression du constat MO3 : l'UI proposait « Organisation » mais n'envoyait
  // jamais `organizationNodeId`. Or `InvitationService.createInvitation` refuse
  // tout scope non-TENANT sans nœud (INVITATION_SCOPE_INVALID, 400) : la
  // création échouait donc à 100 % pour cette portée. Ces 3 tests tombent si le
  // correctif est neutralisé.

  testWidgets('scope ORGANIZATION : le nœud choisi est transmis au serveur',
      (tester) async {
    final service = _FakeInvitationService()
      ..createResult = CreateInvitationResult(success: true, emailSent: true);

    await _pump(tester, service);
    await tester.tap(find.text('Inviter'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const ValueKey<String>('invitation-email-field')), 'membre@eglise.com');
    await tester.pumpAndSettle();

    // Bascule de portée
    await tester.tap(find.byKey(const ValueKey<String>('invitation-scope-dropdown')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Organisation').last);
    await tester.pumpAndSettle();

    // Le sélecteur de nœud n'apparaît QUE pour un scope non-TENANT.
    expect(find.text('Nœud organisationnel'), findsOneWidget);
    // Ouvrir le sélecteur de nœud, puis choisir.
    await tester.tap(find.byKey(const ValueKey<String>('invitation-node-dropdown')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Campus Nord — CAMPUS').last);
    await tester.pumpAndSettle();

    await tester.tap(find.text('Envoyer l’invitation'));
    await tester.pumpAndSettle();

    expect(service.created, hasLength(1));
    expect(service.created.first['scopeType'], 'ORGANIZATION');
    expect(service.created.first['organizationNodeId'], 'node-a');
  });

  testWidgets(
      'scope ORGANIZATION : le bouton d\'envoi reste inactif tant qu\'aucun nœud n\'est choisi',
      (tester) async {
    final service = _FakeInvitationService();

    await _pump(tester, service);
    await tester.tap(find.text('Inviter'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const ValueKey<String>('invitation-email-field')), 'membre@eglise.com');
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey<String>('invitation-scope-dropdown')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Organisation').last);
    await tester.pumpAndSettle();

    final submit = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Envoyer l’invitation'),
    );
    expect(submit.onPressed, isNull,
        reason: 'envoi bloqué : le serveur exigerait organizationNodeId');
    expect(find.text('Choisissez un nœud organisationnel pour continuer.'),
        findsOneWidget);
    expect(service.created, isEmpty);
  });

  testWidgets('scope ORGANIZATION : changer de portée réinitialise le nœud choisi',
      (tester) async {
    final service = _FakeInvitationService();

    await _pump(tester, service);
    await tester.tap(find.text('Inviter'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const ValueKey<String>('invitation-email-field')), 'membre@eglise.com');
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey<String>('invitation-scope-dropdown')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Organisation').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey<String>('invitation-node-dropdown')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Campus Nord — CAMPUS').last);
    await tester.pumpAndSettle();

    // Retour sur TENANT : plus aucun sélecteur de nœud, et surtout pas de nœud
    // fantôme qui fuiterait dans le payload d'un scope Église.
    await tester.tap(find.byKey(const ValueKey<String>('invitation-scope-dropdown')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Église').last);
    await tester.pumpAndSettle();

    expect(find.text('Nœud organisationnel'), findsNothing);

    await tester.tap(find.text('Envoyer l’invitation'));
    await tester.pumpAndSettle();

    expect(service.created.first['scopeType'], 'TENANT');
    expect(service.created.first['organizationNodeId'], isNull);
  });

  testWidgets('les rôles proposés viennent de l\'API (rôles custom inclus)',
      (tester) async {
    final service = _FakeInvitationService()
      ..rolesResult = const <AssignableRole>[
        AssignableRole(key: 'PASTEUR', label: 'Pasteur'),
        AssignableRole(key: 'SECRETAIRE_STUDIO', label: 'Secrétaire studio'),
      ];

    await _pump(tester, service);
    await tester.tap(find.text('Inviter'));
    await tester.pumpAndSettle();

    // Ouvre le sélecteur : c'est le seul endroit où les libellés sont rendus.
    await tester.tap(find.byKey(const ValueKey<String>('invitation-role-dropdown')));
    await tester.pumpAndSettle();

    // Un rôle custom du tenant doit être proposable : la liste n'est plus figée.
    expect(find.text('Secrétaire studio'), findsOneWidget);
    // …et un rôle de l'ancienne liste en dur ne doit PAS apparaître s'il n'est
    // pas renvoyé par l'API.
    expect(find.text('Chef de famille'), findsNothing);
  });
}
