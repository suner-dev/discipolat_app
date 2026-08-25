import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../data/services/biometric_auth_service.dart';
import '../../../data/services/session_timeout_service.dart';
import '../../../data/services/screenshot_protection_service.dart';
import '../../../data/services/audit_log_service.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';

/// Options de délai d'expiration de session proposées dans les réglages.
class SessionTimeoutOption {
  final int minutes;
  final String label;
  const SessionTimeoutOption(this.minutes, this.label);
}

class SessionTimeoutConfig {
  static const List<SessionTimeoutOption> options = <SessionTimeoutOption>[
    SessionTimeoutOption(0, 'Jamais'),
    SessionTimeoutOption(5, '5 minutes'),
    SessionTimeoutOption(15, '15 minutes'),
    SessionTimeoutOption(30, '30 minutes'),
    SessionTimeoutOption(60, '60 minutes'),
  ];
}

/// Screen for security and privacy settings
/// Features: biometric auth, session timeout, screenshot protection, PIN, audit logs
class SecuritySettingsScreen extends ConsumerStatefulWidget {
  const SecuritySettingsScreen({super.key});

  @override
  ConsumerState<SecuritySettingsScreen> createState() => _SecuritySettingsScreenState();
}

class _SecuritySettingsScreenState extends ConsumerState<SecuritySettingsScreen> {
  final BiometricAuthService _biometricService = BiometricAuthService();
  final SessionTimeoutService _session = SessionTimeoutService.instance;
  final ScreenshotProtectionService _screenshotService = ScreenshotProtectionService.instance;
  final AuditLogService _auditService = AuditLogService();

    int _timeoutMinutes = 30;
  bool _biometricEnabled = false;
  bool _screenshotProtection = true;
  bool _isLoading = true;
  int _logCount = 0;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final timeout = await _session.getTimeoutMinutes();
    final biometric = await _biometricService.isBiometricEnabled();
    final screenshot = _screenshotService.isGlobalEnabled;
    final logCount = await _auditService.getLogCount();
    final biometricAvailable = await _biometricService.isBiometricAvailable();

