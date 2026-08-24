import 'package:flutter/material.dart';

/// P1 #17 — Notes IA automatiques pendant visites pastorales
class AiVisitNotesScreen extends StatelessWidget {
  const AiVisitNotesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🎙️ Notes IA Visites'),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {},
        backgroundColor: Colors.teal.shade700,
        icon: const Icon(Icons.mic, color: Colors.white),
        label: const Text('Nouvelle note', style: TextStyle(color: Colors.white)),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Active recording card
          Card(
            color: Colors.teal.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  const Icon(Icons.fiber_manual_record, color: Colors.red, size: 16),
                  const SizedBox(width: 8),
                  const Text('Dernière visite: Marie Dupont', style: TextStyle(fontWeight: FontWeight.bold)),
                ]),
                const SizedBox(height: 8),
                const Text('Transcription: "Marie traverse une période difficile suite au décès de sa mère..."', style: TextStyle(fontSize: 13)),
                const SizedBox(height: 8),
                const Text('Résumé IA: Situation de deuil — accompagnement pastoral recommandé', style: TextStyle(fontSize: 13, color: Colors.teal, fontWeight: FontWeight.w500)),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Notes récentes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _noteCard('Jean M.', '24 août', 'Visite à domicile', 'Accompagnement maladie'),
          _noteCard('Famille K.', '22 août', 'Réunion familiale', 'Cohésion renforcée'),
          _noteCard('David L.', '20 août', 'Entretien pastoral', 'Orientation formation'),
          _noteCard('Sarah B.', '18 août', 'Suivi post-baptême', 'Intégration en cours'),
        ],
      ),
    );
  }

  Widget _noteCard(String name, String date, String type, String summary) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(backgroundColor: Colors.teal.shade50, child: Icon(Icons.mic, color: Colors.teal.shade700, size: 18)),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text('$date • $type'),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }
}
