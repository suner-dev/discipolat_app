import 'package:flutter/material.dart';

class WeeklyChallengesScreen extends StatelessWidget {
  const WeeklyChallengesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final challenges = [
      {'title': '5 jours de prière consécutive', 'emoji': '🙏', 'difficulty': 'EASY', 'xp': 50, 'progress': 60.0, 'category': 'PRAYER'},
      {'title': 'Lisez 3 chapitres de la Bible', 'emoji': '📖', 'difficulty': 'MEDIUM', 'xp': 75, 'progress': 33.0, 'category': 'READING'},
      {'title': 'Rendez visite à un membre', 'emoji': '🤝', 'difficulty': 'MEDIUM', 'xp': 100, 'progress': 100.0, 'category': 'SERVICE'},
      {'title': 'Jour de jeûne', 'emoji': '💧', 'difficulty': 'HARD', 'xp': 150, 'progress': 0.0, 'category': 'FASTING'},
      {'title': 'Gratitude quotidienne', 'emoji': '✨', 'difficulty': 'EASY', 'xp': 40, 'progress': 80.0, 'category': 'GRATITUDE'},
    ];

    Color diffColor(String d) {
      switch (d) {
        case 'HARD': return Colors.red;
        case 'MEDIUM': return Colors.orange;
        default: return Colors.green;
      }
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Défis hebdomadaires'), backgroundColor: Colors.orange.shade600, foregroundColor: Colors.white),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: challenges.length,
        itemBuilder: (context, i) {
          final c = challenges[i];
          final isComplete = (c['progress'] as double) >= 100;
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            color: isComplete ? Colors.green.shade50 : Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Text(c['emoji'] as String, style: const TextStyle(fontSize: 28)),
                    const SizedBox(width: 12),
                    Expanded(child: Text(c['title'] as String, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(color: diffColor(c['difficulty'] as String).withOpacity(0.15), borderRadius: BorderRadius.circular(12)),
                      child: Text(c['difficulty'] as String, style: TextStyle(color: diffColor(c['difficulty'] as String), fontSize: 11, fontWeight: FontWeight.bold)),
                    ),
                  ]),
                  const SizedBox(height: 12),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: (c['progress'] as double) / 100,
                      backgroundColor: Colors.grey.shade200,
                      color: isComplete ? Colors.green : Colors.orange,
                      minHeight: 8,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(children: [
                    Text('${c['progress']}%', style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                    const Spacer(),
                    Icon(Icons.bolt, color: Colors.orange.shade600, size: 16),
                    Text('+${c['xp']} XP', style: TextStyle(color: Colors.orange.shade600, fontWeight: FontWeight.bold)),
                  ]),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
