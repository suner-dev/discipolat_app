import 'package:flutter/material.dart';

/// P1 #39 — Suivi de développement faiseur
class MakerTrackingScreen extends StatelessWidget {
  const MakerTrackingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🌱 Mon Parcours de Faiseur'),
        backgroundColor: Colors.green.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Summary cards
          Row(
            children: [
              _summaryCard('Formations', '5', Icons.school, Colors.blue),
              const SizedBox(width: 8),
              _summaryCard('Compétences', '8', Icons.psychology, Colors.purple),
              const SizedBox(width: 8),
              _summaryCard('Âmes', '12', Icons.people, Colors.green),
            ],
          ),
          const SizedBox(height: 16),
          // Timeline
          const Text('Timeline', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _timelineItem(Icons.school, 'Formation accueil', 'Il y a 2 mois', Colors.blue),
          _timelineItem(Icons.psychology, 'Compétence: Animation', 'Il y a 1 mois', Colors.purple),
          _timelineItem(Icons.people, 'Accompagnement: Marie D.', 'Il y a 2 semaines', Colors.green),
          _timelineItem(Icons.emoji_events, 'Défi: 30 jours de prière', 'Il y a 1 semaine', Colors.orange),
          _timelineItem(Icons.card_membership, 'Certificat: Leader d\'équipe', 'Hier', Colors.amber),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Points totaux', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      const Icon(Icons.star, color: Colors.amber, size: 32),
                      const SizedBox(width: 8),
                      Text('1,250 points', style: TextStyle(fontSize: 24, color: Colors.green.shade700, fontWeight: FontWeight.bold)),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _summaryCard(String title, String count, IconData icon, Color color) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            children: [
              Icon(icon, color: color, size: 28),
              const SizedBox(height: 4),
              Text(count, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
              Text(title, style: const TextStyle(fontSize: 11)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _timelineItem(IconData icon, String title, String date, Color color) {
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: color.withOpacity(0.1),
        child: Icon(icon, color: color, size: 18),
      ),
      title: Text(title, style: const TextStyle(fontSize: 14)),
      subtitle: Text(date, style: const TextStyle(fontSize: 12)),
    );
  }
}
