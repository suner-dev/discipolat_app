import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/l10n/app_localizations.dart';

void main() {
  group('AppLocalizations', () {
    test('French translations loaded for fr locale', () {
      final localizations = AppLocalizations(const Locale('fr'));
      expect(localizations.login, 'Connexion');
      expect(localizations.logout, 'Déconnexion');
      expect(localizations.save, 'Enregistrer');
      expect(localizations.cancel, 'Annuler');
      expect(localizations.delete, 'Supprimer');
      expect(localizations.search, 'Rechercher');
      expect(localizations.loading, 'Chargement...');
    });

    test('English translations loaded for en locale', () {
      final localizations = AppLocalizations(const Locale('en'));
      expect(localizations.login, 'Login');
      expect(localizations.logout, 'Logout');
      expect(localizations.save, 'Save');
      expect(localizations.cancel, 'Cancel');
      expect(localizations.delete, 'Delete');
      expect(localizations.search, 'Search');
    });

    test('Portuguese translations loaded for pt locale', () {
      final localizations = AppLocalizations(const Locale('pt'));
      expect(localizations.login, 'Entrar');
      expect(localizations.logout, 'Sair');
      expect(localizations.save, 'Salvar');
      expect(localizations.cancel, 'Cancelar');
      expect(localizations.delete, 'Excluir');
      expect(localizations.search, 'Pesquisar');
    });

    test('Fallback to French for unknown locale', () {
      final localizations = AppLocalizations(const Locale('de'));
      // Should fall back to French
      expect(localizations.login, 'Connexion');
      expect(localizations.logout, 'Déconnexion');
    });

    test('translate method works for all keys', () {
      final fr = AppLocalizations(const Locale('fr'));
      final en = AppLocalizations(const Locale('en'));
      final pt = AppLocalizations(const Locale('pt'));

      // Common strings
      expect(fr.translate('appTitle'), 'Discipolat');
      expect(en.translate('appTitle'), 'Discipolat');
      expect(pt.translate('appTitle'), 'Discipolat');

      // Navigation
      expect(fr.translate('navDashboard'), 'Tableau de bord');
      expect(en.translate('navDashboard'), 'Dashboard');
      expect(pt.translate('navDashboard'), 'Painel');

      // Unknown key returns the key itself
      expect(fr.translate('unknown_key'), 'unknown_key');
    });

    test('supportedLocales contains fr, en, pt', () {
      expect(AppLocalizations.supportedLocales, contains(const Locale('fr')));
      expect(AppLocalizations.supportedLocales, contains(const Locale('en')));
      expect(AppLocalizations.supportedLocales, contains(const Locale('pt')));
    });

    test('delegate is not null', () {
      expect(AppLocalizations.delegate, isNotNull);
    });

    test('delegate supports fr, en, pt', () {
      final delegate = AppLocalizations.delegate;
      expect(delegate.isSupported(const Locale('fr')), true);
      expect(delegate.isSupported(const Locale('en')), true);
      expect(delegate.isSupported(const Locale('pt')), true);
      expect(delegate.isSupported(const Locale('de')), false);
    });

    test('all navigation keys have translations', () async {
      for (final locale in ['fr', 'en', 'pt']) {
        final loc = AppLocalizations(Locale(locale));
        expect(loc.navDashboard.isNotEmpty, true, reason: 'navDashboard missing for $locale');
        expect(loc.navSouls.isNotEmpty, true, reason: 'navSouls missing for $locale');
        expect(loc.navFamilies.isNotEmpty, true, reason: 'navFamilies missing for $locale');
        expect(loc.navDepartments.isNotEmpty, true, reason: 'navDepartments missing for $locale');
        expect(loc.navReports.isNotEmpty, true, reason: 'navReports missing for $locale');
        expect(loc.navPrayers.isNotEmpty, true, reason: 'navPrayers missing for $locale');
        expect(loc.navEvents.isNotEmpty, true, reason: 'navEvents missing for $locale');
        expect(loc.navAlerts.isNotEmpty, true, reason: 'navAlerts missing for $locale');
        expect(loc.navMessages.isNotEmpty, true, reason: 'navMessages missing for $locale');
      }
    });

    test('all dashboard keys have translations', () async {
      for (final locale in ['fr', 'en', 'pt']) {
        final loc = AppLocalizations(Locale(locale));
        expect(loc.totalMembers.isNotEmpty, true, reason: 'totalMembers missing for $locale');
        expect(loc.activeMembers.isNotEmpty, true, reason: 'activeMembers missing for $locale');
        expect(loc.presentToday.isNotEmpty, true, reason: 'presentToday missing for $locale');
      }
    });

    test('all feature keys have translations', () async {
      for (final locale in ['fr', 'en', 'pt']) {
        final loc = AppLocalizations(Locale(locale));
        expect(loc.spiritualScore.isNotEmpty, true, reason: 'spiritualScore missing for $locale');
        expect(loc.smartAlertsTitle.isNotEmpty, true, reason: 'smartAlertsTitle missing for $locale');
        expect(loc.scanQrCode.isNotEmpty, true, reason: 'scanQrCode missing for $locale');
        expect(loc.offline.isNotEmpty, true, reason: 'offline missing for $locale');
        expect(loc.syncNow.isNotEmpty, true, reason: 'syncNow missing for $locale');
      }
    });
  });
}
