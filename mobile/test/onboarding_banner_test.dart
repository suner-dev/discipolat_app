// B7 — Test widget de la bannière d'onboarding (constat MO1).
//
// Couvre : affichée (configuration non terminée), masquée (terminée), erreur de
// statut (masquée gracieusement, sans crash), rejet en session, navigation.
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/tenant_onboarding_service.dart';
import 'package:discipolat_mobile/models/onboarding_step.dart';
import 'package:discipolat_mobile/presentation/widgets/onboarding_banner.dart';

class _FakeOnboardingService extends TenantOnboardingService {
  _FakeOnboardingService();

  OnboardingStatus status = const OnboardingStatus(
    completed: false,
    completedAt: null,
    completedBy: null,
    totalSteps: 7,
    completedSteps: 2,
    skippedSteps: 0,
    percentage: 28,
  );
  bool shouldThrow = false;

  @override
  Future<OnboardingStatus> fetchStatus() async {
    if (shouldThrow) throw Exception('réseau indisponible');
    return status;
  }
}

Future<void> _pumpBanner(
  WidgetTester tester,
  TenantOnboardingService service, {
  void Function(BuildContext, String)? onNavigate,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: OnboardingBanner(service: service, onNavigate: onNavigate),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  setUp(() => OnboardingBanner.debugResetSession());

  testWidgets('affichée tant que la configuration n\'est pas terminée',
      (tester) async {
    await _pumpBanner(tester, _FakeOnboardingService());

    expect(find.text('Finalisez la configuration de votre église'),
        findsOneWidget);
  });

  testWidgets('masquée quand la configuration est terminée', (tester) async {
    final service = _FakeOnboardingService()
      ..status = const OnboardingStatus(
        completed: true,
        completedAt: null,
        completedBy: null,
        totalSteps: 7,
        completedSteps: 7,
        skippedSteps: 0,
        percentage: 100,
      );

    await _pumpBanner(tester, service);

    expect(find.text('Finalisez la configuration de votre église'),
        findsNothing);
  });

  testWidgets('erreur de statut → masquée sans crash', (tester) async {
    final service = _FakeOnboardingService()..shouldThrow = true;

    await _pumpBanner(tester, service);

    expect(find.text('Finalisez la configuration de votre église'),
        findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('un tap navigue vers l\'écran d\'onboarding', (tester) async {
    String? navigatedTo;
    await _pumpBanner(
      tester,
      _FakeOnboardingService(),
      onNavigate: (context, route) => navigatedTo = route,
    );

    await tester.tap(find.text('Finalisez la configuration de votre église'));
    await tester.pumpAndSettle();

    expect(navigatedTo, '/tenant/onboarding');
  });

  testWidgets('dismissible pour la session', (tester) async {
    await _pumpBanner(tester, _FakeOnboardingService());

    await tester.tap(find.byTooltip('Ne plus afficher'));
    await tester.pumpAndSettle();

    expect(find.text('Finalisez la configuration de votre église'),
        findsNothing);
    expect(OnboardingBanner.sessionDismissed, isTrue);
  });
}
