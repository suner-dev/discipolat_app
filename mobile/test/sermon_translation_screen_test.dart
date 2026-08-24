import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/sermon_translations/sermon_translation_screen.dart';

void main() {
  group('SermonTranslationScreen', () {
    testWidgets('renders app bar with title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SermonTranslationScreen()));
      expect(find.text('🌍 Traduction des sermons'), findsOneWidget);
    });

    testWidgets('shows active translation card', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SermonTranslationScreen()));
      expect(find.text('Traduction en cours'), findsOneWidget);
      expect(find.text('Français → Anglais, Espagnol, Swahili'), findsOneWidget);
    });

    testWidgets('shows available languages', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SermonTranslationScreen()));
      expect(find.text('🇬🇧 Anglais'), findsOneWidget);
      expect(find.text('🇪🇸 Espagnol'), findsOneWidget);
      expect(find.text('🇰🇪 Swahili'), findsOneWidget);
    });

    testWidgets('shows previous translations', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SermonTranslationScreen()));
      expect(find.text('Traductions récentes'), findsOneWidget);
      expect(find.text('Culte du 24 août 2025'), findsOneWidget);
    });
  });
}
