import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/models/branding.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/main.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

void main() {
  group('colorFromHex', () {
    test('parse une couleur #RRGGBB', () {
      final color = colorFromHex('#0F766E', AppColors.defaultPrimary);
      expect(color, const Color(0xFF0F766E));
    });

    test('ignore le dièse optionnel', () {
      final color = colorFromHex('B91C1C', AppColors.defaultPrimary);
      expect(color, const Color(0xFFB91C1C));
    });

    test('retourne le fallback pour une valeur invalide', () {
      expect(colorFromHex('rouge', AppColors.defaultPrimary), AppColors.defaultPrimary);
      expect(colorFromHex('#12', AppColors.defaultPrimary), AppColors.defaultPrimary);
      expect(colorFromHex('', AppColors.defaultPrimary), AppColors.defaultPrimary);
      expect(colorFromHex(null, AppColors.defaultPrimary), AppColors.defaultPrimary);
    });
  });

  group('Branding.fromJson', () {
    test('parse le contrat backend PublicBrandingResponse', () {
      final branding = Branding.fromJson(const {
        'churchName': 'Église de la Grâce',
        'platformName': 'Discipolat',
        'slogan': 'Sauver et discipler',
        'primaryColor': '#0F766E',
        'accentColor': '#B91C1C',
        'buttonColor': '#0F766E',
        'fontFamily': 'Poppins',
        'allowDarkMode': true,
      });

      expect(branding.churchName, 'Église de la Grâce');
      expect(branding.primaryColor, const Color(0xFF0F766E));
      expect(branding.accentColor, const Color(0xFFB91C1C));
    });

    test('utilise les valeurs par défaut quand le champ est absent', () {
      final branding = Branding.fromJson(const {});
      expect(branding.churchName, 'Discipolat');
      expect(branding.primaryColor, AppColors.defaultPrimary);
      expect(branding.accentColor, AppColors.defaultGold);
    });
  });

  group('AppColors.applyBranding', () {
    tearDown(() {
      // Restaure la palette par défaut pour ne pas polluer les autres tests.
      AppColors.applyBranding(AppColors.defaultPrimary, accentColor: AppColors.defaultGold);
    });

    test('dérive primaryLight et primaryDark de la couleur principale', () {
      AppColors.applyBranding(const Color(0xFF0F766E), accentColor: const Color(0xFFB91C1C));

      expect(AppColors.primary, const Color(0xFF0F766E));
      expect(AppColors.primaryLight, isNot(const Color(0xFF0F766E)));
      expect(AppColors.primaryDark, isNot(const Color(0xFF0F766E)));
      expect(AppColors.accent, const Color(0xFFB91C1C));
    });

    test('le thème sombre suit la palette appliquée', () {
      AppColors.applyBranding(const Color(0xFF0F766E), accentColor: const Color(0xFFB91C1C));
      final theme = GlassTheme.darkTheme;

      // colorSchemeSeed est dérivé de AppColors.primary → la couleur primaire
      // du ColorScheme reflète l'identité de l'église.
      expect(theme.colorScheme.primary, isNot(AppColors.defaultPrimary));
    });
  });

  group('DiscipolatApp — thème dynamique', () {
    testWidgets('applique la couleur primaire chargée depuis le branding', (tester) async {
      // Branding « Église de la Grâce » avec une couleur distincte du vert par défaut.
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            brandingProvider.overrideWith((ref) async => const Branding(
                  churchName: 'Église de la Grâce',
                  primaryColor: Color(0xFF7C3AED),
                  accentColor: Color(0xFFF59E0B),
                )),
          ],
          child: const DiscipolatApp(),
        ),
      );
      await tester.pumpAndSettle();

      // La palette a été appliquée (le thème et tous les widgets la lisent au build).
      expect(AppColors.primary, const Color(0xFF7C3AED));
    });

    testWidgets('en cas de branding indisponible, garde la palette par défaut', (tester) async {
      AppColors.applyBranding(AppColors.defaultPrimary, accentColor: AppColors.defaultGold);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            brandingProvider.overrideWith((ref) async => const Branding()),
          ],
          child: const DiscipolatApp(),
        ),
      );
      await tester.pumpAndSettle();

      expect(AppColors.primary, AppColors.defaultPrimary);
    });
  });
}
