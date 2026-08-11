import 'dart:io' show Platform;

import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';

/// Catégories de retour acceptées par le backend (`CreateFeedbackRequest`).
const kFeedbackCategories = <String, String>{
  'BUG': 'Bug / problème technique',
  'UX': "Problème d'utilisation (UX)",
  'SUGGESTION': "Suggestion d'amélioration",
  'FONCTIONNALITE_MANQUANTE': 'Fonctionnalité manquante',
  'PERFORMANCE': 'Problème de performance',
  'TRADUCTION': 'Problème de traduction',
  'AFFICHAGE': "Problème d'affichage",
  'AUTRE': 'Autre',
};

const kFeedbackPriorities = <String, String>{
  'BASSE': 'Basse',
  'MOYENNE': 'Moyenne',
  'HAUTE': 'Haute',
  'CRITIQUE': 'Critique',
};

/// Ouvre la feuille de retour testeur (bouton « Un retour ? »).
///
/// Soumet `POST /api/v1/feedback` avec la catégorie, la priorité, le sujet,
/// la description et le contexte technique (système, appareil) — aucune donnée
/// personnelle au-delà du compte connecté.
///
/// [apiService] est injectable pour les tests ; [pageUrl] permet de remonter
/// l'écran courant (résolu par l'appelant via la route active).
Future<void> showFeedbackSheet(
  BuildContext context, {
  ApiService? apiService,
  String? pageUrl,
}) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (_) => _FeedbackSheet(
      apiService: apiService ?? ApiService(),
      pageUrl: pageUrl,
    ),
  );
}

class _FeedbackSheet extends StatefulWidget {
  const _FeedbackSheet({required this.apiService, this.pageUrl});

  final ApiService apiService;
  final String? pageUrl;

  @override
  State<_FeedbackSheet> createState() => _FeedbackSheetState();
}

class _FeedbackSheetState extends State<_FeedbackSheet> {
  final _subjectCtrl = TextEditingController();
  final _descriptionCtrl = TextEditingController();
  String _category = 'BUG';
  String _priority = 'MOYENNE';
  bool _sending = false;

  /// Contexte technique du testeur (aligné sur le widget web).
  late final String _os = Platform.operatingSystem;
  late final String _device = (Platform.isAndroid || Platform.isIOS) ? 'Mobile' : _os;

  @override
  void dispose() {
    _subjectCtrl.dispose();
    _descriptionCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final subject = _subjectCtrl.text.trim();
    if (subject.length < 3) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Le sujet doit contenir au moins 3 caractères')),
      );
      return;
    }
    setState(() => _sending = true);
    try {
      await widget.apiService.post('/feedback', data: {
        'category': _category,
        'priority': _priority,
        'subject': subject,
        'description': _descriptionCtrl.text.trim().isEmpty ? null : _descriptionCtrl.text.trim(),
        'pageUrl': widget.pageUrl,
        'userAgent': '',
        'browser': 'App mobile',
        'os': _os,
        'device': _device,
      });
      if (!mounted) return;
      // Capture du messenger AVANT de fermer la feuille : le context de la
      // feuille est démonté pendant l'animation de pop.
      final messenger = ScaffoldMessenger.of(context);
      Navigator.pop(context);
      messenger.showSnackBar(
        const SnackBar(content: Text('Merci ! Votre retour a bien été transmis à l\u2019équipe.')),
      );
    } catch (_) {
      if (!mounted) return;
      setState(() => _sending = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Échec de l\u2019envoi du retour. Réessayez.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF111827),
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        border: Border(top: BorderSide(color: Colors.white12)),
      ),
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Row(
              children: [
                Icon(Icons.feedback_outlined, color: Color(0xFFF59E0B), size: 22),
                SizedBox(width: 10),
                Text(
                  'Envoyer un retour',
                  style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              "Aidez-nous à améliorer la plateforme : bug, suggestion, problème d'utilisation…",
              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _category,
                    isExpanded: true,
                    dropdownColor: const Color(0xFF1E2A4A),
                    decoration: const InputDecoration(labelText: 'Catégorie'),
                    items: kFeedbackCategories.entries
                        .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis)))
                        .toList(),
                    onChanged: (v) => setState(() => _category = v ?? 'BUG'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _priority,
                    isExpanded: true,
                    dropdownColor: const Color(0xFF1E2A4A),
                    decoration: const InputDecoration(labelText: 'Priorité'),
                    items: kFeedbackPriorities.entries
                        .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis)))
                        .toList(),
                    onChanged: (v) => setState(() => _priority = v ?? 'MOYENNE'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _subjectCtrl,
              maxLength: 255,
              decoration: const InputDecoration(
                labelText: 'Sujet *',
                hintText: 'Résumez le problème ou la suggestion',
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _descriptionCtrl,
              maxLines: 4,
              maxLength: 5000,
              decoration: const InputDecoration(
                labelText: 'Description',
                hintText: "Décrivez ce qui s'est passé, ce que vous attendiez…",
                alignLabelWithHint: true,
              ),
            ),
            Text(
              'Le système et l\u2019appareil ($_os / $_device) sont transmis automatiquement pour le diagnostic.',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: FilledButton(
                onPressed: _sending ? null : _submit,
                child: _sending
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white),
                      )
                    : const Text('Envoyer le retour', style: TextStyle(fontSize: 15)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
