import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/predictions/predictions_screen.dart';

void main() {
  group('PredictionsScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PredictionsScreen()));
      expect(find.text('🔮 Prédictions ML'), findsOneWidget);
    });

    testWidgets('shows model overview', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PredictionsScreen()));
      expect(find.text('Modèle prédictif'), findsOneWidget);
      expect(find.text('198 (+27%)'), findsOneWidget);
      expect(find.text('12'), findsOneWidget); // baptêmes
    });

    testWidgets('shows department predictions', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PredictionsScreen()));
      expect(find.text('Prédictions par département'), findsOneWidget);
      expect(find.text('Louange'), findsOneWidget);
      expect(find.text('Jeunesse'), findsOneWidget);
    });

    testWidgets('shows confidence', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PredictionsScreen()));
      await tester.scrollUntilVisible(
        find.textContaining('78% de confiance'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.textContaining('78% de confiance'), findsOneWidget);
    });
  });
}
