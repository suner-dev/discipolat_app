import 'package:flutter/material.dart';

/// P1 #58 — Annuaire de l'église (fiches publiques opt-in)
class ChurchDirectoryScreen extends StatelessWidget {
  const ChurchDirectoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📒 Annuaire de l\'Église'),
        backgroundColor: Colors.blue.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.search), onPressed: () {})],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Search bar
          TextField(
            decoration: InputDecoration(
              hintText: 'Rechercher un membre...',
              prefixIcon: const Icon(Icons.search),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              filled: true,
              fillColor: Colors.grey.shade100,
            ),
          ),
          const SizedBox(height: 16),
          // Members list
          _memberTile('Jean-Pierre M.', 'Famille Grâce', 'Chef de famille', true),
          _memberTile('Marie K.', 'Famille Espoir', 'Animateur', true),
          _memberTile('David L.', 'Famille Paix', 'Responsable technique', true),
          _memberTile('Sarah B.', 'Famille Grâce', 'Accueil', true),
          _memberTile('Paul T.', 'Famille Joie', 'Enseignant', false),
          _memberTile('Anne D.', 'Famille Espoir', 'Louange', true),
          _memberTile('Lucas M.', 'Famille Paix', 'Jeunesse', true),
        ],
      ),
    );
  }

  Widget _memberTile(String name, String family, String role, bool public) {
    return Card(child: ListTile(
      leading: CircleAvatar(
        backgroundColor: public ? Colors.blue.shade50 : Colors.grey.shade200,
        child: Text(name[0], style: TextStyle(color: public ? Colors.blue : Colors.grey)),
      ),
      title: Text(name),
      subtitle: Text('$family • $role'),
      trailing: public
          ? const Icon(Icons.visibility, color: Colors.green, size: 18)
          : const Icon(Icons.visibility_off, color: Colors.grey, size: 18),
    ));
  }
}
