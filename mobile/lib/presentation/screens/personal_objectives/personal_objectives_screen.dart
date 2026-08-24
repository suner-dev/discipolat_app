import 'package:flutter/material.dart';

/// P1 #56 — Objectifs spirituels personnels
class PersonalObjectivesScreen extends StatelessWidget {
  const PersonalObjectivesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🎯 Mes Objectifs Spirituels'),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.teal.shade700,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Overview
          Card(
            color: Colors.teal.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Progression', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
                  _objStat('Actifs', '4', Colors.blue),
                  _objStat('Terminés', '6', Colors.green),
                  _objStat('Taux', '60%', Colors.teal),
                ]),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Active objectives
          const Text('Objectifs actifs', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _objectiveItem('📖 Lire la Bible chaque jour', 0.45, 'Jour 12/30', Colors.blue),
          _objectiveItem('🙏 Prier 30min/jour', 0.67, '20/30 jours', Colors.green),
          _objectiveItem('💪 Servir 1 fois/mois', 0.50, '1/2 cette mois', Colors.orange),
          _objectiveItem('🤝 Inviter 1 personne', 0.0, '0/1', Colors.red),
          const SizedBox(height: 16),
          // Achievements
          const Text('🏆 Accomplissements', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _achievementItem('Complété 7 jours de prière consécutifs'),
          _achievementItem('Lu 5 livres de la Bible'),
          _achievementItem('Servi 3 fois ce trimestre'),
        ],
      ),
    );
  }

  Widget _objStat(String label, String value, Color color) {
    return Column(children: [
      Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
      Text(label, style: const TextStyle(fontSize: 12)),
    ]);
  }

  Widget _objectiveItem(String title, double progress, String detail, Color color) {
    return Card(child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
        const SizedBox(height: 4),
        Text(detail, style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
        const SizedBox(height: 8),
        LinearProgressIndicator(value: progress, color: color),
      ]),
    ));
  }

  Widget _achievementItem(String text) {
    return ListTile(
      leading: const Icon(Icons.emoji_events, color: Colors.amber, size: 20),
      title: Text(text, style: const TextStyle(fontSize: 13)),
      dense: true,
    );
  }
}
