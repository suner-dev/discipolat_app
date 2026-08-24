import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/growth_projection/growth_projection_screen.dart';

void main() {
  group('GrowthProjectionScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GrowthProjectionScreen()));
      expect(find.text('📊 Projection de Croissance'), findsOneWidget);
    });

    testWidgets('shows current vs projected', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GrowthProjectionScreen()));
      expect(find.text('156'), findsOneWidget);
      expect(find.text('198'), findsOneWidget);
      expect(find.text('+27% de croissance prévue'), findsOneWidget);
    });

    testWidgets('shows simulator section', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GrowthProjectionScreen()));
      expect(find.text('Simulateur'), findsOneWidget);
      expect(find.text('Conversions/mois'), findsOneWidget);
    });

    testWidgets('shows family breakdown', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GrowthProjectionScreen()));
      expect(find.text('Famille Grâce'), findsOneWidget);
      expect(find.text('Famille Espoir'), findsOneWidget);
    });
  });
}
