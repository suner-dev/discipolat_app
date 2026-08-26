import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/l10n/app_localizations.dart';

/// Pompe un écran dans un MaterialApp localisé en FR (langue par défaut du
/// produit) afin que les écrans utilisant AppLocalizations fonctionnent
/// dans les widget tests.
Future<void> pumpLocalized(
  WidgetTester tester,
  Widget screen, {
  Locale locale = const Locale('fr'),
}) async {
  await tester.pumpWidget(MaterialApp(
    locale: locale,
    localizationsDelegates: const [
      AppLocalizations.delegate,
      GlobalMaterialLocalizations.delegate,
      GlobalWidgetsLocalizations.delegate,
      GlobalCupertinoLocalizations.delegate,
    ],
    supportedLocales: AppLocalizations.supportedLocales,
    home: screen,
  ));
}
