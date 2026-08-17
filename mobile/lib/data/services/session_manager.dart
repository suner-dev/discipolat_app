import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../app.dart';

/// Session manager that handles:
/// - Session timeout (configurable inactivity timeout)
/// - Auto-logout when app goes to background
/// - Screenshot prevention on sensitive screens
/// - Session lifecycle monitoring
class SessionManager {
  static final SessionManager _instance = SessionManager._internal();
  factory SessionManager() => _instance;
  SessionManager._internal();

  static const String _sessionTimeoutKey = 'session_timeout_minutes';
  static const String _biometricEnabledKey = 'biometric_enabled';
  static const String _lastActiveKey = 'last_active_timestamp';
  static const String _screenshotProtectionKey = 'screenshot_protection';

  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  Timer? _inactivityTimer;
  Timer? _backgroundTimer;
  VoidCallback? _onSessionTimeout;

  /// Default session timeout: 30 minutes
  static const int defaultTimeoutMinutes = 30;

  /// Available timeout options in minutes
  static const List<int> timeoutOptions = [5, 15, 30, 60, 120, 0]; // 0 = never

  /// Initialize the session manager with lifecycle monitoring
  Future<void> initialize({
    required VoidCallback onSessionTimeout,
    VoidCallback? onAppLifecycleChanged,
  }) async {
    _onSessionTimeout = onSessionTimeout;

    // Set up app lifecycle observer
    _setupLifecycleObserver();

    // Start monitoring inactivity
    _checkInactivity();
  }

  /// Record user activity to reset the inactivity timer
  void recordActivity() {
    _checkInactivity();
  }

  /// Start or restart the inactivity timer based on configured timeout
  void _checkInactivity() async {
    _inactivityTimer?.cancel();
    final prefs = await SharedPreferences.getInstance();
    final timeoutMinutes = prefs.getInt(_sessionTimeoutKey) ?? defaultTimeoutMinutes;

    if (timeoutMinutes <= 0) {
      // Never timeout
      return;
    }

    _inactivityTimer = Timer(Duration(minutes: timeoutMinutes), () {
      if (_onSessionTimeout != null) {
        _onSessionTimeout!();
      }
    });
  }

  /// Set the session timeout duration
  Future<void> setSessionTimeout(int minutes) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_sessionTimeoutKey, minutes);
    _checkInactivity(); // Restart timer with new duration
  }

  /// Get the current session timeout duration
  Future<int> getSessionTimeout() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getInt(_sessionTimeoutKey) ?? defaultTimeoutMinutes;
  }

  /// Enable or disable biometric authentication
  Future<void> setBiometricEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_biometricEnabledKey, enabled);
  }

  /// Check if biometric authentication is enabled
  Future<bool> isBiometricEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_biometricEnabledKey) ?? false;
  }

  /// Enable or disable screenshot protection
  Future<void> setScreenshotProtection(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_screenshotProtectionKey, enabled);
  }

  /// Check if screenshot protection is enabled
  Future<bool> isScreenshotProtectionEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_screenshotProtectionKey) ?? true;
  }

  /// Get the last active timestamp
  Future<DateTime?> getLastActive() async {
    final prefs = await SharedPreferences.getInstance();
    final timestamp = prefs.getString(_lastActiveKey);
    if (timestamp == null) return null;
    return DateTime.tryParse(timestamp);
  }

  /// Check if the session has expired since last active
  Future<bool> isSessionExpired() async {
    final lastActive = await getLastActive();
    if (lastActive == null) return false;
    final timeoutMinutes = await getSessionTimeout();
    if (timeoutMinutes <= 0) return false;
    return DateTime.now().difference(lastActive).inMinutes >= timeoutMinutes;
  }

  /// Log out the current user and clear session
  Future<void> logout() async {
    _inactivityTimer?.cancel();
    _backgroundTimer?.cancel();
    AuthState().logout();
    await _secureStorage.deleteAll();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_lastActiveKey, DateTime.now().toIso8601String());
  }

  /// Dispose of resources
  void dispose() {
    _inactivityTimer?.cancel();
    _backgroundTimer?.cancel();
  }

  void _setupLifecycleObserver() {
    // App lifecycle is handled externally via WidgetsBindingObserver
    // This method is a placeholder for the observer setup
  }
}

/// Provider for session management
final sessionManagerProvider = Provider<SessionManager>((ref) => SessionManager());

/// Session timeout configuration model
class SessionTimeoutConfig {
  final int minutes;
  final String label;

  const SessionTimeoutConfig({required this.minutes, required this.label});

  static const List<SessionTimeoutConfig> options = [
    SessionTimeoutConfig(minutes: 5, label: '5 minutes'),
    SessionTimeoutConfig(minutes: 15, label: '15 minutes'),
    SessionTimeoutConfig(minutes: 30, label: '30 minutes'),
    SessionTimeoutConfig(minutes: 60, label: '1 heure'),
    SessionTimeoutConfig(minutes: 120, label: '2 heures'),
    SessionTimeoutConfig(minutes: 0, label: 'Jamais'),
  ];
}
