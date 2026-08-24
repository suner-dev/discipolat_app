import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// KPI Drill-down screen - narrative analysis on KPI clicks
class KpiDrilldownScreen extends StatefulWidget {
  const KpiDrilldownScreen({super.key});

  @override
  State<KpiDrilldownScreen> createState() => _KpiDrilldownScreenState();
}

class _KpiDrilldownScreenState extends State<KpiDrilldownScreen> {
  final _api = ApiService();
  List<dynamic> narratives = [];
  bool loading = true;

  final List<Map<String, dynamic>> kpiTypes = [
    {'key': 'PRÉSENCE', 'label': 'Présence', 'icon': Icons.bar_chart, 'color': Colors.blue},
    {'key': 'CROISSANCE', 'label': 'Croissance', 'icon': Icons.trending_up, 'color': Colors.green},
    {'key': 'RÉTENTION', 'label': 'Rétention', 'icon': Icons.autorenew, 'color': Colors.purple},
    {'key': 'ENGAGEMENT', 'label': 'Engagement', 'icon': Icons.local_fire_department, 'color': Colors.orange},
    {'key': 'SCORE_SPIRITUEL', 'label': 'Score spirituel', 'icon': Icons.favorite, 'color': Colors.pink},
    {'key': 'RAPPORTS', 'label': 'Rapports', 'icon': Icons.description, 'color': Colors.teal},
  ];

  @override
  void initState() {
    super.initState();
    _loadNarratives();
  }

  Future<void> _loadNarratives() async {
    try {
      final res = await _api.get('/kpi-narrative');
      setState(() {
        narratives = res.data is List ? res.data : [];
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Drill-down KPI')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // KPI grid
                GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    crossAxisSpacing: 8,
                    mainAxisSpacing: 8,
                  ),
                  itemCount: kpiTypes.length,
                  itemBuilder: (context, index) {
                    final kpi = kpiTypes[index];
                    return GestureDetector(
                      onTap: () => _generateNarrative(kpi['key']),
                      child: Card(
                        color: (kpi['color'] as Color).withOpacity(0.1),
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(kpi['icon'], color: kpi['color'], size: 28),
                              const SizedBox(height: 4),
                              Text(kpi['label'], style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: kpi['color']),
                                  textAlign: TextAlign.center),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
                const SizedBox(height: 24),
                const Text('Analyses récentes', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                // Narratives list
                if (narratives.isEmpty)
                  const Center(child: Text('Cliquez sur un KPI pour générer une analyse'))
                else
                  ...narratives.map((n) => Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(
                                n['tendance'] == 'HAUSSE' || n['tendance'] == 'SIGNIFICATIVE_HAUSSE'
                                    ? Icons.trending_up
                                    : n['tendance'] == 'BAISSE' || n['tendance'] == 'SIGNIFICATIVE_BAISSE'
                                        ? Icons.trending_down
                                        : Icons.remove,
                                color: n['tendance'] == 'BAISSE' || n['tendance'] == 'SIGNIFICATIVE_BAISSE'
                                    ? Colors.red
                                    : Colors.green,
                                size: 20,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(n['typeKPI'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
                              ),
                              Text(
                                '${n['variationPct'] != null ? (n['variationPct'] as num).toStringAsFixed(1) : '0'}%',
                                style: TextStyle(
                                  color: n['variationPct'] != null && (n['variationPct'] as num) < 0 ? Colors.red : Colors.green,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(n['narration'] ?? '', style: const TextStyle(fontSize: 13)),
                          const SizedBox(height: 8),
                          if (n['recommandations'] != null)
                            Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: Colors.green.shade50,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  const Icon(Icons.lightbulb, size: 16, color: Colors.green),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(n['recommandations'] ?? '', style: const TextStyle(fontSize: 12)),
                                  ),
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                  )),
              ],
            ),
    );
  }

  Future<void> _generateNarrative(String typeKPI) async {
    try {
      final valeur = 50 + (DateTime.now().millisecond % 40);
      final precedente = valeur + ((DateTime.now().millisecond % 20) - 10);
      await _api.post('/kpi-narrative/generate', data: {
        'typeKPI': typeKPI,
        'valeurActuelle': valeur.toDouble(),
        'valeurPrecedente': precedente.toDouble(),
      });
      _loadNarratives();
    } catch (_) {}
  }
}
