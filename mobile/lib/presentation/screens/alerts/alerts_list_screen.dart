import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class AlertsListScreen extends StatefulWidget {
  const AlertsListScreen({super.key});

  @override
  State<AlertsListScreen> createState() => _AlertsListScreenState();
}

class _AlertsListScreenState extends State<AlertsListScreen> {
  final _apiService = ApiService();
  List<dynamic> _alerts = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _loadAlerts(); }

  Future<void> _loadAlerts() async {
    try {
      final response = await _apiService.get('/alerts', params: {'size': '50'});
      final data = response.data as Map<String, dynamic>;
      if (mounted) setState(() { _alerts = data['content'] as List<dynamic>; _isLoading = false; });
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _resolveAlert(String id) async {
    try { await _apiService.patch('/alerts/$id/resolve'); _loadAlerts(); } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final activeCount = _alerts.where((a) => a is Map && a['statut'] == 'ACTIVE').length;
    final resolvedCount = _alerts.where((a) => a is Map && a['statut'] == 'RESOLUE').length;

    return Scaffold(
      appBar: AppBar(title: const Text('Alertes')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadAlerts,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                child: Column(children: [
                  // Summary cards
                  Row(children: [
                    Expanded(child: GlassCard(
                      padding: const EdgeInsets.all(12),
                      child: Column(children: [
                        Icon(Icons.warning_amber, color: Colors.orange, size: 28),
                        const SizedBox(height: 8),
                        Text('$activeCount', style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                        Text('Actives', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                      ]),
                    )),
                    const SizedBox(width: 12),
                    Expanded(child: GlassCard(
                      padding: const EdgeInsets.all(12),
                      child: Column(children: [
                        Icon(Icons.check_circle, color: Colors.green, size: 28),
                        const SizedBox(height: 8),
                        Text('$resolvedCount', style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                        Text('Résolues', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                      ]),
                    )),
                  ]),
                  const SizedBox(height: 16),

                  // Alerts list
                  if (_alerts.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(children: [
                        Icon(Icons.check_circle_outline, color: Colors.green.withValues(alpha: 0.7), size: 48),
                        const SizedBox(height: 12),
                        const Text('Aucune alerte', style: TextStyle(color: Colors.white, fontSize: 16)),
                      ]),
                    )
                  else
                    ...List.generate(_alerts.length, (i) {
                      final alert = _alerts[i] as Map<String, dynamic>;
                      final isActive = alert['statut'] == 'ACTIVE';
                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 8),
                        padding: const EdgeInsets.all(12),
                        borderColor: isActive ? Colors.red.withValues(alpha: 0.3) : Colors.green.withValues(alpha: 0.2),
                        child: Row(children: [
                          Container(
                            width: 4, height: 48,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(2),
                              color: isActive ? Colors.red : Colors.green,
                              boxShadow: isActive ? [BoxShadow(color: Colors.red.withValues(alpha: 0.5), blurRadius: 6)] : null,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            StatusBadge(
                              label: alert['typeAlerte'] == 'ABSENCE_48H' ? 'Absence 48h' : 'Rapport',
                              color: isActive ? Colors.orange : Colors.green,
                            ),
                            const SizedBox(height: 6),
                            Text(alert['message'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 13)),
                            Text(alert['dateDeclenchement']?.toString().substring(0, 10) ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                          ])),
                          if (isActive)
                            IconButton(
                              icon: const Icon(Icons.check_circle, color: Colors.green),
                              onPressed: () => _resolveAlert(alert['id'] as String),
                            ),
                        ]),
                      );
                    }),
                ]),
              ),
            ),
    );
  }
}
