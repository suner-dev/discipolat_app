import 'package:flutter/material.dart';
import '../../data/services/screenshot_protection_service.dart';
import '../../data/services/audit_log_service.dart';

/// Wrapper widget that automatically enables screenshot protection
/// and logs screen access for sensitive screens.
///
/// Usage:
/// ```dart
/// SecureScreen(
///   screenName: 'FinanceScreen',
///   auditAction: 'VIEW_FINANCES',
///   child: MyFinanceScreen(),
/// )
/// ```
class SecureScreen extends StatefulWidget {
  final Widget child;
  final String screenName;
  final String? auditAction;
  final bool enableScreenshotProtection;
  final bool logAccess;

  const SecureScreen({
    super.key,
    required this.child,
    required this.screenName,
    this.auditAction,
    this.enableScreenshotProtection = true,
    this.logAccess = true,
  });

  @override
  State<SecureScreen> createState() => _SecureScreenState();
}

class _SecureScreenState extends State<SecureScreen>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);

    if (widget.enableScreenshotProtection) {
      ScreenshotProtectionService.enable();
    }

    if (widget.logAccess) {
      AuditLogger.sensitiveAction(
        'current_user',
        'current_org',
        widget.auditAction ?? 'VIEW_SCREEN',
        'screen',
        widget.screenName,
      );
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    if (widget.enableScreenshotProtection) {
      ScreenshotProtectionService.disable();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}

/// Mixin for screens that need audit logging on navigation
mixin AuditLoggingMixin<T extends StatefulWidget> on State<T> {
  String get auditScreenName;
  String? get auditAction;

  @override
  void initState() {
    super.initState();
    _logScreenAccess();
  }

  void _logScreenAccess() {
    if (auditAction != null) {
      AuditLogger.sensitiveAction(
        'current_user',
        'current_org',
        auditAction!,
        'screen',
        auditScreenName,
      );
    }
  }

  void logAction(String action, {String? resource, String? resourceId}) {
    AuditLogger.sensitiveAction(
      'current_user',
      'current_org',
      action,
      resource ?? 'screen',
      resourceId ?? auditScreenName,
    );
  }
}

/// Constants for common audit actions used across the app
class AuditActions {
  static const String viewFinances = 'VIEW_FINANCES';
  static const String viewPayments = 'VIEW_PAYMENTS';
  static const String createPayment = 'CREATE_PAYMENT';
  static const String viewPrayers = 'VIEW_PRAYERS';
  static const String createPrayer = 'CREATE_PRAYER';
  static const String viewProfiles = 'VIEW_PROFILES';
  static const String editProfile = 'EDIT_PROFILE';
  static const String viewReports = 'VIEW_REPORTS';
  static const String createReport = 'CREATE_REPORT';
  static const String viewAdmin = 'VIEW_ADMIN';
  static const String editAdmin = 'EDIT_ADMIN';
  static const String viewCompliance = 'VIEW_COMPLIANCE';
  static const String exportData = 'EXPORT_DATA';
  static const String deleteData = 'DELETE_DATA';
  static const String viewMessages = 'VIEW_MESSAGES';
  static const String sendMessage = 'SEND_MESSAGE';
  static const String viewMembers = 'VIEW_MEMBERS';
  static const String viewSensitiveData = 'VIEW_SENSITIVE_DATA';
  static const String screenshotAttempt = 'SCREENSHOT_ATTEMPT';
  static const String loginAttempt = 'LOGIN_ATTEMPT';
  static const String logout = 'LOGOUT';
  static const String biometricAuth = 'BIOMETRIC_AUTH';
  static const String sessionTimeout = 'SESSION_TIMEOUT';
}
