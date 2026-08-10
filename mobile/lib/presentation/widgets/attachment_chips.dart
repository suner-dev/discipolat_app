import 'package:flutter/material.dart';
import 'open_url.dart';

/// Chips des pièces jointes d'une entité (listes/détails) : nom + trombone,
/// tap = lien du document (module Fichiers). Équivalent mobile du composant
/// web `AttachmentLinks`. Réutilisé par les écrans demandes membres, événements…
class AttachmentChips extends StatelessWidget {
  const AttachmentChips({super.key, required this.pieces, this.sourceKey});

  /// Liste des pièces jointes (items `{ id, fileId, nom, url }`).
  final List<dynamic> pieces;

  /// Clé optionnelle portant le contexte d'origine (ex: « source » sur le dossier 360).
  final String? sourceKey;

  @override
  Widget build(BuildContext context) {
    if (pieces.isEmpty) return const SizedBox.shrink();
    return Wrap(
      spacing: 6,
      runSpacing: 6,
      children: pieces.map((p) {
        final m = p as Map;
        final source = sourceKey != null ? m[sourceKey]?.toString() : null;
        return InkWell(
          onTap: () => showUrlLink(context, m['url']?.toString() ?? ''),
          borderRadius: BorderRadius.circular(8),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              const Icon(Icons.attach_file, size: 12, color: Colors.blueAccent),
              const SizedBox(width: 4),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    m['nom']?.toString() ?? 'Document',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11),
                  ),
                  if (source != null && source.isNotEmpty)
                    Text(
                      source,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 9),
                    ),
                ],
              ),
            ]),
          ),
        );
      }).toList(),
    );
  }
}
