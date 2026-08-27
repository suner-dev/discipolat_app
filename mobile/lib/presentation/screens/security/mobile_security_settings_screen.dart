import 'package:flutter/material.dart';
import '../../../data/services/session_timeout_service.dart';
import '../../../data/services/biometric_auth_service.dart';
import '../../../data/services/data_saver_service.dart';
import '../../../data/services/orientation_service.dart';
import '../../../data/services/screenshot_protection_service.dart';
import '../../../data/services/audit_log_service.dart';
import '../../../l10n/app_localizations.dart';

/// Unified security & settings screen for mobile.
/// Combines: session timeout, biometric auth, data saver, orientation.
class MobileSecuritySettingsScreen extends StatefulWidget {
  const MobileSecuritySettingsScreen({super.key});

  @override
  State<MobileSecuritySettingsScreen> createState() => _MobileSecuritySettingsScreenState();
}

class _MobileSecuritySettingsScreenState extends State<MobileSecuritySettingsScreen> {
  final _sessionTimeout = SessionTimeoutService.instance;
  final _biometric = BiometricAuthService.instance;
  final _dataSaver = DataSaverService.instance;
  final _orientation = OrientationService.instance;

  bool _biometricAvailable = false;
  int _remainingSeconds = 0;
  bool _screenshotProtection = true;
  int _auditLogCount = 0;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    await _biometric.init();
    await ScreenshotProtectionService.instance.init();
    final logService = AuditLogService();
    final logCount = await logService.getLogCount();
    setState(() {
      _biometricAvailable = _biometric.isAvailable;
      _remainingSeconds = _sessionTimeout.getRemainingSeconds();
      _screenshotProtection = ScreenshotProtectionService.instance.isGlobalEnabled;
      _auditLogCount = logCount;
    });
    // Start a periodic timer to update remaining time
    Stream.periodic(const Duration(seconds: 1)).listen((_) {
      if (mounted) {
        setState(() {
          _remainingSeconds = _sessionTimeout.getRemainingSeconds();
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).secTitle),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── Session Timeout ──
          _sectionHeader(Icons.timer_outlined, AppLocalizations.of(context).secSessionSection),
          Card(
            margin: const EdgeInsets.only(bottom: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.lock_clock, size: 20, color: Colors.orange),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(AppLocalizations.of(context).secAutoLogout, style: TextStyle(fontWeight: FontWeight.w600)),
                            Text(
                              'Après ${_sessionTimeout.timeoutMinutes} minutes d\'inactivité',
                              style: const TextStyle(fontSize: 12, color: Colors.grey),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: _remainingSeconds > 0 ? Colors.green.shade50 : Colors.red.shade50,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          SessionTimeoutService.formatRemaining(_remainingSeconds),
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: _remainingSeconds > 0 ? Colors.green : Colors.red,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(AppLocalizations.of(context).secSessionDuration, style: TextStyle(fontSize: 13)),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [5, 15, 30, 60, 120].map((min) {
                      final isSelected = _sessionTimeout.timeoutMinutes == min;
                      return ChoiceChip(
                        label: Text('${min}min'),
                        selected: isSelected,
                        onSelected: (_) {
                          _sessionTimeout.setTimeoutMinutes(min);
                          setState(() {});
                        },
                        selectedColor: Colors.orange.shade100,
                      );
                    }).toList(),
                  ),
                ],
              ),
            ),
          ),

