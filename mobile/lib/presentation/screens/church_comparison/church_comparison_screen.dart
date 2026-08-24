import 'package:flutter/material.dart';

/// P1 #47 — Comparaison d'églises (réseau)
class ChurchComparisonScreen extends StatelessWidget {
  const ChurchComparisonScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('⚖️ Comparaison d\'Églises'),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Our church card
          Card(
            color: Colors.indigo.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Notre Église', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 8),
                  _benchmarkBar('Présence', 0.78, 0.72, '78% vs 72%'),
                  _benchmarkBar('Rétention', 0.85, 0.79, '85% vs 79%'),
                  _benchmarkBar('Conversion', 0.12, 0.08, '12% vs 8%'),
                  _benchmarkBar('Score spirituel', 0.71, 0.65, '71 vs 65'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Category filter
          const Text('Églises du réseau', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [
              ChoiceChip(label: const Text('Toutes'), selected: true, onSelected: (_) {}),
              ChoiceChip(label: const Text('Petite (<50)'), selected: false, onSelected: (_) {}),
              ChoiceChip(label: const Text('Moyenne'), selected: false, onSelected: (_) {}),
              ChoiceChip(label: const Text('Grande'), selected: false, onSelected: (_) {}),
            ],
          ),
          const SizedBox(height: 8),
          _churchRow('Église Espoir', '85', '75%', '82%', '68'),
          _churchRow('Église Paix', '120', '68%', '78%', '62'),
          _churchRow('Église Grâce', '200', '82%', '90%', '75'),
          _churchRow('Église Lumière', '45', '60%', '72%', '58'),
        ],
      ),
    );
  }

  Widget _benchmarkBar(String label, double ours, double avg, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [Text(label, style: const TextStyle(fontSize: 13)), Text(text, style: const TextStyle(fontSize: 12, color: Colors.grey))],
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              Expanded(
                child: LinearProgressIndicator(value: ours, color: Colors.indigo, backgroundColor: Colors.grey.shade200),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: LinearProgressIndicator(value: avg, color: Colors.grey, backgroundColor: Colors.grey.shade200),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _churchRow(String name, String members, String presence, String retention, String score) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Expanded(flex: 2, child: Text(name, style: const TextStyle(fontWeight: FontWeight.bold))),
            Expanded(child: Text(members, textAlign: TextAlign.center)),
            Expanded(child: Text(presence, textAlign: TextAlign.center)),
            Expanded(child: Text(retention, textAlign: TextAlign.center)),
            Expanded(child: Text(score, textAlign: TextAlign.center)),
          ],
        ),
      ),
    );
  }
}
