import 'package:flutter/material.dart';

/// P1 #28 — Broadcast ciblé avec accusé de lecture
class BroadcastScreen extends StatelessWidget {
  const BroadcastScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📢 Diffusion / Broadcast'),
        backgroundColor: Colors.deepOrange.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {},
        backgroundColor: Colors.deepOrange.shade700,
        icon: const Icon(Icons.send, color: Colors.white),
        label: const Text('Nouvelle diffusion', style: TextStyle(color: Colors.white)),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Stats
          Row(children: [
            _statCard('Envoyées', '12', Colors.deepOrange),
            const SizedBox(width: 8),
            _statCard('Lues', '89%', Colors.green),
          ]),
          const SizedBox(height: 16),
          // Recent broadcasts
          const Text('Diffusions récentes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _broadcastCard('Rappel: Culte spécial dimanche', 'Tous les membres', '24 août', '92% lu', 0.92),
          _broadcastCard('Réunion équipe technique', 'Département Technique', '22 août', '85% lu', 0.85),
          _broadcastCard('Activité jeunesse vendredi', 'Jeunesse (18-25)', '20 août', '78% lu', 0.78),
          const SizedBox(height: 16),
          // Targeting options
          const Text('Ciblage', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _targetChip('Tous les membres'),
          _targetChip('Par département'),
          _targetChip('Par famille'),
          _targetChip('Par rôle'),
        ],
      ),
    );
  }

  Widget _statCard(String label, String value, Color color) {
    return Expanded(
      child: Card(child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
          Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
        ]),
      )),
    );
  }

  Widget _broadcastCard(String title, String target, String date, String readRate, double ratio) {
    return Card(child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold))),
          Text(readRate, style: const TextStyle(fontSize: 12, color: Colors.green)),
        ]),
        Text('$target • $date', style: const TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 8),
        LinearProgressIndicator(value: ratio, color: Colors.green),
      ]),
    ));
  }

  Widget _targetChip(String label) {
    return Padding(
      padding: const EdgeInsets.only(right: 8, bottom: 8),
      child: ActionChip(label: Text(label), onPressed: () {}),
    );
  }
}
