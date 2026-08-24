import 'package:flutter/material.dart';

/// P1 #37 — Plan de développement individuel
class DevelopmentPlanScreen extends StatelessWidget {
  const DevelopmentPlanScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📈 Mon Plan de Développement'),
        backgroundColor: Colors.deepOrange.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Progress overview
          Card(
            color: Colors.deepOrange.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                  const Text('Progression globale', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  const LinearProgressIndicator(value: 0.45),
                  const SizedBox(height: 4),
                  const Text('45% — 3 objectifs actifs sur 6'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Active plans
          const Text('Objectifs actifs', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _planItem('Améliorer la présence aux cultes', 'Haute', 0.6, Colors.red),
          _planItem('Développer les compétences de leadership', 'Moyenne', 0.3, Colors.orange),
          _planItem('Servir dans un département', 'Haute', 0.2, Colors.blue),
          const SizedBox(height: 16),
          const Text('Objectifs terminés', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _planItem('Completer la formation d\'accueil', 'Faible', 1.0, Colors.green),
          _planItem('Participer à 3 reprises au culte', 'Moyenne', 1.0, Colors.green),
          const SizedBox(height: 16),
          // Auto-generate
          Card(
            child: ListTile(
              leading: const Icon(Icons.auto_awesome, color: Colors.deepOrange),
              title: const Text('Générer des objectifs automatiquement'),
              subtitle: const Text('L\'IA analyse vos besoins et propose des objectifs'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Objectifs générés par l\'IA !')),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _planItem(String title, String priority, double progress, Color color) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold))),
                Chip(label: Text(priority, style: const TextStyle(fontSize: 11)), backgroundColor: color.withOpacity(0.1)),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(value: progress, backgroundColor: Colors.grey.shade200, color: color),
            const SizedBox(height: 4),
            Text('${(progress * 100).toInt()}%', style: TextStyle(color: Colors.grey.shade600)),
          ],
        ),
      ),
    );
  }
}
