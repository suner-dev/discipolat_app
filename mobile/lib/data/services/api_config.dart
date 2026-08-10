import 'package:flutter/foundation.dart';

/// Configuration de l'URL de base de l'API pour l'application mobile.
///
/// ## Ordre de résolution
/// 1. `--dart-define=API_URL=...` (défini au build — priorité absolue)
/// 2. Mode debug (`kDebugMode`) : API locale (`http://10.0.2.2:8080/api/v1`)
///    pour tester sur l'émulateur Android sans paramètre supplémentaire
/// 3. Sinon : URL de production par défaut (Render)
///
/// ## Utilisation
/// ```bash
/// # Production (URL Render par défaut)
/// flutter run --release
///
/// # Debug : pointe automatiquement vers l'API locale (10.0.2.2)
/// flutter run
///
/// # Dépasser la valeur automatique (périphérique physique, etc.)
/// flutter run --dart-define=API_URL=http://192.168.1.42:8080/api/v1
///
/// # iOS Simulator (localhost fonctionne)
/// flutter run --dart-define=API_URL=http://localhost:8080/api/v1
/// ```
class ApiConfig {
  ApiConfig._();

  /// URL de production — Render
  static const String productionUrl = 'https://discipolat-api.onrender.com/api/v1';

  /// URL locale pour le développement — `10.0.2.2` = localhost vu depuis
  /// l'émulateur Android (le port 8080 correspond au backend Spring Boot).
  static const String localUrl = 'http://10.0.2.2:8080/api/v1';

  /// URL de l'API résolue dynamiquement.
  ///
  /// 1. Priorité à `--dart-define=API_URL=...` (compile-time)
  /// 2. En mode debug : API locale (émulateur Android)
  /// 3. Par défaut : production (Render)
  static String get baseUrl {
    const fromEnvironment = String.fromEnvironment('API_URL');
    if (fromEnvironment.isNotEmpty) {
      return fromEnvironment;
    }
    if (kDebugMode) {
      return localUrl;
    }
    return productionUrl;
  }
}
