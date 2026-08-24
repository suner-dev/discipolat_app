import 'package:flutter/material.dart';

/// P1 #15 — Tableau de bord exécutif avec insights IA
class ExecutiveInsightsScreen extends StatelessWidget {
  const ExecutiveInsightsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🧠 Insights Exécutifs'),
        backgroundColor: Colors.deepPurple.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // AI Insight card
          Card(
            color: Colors.deepPurple.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    const Icon(Icons.auto_awesome, color: Colors.deepPurple),
                    const SizedBox(width: 8),
                    Text('Insight IA', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.deepPurple.shade700)),
                  ]),
                  const SizedBox(height: 8),
                  const Text(
                    'La présence a baissé de 12% chez les 18-25 ans ce mois-ci, '
                    'principalement le dimanche soir. Recommandation: organiser un événement jeune-specific.',
                    style: TextStyle(fontSize: 14),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // KPI cards
          const Text('KPIs Clés', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Row(children: [
            _kpiCard('Présence', '78%', '+2%', Colors.green),
            const SizedBox(width: 8),
            _kpiCard('Conversion', '12%', '+5%', Colors.green),
          ]),
          const SizedBox(height: 8),
          Row(children: [
            _kpiCard('Rétention', '85%', '-1%', Colors.orange),
            const SizedBox(width: 8),
            _kpiCard('Générosité', '45K', '+8%', Colors.green),
          ]),
          const SizedBox(height: 16),
          // Trends
          const Text('Tendances', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _trendItem(Icons.trending_up, 'Croissance baptized +23%', Colors.green),
          _trendItem(Icons.trending_down, 'Décrochage jeunes -12%', Colors.red),
          _trendItem(Icons.trending_up, 'Engagement en ligne +34%', Colors.green),
          _trendItem(Icons.stay_current_portrait, 'Nouveaux membres: stable', Colors.orange),
        ],
      ),
    );
  }

  Widget _kpiCard(String label, String value, String delta, Color deltaColor) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(label, style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
            const SizedBox(height: 4),
            Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            Text(delta, style: TextStyle(fontSize: 12, color: deltaColor, fontWeight: FontWeight.bold)),
          ]),
        ),
      ),
    );
  }

  Widget _trendItem(IconData icon, String text, Color color) {
    return ListTile(
      leading: Icon(icon, color: color, size: 20),
      title: Text(text, style: const TextStyle(fontSize: 14)),
      dense: true,
    );
  }
}
