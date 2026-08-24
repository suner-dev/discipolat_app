import 'package:flutter/material.dart';

/// P1 #35 — Matching membres ↔ compétences
class SkillMatchingScreen extends StatelessWidget {
  const SkillMatchingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🧩 Matching Compétences'),
        backgroundColor: Colors.amber.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.amber.shade700,
        child: const Icon(Icons.auto_awesome, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // AI Match button
          Card(
            color: Colors.amber.shade50,
            child: ListTile(
              leading: const Icon(Icons.psychology, color: Colors.amber),
              title: const Text('Lancer le matching IA'),
              subtitle: const Text('Analyser les compétences vs besoins'),
              trailing: const Icon(Icons.play_arrow),
              onTap: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Matching en cours...')),
                );
              },
            ),
          ),
          const SizedBox(height: 16),
          // Proposed matches
          const Text('Propositions', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _matchCard('Jean-Pierre M.', 'Département Louange', 'Animation', 92, Colors.green),
          _matchCard('Marie K.', 'Département Accueil', 'Hospitalité', 87, Colors.green),
          _matchCard('David L.', 'Département Technique', 'Son & Lumière', 78, Colors.orange),
          _matchCard('Sarah B.', 'Département Enseignement', 'Enfants', 65, Colors.orange),
          const SizedBox(height: 16),
          // My skills
          const Text('Mes compétences déclarées', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              Chip(label: const Text('Animation'), avatar: const Icon(Icons.star, size: 16)),
              Chip(label: const Text('Musique'), avatar: const Icon(Icons.star, size: 16)),
              Chip(label: const Text('Accueil'), avatar: const Icon(Icons.star, size: 16)),
              Chip(
                label: const Text('Ajouter +'),
                backgroundColor: Colors.grey.shade100,
                avatar: const Icon(Icons.add, size: 16),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _matchCard(String name, String dept, String skill, int score, Color color) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: color.withOpacity(0.1),
          child: Text('$score', style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12)),
        ),
        title: Text(name),
        subtitle: Text('$dept — $skill'),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(icon: const Icon(Icons.check_circle, color: Colors.green), onPressed: () {}),
            IconButton(icon: const Icon(Icons.cancel, color: Colors.red), onPressed: () {}),
          ],
        ),
      ),
    );
  }
}
