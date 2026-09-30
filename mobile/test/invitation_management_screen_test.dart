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
  Future<CreateInvitationResult> create({
    required String email,
    required String role,
    String scopeType = 'TENANT',
    String? scopeId,
    String? organizationNodeId,
  }) async =>
      createResult;

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

    await tester.enterText(find.byType(TextField), 'nouveau@eglise.com');
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
}
