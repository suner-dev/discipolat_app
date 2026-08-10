import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

/// Ouvre le lien d'un document du module Fichiers dans le navigateur externe
/// (url_launcher, mode externalApplication). En cas d'échec (URL invalide,
/// aucune app capable de l'ouvrir, plugin indisponible), affiche une SnackBar
/// avec l'URL pour ne pas laisser l'utilisateur sans retour.
Future<void> showUrlLink(BuildContext context, String url) async {
  if (url.isEmpty) return;
  final uri = Uri.tryParse(url);
  if (uri == null || !uri.hasScheme) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lien invalide')),
      );
    }
    return;
  }
  var opened = false;
  try {
    opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
  } catch (_) {
    opened = false;
  }
  if (!opened && context.mounted) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Impossible d\'ouvrir le lien: $url')),
    );
  }
}
