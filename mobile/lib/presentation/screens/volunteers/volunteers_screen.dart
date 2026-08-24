import 'package:flutter/material.dart';

/// P1 #62 — Gestion avancée des bénévoles
class VolunteersScreen extends StatelessWidget {
  const VolunteersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🙋 Bénévoles'),
        backgroundColor: Colors.cyan.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () {},
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.cyan.shade600,
        child: const Icon(Icons.person_add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Stats
          Row(
            children: [
              _statChip('Actifs', '24', Colors.green),
              _statChip('Disponibles', '18', Colors.blue),
              _statChip('En attente', '5', Colors.orange),
            ],
          ),
          const SizedBox(height: 16),
          // Skill matching
          Card(
            child: ListTile(
              leading: const Icon(Icons.auto_awesome, color: Colors.cyan),
              title: const Text('Matcher bénévoles → Événement'),
              subtitle: const Text('Trouvez les bénévoles idéaux'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {},
            ),
          ),
          const SizedBox(height: 16),
          // Volunteers list
          const Text('Bénévoles actifs', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _volunteerTile('Jean-Pierre M.', 'Animation, Accueil', 'Plein temps', true),
          _volunteerTile('Marie K.', 'Musique, Louange', 'Week-end', true),
          _volunteerTile('David L.', 'Technique, Sound', 'Soir', true),
          _volunteerTile('Sarah B.', 'Accueil, Decoration', 'Occasionnel', true),
          _volunteerTile('Paul T.', 'Enseignement', 'Matin', false),
        ],
      ),
    );
  }

  Widget _statChip(String label, String count, Color color) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Column(
            children: [
              Text(count, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color)),
              Text(label, style: const TextStyle(fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _volunteerTile(String name, String skills, String avail, bool active) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: active ? Colors.green.shade50 : Colors.grey.shade100,
          child: Text(name[0], style: TextStyle(color: active ? Colors.green : Colors.grey)),
        ),
        title: Text(name),
        subtitle: Text('$skills • $avail'),
        trailing: Icon(Icons.chevron_right, color: active ? Colors.green : Colors.grey),
      ),
    );
  }
}
