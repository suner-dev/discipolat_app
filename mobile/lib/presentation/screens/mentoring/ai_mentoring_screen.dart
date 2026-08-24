import 'package:flutter/material.dart';

/// P1 #38 — Mentorat IA pour chefs de famille
class AiMentoringScreen extends StatelessWidget {
  const AiMentoringScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🎓 Mentorat IA'),
        backgroundColor: Colors.indigo.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // AI suggestion card
          Card(
            color: Colors.indigo.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  const Icon(Icons.auto_awesome, color: Colors.indigo),
                  const SizedBox(width: 8),
                  const Text('Suggestion IA', style: TextStyle(fontWeight: FontWeight.bold)),
                ]),
                const SizedBox(height: 8),
                const Text(
                  'Basé sur le profil de Jean-Pierre (style visuel, force: accueil, '
                  'zone de croissance: leadership), suggérez-lui de co-animer un accueil.',
                  style: TextStyle(fontSize: 13),
                ),
                const SizedBox(height: 8),
                Row(children: [
                  ElevatedButton.icon(onPressed: () {}, icon: const Icon(Icons.check, size: 16), label: const Text('Appliquer')),
                  const SizedBox(width: 8),
                  OutlinedButton.icon(onPressed: () {}, icon: const Icon(Icons.close, size: 16), label: const Text('Passer')),
                ]),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Mentoring profiles
          const Text('Mes chefs de famille', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _profileCard('Jean-Pierre M.', 'Famille Grâce', '8/10', 'Leadership', Colors.green),
          _profileCard('Marie K.', 'Famille Espoir', '6/10', 'Accueil', Colors.orange),
          _profileCard('David L.', 'Famille Paix', '7/10', 'Enseignement', Colors.blue),
          const SizedBox(height: 16),
          // Approaches
          const Text('Approches recommandées', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _approachItem('Style visuel → utiliser des schémas', Icons.visibility),
          _approachItem('Préfère l\'apprentissage pratique', Icons.build),
          _approachItem('Besoin de feedback régulier', Icons.chat),
        ],
      ),
    );
  }

  Widget _profileCard(String name, String family, String score, String strength, Color color) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(backgroundColor: color.withOpacity(0.1), child: Text(score.split('/')[0], style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12))),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text('$family • Force: $strength'),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }

  Widget _approachItem(String text, IconData icon) {
    return ListTile(
      leading: Icon(icon, size: 20, color: Colors.indigo),
      title: Text(text, style: const TextStyle(fontSize: 13)),
      dense: true,
    );
  }
}
