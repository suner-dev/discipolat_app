import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

/// Audit logging service for tracking user actions on mobile
///
/// Logs:
/// - Authentication events (login, logout, biometric)
/// - Sensitive operations (reports, transfers, approvals)
/// - Data access patterns
/// - Security events (failed attempts, timeouts)
///
/// Logs are stored locally and can be synced to backend when online
class AuditLogService {
  static final AuditLogService _instance = AuditLogService._internal();
  factory AuditLogService() => _instance;
  AuditLogService._internal();

  static const String _logsKey = 'audit_logs';
  static const String _maxLogsKey = 'max_audit_logs';
  static const int _defaultMaxLogs = 1000;

  /// Log an audit event
  Future<void> log(AuditEvent event) async {
    final logEntry = AuditLogEntry(
      id: const Uuid().v4(),
      timestamp: DateTime.now(),
      userId: event.userId,
      orgId: event.orgId,
      action: event.action,
      resource: event.resource,
      resourceId: event.resourceId,
      ipAddress: event.ipAddress,
      userAgent: event.userAgent,
      details: event.details,
      severity: event.severity,
    );

    final logs = await _getLogs();
    logs.add(logEntry);

    // Trim if exceeding max
    final maxLogs = await _getMaxLogs();
    if (logs.length > maxLogs) {
      logs.removeAt(0);
    }

    await _saveLogs(logs);
  }

  /// Get all audit logs (most recent first)
  Future<List<AuditLogEntry>> getLogs() async {
    final logs = await _getLogs();
    logs.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    return logs;
  }

  /// Get logs filtered by severity
  Future<List<AuditLogEntry>> getLogsBySeverity(AuditSeverity severity) async {
    final logs = await getLogs();
    return logs.where((log) => log.severity == severity).toList();
  }

  /// Get logs filtered by action type
  Future<List<AuditLogEntry>> getLogsByAction(String action) async {
    final logs = await getLogs();
    return logs.where((log) => log.action == action).toList();
  }

  /// Clear all audit logs (for compliance - GDPR right to erasure)
  Future<void> clearLogs() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_logsKey);
  }

  /// Export logs as JSON string
  Future<String> exportLogsJson() async {
    final logs = await getLogs();
    final jsonList = logs.map((log) => log.toJson()).toList();
    return jsonEncode(jsonList);
  }

  /// Export logs as CSV string
  Future<String> exportLogsCsv() async {
    final logs = await getLogs();
    final buffer = StringBuffer();
    buffer.writeln('id,timestamp,userId,orgId,action,resource,resourceId,severity,details');
    for (final log in logs) {
      buffer.writeln('${log.id},"${log.timestamp.toIso8601String()}",${log.userId ?? ''},${log.orgId ?? ''},${log.action},${log.resource ?? ''},${log.resourceId ?? ''},${log.severity.name},${log.details ?? ''}');
    }
    return buffer.toString();
  }

  Future<List<AuditLogEntry>> _getLogs() async {
    final prefs = await SharedPreferences.getInstance();
    final logsJson = prefs.getString(_logsKey);
    if (logsJson == null) return [];
    try {
      final List<dynamic> jsonList = jsonDecode(logsJson);
      return jsonList.map((e) => AuditLogEntry.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> _saveLogs(List<AuditLogEntry> logs) async {
    final prefs = await SharedPreferences.getInstance();
    final jsonList = logs.map((log) => log.toJson()).toList();
    await prefs.setString(_logsKey, jsonEncode(jsonList));
  }

  Future<int> _getMaxLogs() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getInt(_maxLogsKey) ?? _defaultMaxLogs;
  }

  /// Set max number of logs to retain (for GDPR compliance)
  Future<void> setMaxLogs(int max) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_maxLogsKey, max);
  }

  /// Get log count for display
  Future<int> getLogCount() async {
    final logs = await _getLogs();
    return logs.length;
  }
}

/// Audit event to log
class AuditEvent {
  final String? userId;
  final String? orgId;
  final String action;
  final String? resource;
  final String? resourceId;
  final String? ipAddress;
  final String? userAgent;
  final Map<String, dynamic>? details;
  final AuditSeverity severity;

  AuditEvent({
    this.userId,
    this.orgId,
    required this.action,
    this.resource,
    this.resourceId,
    this.ipAddress,
    this.userAgent,
    this.details,
    this.severity = AuditSeverity.info,
  });
}

/// Audit log entry stored locally
class AuditLogEntry {
  final String id;
  final DateTime timestamp;
  final String? userId;
  final String? orgId;
  final String action;
  final String? resource;
  final String? resourceId;
  final String? ipAddress;
  final String? userAgent;
  final Map<String, dynamic>? details;
  final AuditSeverity severity;

  AuditLogEntry({
    required this.id,
    required this.timestamp,
    this.userId,
    this.orgId,
    required this.action,
    this.resource,
    this.resourceId,
    this.ipAddress,
    this.userAgent,
    this.details,
    required this.severity,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'timestamp': timestamp.toIso8601String(),
      'userId': userId,
      'orgId': orgId,
      'action': action,
      'resource': resource,
      'resourceId': resourceId,
      'ipAddress': ipAddress,
      'userAgent': userAgent,
      'details': details,
      'severity': severity.name,
    };
  }

  factory AuditLogEntry.fromJson(Map<String, dynamic> json) {
    return AuditLogEntry(
      id: json['id'] as String,
      timestamp: DateTime.parse(json['timestamp'] as String),
      userId: json['userId'] as String?,
      orgId: json['orgId'] as String?,
      action: json['action'] as String,
      resource: json['resource'] as String?,
      resourceId: json['resourceId'] as String?,
      ipAddress: json['ipAddress'] as String?,
      userAgent: json['userAgent'] as String?,
      details: json['details'] as Map<String, dynamic>?,
      severity: AuditSeverity.values.firstWhere(
        (s) => s.name == (json['severity'] as String? ?? 'info'),
        orElse: () => AuditSeverity.info,
      ),
    );
  }
}

enum AuditSeverity { info, warning, error, critical }

/// Quick log helpers for common events
class AuditLogger {
  static final _service = AuditLogService();

  static Future<void> login(String userId, String orgId) =>
      _service.log(AuditEvent(userId: userId, orgId: orgId, action: 'LOGIN', resource: 'auth', severity: AuditSeverity.info));

  static Future<void> logout(String userId, String orgId) =>
      _service.log(AuditEvent(userId: userId, orgId: orgId, action: 'LOGOUT', resource: 'auth', severity: AuditSeverity.info));

  static Future<void> biometricAuth(String userId, String orgId) =>
      _service.log(AuditEvent(userId: userId, orgId: orgId, action: 'BIOMETRIC_AUTH', resource: 'auth', severity: AuditSeverity.info));

  static Future<void> sessionTimeout(String userId, String orgId) =>
      _service.log(AuditEvent(userId: userId, orgId: orgId, action: 'SESSION_TIMEOUT', resource: 'auth', severity: AuditSeverity.warning));

  static Future<void> sensitiveAction(String userId, String orgId, String action, String resource, String resourceId) =>
      _service.log(AuditEvent(userId: userId, orgId: orgId, action: action, resource: resource, resourceId: resourceId, severity: AuditSeverity.info));

  static Future<void> securityEvent(String userId, String orgId, String action, Map<String, dynamic> details) =>
      _service.log(AuditEvent(userId: userId, orgId: orgId, action: action, resource: 'security', details: details, severity: AuditSeverity.critical));
}
