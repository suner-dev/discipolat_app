/// Configuration de l'URL de base de l'API pour l'application mobile.
///
/// ## Ordre de résolution
/// 1. `--dart-define=API_URL=...` (défini au build)
/// 2. Variable d'environnement `API_URL`
/// 3. URL de production par défaut
///
/// ## Utilisation
/// ```bash
/// # Production (URL Render par défaut)
/// flutter run
///
/// # Android Emulator (localhost)
/// flutter run --dart-define=API_URL=http://10.0.2.2:8080/api/v1
///
/// # iOS Simulator (localhost)
/// flutter run --dart-define=API_URL=http://localhost:8080/api/v1
///
/// # Périphérique physique (réseau local)
/// flutter run --dart-define=API_URL=http://192.168.1.42:8080/api/v1
/// ```
class ApiConfig {
  ApiConfig._();

  /// URL de production — Render
  static const String productionUrl = 'https://discipolat-api.onrender.com/api/v1';

  /// URL de l'API résolue dynamiquement.
  ///
  /// 1. Priorité à `--dart-define=API_URL=...` (compile-time)
  /// 2. Par défaut : production (Render)
  static String get baseUrl =>
      const String.fromEnvironment('API_URL', defaultValue: productionUrl);

}
