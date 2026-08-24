import 'package:flutter/material.dart';

/// P1 #23 — Messagerie de groupe par équipe
class GroupMessagesScreen extends StatelessWidget {
  const GroupMessagesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('💬 Messagerie Groupe'),
        backgroundColor: Colors.blue.shade600,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.blue.shade600,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // My groups
          const Text('Mes groupes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _groupItem('Département Louange', 'Marie: On se retrouve dimanche !', '3 min', 2, Colors.purple),
          _groupItem('Famille Grâce', 'Jean-Pierre: Réunion dimanche 17h', '1h', 0, Colors.green),
          _groupItem('Équipe Technique', 'David: Sonorisation OK', '3h', 5, Colors.blue),
          _groupItem('Jeunesse', 'Sarah: Activité vendredi', '1j', 12, Colors.orange),
          const SizedBox(height: 16),
          // Suggested groups
          const Text('Groupes suggérés', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _suggestedGroup('Département Accueil', '15 membres'),
          _suggestedGroup('Département Enseignement', '8 membres'),
          _suggestedGroup('Responsables', '5 membres'),
        ],
      ),
    );
  }

  Widget _groupItem(String name, String lastMessage, String time, int unread, Color color) {
    return Card(child: ListTile(
      leading: CircleAvatar(backgroundColor: color.withOpacity(0.1), child: Icon(Icons.group, color: color, size: 18)),
      title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
      subtitle: Text(lastMessage, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)),
      trailing: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        Text(time, style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
        if (unread > 0) Container(
          padding: const EdgeInsets.all(4),
          decoration: const BoxDecoration(color: Colors.blue, shape: BoxShape.circle),
          child: Text('$unread', style: const TextStyle(color: Colors.white, fontSize: 10)),
        ),
      ]),
    ));
  }

  Widget _suggestedGroup(String name, String members) {
    return Card(child: ListTile(
      leading: const CircleAvatar(child: Icon(Icons.group_add, size: 18)),
      title: Text(name),
      subtitle: Text(members, style: const TextStyle(fontSize: 12)),
      trailing: const Icon(Icons.add_circle_outline, color: Colors.blue),
    ));
  }
}
