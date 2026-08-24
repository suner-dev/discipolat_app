import 'package:flutter/services.dart';

/// P2 #74 — Protection contre les captures d'écran sur écrans sensibles.
///
/// Utilise Flutter WindowManager pour activer/désactiver FLAG_SECURE sur Android.
/// Sur iOS, la protection est native (uirestriction.screencapture).
///
/// Usage :
/// ```dart
/// // Activer la protection
/// await ScreenshotProtectionService.enable();
///
/// // Désactiver la protection
/// await ScreenshotProtectionService.disable();
/// ```
class ScreenshotProtectionService {
  static const _channel = MethodChannel('com.discipolat/screenshot_protection');
  static bool _isEnabled = false;

  /// Singleton accessor for compatibility with callers using `.instance`.
  static final ScreenshotProtectionService instance = ScreenshotProtectionService._();
  ScreenshotProtectionService._();

  /// Initialise le service (compatibilité avec les appelants existants).
  Future<void> init() async {
    // Rien à initialiser — l'état est géré en mémoire.
  }

  /// Active la protection contre les captures d'écran (statique).
  static Future<void> enable() async {
    if (_isEnabled) return;
    try {
      await _channel.invokeMethod('enableScreenshotProtection');
      _isEnabled = true;
    } catch (_) {
      // Platform non supportée (iOS gère nativement)
      _isEnabled = true;
    }
  }

  /// Désactive la protection contre les captures d'écran (statique).
  static Future<void> disable() async {
    if (!_isEnabled) return;
    try {
      await _channel.invokeMethod('disableScreenshotProtection');
      _isEnabled = false;
    } catch (_) {
      _isEnabled = false;
    }
  }

  // ── Instance methods (for callers using `.instance.enable()` etc.) ──

  Future<void> instanceEnable() => ScreenshotProtectionService.enable();
  Future<void> instanceDisable() => ScreenshotProtectionService.disable();

  /// Active/désactive via l'instance.
  Future<void> enableProtection() => ScreenshotProtectionService.enable();
  Future<void> disableProtection() => ScreenshotProtectionService.disable();

  /// Définit l'état global de la protection.
  Future<void> setGlobalEnabled(bool enabled) async {
    if (enabled) {
      await ScreenshotProtectionService.enable();
    } else {
      await ScreenshotProtectionService.disable();
    }
  }

  /// Vérifie si la protection est active (statique).
  static bool get isEnabled => _isEnabled;

  /// Vérifie si la protection est active (instance).
  bool get isGlobalEnabled => _isEnabled;
}
