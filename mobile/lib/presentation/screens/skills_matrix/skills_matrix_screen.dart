import 'package:flutter/material.dart';

/// P1 #36 — Matrice de compétences
class SkillsMatrixScreen extends StatelessWidget {
  const SkillsMatrixScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🧩 Matrice de Compétences'),
        backgroundColor: Colors.purple.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Skills overview
          Card(
            color: Colors.purple.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Vue d\'ensemble', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
                  _skillStat('Compétences', '15', Colors.purple),
                  _skillStat('Membres évalués', '24', Colors.blue),
                  _skillStat('Gaps identifiés', '5', Colors.orange),
                ]),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Skills by department
          const Text('Compétences par département', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _skillRow('Animation', 4, 6, Colors.green),
          _skillRow('Musique', 3, 5, Colors.orange),
          _skillRow('Accueil', 5, 4, Colors.green),
          _skillRow('Technique', 2, 3, Colors.red),
          _skillRow('Enseignement', 3, 4, Colors.orange),
          const SizedBox(height: 16),
          // Gaps
          const Text('🔍 Gaps identifiés', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _gapItem('Animation — besoin de 2 membres supplémentaires', Colors.orange),
          _gapItem('Technique — aucun backup pour David L.', Colors.red),
          _gapItem('Enseignement — formation requise', Colors.amber),
        ],
      ),
    );
  }

  Widget _skillStat(String label, String count, Color color) {
    return Column(children: [
      Text(count, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
      Text(label, style: const TextStyle(fontSize: 12)),
    ]);
  }

  Widget _skillRow(String skill, int available, int needed, Color color) {
    return Card(child: Padding(
      padding: const EdgeInsets.all(12),
      child: Row(children: [
        Expanded(flex: 2, child: Text(skill, style: const TextStyle(fontWeight: FontWeight.bold))),
        Expanded(flex: 3, child: LinearProgressIndicator(value: available / needed, color: color)),
        const SizedBox(width: 8),
        Text('$available/$needed', style: TextStyle(color: color, fontWeight: FontWeight.bold)),
      ]),
    ));
  }

  Widget _gapItem(String text, Color color) {
    return ListTile(
      leading: Icon(Icons.warning, color: color, size: 18),
      title: Text(text, style: const TextStyle(fontSize: 13)),
      dense: true,
    );
  }
}
