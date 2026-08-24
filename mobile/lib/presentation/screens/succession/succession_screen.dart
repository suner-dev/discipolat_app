import 'package:flutter/material.dart';

/// P1 #40 — Plan de succession
class SuccessionScreen extends StatelessWidget {
  const SuccessionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('👥 Plan de Succession'),
        backgroundColor: Colors.brown.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Overview
          Card(
            color: Colors.brown.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Postes à pourvoir', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
                  _countBadge('Prêts', '3', Colors.green),
                  _countBadge('En formation', '5', Colors.orange),
                  _countBadge('À identifier', '2', Colors.red),
                ]),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Faiseurs prêts', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _candidateCard('Jean-Pierre M.', 'Chef de famille → Responsable', 92, 'Prêt', Colors.green),
          _candidateCard('Marie K.', 'Animateur → Chef de famille', 78, 'En formation', Colors.orange),
          _candidateCard('David L.', 'Technicien → Responsable technique', 85, 'Prêt', Colors.green),
          const SizedBox(height: 16),
          const Text('Postes ouverts', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _positionItem('Responsable Louange', '1 candidat potentiel'),
          _positionItem('Responsable Jeunesse', 'À identifier'),
          _positionItem('Chef Famille Espoir', 'En formation (Marie K.)'),
        ],
      ),
    );
  }

  Widget _countBadge(String label, String count, Color color) {
    return Column(children: [
      Text(count, style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: color)),
      Text(label, style: const TextStyle(fontSize: 12)),
    ]);
  }

  Widget _candidateCard(String name, String trajectory, int readiness, String status, Color color) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(child: Text(name, style: const TextStyle(fontWeight: FontWeight.bold))),
            Chip(label: Text(status, style: TextStyle(fontSize: 11, color: color)), backgroundColor: color.withOpacity(0.1)),
          ]),
          Text(trajectory, style: const TextStyle(fontSize: 12, color: Colors.grey)),
          const SizedBox(height: 8),
          LinearProgressIndicator(value: readiness / 100, color: color),
          Text('$readiness% prêt', style: TextStyle(fontSize: 11, color: color)),
        ]),
      ),
    );
  }

  Widget _positionItem(String title, String candidates) {
    return Card(child: ListTile(
      leading: const Icon(Icons.work_outline),
      title: Text(title),
      subtitle: Text(candidates),
      trailing: const Icon(Icons.chevron_right),
    ));
  }
}
