import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Langues supportées par l'application (mêmes codes que le dictionnaire).
const kSupportedLocales = [
  Locale('fr'),
  Locale('en'),
  Locale('pt'),
  Locale('es'),
  Locale('sw'),
  Locale('ar'),
];

/// Noms affichés (drapeau + libellé) pour le sélecteur de langue.
const kLocaleNames = {
  'fr': '🇫🇷 Français',
  'en': '🇬🇧 English',
  'pt': '🇵🇹 Português',
  'es': '🇪🇸 Español',
  'sw': '🇰🇪 Kiswahili',
  'ar': '🇸🇦 العربية',
};

/// Fournit la locale courante, persistée dans SharedPreferences.
///
/// La langue choisie est appliquée à toute l'application (MaterialApp.locale)
/// et survit aux redémarrages. Le changement est immédiat : tous les écrans
/// qui lisent AppLocalizations.of(context) sont reconstruits.
class LocaleNotifier extends Notifier<Locale> {
  static const _key = 'discipolat_locale';

  @override
  Locale build() {
    // build() est synchrone : la valeur persistée est lue dans `init()`
    // (appelé au démarrage dans main) puis appliquée via `state`.
    return const Locale('fr');
  }

  /// Applique la locale persistée au démarrage (appelé dans main()).
  Future<void> init() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final saved = prefs.getString(_key);
      final system = WidgetsBinding.instance.platformDispatcher.locale.languageCode;
      final resolved = _resolve(saved, system);
      if (resolved != state) state = resolved;
    } catch (_) {
      // fallback silencieux
    }
  }

  Locale _resolve(String? saved, String? system) {
    if (saved != null && kLocaleNames.containsKey(saved)) return Locale(saved);
    if (system != null && kLocaleNames.containsKey(system)) return Locale(system);
    return const Locale('fr');
  }

  Future<void> setLocale(String code) async {
    if (!kLocaleNames.containsKey(code)) return;
    state = Locale(code);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, code);
    } catch (_) {/* best-effort : la langue s'applique pour la session */}
  }
}

final localeProvider = NotifierProvider<LocaleNotifier, Locale>(LocaleNotifier.new);