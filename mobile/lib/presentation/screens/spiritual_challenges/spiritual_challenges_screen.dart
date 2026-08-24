import 'package:flutter/material.dart';

/// P1 #51 — Défis spirituels
class SpiritualChallengesScreen extends StatelessWidget {
  const SpiritualChallengesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('⚡ Défis Spirituels'),
        backgroundColor: Colors.red.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Active challenge
          Card(
            color: Colors.red.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  const Icon(Icons.local_fire_department, color: Colors.orange),
                  const SizedBox(width: 8),
                  const Text('Défi en cours', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ]),
                const SizedBox(height: 8),
                const Text('30 jours de prière', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500)),
                const SizedBox(height: 4),
                const Text('Jour 12/30 — Continuer !'),
                const SizedBox(height: 8),
                const LinearProgressIndicator(value: 0.4),
                const SizedBox(height: 4),
                const Text('40% complété'),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Available challenges
          const Text('Défis disponibles', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _challengeCard('📖 Lecture biblique 7j', '7 jours', '12 participants', Colors.blue),
          _challengeCard('🙏 Prière jeûne 3j', '3 jours', '8 participants', Colors.purple),
          _challengeCard('💪 Service communautaire', '5 jours', '15 participants', Colors.green),
          _challengeCard('✍️ Journal gratitude 21j', '21 jours', '20 participants', Colors.teal),
          const SizedBox(height: 16),
          // Completed
          const Text('Défis complétés', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _completedChallenge('Témoignage partagé', 'Badge gagné: Témoin'),
          _completedChallenge('Inviter 3 personnes', 'Badge gagné: Évangélisateur'),
        ],
      ),
    );
  }

  Widget _challengeCard(String title, String duration, String participants, Color color) {
    return Card(child: ListTile(
      leading: CircleAvatar(backgroundColor: color.withOpacity(0.1), child: Icon(Icons.flash_on, color: color, size: 18)),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
      subtitle: Text('$duration • $participants'),
      trailing: const Icon(Icons.add_circle_outline),
    ));
  }

  Widget _completedChallenge(String title, String badge) {
    return Card(child: ListTile(
      leading: const Icon(Icons.check_circle, color: Colors.green),
      title: Text(title),
      subtitle: Text(badge, style: const TextStyle(fontSize: 12)),
    ));
  }
}
