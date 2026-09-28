final RegExp _invitationTokenPattern = RegExp(r'^[0-9a-f]{32}$');

/// Hôtes autorisés à porter un lien d'invitation.
/// `app.discipolat.com` est le domaine de production (App Links + Universal Links).
/// `localhost` autorise la recette locale.
/// La comparaison ignore la casse (les hôtes DNS le sont par définition).
const Set<String> kInvitationAllowedHosts = {
  'app.discipolat.com',
  'localhost',
};

String? normalizeInvitationToken(String? value) {
  if (value == null) return null;
  final normalized = value.trim().toLowerCase();
  return _invitationTokenPattern.hasMatch(normalized) ? normalized : null;
}

/// SECURITY — la validation porte sur le trio (scheme, hôte, path).
///
/// Avant, seul le `path` était vérifié : n'importe quel site pouvait donc
/// fabriquer `https://evil.example.com/accept-invitation?token=…` et l'app
/// l'acceptait. Un hôte non autorisé est désormais rejeté (`null`).
///
/// L'hôte est exigé pour les URI hiérarchiques (`https://…`, `discipolat://…`).
/// Une URI sans autorité est refusée.
String? invitationTokenFromUri(Uri uri) {
  if (uri.fragment.isNotEmpty) return null;
  if (uri.path != '/accept-invitation') return null;

  // URI relative (`/accept-invitation?token=…`) : c'est du routage interne
  // (go_router en deep-link interne), pas un lien externe. Rien à valider
  // côté autorité : le path a déjà été contrôlé ci-dessus.
  final hasAuthority = uri.hasAuthority;
  if (hasAuthority) {
    final host = uri.host.toLowerCase();
    // Hôte absent ou non listé -> lien forgé : refusé.
    if (host.isEmpty || !kInvitationAllowedHosts.contains(host)) return null;

    // Schemes acceptés uniquement (évite `discipolat://evil.example.com/...`
    // et tout scheme inattendu comme `file:` ou `javascript:`).
    final scheme = uri.scheme.toLowerCase();
    if (scheme != 'https' && scheme != 'discipolat') return null;
  }

  final tokens = uri.queryParametersAll['token'];
  if (tokens == null || tokens.length != 1) return null;
  return normalizeInvitationToken(tokens.single);
}
