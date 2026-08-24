import 'package:flutter/material.dart';

class GroupMessagesScreen extends StatelessWidget {
  const GroupMessagesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final groups = [
      {'name': 'Équipe Louange', 'icon': '🎵', 'members': 12, 'lastMsg': 'Répétition demain à 18h 🎶', 'unread': 3},
      {'name': 'Équipe Accueil', 'icon': '👋', 'members': 8, 'lastMsg': 'Merci pour votre service !', 'unread': 0},
      {'name': 'Famille Mbarga', 'icon': '👨‍👩‍👧‍👦', 'members': 6, 'lastMsg': 'Bonne nuit à tous 🙏', 'unread': 1},
      {'name': 'Dép. Jeunesse', 'icon': '🔥', 'members': 25, 'lastMsg': 'Inscription retraite ouverte', 'unread': 5},
    ];

    return Scaffold(
      appBar: AppBar(title: const Text('Messages de groupe'), backgroundColor: Colors.teal.shade600, foregroundColor: Colors.white),
      body: ListView.builder(
        padding: const EdgeInsets.all(8),
        itemCount: groups.length,
        itemBuilder: (context, i) {
          final g = groups[i];
          return ListTile(
            leading: CircleAvatar(
              radius: 24,
              backgroundColor: Colors.teal.shade100,
              child: Text(g['icon'] as String, style: const TextStyle(fontSize: 22)),
            ),
            title: Text(g['name'] as String, style: const TextStyle(fontWeight: FontWeight.bold)),
            subtitle: Text(g['lastMsg'] as String, maxLines: 1, overflow: TextOverflow.ellipsis),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('${g['members']} membres', style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
                if ((g['unread'] as int) > 0)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(color: Colors.red, borderRadius: BorderRadius.circular(10)),
                    child: Text('${g['unread']}', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}
