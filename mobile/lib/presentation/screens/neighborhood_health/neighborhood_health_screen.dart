import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// P3 #104 — Analyse de santé spirituelle par quartier (heatmap + zones faibles).
class NeighborhoodHealthScreen extends StatefulWidget {
  const NeighborhoodHealthScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<NeighborhoodHealthScreen> createState() => _NeighborhoodHealthScreenState();
}

class _NeighborhoodHealthScreenState extends State<NeighborhoodHealthScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _zones = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final res = await _api.get('/neighborhood-health');
      final data = res.data as Map<String, dynamic>?;
      setState(() { _zones = (data?['zones'] as List<dynamic>?) ?? []; _loading = false; });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  MaterialColor _statusColor(String status) {
    switch (status) {
      case 'BONNE':
        return Colors.green;
      case 'FAIBLE':
        return Colors.red;
      default:
        return Colors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('💗 Santé par quartier'),
        backgroundColor: Colors.pink.shade700,
        foregroundColor: Colors.white,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: _zones.isEmpty
                  ? ListView(children: const [Padding(padding: EdgeInsets.all(32), child: Center(child: Text('Aucune zone définie. Renseignez le champ « zone » des âmes.')))])
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _zones.length,
                      itemBuilder: (context, i) {
                        final z = _zones[i] as Map<String, dynamic>;
                        final status = z['status']?.toString() ?? 'MOYENNE';
                        final color = _statusColor(status);
                        final score = (z['healthScore'] as num?)?.toInt() ?? 0;
                        return Card(
                          margin: const EdgeInsets.only(bottom: 8),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          child: Padding(
                            padding: const EdgeInsets.all(14),
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                                Expanded(child: Text('📍 ${z['zone']}', style: const TextStyle(fontWeight: FontWeight.bold))),
                                Chip(label: Text(status, style: const TextStyle(fontSize: 11, color: Colors.white)), backgroundColor: color),
                              ]),
                              const SizedBox(height: 6),
                              LinearProgressIndicator(value: score / 100, color: color, backgroundColor: Colors.grey.shade200),
                              const SizedBox(height: 6),
                              Wrap(spacing: 12, children: [
                                Text('${z['total']} âmes', style: const TextStyle(fontSize: 12)),
                                Text('Score : $score/100', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                                Text('${z['contactsRecents']} contacts récents', style: const TextStyle(fontSize: 12)),
                              ]),
                              if (z['actionRecommandee'] != null)
                                Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Text(z['actionRecommandee'].toString(), style: TextStyle(fontSize: 11, fontStyle: FontStyle.italic, color: color.shade700)),
                                ),
                            ]),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}
