import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Data Saver service — reduces data usage for users in low-connectivity zones.
///
/// Features:
/// - Auto-detect connectivity changes (WiFi, mobile, none)
/// - Disable image loading on mobile data
/// - Reduce API polling frequency
/// - Compress requests
/// - Cache-first strategy on slow networks
///
/// Usage:
/// ```dart
/// final saver = DataSaverService.instance;
/// await saver.init();
///
/// // In your API service:
/// if (saver.isDataSaverActive) {
///   // Use cache, reduce image quality, skip non-essential requests
/// }
/// ```
class DataSaverService {
  static final DataSaverService instance = DataSaverService._();
  DataSaverService._();

  StreamSubscription? _connectivitySubscription;
  bool _isDataSaverActive = false;
  bool _isAutoMode = true;
  ConnectivityResult _currentConnectivity = ConnectivityResult.none;
  final StreamController<ConnectivityResult> _connectivityController =
      StreamController<ConnectivityResult>.broadcast();

  bool get isDataSaverActive => _isDataSaverActive;
  bool get isAutoMode => _isAutoMode;
  ConnectivityResult get currentConnectivity => _currentConnectivity;
  Stream<ConnectivityResult> get connectivityStream => _connectivityController.stream;

  /// Whether images should be loaded (false = skip images)
  bool get shouldLoadImages => !_isDataSaverActive || _currentConnectivity == ConnectivityResult.wifi;

  /// Whether to use cache-first strategy
  bool get useCacheFirst => _isDataSaverActive || _currentConnectivity == ConnectivityResult.none;

  /// API polling interval in seconds (normal: 30, data saver: 120)
  int get pollingIntervalSeconds => _isDataSaverActive ? 120 : 30;

  /// Whether to compress API responses
  bool get shouldCompress => _isDataSaverActive;

  /// Initialize connectivity listener and load preferences
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _isAutoMode = prefs.getBool('datasaver_auto') ?? true;
    _isDataSaverActive = prefs.getBool('datasaver_active') ?? false;

    // Listen to connectivity changes
    _connectivitySubscription = Connectivity().onConnectivityChanged.listen((results) {
      final result = results.isNotEmpty ? results.first : ConnectivityResult.none;
      _currentConnectivity = result;
      _connectivityController.add(result);

      if (_isAutoMode) {
        _autoToggleDataSaver(result);
      }
    });

    // Initial check
    final results = await Connectivity().checkConnectivity();
    _currentConnectivity = results.isNotEmpty ? results.first : ConnectivityResult.none;
  }

  /// Manually toggle data saver on/off
  Future<void> toggle() async {
    _isDataSaverActive = !_isDataSaverActive;
    _isAutoMode = false; // Switch to manual mode
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('datasaver_active', _isDataSaverActive);
    await prefs.setBool('datasaver_auto', false);
  }

  /// Set data saver explicitly
  Future<void> setDataSaver(bool active) async {
    _isDataSaverActive = active;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('datasaver_active', active);
  }

  /// Switch back to auto mode
  Future<void> setAutoMode(bool auto) async {
    _isAutoMode = auto;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('datasaver_auto', auto);
    if (auto) {
      _autoToggleDataSaver(_currentConnectivity);
    }
  }

  /// Auto-toggle data saver based on connectivity
  void _autoToggleDataSaver(ConnectivityResult result) {
    // Enable data saver on mobile data or no connection
    final shouldSave = result == ConnectivityResult.mobile ||
        result == ConnectivityResult.bluetooth ||
        result == ConnectivityResult.none;

    if (_isDataSaverActive != shouldSave) {
      _isDataSaverActive = shouldSave;
      SharedPreferences.getInstance().then((prefs) {
        prefs.setBool('datasaver_active', shouldSave);
      });
    }
  }

  /// Get a human-readable connectivity status
  String get connectivityLabel {
    switch (_currentConnectivity) {
      case ConnectivityResult.wifi:
        return 'WiFi';
      case ConnectivityResult.mobile:
        return 'Données mobiles';
      case ConnectivityResult.ethernet:
        return 'Ethernet';
      case ConnectivityResult.bluetooth:
        return 'Bluetooth';
      case ConnectivityResult.vpn:
        return 'VPN';
      default:
        return 'Hors ligne';
    }
  }

  /// Whether the device is currently online
  bool get isOnline => _currentConnectivity != ConnectivityResult.none;

  /// Cleanup
  void dispose() {
    _connectivitySubscription?.cancel();
    _connectivityController.close();
  }
}
