import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Centre d'intelligence — branché sur GET /api/v1/intelligence (KPIs réels).
class IntelligenceCenterScreen extends StatefulWidget {
  const IntelligenceCenterScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<IntelligenceCenterScreen> createState() => _IntelligenceCenterScreenState();
}

class _IntelligenceCenterScreenState extends State<IntelligenceCenterScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _kpis = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/intelligence');
      final d = res.data;
      setState(() {
        _kpis = d is List ? d : <dynamic>[];
        _loading = false;
      });
    } catch (_) {
      setState(() { _error = 'Impossible de charger les KPIs. Le centre doit être initialisé côté admin.'; _loading = false; });
    }
  }

  Color _trendColor(String? t) => t == 'UP' ? Colors.green : (t == 'DOWN' ? Colors.red : Colors.grey);

  @override
  Widget build(BuildContext context) {
    final alerts = _kpis.where((k) => k is Map && k['isAlert'] == true).toList();
    return Scaffold(
      appBar: AppBar(title: const Text('🏛️ Centre d\'intelligence'), backgroundColor: Colors.blueGrey, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (alerts.isNotEmpty) ...[
                        Card(color: Colors.amber.shade50, child: ListTile(leading: const Icon(Icons.warning_amber, color: Colors.amber), title: Text('${alerts.length} alerte(s) active(s)', style: const TextStyle(fontWeight: FontWeight.bold)))),
                        const SizedBox(height: 12),
                      ],
                      ..._kpis.map((k) {
                        final kpi = k as Map<String, dynamic>;
                        final trend = kpi['trend']?.toString() ?? 'STABLE';
                        return Card(
                          margin: const EdgeInsets.only(bottom: 8),
                          child: ListTile(
                            leading: Icon(Icons.insert_chart, color: _trendColor(trend)),
                            title: Text(kpi['name']?.toString() ?? 'KPI', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                            subtitle: Text(kpi['description']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
                            trailing: Text('${((kpi['currentValue'] as num?) ?? 0).toStringAsFixed(1)}${kpi['unit'] ?? ''}', style: const TextStyle(fontWeight: FontWeight.bold)),
                          ),
                        );
                      }),
                    ],
                  ),
                ),
    );
  }
}
