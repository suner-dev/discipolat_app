import 'dart:async';
import 'package:flutter/services.dart';
import 'package:local_auth/local_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Biometric authentication service — supports fingerprint and face ID.
///
/// Usage:
/// ```dart
/// final biometric = BiometricAuthService.instance;
/// await biometric.init();
///
/// if (await biometric.isAvailable()) {
///   final success = await biometric.authenticate();
///   if (success) { /* proceed */ }
/// }
/// ```
class BiometricAuthService {
  static final BiometricAuthService instance = BiometricAuthService._();
  BiometricAuthService._();

  // Allow non-singleton construction for backward compatibility
  BiometricAuthService();

  final LocalAuthentication _localAuth = LocalAuthentication();
  bool _isAvailable = false;
  bool _isEnabled = false;
  List<BiometricType> _availableBiometrics = [];

  bool get isAvailable => _isAvailable;
  bool get isEnabled => _isEnabled;
  List<BiometricType> get availableBiometrics => _availableBiometrics;

  /// Initialize — check device capabilities and load preferences
  Future<void> init() async {
    try {
      _isAvailable = await _localAuth.canCheckBiometrics;
      if (_isAvailable) {
        _availableBiometrics = await _localAuth.getAvailableBiometrics();
      }
      final prefs = await SharedPreferences.getInstance();
      _isEnabled = prefs.getBool('biometric_enabled') ?? false;
    } on PlatformException {
      _isAvailable = false;
      _isEnabled = false;
    }
  }

  // ── Backward-compatible methods (used by existing security_settings_screen) ──

  /// Check if biometric auth is enabled (backward compat)
  Future<bool> isBiometricEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool('biometric_enabled') ?? false;
  }

  /// Check if biometric auth is available (backward compat)
  Future<bool> isBiometricAvailable() async {
    try {
      return await _localAuth.canCheckBiometrics;
    } on PlatformException {
      return false;
    }
  }

  /// Set biometric enabled (backward compat)
  Future<void> setBiometricEnabled(bool value) async {
    _isEnabled = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('biometric_enabled', value);
  }

  /// Get stored PIN code (backward compat)
  Future<String?> getPinCode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('pin_code');
  }

  /// Save PIN code (backward compat)
  Future<void> savePinCode(String pin) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('pin_code', pin);
  }

  /// Check if biometric auth is supported on this device
  Future<bool> isDeviceSupported() async {
    try {
      return await _localAuth.isDeviceSupported();
    } on PlatformException {
      return false;
    }
  }

  /// Get the list of available biometric types
  Future<List<BiometricType>> getAvailableTypes() async {
    try {
      return await _localAuth.getAvailableBiometrics();
    } on PlatformException {
      return [];
    }
  }

  /// Authenticate with biometrics
  ///
  /// [reason] — message shown to the user (e.g., "Authentifiez-vous")
  /// [useErrorDialogs] — show system error dialogs on failure
  /// [stickyAuth] — keep auth session alive when app goes to background
  Future<bool> authenticate({
    String reason = 'Authentifiez-vous pour continuer',
    bool useErrorDialogs = true,
    bool stickyAuth = true,
  }) async {
    if (!_isAvailable || !_isEnabled) return false;

    try {
      return await _localAuth.authenticate(
        localizedReason: reason,
        options: AuthenticationOptions(
          useErrorDialogs: useErrorDialogs,
          stickyAuth: stickyAuth,
          biometricOnly: false, // Allow PIN fallback
          sensitiveTransaction: true,
        ),
      );
    } on PlatformException {
      return false;
    }
  }

  /// Enable biometric authentication and persist
  Future<bool> enable() async {
    if (!_isAvailable) return false;

    // First, verify the user can authenticate
    final success = await authenticate(reason: 'Activez la biométrie pour vos prochaines connexions');
    if (success) {
      _isEnabled = true;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('biometric_enabled', true);
      return true;
    }
    return false;
  }

  /// Disable biometric authentication
  Future<void> disable() async {
    _isEnabled = false;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('biometric_enabled', false);
  }

  /// Get a human-readable name for a biometric type
  static String biometricTypeName(BiometricType type) {
    switch (type) {
      case BiometricType.face:
        return 'Reconnaissance faciale';
      case BiometricType.fingerprint:
        return 'Empreinte digitale';
      case BiometricType.iris:
        return 'Reconnaissance de l\'iris';
      default:
        return 'Biométrie';
    }
  }

  /// Get a human-readable list of available biometrics
  String getAvailableTypesText() {
    if (_availableBiometrics.isEmpty) return 'Aucune biométrie disponible';
    return _availableBiometrics.map(biometricTypeName).join(', ');
  }
}
