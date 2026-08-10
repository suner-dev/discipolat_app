import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';

/// Ouvre le dialogue de création d'un document (module Fichiers, POST /files).
/// Retourne l'id du fichier créé, ou null si annulé ou en cas d'erreur.
Future<String?> showDocumentCreateDialog(BuildContext context, ApiService apiService) async {
  final nomCtrl = TextEditingController();
  final cheminCtrl = TextEditingController();
  final mimeCtrl = TextEditingController(text: 'application/pdf');
  final tailleCtrl = TextEditingController(text: '0');
  String categorie = 'AUTRE';
  bool saving = false;

  const categorieLabels = {
    'COMPTE_RENDU': 'Compte rendu',
    'FORMATION': 'Formation',
    'PHOTO': 'Photo',
    'RESOURCES': 'Ressources',
    'AUTRE': 'Autre',
  };

  return showDialog<String>(
    context: context,
    builder: (ctx) => StatefulBuilder(
      builder: (ctx, setDialogState) => AlertDialog(
        backgroundColor: const Color(0xFF111827),
        title: const Text('Nouveau document', style: TextStyle(color: Colors.white)),
        content: SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            TextField(
              controller: nomCtrl,
              decoration: const InputDecoration(labelText: 'Nom du document *', hintText: 'Compte rendu réunion'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: cheminCtrl,
              decoration: const InputDecoration(
                labelText: 'URL / chemin du fichier *',
                hintText: 'https://drive.google.com/...',
                prefixIcon: Icon(Icons.link, size: 16),
              ),
            ),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  value: categorie,
                  dropdownColor: const Color(0xFF111827),
                  decoration: const InputDecoration(labelText: 'Catégorie'),
                  items: categorieLabels.entries
                      .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                      .toList(),
                  onChanged: (v) => setDialogState(() => categorie = v ?? 'AUTRE'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: mimeCtrl,
                  decoration: const InputDecoration(labelText: 'Type MIME', hintText: 'application/pdf'),
                ),
              ),
            ]),
            const SizedBox(height: 10),
            TextField(
              controller: tailleCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Taille (octets)', hintText: '0'),
            ),
            const SizedBox(height: 12),
            Text(
              'Le document sera disponible dans le module Documents.',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
            ),
          ]),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, null),
            child: const Text('Annuler'),
          ),
          FilledButton(
            onPressed: saving
                ? null
                : () async {
                    final nom = nomCtrl.text.trim();
                    final chemin = cheminCtrl.text.trim();
                    if (nom.isEmpty || chemin.isEmpty) {
                      ScaffoldMessenger.of(ctx).showSnackBar(
                        const SnackBar(content: Text('Le nom et l\u2019URL du document sont requis')),
                      );
                      return;
                    }
                    setDialogState(() => saving = true);
                    try {
                      final res = await apiService.post('/files', data: {
                        'nom': nom,
                        'typeFichier': mimeCtrl.text.trim().isEmpty ? 'application/pdf' : mimeCtrl.text.trim(),
                        'taille': int.tryParse(tailleCtrl.text) ?? 0,
                        'chemin': chemin,
                        'categorie': categorie,
                      });
                      if (ctx.mounted) Navigator.pop(ctx, res.data['id']?.toString());
                    } catch (_) {
                      if (ctx.mounted) {
                        setDialogState(() => saving = false);
                        ScaffoldMessenger.of(ctx).showSnackBar(
                          const SnackBar(content: Text('Erreur lors de la création du document')),
                        );
                      }
                    }
                  },
            child: saving
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Créer'),
          ),
        ],
      ),
    ),
  );
}
