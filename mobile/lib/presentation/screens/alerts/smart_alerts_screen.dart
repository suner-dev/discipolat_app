import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Smart Alerts screen — anomaly detection dashboard.
/// Shows alert summary and allows manual scan trigger.
class SmartAlertsScreen extends StatefulWidget {
  final ApiService? apiService;
  const SmartAlertsScreen({super.key, this.apiService});

  @override
  State<SmartAlertsScreen> createState() => _SmartAlertsScreenState();
}

class _SmartAlertsScreenState extends State<SmartAlertsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _summary;
  List<dynamic> _activeAlerts = [];
  bool _isLoading = true;
  bool _isScanning = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final results = await Future.wait([
        _api.get('/smart-alerts/summary'),
        _api.get('/alerts/active'),
      ]);
      final summaryData = results[0].data is Map<String, dynamic>
          ? results[0].data as Map<String, dynamic>
          : <String, dynamic>{};
      final alertsData = results[1].data;
      List<dynamic> alerts = [];
      if (alertsData is Map && alertsData.containsKey('content')) {
        alerts = alertsData['content'] as List<dynamic>;
      } else if (alertsData is List) {
        alerts = alertsData;
      }
      if (mounted) {
        setState(() {
          _summary = summaryData;
          _activeAlerts = alerts;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  Future<void> _runScan() async {
    setState(() { _isScanning = true; });
    try {
      await _api.post('/smart-alerts/scan');
      await _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Analyse terminée'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() { _isScanning = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('🔍 Alertes Intelligentes', style: TextStyle(color: Colors.white)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: _isScanning
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.refresh, color: Colors.white),
            onPressed: _isScanning ? null : _runScan,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _buildScanButton(),
                      const SizedBox(height: 16),
                      _buildSummaryCards(),
                      const SizedBox(height: 20),
                      _buildActiveAlerts(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, color: Colors.red, size: 48),
          const SizedBox(height: 16),
          Text('Erreur: $_error', style: const TextStyle(color: Colors.white70)),
          const SizedBox(height: 16),
          ElevatedButton(onPressed: _loadData, child: const Text('Réessayer')),
        ],
      ),
    );
  }

  Widget _buildScanButton() {
    return GlassCard(
      child: InkWell(
        onTap: _isScanning ? null : _runScan,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(20),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.cyanAccent.withAlpha(25),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: _isScanning
                    ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.cyanAccent))
                    : const Icon(Icons.radar, color: Colors.cyanAccent, size: 28),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _isScanning ? 'Analyse en cours…' : 'Lancer l\'analyse',
                      style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Détecte automatiquement les anomalies dans vos données',
                      style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 12),
                    ),
                  ],
                ),
              ),
              Icon(Icons.arrow_forward_ios, color: Colors.white.withAlpha(100), size: 16),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSummaryCards() {
    if (_summary == null) return const SizedBox.shrink();

    final totalActive = _summary!['totalActive'] ?? 0;
    final criticalActive = _summary!['criticalActive'] ?? 0;

    return Row(
      children: [
        Expanded(
          child: _buildStatCard(
            'Alertes actives',
            '$totalActive',
            Icons.warning_amber,
            Colors.amber,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildStatCard(
            'Critiques',
            '$criticalActive',
            Icons.error,
            criticalActive > 0 ? Colors.red : Colors.green,
          ),
        ),
      ],
    );
  }

  Widget _buildStatCard(String label, String value, IconData icon, Color color) {
    return GlassCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: 8),
            Text(value, style: TextStyle(color: color, fontSize: 28, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 12)),
          ],
        ),
      ),
    );
  }

  Widget _buildActiveAlerts() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('Alertes actives', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
            Text('${_activeAlerts.length}', style: TextStyle(color: Colors.white.withAlpha(150))),
          ],
        ),
        const SizedBox(height: 12),
        if (_activeAlerts.isEmpty)
          GlassCard(
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Center(
                child: Column(
                  children: [
                    Icon(Icons.check_circle, color: Colors.green.withAlpha(200), size: 48),
                    const SizedBox(height: 12),
                    Text('Aucune alerte active', style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 16)),
                    const SizedBox(height: 8),
                    Text('Tout est en ordre ! 🎉', style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 13)),
                  ],
                ),
              ),
            ),
          )
        else
          ...List.generate(_activeAlerts.length, (i) {
            final alert = _activeAlerts[i];
            return _buildAlertItem(alert);
          }),
      ],
    );
  }

  Widget _buildAlertItem(Map<String, dynamic> alert) {
    final title = alert['titre']?.toString() ?? alert['message']?.toString() ?? 'Alerte';
    final priority = alert['priorite']?.toString() ?? 'MOYENNE';
    final dateStr = alert['dateDeclenchement']?.toString() ?? '';

    Color priorityColor;
    IconData priorityIcon;
    switch (priority) {
      case 'HAUTE':
        priorityColor = Colors.red;
        priorityIcon = Icons.error;
        break;
      case 'MOYENNE':
        priorityColor = Colors.orange;
        priorityIcon = Icons.warning_amber;
        break;
      default:
        priorityColor = Colors.blue;
        priorityIcon = Icons.info;
    }

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: priorityColor.withAlpha(30),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(priorityIcon, color: priorityColor, size: 20),
        ),
        title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500)),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: priorityColor.withAlpha(30),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(priority, style: TextStyle(color: priorityColor, fontSize: 10, fontWeight: FontWeight.bold)),
              ),
              const SizedBox(width: 8),
              if (dateStr.isNotEmpty)
                Text(dateStr.substring(0, dateStr.length > 10 ? 10 : dateStr.length),
                    style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 11)),
            ],
          ),
        ),
        trailing: Icon(Icons.arrow_forward_ios, color: Colors.white.withAlpha(80), size: 14),
      ),
    );
  }
}

/// Simple glass card widget
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? margin;
  final VoidCallback? onTap;

  const GlassCard({super.key, required this.child, this.margin, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(8),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withAlpha(15)),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: onTap != null
            ? Material(color: Colors.transparent, child: InkWell(onTap: onTap, child: child))
            : child,
      ),
    );
  }
}