          // ── Biometric Auth ──
          _sectionHeader(Icons.fingerprint, AppLocalizations.of(context).secBiometricSection),
          Card(
            margin: const EdgeInsets.only(bottom: 16),
            child: SwitchListTile(
              title: const Text('Connexion par biométrie'),
              subtitle: Text(
                _biometricAvailable
                    ? _biometric.getAvailableTypesText()
                    : AppLocalizations.of(context).secBiometricUnavailable,
                style: const TextStyle(fontSize: 12),
              ),
              value: _biometric.isEnabled,
              onChanged: _biometricAvailable
                  ? (val) async {
                      if (val) {
                        final success = await _biometric.enable();
                        if (!success && mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text(AppLocalizations.of(context).secBiometricError)),
                          );
                        }
                      } else {
                        await _biometric.disable();
                      }
                      setState(() {});
                    }
                  : null,
              secondary: Icon(
                _biometricAvailable ? Icons.fingerprint : Icons.fingerprint_outlined,
                color: _biometric.isEnabled ? Colors.green : Colors.grey,
              ),
            ),
          ),

          // ── Data Saver ──
          _sectionHeader(Icons.data_saver_on, AppLocalizations.of(context).secDataSaverSection),
          Card(
            margin: const EdgeInsets.only(bottom: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        _dataSaver.isOnline ? Icons.wifi : Icons.wifi_off,
                        color: _dataSaver.isOnline ? Colors.green : Colors.red,
                        size: 20,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(AppLocalizations.of(context).secDataSaverMode, style: TextStyle(fontWeight: FontWeight.w600)),
                            Text(
                              'Réseau : ${_dataSaver.connectivityLabel}',
                              style: const TextStyle(fontSize: 12, color: Colors.grey),
                            ),
                          ],
                        ),
                      ),
                      Switch(
                        value: _dataSaver.isDataSaverActive,
                        onChanged: (_) async {
                          await _dataSaver.toggle();
                          setState(() {});
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Auto mode toggle
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(AppLocalizations.of(context).secAutoMode, style: TextStyle(fontSize: 13)),
                    subtitle: Text(AppLocalizations.of(context).secAutoModeDesc,
                      style: TextStyle(fontSize: 11),
                    ),
                    value: _dataSaver.isAutoMode,
                    onChanged: (val) async {
                      await _dataSaver.setAutoMode(val);
                      setState(() {});
                    },
                  ),
                  const Divider(),
                  _dataSaverInfo(AppLocalizations.of(context).secImgLoading, _dataSaver.shouldLoadImages ? 'Activé' : 'Désactivé'),
                  _dataSaverInfo(AppLocalizations.of(context).secCacheStrategy, _dataSaver.useCacheFirst ? 'Cache d\'abord' : 'Toujours le réseau'),
                  _dataSaverInfo(AppLocalizations.of(context).secRefreshInterval, '${_dataSaver.pollingIntervalSeconds}s'),
                ],
              ),
            ),
          ),

          // ── Orientation ──
          _sectionHeader(Icons.screen_rotation, AppLocalizations.of(context).secOrientationSection),
          Card(
            margin: const EdgeInsets.only(bottom: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: OrientationMode.values.map((mode) {
                      final isSelected = _orientation.currentMode == mode;
                      return GestureDetector(
                        onTap: () {
                          switch (mode) {
                            case OrientationMode.portrait:
                              _orientation.lockPortrait();
                              break;
                            case OrientationMode.landscape:
                              _orientation.lockLandscape();
                              break;
                            case OrientationMode.auto:
                              _orientation.allowAll();
                              break;
                          }
                          setState(() {});
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                          decoration: BoxDecoration(
                            color: isSelected ? Colors.indigo.shade50 : Colors.grey.shade50,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: isSelected ? Colors.indigo : Colors.grey.shade200,
                              width: isSelected ? 2 : 1,
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(mode.icon, size: 18, color: isSelected ? Colors.indigo : Colors.grey),
                              const SizedBox(width: 8),
                              Text(mode.label, style: TextStyle(
                                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                                color: isSelected ? Colors.indigo : Colors.grey.shade700,
                              )),
                            ],
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ],
              ),
            ),
          ),

          // ── Screenshot Protection ──
          _sectionHeader(Icons.shield, AppLocalizations.of(context).secScreenshotSection),
          Card(
            margin: const EdgeInsets.only(bottom: 16),
            child: SwitchListTile(
              title: Text(AppLocalizations.of(context).secScreenshotToggle),
              subtitle: Text(AppLocalizations.of(context).secScreenshotDesc,
                style: TextStyle(fontSize: 12),
              ),
              value: _screenshotProtection,
              onChanged: (val) async {
                await ScreenshotProtectionService.instance.setGlobalEnabled(val);
                setState(() {
                  _screenshotProtection = val;
                });
                AuditLogger.sensitiveAction(
                  'current_user', 'current_org',
                  val ? 'ENABLE_SCREENSHOT_PROTECTION' : 'DISABLE_SCREENSHOT_PROTECTION',
                  'security', 'settings',
                );
              },
            ),
          ),

          // ── Audit Logging ──
          _sectionHeader(Icons.history, AppLocalizations.of(context).secAuditSection),
          Card(
            margin: const EdgeInsets.only(bottom: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.list_alt, size: 20, color: Colors.teal),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(AppLocalizations.of(context).secAuditEvents, style: TextStyle(fontWeight: FontWeight.w600)),
                            Text(
                              '$_auditLogCount événements dans le journal',
                              style: const TextStyle(fontSize: 12, color: Colors.grey),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () async {
                            final logs = await AuditLogService().getLogs();
                            if (mounted) {
                              showDialog(
                                context: context,
                                builder: (ctx) => AlertDialog(
                                  title: Text(AppLocalizations.of(context).secAuditLogTitle),
                                  content: SizedBox(
                                    width: double.maxFinite,
                                    height: 400,
                                    child: ListView.builder(
                                      itemCount: logs.length,
                                      itemBuilder: (ctx, i) {
                                        final log = logs[i];
                                        return ListTile(
                                          dense: true,
                                          leading: Icon(
                                            log.severity == AuditSeverity.critical ? Icons.error :
                                            log.severity == AuditSeverity.warning ? Icons.warning :
                                            log.severity == AuditSeverity.error ? Icons.error_outline :
                                            Icons.info_outline,
                                            color: log.severity == AuditSeverity.critical ? Colors.red :
                                                   log.severity == AuditSeverity.warning ? Colors.orange : Colors.blue,
                                            size: 18,
                                          ),
                                          title: Text(log.action, style: const TextStyle(fontSize: 13)),
                                          subtitle: Text(
                                            '${log.resource ?? ''} ${log.resourceId ?? ''}'.trim(),
                                            style: const TextStyle(fontSize: 11),
                                          ),
                                          trailing: Text(
                                            '${log.timestamp.day}/${log.timestamp.month} ${log.timestamp.hour}:${log.timestamp.minute.toString().padLeft(2, '0')}',
                                            style: const TextStyle(fontSize: 10, color: Colors.grey),
                                          ),
                                        );
                                      },
                                    ),
                                  ),
                                  actions: [
                                    TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Fermer')),
                                    TextButton(
                                      onPressed: () async {
                                        final csv = await AuditLogService().exportLogsCsv();
                                        Navigator.pop(ctx);
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          SnackBar(content: Text('Journal exporté (${csv.length} caractères)')),
                                        );
                                      },
                                      child: Text(AppLocalizations.of(context).secAuditExportCsv),
                                    ),
                                    TextButton(
                                      onPressed: () async {
                                        await AuditLogService().clearLogs();
                                        setState(() => _auditLogCount = 0);
                                        Navigator.pop(ctx);
                                      },
                                      child: Text(AppLocalizations.of(context).secAuditClear, style: TextStyle(color: Colors.red)),
                                    ),
                                  ],
                                ),
                              );
                            }
                          },
                          icon: const Icon(Icons.visibility, size: 18),
                          label: Text(AppLocalizations.of(context).secAuditViewLog),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          // ── Quick info ──
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.blue.shade50,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Icon(Icons.info_outline, size: 18, color: Colors.blue.shade600),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    AppLocalizations.of(context).secInfoPersisted,
                    style: TextStyle(fontSize: 12, color: Colors.blue.shade700),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _sectionHeader(IconData icon, String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 18, color: Colors.indigo.shade600),
          const SizedBox(width: 8),
          Text(title, style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            color: Colors.indigo.shade600,
          )),
        ],
      ),
    );
  }

  Widget _dataSaverInfo(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
          Text(value, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}
