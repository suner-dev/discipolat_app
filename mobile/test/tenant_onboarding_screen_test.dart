// B7 — Test widget du wizard d'onboarding mobile (constat MO1, contrat §3.1).
//
// Couvre le §5.0.2 et le parcours : rendu de l'étape courante + progression,
// complétion, skip avec motif obligatoire, skip bloqué sans motif, erreur 409
// actionnable, reprise sur la première étape ouverte, fin de parcours.
// Le service est remplacé par un faux (même contrat) : AUCUN appel réseau.
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/tenant_onboarding_service.dart';
import 'package:discipolat_mobile/models/onboarding_step.dart';
import 'package:discipolat_mobile/presentation/screens/tenant/tenant_onboarding_screen.dart';

OnboardingStep _step({
  required int order,
  required String title,
  OnboardingStepType type = OnboardingStepType.memberImport,
  bool completed = false,
  bool skippable = false,
  bool skipRequiresReason = false,
}) =>
    OnboardingStep(
      id: 'step-$order',
      stepType: type,
      stepOrder: order,
      title: title,
      description: 'Description $title',
      status: completed
          ? OnboardingStepStatus.completed
          : OnboardingStepStatus.pending,
      isCompleted: completed,
      isSkippable: skippable,
      skipRequiresReason: skipRequiresReason,
      startedAt: null,
      completedAt: null,
      completedData: null,
    );

/// Parcours à 7 étapes ; `open` = index de la première étape non terminée.
OnboardingProgress _progress({
  required List<OnboardingStep> steps,
  bool isComplete = false,
}) =>
    OnboardingProgress(
      totalSteps: steps.length,
      completedSteps: steps.where((s) => s.isCompleted).length,
      skippedSteps: 0,
      percentage: isComplete ? 100 : 0,
      isComplete: isComplete,
      steps: steps,
    );

class _FakeService extends TenantOnboardingService {
  _FakeService();

  OnboardingProgress progress = _progress(steps: const <OnboardingStep>[]);
  final List<MapEntry<String, Map<String, dynamic>?>> completed =
      <MapEntry<String, Map<String, dynamic>?>>[];
  final List<String> skipped = <String>[];
  Object? completeError;
  Object? loadError;

  @override
  Future<OnboardingProgress> fetchProgress() async {
    if (loadError != null) throw loadError!;
    return progress;
  }

  @override
  Future<OnboardingStep> completeStep(
      String stepId, [Map<String, dynamic>? data]) async {
    if (completeError != null) throw completeError!;
    completed.add(MapEntry(stepId, data));
    return progress.steps.firstWhere((s) => s.id == stepId);
  }

  @override
  Future<OnboardingStep> skipStep(String stepId, {String? reason}) async {
    skipped.add('$stepId:$reason');
    return progress.steps.firstWhere((s) => s.id == stepId);
  }
}

Future<void> _pump(WidgetTester tester, _FakeService service) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [tenantOnboardingServiceProvider.overrideWithValue(service)],
      child: const MaterialApp(home: TenantOnboardingScreen()),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('affiche l\'étape courante et la progression (7 étapes)',
      (tester) async {
    final service = _FakeService()
      ..progress = _progress(steps: <OnboardingStep>[
        for (var i = 0; i < 7; i++)
          _step(order: i, title: 'Étape $i', completed: false),
      ]);

    await _pump(tester, service);

    expect(find.text('Étape 0'), findsWidgets);
    expect(find.textContaining('0/7'), findsOneWidget);
  });

  testWidgets('complétion envoie l\'étape courante avec ses données',
      (tester) async {
    final service = _FakeService()
      ..progress = _progress(steps: <OnboardingStep>[
        _step(order: 0, title: 'Import des membres'),
      ]);

    await _pump(tester, service);

    await tester.enterText(find.byType(TextField).first, '12');
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(service.completed, hasLength(1));
    expect(service.completed.first.key, 'step-0');
    expect(service.completed.first.value, {'importedCount': 12});
  });

  testWidgets('skip avec motif obligatoire appelle le service', (tester) async {
    final service = _FakeService()
      ..progress = _progress(steps: <OnboardingStep>[
        _step(order: 0, title: 'Import', skippable: true, skipRequiresReason: true),
      ]);

    await _pump(tester, service);

    // Le champ « Motif » est le dernier TextField (sous le formulaire).
    await tester.enterText(find.byType(TextField).last, 'plus tard');
    await tester.tap(find.text('Ignorer cette étape'));
    await tester.pumpAndSettle();

    expect(service.skipped, contains('step-0:plus tard'));
  });

  testWidgets('skip bloqué si un motif est requis', (tester) async {
    final service = _FakeService()
      ..progress = _progress(steps: <OnboardingStep>[
        _step(order: 0, title: 'Import', skippable: true, skipRequiresReason: true),
      ]);

    await _pump(tester, service);

    await tester.tap(find.text('Ignorer cette étape'));
    await tester.pumpAndSettle();

    expect(
      find.text('Un motif est requis pour ignorer cette étape.'),
      findsOneWidget,
    );
    expect(service.skipped, isEmpty);
  });

  testWidgets('erreur 409 STEP_ORDER_VIOLATION → message actionnable',
      (tester) async {
    final service = _FakeService()
      ..progress = _progress(steps: <OnboardingStep>[
        _step(order: 0, title: 'Import'),
      ])
      ..completeError = Exception('HTTP 409 CONFLICT: STEP_ORDER_VIOLATION');

    await _pump(tester, service);

    await tester.enterText(find.byType(TextField).first, '5');
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(find.text('Ordre des étapes invalide.'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('reprise sur la première étape ouverte', (tester) async {
    final service = _FakeService()
      ..progress = _progress(steps: <OnboardingStep>[
        _step(order: 0, title: 'Identité', completed: true),
        _step(order: 1, title: 'Structure', type: OnboardingStepType.structure),
        _step(order: 2, title: 'Rôles', type: OnboardingStepType.roles),
      ]);

    await _pump(tester, service);

    // L'étape courante affichée est la première non terminée (« Structure »).
    // Le titre apparaît à la fois dans le corps et la puce du stepper.
    expect(find.text('Structure'), findsWidgets);
    expect(find.textContaining('1/3'), findsOneWidget);
  });

  testWidgets('fin de parcours : écran de confirmation', (tester) async {
    final service = _FakeService()
      ..progress = _progress(
        isComplete: true,
        steps: <OnboardingStep>[
          _step(order: 0, title: 'Identité', completed: true),
        ],
      );

    await _pump(tester, service);

    expect(find.text('Configuration terminée'), findsOneWidget);
    expect(find.text('Aller au tableau de bord'), findsOneWidget);
  });
}
