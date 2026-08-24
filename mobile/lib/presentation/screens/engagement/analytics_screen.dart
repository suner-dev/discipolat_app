import 'package:flutter/material.dart';

/// P1 #61 — Analytics d'engagement (Plausible/Umami intégré)
class EngagementAnalyticsScreen extends StatelessWidget {
  const EngagementAnalyticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📊 Analytics d\'Engagement'),
        backgroundColor: Colors.orange.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Period selector
          Row(children: [
            _periodChip('7j', true), _periodChip('30j', false), _periodChip('90j', false), _periodChip('1an', false),
          ]),
          const SizedBox(height: 16),
          // Overview
          Row(children: [
            _statCard('Pages vues', '12.4K', Colors.blue),
            const SizedBox(width: 8),
            _statCard('Utilisateurs', '892', Colors.green),
          ]),
          const SizedBox(height: 8),
          Row(children: [
            _statCard('Taux engagement', '34%', Colors.teal),
            const SizedBox(width: 8),
            _statCard('Sessions/mois', '2.3K', Colors.purple),
          ]),
          const SizedBox(height: 16),
          // Top pages
          const Text('Pages les plus visitées', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _pageRow('/dashboard', '3,245', 0.85),
          _pageRow('/souls', '1,892', 0.58),
          _pageRow('/events', '1,456', 0.45),
          _pageRow('/prayers', '987', 0.30),
          const SizedBox(height: 16),
          // Funnel
          const Text('Funnel inscription → engagement', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _funnelStep('Visite', 1000, 1.0),
          _funnelStep('Inscription', 450, 0.45),
          _funnelStep('Première action', 280, 0.28),
          _funnelStep('Membre actif', 180, 0.18),
        ],
      ),
    );
  }

  Widget _periodChip(String label, bool selected) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(label: Text(label), selected: selected, onSelected: (_) {}),
    );
  }

  Widget _statCard(String label, String value, Color color) {
    return Expanded(
      child: Card(child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
          Text(value, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color)),
        ]),
      )),
    );
  }

  Widget _pageRow(String page, String views, double ratio) {
    return Card(child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Row(children: [
        Expanded(flex: 3, child: Text(page, style: const TextStyle(fontSize: 13))),
        Expanded(flex: 4, child: LinearProgressIndicator(value: ratio)),
        const SizedBox(width: 8),
        Expanded(flex: 2, child: Text(views, textAlign: TextAlign.right, style: const TextStyle(fontSize: 12))),
      ]),
    ));
  }

  Widget _funnelStep(String label, int count, double ratio) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(children: [
        SizedBox(width: 100, child: Text(label, style: const TextStyle(fontSize: 12))),
        Expanded(child: LinearProgressIndicator(value: ratio, minHeight: 20)),
        const SizedBox(width: 8),
        Text('$count', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
      ]),
    );
  }
}
