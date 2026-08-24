import 'package:flutter/material.dart';

/// P1 #43 — Banque de ressources familiales
class FamilyResourcesScreen extends StatelessWidget {
  const FamilyResourcesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📚 Ressources Familiales'),
        backgroundColor: Colors.amber.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.amber.shade700,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Categories
          const Text('Catégories', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          Wrap(spacing: 8, runSpacing: 8, children: [
            _catChip('📖 Études bibliques', 12, Colors.blue),
            _catChip('🎥 Vidéos', 8, Colors.red),
            _catChip('📄 Documents', 15, Colors.green),
            _catChip('🎵 Musique', 5, Colors.purple),
            _catChip('📝 Notes', 20, Colors.orange),
          ]),
          const SizedBox(height: 16),
          // Recent resources
          const Text('Ressources récentes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _resourceItem('Étude Jean 3:16 — Commentaire', '📖', 'Étude biblique', 'Il y a 2 jours'),
          _resourceItem('Vidéo: Témoignage Famille Grâce', '🎥', 'Vidéo', 'Il y a 3 jours'),
          _resourceItem('Guide prière familiale', '📄', 'Document', 'Il y a 1 semaine'),
          _resourceItem('Chant: Amazing Grace (chorale)', '🎵', 'Musique', 'Il y a 2 semaines'),
          const SizedBox(height: 16),
          // Shared by
          const Text('Partagé par', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ListTile(
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: const Text('Jean-Pierre M.'),
            subtitle: const Text('5 ressources partagées'),
          ),
        ],
      ),
    );
  }

  Widget _catChip(String label, int count, Color color) {
    return Chip(
      avatar: Text('$count', style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.bold)),
      label: Text(label, style: const TextStyle(fontSize: 12)),
    );
  }

  Widget _resourceItem(String title, String icon, String category, String date) {
    return Card(child: ListTile(
      leading: Text(icon, style: const TextStyle(fontSize: 24)),
      title: Text(title, style: const TextStyle(fontSize: 13)),
      subtitle: Text('$category • $date'),
      trailing: const Icon(Icons.chevron_right, size: 20),
    ));
  }
}
