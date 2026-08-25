import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Narration IA des KPIs — branché sur GET /api/v1/kpi-narrative.
class KpiNarrativeScreen extends StatefulWidget {
  const KpiNarrativeScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<KpiNarrativeScreen> createState() => _KpiNarrativeScreenState();
}

class _KpiNarrativeScreenState extends State<KpiNarrativeScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/kpi-narrative');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      setState(() { _error = 'Impossible de charger les narrations KPI.'; _loading = false; });
    }
  }

  Color _tendanceColor(String? t) => t == 'HAUSSE' ? Colors.green : (t == 'BAISSE' ? Colors.red : Colors.grey);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('📖 Narration des KPIs'), backgroundColor: Colors.cyan.shade700, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : _items.isEmpty
                  ? const Center(child: Text('Aucune narration générée. Utilisez « Générer » côté web.'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final k = _items[i] as Map<String, dynamic>;
                          final tendance = k['tendance']?.toString() ?? 'STABLE';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Row(children: [
                                  Expanded(child: Text(k['typeKPI']?.toString() ?? 'KPI', style: const TextStyle(fontWeight: FontWeight.w600))),
                                  Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: _tendanceColor(tendance).withValues(alpha: .12), borderRadius: BorderRadius.circular(10)), child: Text('${k['variationPct'] ?? 0}%', style: TextStyle(fontSize: 10, color: _tendanceColor(tendance), fontWeight: FontWeight.bold))),
                                ]),
                                const SizedBox(height: 6),
                                if ((k['narration'] ?? '').toString().isNotEmpty) Text(k['narration'].toString(), style: const TextStyle(fontSize: 13)),
                                if ((k['recommandations'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 4), child: Text('💡 ${k['recommandations']}', style: TextStyle(fontSize: 12, color: Colors.blueGrey.shade700))),
                              ]),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
