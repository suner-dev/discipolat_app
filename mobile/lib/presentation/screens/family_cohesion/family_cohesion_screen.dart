import 'package:flutter/material.dart';

/// P1 #42 — Indicateur de cohésion familiale
class FamilyCohesionScreen extends StatelessWidget {
  const FamilyCohesionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('💞 Cohésion Familiale'),
        backgroundColor: Colors.pink.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Family cohesion score
          Card(
            color: Colors.pink.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Score de cohésion', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 12),
                Center(child: Text('7.5/10', style: TextStyle(fontSize: 36, fontWeight: FontWeight.bold, color: Colors.pink.shade600))),
                const Center(child: Text('Bon — Maintenir les efforts')),
                const SizedBox(height: 12),
                _indicatorBar('Participation événements', 0.82, Colors.green),
                _indicatorBar('Diversité âmes', 0.65, Colors.orange),
                _indicatorBar('Équilibre charges', 0.78, Colors.green),
                _indicatorBar('Communication', 0.70, Colors.blue),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Familles du réseau', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _familyRow('Famille Grâce', 8.2, Colors.green),
          _familyRow('Famille Espoir', 7.1, Colors.blue),
          _familyRow('Famille Paix', 6.8, Colors.orange),
          _familyRow('Famille Joie', 7.5, Colors.green),
        ],
      ),
    );
  }

  Widget _indicatorBar(String label, double value, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(label, style: const TextStyle(fontSize: 12)),
          Text('${(value * 100).toInt()}%', style: TextStyle(fontSize: 12, color: color)),
        ]),
        LinearProgressIndicator(value: value, color: color),
      ]),
    );
  }

  Widget _familyRow(String name, double score, Color color) {
    return Card(child: ListTile(
      title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
      trailing: Text('$score/10', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: color)),
    ));
  }
}
