import 'dart:convert';
import 'dart:ui';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'api_service.dart';

/// Background message handler (must be top-level)
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  debugPrint('[Push] Background message: ${message.messageId}');
}

/// Comprehensive push notification service using Firebase Cloud Messaging.
/// Handles token management, foreground display, and backend registration.
class PushNotificationService {
  final ApiService _api;
  final FirebaseMessaging _messaging = FirebaseMessaging.instance;
  final FlutterLocalNotificationsPlugin _localNotifications =
      FlutterLocalNotificationsPlugin();

  String? _fcmToken;
  bool _initialized = false;

  PushNotificationService(this._api);

  String? get fcmToken => _fcmToken;

  /// Initialize push notifications
  Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;

    // Register background handler
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

    // Request permission
    final settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
      criticalAlert: true,
    );

    debugPrint('[Push] Authorization status: ${settings.authorizationStatus}');

    if (settings.authorizationStatus == AuthorizationStatus.authorized ||
        settings.authorizationStatus == AuthorizationStatus.provisional) {
      await _initializeLocalNotifications();
      await _getAndRegisterToken();
      await _setupMessageHandlers();
    }
  }

  /// Initialize local notifications for foreground display
  Future<void> _initializeLocalNotifications() async {
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );

    await _localNotifications.initialize(
      const InitializationSettings(
        android: androidSettings,
        iOS: iosSettings,
      ),
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );
  }

  /// Get FCM token and register it with the backend
  Future<void> _getAndRegisterToken() async {
    _fcmToken = await _messaging.getToken();
    debugPrint('[Push] FCM Token: $_fcmToken');

    if (_fcmToken != null) {
      await _registerTokenWithBackend(_fcmToken!);
    }

    // Listen for token refresh
    _messaging.onTokenRefresh.listen((newToken) {
      _fcmToken = newToken;
      debugPrint('[Push] Token refreshed: $newToken');
      _registerTokenWithBackend(newToken);
    });
  }

  /// Register FCM token with the backend for targeted notifications
  Future<void> _registerTokenWithBackend(String token) async {
    try {
      await _api.post('/notifications/register-token', data: {
        'token': token,
        'platform': defaultTargetPlatform == TargetPlatform.iOS ? 'IOS' : 'ANDROID',
        'appVersion': '1.0.0',
      });
      debugPrint('[Push] Token registered with backend');
    } catch (e) {
      debugPrint('[Push] Failed to register token: $e');
    }
  }

  /// Setup message handlers
  Future<void> _setupMessageHandlers() async {
    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);
    FirebaseMessaging.onMessageOpenedApp.listen(_handleMessageOpenedApp);

    final initialMessage = await _messaging.getInitialMessage();
    if (initialMessage != null) {
      _handleMessageOpenedApp(initialMessage);
    }
  }

  /// Handle messages received while the app is in foreground
  void _handleForegroundMessage(RemoteMessage message) {
    debugPrint('[Push] Foreground: ${message.notification?.title}');
    _showLocalNotification(message);
  }

  /// Handle notification tap from background
  void _handleMessageOpenedApp(RemoteMessage message) {
    debugPrint('[Push] Opened from: ${message.messageId}');
    _navigateFromNotification(message.data);
  }

  /// Display a local notification when the app is in foreground
  Future<void> _showLocalNotification(RemoteMessage message) async {
    final notification = message.notification;
    if (notification == null) return;

    final androidDetails = AndroidNotificationDetails(
      'discipolat_channel',
      'Discipolat Notifications',
      channelDescription: 'Notifications de l\'application Discipolat',
      importance: Importance.high,
      priority: Priority.high,
      icon: '@mipmap/ic_launcher',
      color: const Color(0xFF6C63FF),
    );

    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    final details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _localNotifications.show(
      notification.hashCode,
      notification.title,
      notification.body,
      details,
      payload: jsonEncode(message.data),
    );
  }

  /// Handle notification tap — navigate to the relevant screen
  void _onNotificationTapped(NotificationResponse response) {
    if (response.payload == null) return;
    try {
      final data = jsonDecode(response.payload!) as Map<String, dynamic>;
      _navigateFromNotification(data);
    } catch (e) {
      debugPrint('[Push] Failed to parse notification payload: $e');
    }
  }

  /// Navigate based on notification data
  void _navigateFromNotification(Map<String, dynamic> data) {
    final type = data['type'] as String?;
    final id = data['id'] as String?;
    debugPrint('[Push] Navigate: type=$type, id=$id');
    // Navigation handled by app router based on notification type
  }

  /// Subscribe to a topic (for broadcast notifications)
  Future<void> subscribeToTopic(String topic) async {
    await _messaging.subscribeToTopic(topic);
    debugPrint('[Push] Subscribed to topic: $topic');
  }

  /// Unsubscribe from a topic
  Future<void> unsubscribeFromTopic(String topic) async {
    await _messaging.unsubscribeFromTopic(topic);
    debugPrint('[Push] Unsubscribed from topic: $topic');
  }

  /// Subscribe to tenant-specific notification topic
  Future<void> subscribeToTenantTopic(String tenantId) async {
    await subscribeToTopic('tenant_$tenantId');
  }

  /// Subscribe to role-specific notification topic
  Future<void> subscribeToRoleTopic(String role) async {
    await subscribeToTopic('role_${role.toLowerCase()}');
  }

  /// Delete FCM token (on logout)
  Future<void> deleteToken() async {
    try {
      await _api.post('/notifications/unregister-token', data: {
        'token': _fcmToken,
      });
    } catch (_) {}
    await _messaging.deleteToken();
    _fcmToken = null;
    debugPrint('[Push] Token deleted');
  }

  /// Get notification permission status
  Future<AuthorizationStatus> getPermissionStatus() async {
    final settings = await _messaging.getNotificationSettings();
    return settings.authorizationStatus;
  }
}
