import 'package:flutter/material.dart';

/// Stub AppLocalizations for compilation.
/// Full i18n will be generated via `flutter gen-l10n` when pub get is run.
class AppLocalizations {
  final Locale locale;
  AppLocalizations(this.locale);

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate = _AppLocalizationsDelegate();

  // Common strings
  String get appTitle => 'Discipolat';
  String get login => 'Connexion';
  String get logout => 'Déconnexion';
  String get email => 'Adresse e-mail';
  String get password => 'Mot de passe';
  String get signIn => 'Se connecter';
  String get save => 'Enregistrer';
  String get cancel => 'Annuler';
  String get delete => 'Supprimer';
  String get search => 'Rechercher';
  String get loading => 'Chargement...';
  String get error => 'Erreur';
  String get noData => 'Aucune donnée';
  String get confirm => 'Confirmer';
  String get back => 'Retour';
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => ['fr', 'en'].contains(locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) async => AppLocalizations(locale);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
