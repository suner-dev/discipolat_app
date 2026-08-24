import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Orientation service — manages screen orientation preferences.
///
/// Supports:
/// - Portrait only (default)
/// - Landscape only
/// - Auto-rotate (both orientations)
/// - Per-screen orientation overrides
///
/// Usage:
/// ```dart
/// final orientation = OrientationService.instance;
/// await orientation.init();
///
/// // Lock to portrait
/// orientation.lockPortrait();
///
/// // Allow rotation
/// orientation.allowAll();
///
/// // In a specific screen that needs landscape:
/// orientation.lockLandscape();
/// ```
class OrientationService {
  static final OrientationService instance = OrientationService._();
  OrientationService._();

  OrientationMode _currentMode = OrientationMode.portrait;
  bool _isInitialized = false;

  OrientationMode get currentMode => _currentMode;

  /// Initialize — load saved preference
  Future<void> init() async {
    if (_isInitialized) return;
    final prefs = await SharedPreferences.getInstance();
    final savedMode = prefs.getString('orientation_mode') ?? 'portrait';
    _currentMode = OrientationMode.fromString(savedMode);
    _applyMode(_currentMode);
    _isInitialized = true;
  }

  /// Lock to portrait orientation
  void lockPortrait() {
    _currentMode = OrientationMode.portrait;
    _applyMode(_currentMode);
    _savePreference();
  }

  /// Lock to landscape orientation
  void lockLandscape() {
    _currentMode = OrientationMode.landscape;
    _applyMode(_currentMode);
    _savePreference();
  }

  /// Allow all orientations (auto-rotate)
  void allowAll() {
    _currentMode = OrientationMode.auto;
    _applyMode(_currentMode);
    _savePreference();
  }

  /// Reset to default (portrait)
  void reset() {
    lockPortrait();
  }

  /// Check if device is currently in landscape
  bool isLandscape(BuildContext context) {
    return MediaQuery.of(context).orientation == Orientation.landscape;
  }

  /// Apply orientation lock via SystemChrome
  void _applyMode(OrientationMode mode) {
    switch (mode) {
      case OrientationMode.portrait:
        SystemChrome.setPreferredOrientations([
          DeviceOrientation.portraitUp,
          DeviceOrientation.portraitDown,
        ]);
        break;
      case OrientationMode.landscape:
        SystemChrome.setPreferredOrientations([
          DeviceOrientation.landscapeLeft,
          DeviceOrientation.landscapeRight,
        ]);
        break;
      case OrientationMode.auto:
        SystemChrome.setPreferredOrientations([
          DeviceOrientation.portraitUp,
          DeviceOrientation.portraitDown,
          DeviceOrientation.landscapeLeft,
          DeviceOrientation.landscapeRight,
        ]);
        break;
    }
  }

  /// Persist preference
  Future<void> _savePreference() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('orientation_mode', _currentMode.value);
  }

  /// Get responsive breakpoints based on screen width
  static ScreenSize getScreenSize(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    if (width < 360) return ScreenSize.small;
    if (width < 600) return ScreenSize.medium;
    if (width < 900) return ScreenSize.large;
    return ScreenSize.xlarge;
  }

  /// Check if the screen is narrow (portrait phone)
  static bool isNarrow(BuildContext context) {
    return MediaQuery.of(context).size.width < 600;
  }

  /// Get grid column count based on screen size
  static int getGridColumns(BuildContext context) {
    final size = getScreenSize(context);
    switch (size) {
      case ScreenSize.small:
        return 1;
      case ScreenSize.medium:
        return 2;
      case ScreenSize.large:
        return 3;
      case ScreenSize.xlarge:
        return 4;
    }
  }
}

/// Orientation mode enum
enum OrientationMode {
  portrait('portrait'),
  landscape('landscape'),
  auto('auto');

  const OrientationMode(this.value);
  final String value;

  factory OrientationMode.fromString(String value) {
    switch (value) {
      case 'landscape':
        return OrientationMode.landscape;
      case 'auto':
        return OrientationMode.auto;
      default:
        return OrientationMode.portrait;
    }
  }

  String get label {
    switch (this) {
      case OrientationMode.portrait:
        return 'Portrait';
      case OrientationMode.landscape:
        return 'Paysage';
      case OrientationMode.auto:
        return 'Auto-rotation';
    }
  }

  IconData get icon {
    switch (this) {
      case OrientationMode.portrait:
        return Icons.screen_lock_portrait;
      case OrientationMode.landscape:
        return Icons.screen_lock_landscape;
      case OrientationMode.auto:
        return Icons.screen_rotation;
    }
  }
}

/// Screen size breakpoints
enum ScreenSize { small, medium, large, xlarge }
