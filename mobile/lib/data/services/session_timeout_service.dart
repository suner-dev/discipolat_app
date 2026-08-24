import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Session timeout service — tracks user activity and auto-logs out after
/// a configurable inactivity period. Default: 30 minutes.
///
/// Usage:
/// ```dart
/// // In your main widget or app wrapper:
/// SessionTimeoutService.instance.start(
///   onTimeout: () => Navigator.pushReplacementNamed(context, '/login'),
/// );
///
/// // In your build method or gesture detector:
/// SessionTimeoutService.instance.resetTimer();
/// ```
class SessionTimeoutService {
  // Singleton
  static final SessionTimeoutService instance = SessionTimeoutService._();
  SessionTimeoutService._();

  Timer? _inactivityTimer;
  Timer? _warningTimer;
  DateTime? _lastActivityTime;
  bool _isActive = false;
  int _timeoutMinutes = 30;
  int _warningMinutes = 5; // Warning 5 minutes before timeout
  VoidCallback? _onTimeout;
  VoidCallback? _onWarning;
  Function(int remainingSeconds)? _onTick;

  bool get isActive => _isActive;
  int get timeoutMinutes => _timeoutMinutes;
  DateTime? get lastActivityTime => _lastActivityTime;

  /// Initialize and load saved preferences
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _timeoutMinutes = prefs.getInt('session_timeout_minutes') ?? 30;
    _warningMinutes = prefs.getInt('session_warning_minutes') ?? 5;
  }

  /// Start monitoring user activity
  void start({
    required VoidCallback onTimeout,
    VoidCallback? onWarning,
    Function(int remainingSeconds)? onTick,
    int? timeoutMinutes,
    int? warningMinutes,
  }) {
    _onTimeout = onTimeout;
    _onWarning = onWarning;
    _onTick = onTick;
    if (timeoutMinutes != null) _timeoutMinutes = timeoutMinutes;
    if (warningMinutes != null) _warningMinutes = warningMinutes;
    _isActive = true;
    _lastActivityTime = DateTime.now();
    _startInactivityTimer();
  }

  /// Stop monitoring (e.g., on logout)
  void stop() {
    _isActive = false;
    _inactivityTimer?.cancel();
    _warningTimer?.cancel();
    _inactivityTimer = null;
    _warningTimer = null;
  }

  /// Reset the inactivity timer — call this on every user interaction
  void resetTimer() {
    if (!_isActive) return;
    _lastActivityTime = DateTime.now();
    _startInactivityTimer();
  }

  /// Get remaining time before auto-logout (in seconds)
  int getRemainingSeconds() {
    if (_lastActivityTime == null) return _timeoutMinutes * 60;
    final elapsed = DateTime.now().difference(_lastActivityTime!).inSeconds;
    final remaining = (_timeoutMinutes * 60) - elapsed;
    return remaining > 0 ? remaining : 0;
  }

  /// Update timeout duration and persist
  Future<void> setTimeoutMinutes(int minutes) async {
    _timeoutMinutes = minutes;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('session_timeout_minutes', minutes);
    resetTimer();
  }

  /// Update warning duration and persist
  Future<void> setWarningMinutes(int minutes) async {
    _warningMinutes = minutes;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('session_warning_minutes', minutes);
  }

  void _startInactivityTimer() {
    _inactivityTimer?.cancel();
    _warningTimer?.cancel();

    // Warning timer — fires before timeout
    final warningSeconds = (_timeoutMinutes - _warningMinutes) * 60;
    if (warningSeconds > 0) {
      _warningTimer = Timer(Duration(seconds: warningSeconds), () {
        _onWarning?.call();
      });
    }

    // Inactivity timer — fires at timeout
    _inactivityTimer = Timer(Duration(minutes: _timeoutMinutes), () {
      _isActive = false;
      _onTimeout?.call();
    });

    // Tick timer for countdown UI
    if (_onTick != null) {
      Timer.periodic(const Duration(seconds: 1), (timer) {
        if (!_isActive) {
          timer.cancel();
          return;
        }
        final remaining = getRemainingSeconds();
        _onTick!(remaining);
        if (remaining <= 0) timer.cancel();
      });
    }
  }

  /// Format remaining time as MM:SS
  static String formatRemaining(int seconds) {
    final m = seconds ~/ 60;
    final s = seconds % 60;
    return '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }
}
