import 'package:flutter/material.dart';

/// P1 #22 — Traduction en direct des sermons: Whisper → LLM → sous-titres
class SermonTranslationScreen extends StatelessWidget {
  const SermonTranslationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🌍 Traduction des sermons'),
        backgroundColor: Colors.indigo.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Active translation card
          Card(
            color: Colors.indigo.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.circle, color: Colors.red, size: 12),
                      const SizedBox(width: 8),
                      const Text('Traduction en cours', style: TextStyle(fontWeight: FontWeight.bold)),
                      const Spacer(),
                      const CircularProgressIndicator(strokeWidth: 2),
                    ],
                  ),
                  const SizedBox(height: 12),
                  const Text('Français → Anglais, Espagnol, Swahili', style: TextStyle(color: Colors.grey)),
                  const SizedBox(height: 8),
                  const LinearProgressIndicator(value: 0.65),
                  const SizedBox(height: 4),
                  const Text('65% — Transcription terminée, traduction en cours...'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Available languages
          const Text('Langues disponibles', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _langChip('🇬🇧 Anglais', true),
              _langChip('🇪🇸 Espagnol', true),
              _langChip('🇵🇹 Portugais', false),
              _langChip('🇰🇪 Swahili', true),
              _langChip('🇸🇦 Arabe', false),
              _langChip('🇨🇳 Chinois', false),
            ],
          ),
          const SizedBox(height: 24),
          // Previous translations
          const Text('Traductions récentes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _translationCard('Culte du 24 août 2025', '5 langues', 'Terminée'),
          _translationCard('Culte du 17 août 2025', '3 langues', 'Terminée'),
          _translationCard('Culte du 10 août 2025', '4 langues', 'Terminée'),
        ],
      ),
    );
  }

  Widget _langChip(String label, bool active) {
    return Chip(
      avatar: Icon(active ? Icons.check_circle : Icons.add_circle_outline, size: 18),
      label: Text(label),
      backgroundColor: active ? Colors.green.shade50 : Colors.grey.shade100,
    );
  }

  Widget _translationCard(String title, String langs, String status) {
    return Card(
      child: ListTile(
        leading: const Icon(Icons.translate),
        title: Text(title),
        subtitle: Text(langs),
        trailing: Chip(
          label: Text(status, style: const TextStyle(fontSize: 12)),
          backgroundColor: Colors.green.shade50,
        ),
      ),
    );
  }
}
