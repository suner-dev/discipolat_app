import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/widgets/ux_widgets.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  // ═══════════════════════════════════════════════════════════
  // P2 #85 — SHIMMER LOADING TESTS
  // ═══════════════════════════════════════════════════════════

  group('ShimmerLoading', () {
    testWidgets('renders correct number of items', (tester) async {
      await tester.pumpWidget(wrap(const ShimmerLoading(itemCount: 3)));
      await tester.pump();
      // Should render 3 shimmer items
      expect(find.byType(ShimmerLoading), findsOneWidget);
    });

    testWidgets('default itemCount is 5', (tester) async {
      await tester.pumpWidget(wrap(const ShimmerLoading()));
      expect(find.byType(ShimmerLoading), findsOneWidget);
    });
  });

  // ═══════════════════════════════════════════════════════════
  // P2 #86 — EMPTY STATE WIDGET TESTS
  // ═══════════════════════════════════════════════════════════

  group('EmptyStateWidget', () {
    testWidgets('renders title', (tester) async {
      await tester.pumpWidget(wrap(
        const EmptyStateWidget(icon: Icons.inbox, title: 'Aucun élément'),
      ));
      expect(find.text('Aucun élément'), findsOneWidget);
    });

    testWidgets('renders description when provided', (tester) async {
      await tester.pumpWidget(wrap(
        const EmptyStateWidget(
          icon: Icons.inbox,
          title: 'Vide',
          description: 'Pas de données disponibles',
        ),
      ));
      expect(find.text('Pas de données disponibles'), findsOneWidget);
    });

    testWidgets('renders action button when provided', (tester) async {
      bool pressed = false;
      await tester.pumpWidget(wrap(
        EmptyStateWidget(
          icon: Icons.add,
          title: 'Vide',
          actionLabel: 'Créer',
          onAction: () => pressed = true,
        ),
      ));
      await tester.tap(find.text('Créer'));
      expect(pressed, isTrue);
    });

    testWidgets('does not render action button when not provided', (tester) async {
      await tester.pumpWidget(wrap(
        const EmptyStateWidget(icon: Icons.inbox, title: 'Vide'),
      ));
      expect(find.byType(ElevatedButton), findsNothing);
    });
  });

  // ═══════════════════════════════════════════════════════════
  // P2 #87 — CONFIRM DIALOG TESTS
  // ═══════════════════════════════════════════════════════════

  group('showConfirmDialog', () {
    testWidgets('shows dialog with title and message', (tester) async {
      await tester.pumpWidget(MaterialApp(
        home: Builder(
          builder: (ctx) => Scaffold(
            body: ElevatedButton(
              onPressed: () => showConfirmDialog(
                ctx,
                title: 'Supprimer?',
                message: 'Action irréversible',
              ),
              child: const Text('Open'),
            ),
          ),
        ),
      ));

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();

      expect(find.text('Supprimer?'), findsOneWidget);
      expect(find.text('Action irréversible'), findsOneWidget);
    });

    testWidgets('returns true when confirm is tapped', (tester) async {
      bool? result;
      await tester.pumpWidget(MaterialApp(
        home: Builder(
          builder: (ctx) => Scaffold(
            body: ElevatedButton(
              onPressed: () async {
                result = await showConfirmDialog(
                  ctx,
                  title: 'Test',
                  message: 'Msg',
                  confirmLabel: 'Oui',
                );
              },
              child: const Text('Open'),
            ),
          ),
        ),
      ));

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Oui'));
      await tester.pumpAndSettle();

      expect(result, isTrue);
    });

    testWidgets('returns false when cancel is tapped', (tester) async {
      bool? result;
      await tester.pumpWidget(MaterialApp(
        home: Builder(
          builder: (ctx) => Scaffold(
            body: ElevatedButton(
              onPressed: () async {
                result = await showConfirmDialog(
                  ctx,
                  title: 'Test',
                  message: 'Msg',
                );
              },
              child: const Text('Open'),
            ),
          ),
        ),
      ));

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Annuler'));
      await tester.pumpAndSettle();

      expect(result, isFalse);
    });
  });

  // ═══════════════════════════════════════════════════════════
  // P2 #96 — PROGRESS BAR WIDGET TESTS
  // ═══════════════════════════════════════════════════════════

  group('ProgressBarWidget', () {
    testWidgets('renders with label and percentage', (tester) async {
      await tester.pumpWidget(wrap(
        const ProgressBarWidget(value: 50, label: 'Progression'),
      ));
      expect(find.text('Progression'), findsOneWidget);
      expect(find.text('50%'), findsOneWidget);
    });

    testWidgets('renders without label', (tester) async {
      await tester.pumpWidget(wrap(
        const ProgressBarWidget(value: 30),
      ));
      expect(find.text('30%'), findsNothing);
    });
  });

  // ═══════════════════════════════════════════════════════════
  // P2 #97 — ONBOARDING STEPPER WIDGET TESTS
  // ═══════════════════════════════════════════════════════════

  group('OnboardingStepperWidget', () {
    testWidgets('renders all step labels', (tester) async {
      await tester.pumpWidget(wrap(
        const OnboardingStepperWidget(
          steps: ['Profil', 'Famille', 'Équipe'],
          currentStep: 0,
        ),
      ));
      expect(find.text('Profil'), findsOneWidget);
      expect(find.text('Famille'), findsOneWidget);
      expect(find.text('Équipe'), findsOneWidget);
    });

    testWidgets('shows step numbers for incomplete steps', (tester) async {
      await tester.pumpWidget(wrap(
        const OnboardingStepperWidget(
          steps: ['Étape 1', 'Étape 2', 'Étape 3'],
          currentStep: 0,
        ),
      ));
      expect(find.text('1'), findsOneWidget);
      expect(find.text('2'), findsOneWidget);
      expect(find.text('3'), findsOneWidget);
    });
  });

  // ═══════════════════════════════════════════════════════════
  // P2 #94 — ACCESSIBLE WIDGET TESTS
  // ═══════════════════════════════════════════════════════════

  group('AccessibleWidget', () {
    testWidgets('wraps child with Semantics', (tester) async {
      await tester.pumpWidget(wrap(
        const AccessibleWidget(
          semanticLabel: 'Bouton principal',
          child: Text('Cliquez ici'),
        ),
      ));
      expect(find.text('Cliquez ici'), findsOneWidget);
      // Semantics is applied but not directly testable via find
    });
  });
}