    setState(() {
      _timeoutMinutes = timeout;
      _biometricEnabled = biometric && biometricAvailable;
      _screenshotProtection = screenshot;
      _logCount = logCount;
      _isLoading = false;
    });
  }

  Future<void> _updateTimeout(int? minutes) async {
    if (minutes == null) return;
    await _session.setTimeoutMinutes(minutes);
    setState(() => _timeoutMinutes = minutes);
    AuditLogger.securityEvent(
      AuthState().userId ?? '',
      AuthState().orgId ?? 'unknown',
      'SESSION_TIMEOUT_CONFIGURED',
      {'timeoutMinutes': minutes},
    );
  }

  Future<void> _toggleBiometric(bool value) async {
    await _biometricService.setBiometricEnabled(value);
    setState(() => _biometricEnabled = value);
    if (value) {
      AuditLogger.biometricAuth(AuthState().userId ?? '', AuthState().orgId ?? '');
    } else {
      AuditLogger.securityEvent('', AuthState().orgId ?? '', 'BIOMETRIC_DISABLED', {});
    }
  }

  Future<void> _toggleScreenshotProtection(bool value) async {
    await _screenshotService.setGlobalEnabled(value);
    setState(() => _screenshotProtection = value);
  }

  Future<void> _changePinCode() async {
    final currentPinController = TextEditingController();
    final newPinController = TextEditingController();
    final confirmPinController = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E2A4A),
        title: const Text('Changer le code PIN', style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: currentPinController, obscureText: true, maxLength: 6,
              decoration: InputDecoration(hintText: 'Code PIN actuel', hintStyle: TextStyle(color: Colors.white70)),
              style: TextStyle(color: Colors.white),
            ),
            TextField(
              controller: newPinController, obscureText: true, maxLength: 6,
              decoration: InputDecoration(hintText: 'Nouveau code PIN', hintStyle: TextStyle(color: Colors.white70)),
              style: TextStyle(color: Colors.white),
            ),
            TextField(
              controller: confirmPinController, obscureText: true, maxLength: 6,
              decoration: InputDecoration(hintText: 'Confirmer', hintStyle: TextStyle(color: Colors.white70)),
              style: TextStyle(color: Colors.white),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text('Annuler')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text('Enregistrer')),
        ],
      ),
    );

        if (result == true) {
      final storedPin = await _biometricService.getPinCode();
      if (currentPinController.text == storedPin) {
        await _biometricService.savePinCode(newPinController.text);
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Code PIN mis a jour')));
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Code PIN incorrect')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(backgroundColor: Color(0xFF030712), body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: const Text('Securite et confidentialite'),
        centerTitle: true,
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712)],
          ),
        ),
        child: SafeArea(
          child: ListView(
            padding: EdgeInsets.all(16),
            children: [
              _buildSectionHeader('Authentification', Icons.lock_outline),
              GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                child: SwitchListTile(
                  title: Text('Authentification biometrique'),
                  subtitle: Text('Empreintes ou Face ID'),
                  secondary: Icon(Icons.fingerprint, color: AppColors.primary),
                  value: _biometricEnabled,
                  onChanged: _toggleBiometric,
                ),
              ),
              GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                child: ListTile(
                  leading: Icon(Icons.pin, color: AppColors.primary),
                  title: Text('Code PIN'),
                  subtitle: Text('Configurer un code PIN de secours'),
                  trailing: Icon(Icons.arrow_forward_ios, color: Colors.white70, size: 16),
                  onTap: _changePinCode,
                ),
              ),
              SizedBox(height: 16),
              _buildSectionHeader('Session', Icons.timer_outlined),
              GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                child: ListTile(
                  leading: Icon(Icons.logout, color: AppColors.primary),
                  title: Text('Expiration de session'),
                  subtitle: Text('Deconnexion apres ${_timeoutMinutes == 0 ? "jamais" : "$_timeoutMinutes min"} d inactivite'),
                  trailing: DropdownButton<int>(
                    value: _timeoutMinutes,
                    dropdownColor: Color(0xFF111827),
                    items: SessionTimeoutConfig.options.map((opt) => DropdownMenuItem(value: opt.minutes, child: Text(opt.label, style: TextStyle(color: Colors.white70)))).toList(),
                    onChanged: _updateTimeout,
                  ),
                ),
              ),
              GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                child: SwitchListTile(
                  title: Text("Protection d'ecran"),
                  subtitle: Text('Empecher les captures d ecran'),
                  secondary: Icon(Icons.screenshot_monitor, color: AppColors.primary),
                  value: _screenshotProtection,
                  onChanged: _toggleScreenshotProtection,
                                ),
              ),
              SizedBox(height: 16),
              _buildSectionHeader('Audit et activite', Icons.history),
              GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                child: Column(
                  children: [
                    ListTile(
                      leading: Icon(Icons.description, color: AppColors.primary),
                      title: Text('Journal d audit ($_logCount entrees)'),
                      subtitle: Text('Consulter les actions enregistrees'),
                      trailing: Icon(Icons.arrow_forward_ios, color: Colors.white70, size: 16),
                      onTap: _viewAuditLogs,
                    ),
                    ListTile(
                      leading: Icon(Icons.share, color: AppColors.primary),
                      title: Text('Exporter le journal'),
                      subtitle: Text('CSV ou JSON pour archivage'),
                      trailing: Icon(Icons.arrow_forward_ios, color: Colors.white70, size: 16),
                      onTap: _exportAuditLogs,
                    ),
                    ListTile(
                      leading: Icon(Icons.delete_sweep, color: Colors.red),
                      title: Text('Effacer le journal'),
                      subtitle: Text('Supprimer toutes les entrees (RGPD)'),
                      trailing: Icon(Icons.arrow_forward_ios, color: Colors.white70, size: 16),
                      onTap: _clearAuditLogs,
                    ),
                  ],
                ),
              ),
              SizedBox(height: 16),
              _buildSectionHeader('Informations du compte', Icons.account_circle),
              GlassCard(
                margin: EdgeInsets.only(bottom: 8),
                child: Column(
                  children: [
                    _buildInfoRow('Utilisateur', AuthState().userId ?? 'N/A'),
                    _buildInfoRow('Organisation', AuthState().orgId ?? 'N/A'),
                    _buildInfoRow('Role actif', AuthState().activeRole),
                    _buildInfoRow('Roles', AuthState().roles.join(', ')),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, IconData icon) {
    return Padding(
      padding: EdgeInsets.only(bottom: 8, top: 8),
      child: Row(
        children: [
          Icon(icon, color: AppColors.primary, size: 20),
          SizedBox(width: 8),
          Text(title, style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 14)),
          Text(value, style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }

  Future<void> _viewAuditLogs() async {
    final logs = await _auditService.getLogs();
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (ctx) => Dialog(
        backgroundColor: Color(0xFF1E2A4A),
        child: Container(
          width: double.infinity,
          height: 500,
          padding: EdgeInsets.all(16),
          child: Column(
            children: [
              Text('Journal d audit', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
              SizedBox(height: 12),
              Expanded(
                child: logs.isEmpty
                    ? Center(child: Text('Aucune entree', style: TextStyle(color: Colors.white70)))
                    : ListView.builder(
                        itemCount: logs.length,
                        itemBuilder: (context, index) {
                          final log = logs[index];
                          return ListTile(
                            leading: Icon(_severityIcon(log.severity), color: _severityColor(log.severity), size: 16),
                            title: Text(log.action, style: TextStyle(color: Colors.white, fontSize: 12)),
                            subtitle: Text(DateFormat('dd/MM/yyyy HH:mm').format(log.timestamp), style: TextStyle(color: Colors.white70, fontSize: 10)),
                          );
                        },
                      ),
              ),
              TextButton(onPressed: () => Navigator.pop(ctx), child: Text('Fermer')),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _exportAuditLogs() async {
    await _auditService.exportLogsCsv();
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Color(0xFF1E2A4A),
        title: Text('Journal exporte', style: TextStyle(color: Colors.white)),
        content: Text('$_logCount entrees exportees', style: TextStyle(color: Colors.white70)),
        actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: Text('OK'))],
      ),
    );
  }

  Future<void> _clearAuditLogs() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Color(0xFF1E2A4A),
        title: Text('Confirmer', style: TextStyle(color: Colors.white)),
        content: Text('Effacer le journal d audit ?', style: TextStyle(color: Colors.white70)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text('Annuler')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text('Effacer', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirmed == true) {
      await _auditService.clearLogs();
      setState(() => _logCount = 0);
    }
  }

  IconData _severityIcon(AuditSeverity severity) {
    switch (severity) {
      case AuditSeverity.info: return Icons.info_outline;
      case AuditSeverity.warning: return Icons.warning;
      case AuditSeverity.error: return Icons.error_outline;
      case AuditSeverity.critical: return Icons.report_problem;
    }
  }

  Color _severityColor(AuditSeverity severity) {
    switch (severity) {
      case AuditSeverity.info: return Colors.blue;
      case AuditSeverity.warning: return Colors.orange;
      case AuditSeverity.error: return Colors.red;
      case AuditSeverity.critical: return Colors.red;
    }
  }
}
