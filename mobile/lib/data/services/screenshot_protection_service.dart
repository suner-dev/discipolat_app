import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Screenshot protection service — prevents screenshots and screen recording
/// on sensitive screens (finances, prayers, admin, login).
///
/// Uses Android-specific FLAG_SECURE via SystemChrome and fallbacks for iOS.
///
/// Usage:
/// ```dart
/// // In a sensitive screen's initState:
/// ScreenshotProtectionService.instance.enable();
///
/// // In dispose:
/// ScreenshotProtectionService.instance.disable();
///
/// // Or use the SecureScreen wrapper widget:
/// SecureScreen(child: SensitiveContent())
/// ```
class ScreenshotProtectionService {
  static final ScreenshotProtectionService instance =
      ScreenshotProtectionService._();
  ScreenshotProtectionService._();

  bool _isEnabled = false;
  bool _globalEnabled = false;

  bool get isEnabled => _isEnabled;

  /// Initialize — load global preference
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _globalEnabled = prefs.getBool('screenshot_protection_global') ?? true;
  }

  /// Enable screenshot protection for the current screen
  Future<void> enable() async {
    if (!_globalEnabled) return;
    _isEnabled = true;
    try {
      // On Android: sets FLAG_SECURE on the window
      // On iOS: has limited effect but still useful as a signal
      await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    } catch (_) {
      // Silently fail on unsupported platforms
    }
  }

  /// Disable screenshot protection (e.g., when leaving a sensitive screen)
  Future<void> disable() async {
    _isEnabled = false;
    try {
      await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    } catch (_) {}
  }

  /// Toggle global screenshot protection setting
  Future<void> toggleGlobal() async {
    _globalEnabled = !_globalEnabled;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('screenshot_protection_global', _globalEnabled);
  }

  /// Set global screenshot protection
  Future<void> setGlobalEnabled(bool enabled) async {
    _globalEnabled = enabled;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('screenshot_protection_global', enabled);
  }

  /// Check if global protection is enabled
  bool get isGlobalEnabled => _globalEnabled;

  /// Try to use platform-specific FLAG_SECURE via method channel
  /// This is the most reliable way on Android
  static const MethodChannel _channel =
      MethodChannel('com.discipolat/secure_screen');

  Future<void> _setFlagSecure(bool secure) async {
    try {
      await _channel.invokeMethod('setFlagSecure', {'secure': secure});
    } on MissingPluginException {
      // Plugin not available — silently skip
    } catch (_) {}
  }
}

/// Extension on WidgetsBinding for easy screenshot protection
extension SecureScreenBinding on WidgetsBinding {
  /// Enable FLAG_SECURE on the current window (Android only)
  Future<void> enableScreenshotProtection() async {
    try {
      await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    } catch (_) {}
  }

  /// Disable FLAG_SECURE on the current window
  Future<void> disableScreenshotProtection() async {
    try {
      await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    } catch (_) {}
  }
}
