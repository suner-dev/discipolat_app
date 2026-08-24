import 'package:flutter/material.dart';

/// P1 #54 — Mentorat inversé: demander de l'aide à un faiseur expérimenté
class ReverseMentoringScreen extends StatelessWidget {
  const ReverseMentoringScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🔄 Mentorat Inversé'),
        backgroundColor: Colors.orange.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {},
        backgroundColor: Colors.orange.shade700,
        icon: const Icon(Icons.help, color: Colors.white),
        label: const Text('Demander de l\'aide', style: TextStyle(color: Colors.white)),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // How it works
          Card(
            color: Colors.orange.shade50,
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                'Vous pouvez demander de l\'aide à un faiseur plus expérimenté ou au pasteur pour des cas difficiles.',
                style: TextStyle(fontSize: 14),
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Active requests
          const Text('Demandes actives', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _requestCard('Cas difficile: Famille en crise', 'Assigné à: Pasteur Samuel', 'En cours', Colors.orange),
          _requestCard('Orientation: Formation leadership', 'Assigné à: Jean-Pierre M.', 'Planifié', Colors.blue),
          const SizedBox(height: 16),
          // Available mentors
          const Text('Mentors disponibles', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _mentorCard('Pasteur Samuel', 'Accompagnement pastoral', 5.0),
          _mentorCard('Jean-Pierre M.', 'Leadership famille', 4.5),
          _mentorCard('Marie K.', 'Accueil & intégration', 4.8),
        ],
      ),
    );
  }

  Widget _requestCard(String title, String assignee, String status, Color color) {
    return Card(child: ListTile(
      leading: CircleAvatar(backgroundColor: color.withOpacity(0.1), child: Icon(Icons.help, color: color, size: 18)),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
      subtitle: Text(assignee),
      trailing: Chip(label: Text(status, style: TextStyle(fontSize: 11, color: color)), backgroundColor: color.withOpacity(0.1)),
    ));
  }

  Widget _mentorCard(String name, String expertise, double rating) {
    return Card(child: ListTile(
      leading: CircleAvatar(child: Text(name[0])),
      title: Text(name),
      subtitle: Text(expertise),
      trailing: Row(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.star, color: Colors.amber, size: 16),
        Text(rating.toString(), style: const TextStyle(fontSize: 13)),
      ]),
    ));
  }
}
