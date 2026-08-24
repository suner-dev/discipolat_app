import 'package:flutter/material.dart';

/// P1 #33 — Checklist événementielle
class EventChecklistScreen extends StatelessWidget {
  const EventChecklistScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('✅ Checklist Événement'),
        backgroundColor: Colors.green.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.green.shade700,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Upcoming event
          Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Culte dimanche 31 août', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                const LinearProgressIndicator(value: 0.6),
                const SizedBox(height: 4),
                const Text('12/20 tâches complétées'),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Material
          const Text('📦 Matériel', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
          _checkItem('Sonorisation vérifiée', true),
          _checkItem('Projecteur prêt', true),
          _checkItem('Chaises disposées', false),
          _checkItem('Boutique merchandise', false),
          const SizedBox(height: 16),
          // Équipes
          const Text('👥 Équipes', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
          _checkItem('Équipe accueil confirmée', true),
          _checkItem('Équipe louange confirmée', true),
          _checkItem('Équipe technique confirmée', false),
          _checkItem('Équipe enfant confirmée', true),
          const SizedBox(height: 16),
          // Documents
          const Text('📄 Documents', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
          _checkItem('Paroles proclamées', true),
          _checkItem('Ordre du jour', true),
          _checkItem('Annonces préparées', false),
        ],
      ),
    );
  }

  Widget _checkItem(String text, bool done) {
    return Card(child: CheckboxListTile(
      value: done,
      onChanged: (_) {},
      title: Text(text, style: TextStyle(
        decoration: done ? TextDecoration.lineThrough : null,
        color: done ? Colors.grey : null,
      )),
      activeColor: Colors.green,
    ));
  }
}
