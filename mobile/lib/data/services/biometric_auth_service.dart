import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Service for handling biometric authentication (fingerprint, Face ID)
///
/// Features:
/// - Check biometric availability
/// - Authenticate with biometrics
/// - Save/retrieve biometric preference
/// - Fallback to PIN if biometrics unavailable
/// - Secure credential storage
class BiometricAuthService {
  static final BiometricAuthService _instance = BiometricAuthService._internal();
  factory BiometricAuthService() => _instance;
  BiometricAuthService._internal();

  static const String _biometricEnabledKey = 'biometric_auth_enabled';
  static const String _pinCodeKey = 'pin_code';

  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  /// Check if biometric authentication is available on the device
  Future<bool> isBiometricAvailable() async {
    // Using platform channels to check biometric availability
    // This avoids hard dependency on local_auth
    try {
      const platform = MethodChannel('discipolat/biometric');
      final result = await platform.invokeMethod<bool>('isBiometricAvailable');
      return result ?? false;
    } on PlatformException {
      return false;
    }
  }

  /// Authenticate using biometrics (fingerprint or Face ID)
  Future<BiometricAuthResult> authenticate() async {
    try {
      const platform = MethodChannel('discipolat/biometric');
      final result = await platform.invokeMethod<Map>('authenticate');

      final success = result?['success'] == true;
      final errorMessage = result?['errorMessage'] as String?;

      return BiometricAuthResult(
        success: success,
        errorMessage: errorMessage,
        authType: _parseAuthType(result?['authType'] as String?),
      );
    } on PlatformException catch (e) {
      return BiometricAuthResult(
        success: false,
        errorMessage: e.message ?? 'Biometric authentication failed',
      );
    }
  }

  /// Get the available biometric types
  Future<List<BiometricType>> getAvailableBiometrics() async {
    try {
      const platform = MethodChannel('discipolat/biometric');
      final result = await platform.invokeListMethod<String>('getAvailableBiometrics');
      if (result == null) return [];
      return result.map(_parseBiometricType).toList();
    } on PlatformException {
      return [];
    }
  }

  /// Check if biometric auth is enabled for the current user
  Future<bool> isBiometricEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_biometricEnabledKey) ?? false;
  }

  /// Enable/disable biometric auth for the current user
  Future<void> setBiometricEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_biometricEnabledKey, enabled);
  }

  /// Save PIN code as fallback for biometric auth
  Future<void> savePinCode(String pin) async {
    await _secureStorage.write(key: _pinCodeKey, value: pin);
  }

  /// Validate PIN code
  Future<bool> validatePin(String pin) async {
    final storedPin = await _secureStorage.read(key: _pinCodeKey);
    return storedPin == pin;
  }

  /// Delete PIN code
  Future<void> deletePinCode() async {
    await _secureStorage.delete(key: _pinCodeKey);
  }

  /// Get saved PIN code (for validation only, never for display)
  Future<String?> getPinCode() async {
    return await _secureStorage.read(key: _pinCodeKey);
  }

  BiometricType _parseBiometricType(String? type) {
    switch (type) {
      case 'fingerprint':
        return BiometricType.fingerprint;
      case 'face':
        return BiometricType.face;
      case 'iris':
        return BiometricType.iris;
      default:
        return BiometricType.unknown;
    }
  }

  AuthType _parseAuthType(String? type) {
    switch (type) {
      case 'biometric':
        return AuthType.biometric;
      case 'pin':
        return AuthType.pin;
      default:
        return AuthType.unknown;
    }
  }
}

enum BiometricType { fingerprint, face, iris, unknown }

enum AuthType { biometric, pin, unknown }

class BiometricAuthResult {
  final bool success;
  final String? errorMessage;
  final AuthType? authType;

  BiometricAuthResult({required this.success, this.errorMessage, this.authType});
}

/// Provider for biometric auth service
final biometricAuthServiceProvider = Provider<BiometricAuthService>((ref) {
  return BiometricAuthService();
});
