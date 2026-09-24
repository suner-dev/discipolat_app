final RegExp _invitationTokenPattern = RegExp(r'^[0-9a-f]{32}$');

String? normalizeInvitationToken(String? value) {
  if (value == null) return null;
  final normalized = value.trim().toLowerCase();
  return _invitationTokenPattern.hasMatch(normalized) ? normalized : null;
}

String? invitationTokenFromUri(Uri uri) {
  if (uri.path != '/accept-invitation' || uri.fragment.isNotEmpty) {
    return null;
  }
  final tokens = uri.queryParametersAll['token'];
  if (tokens == null || tokens.length != 1) return null;
  return normalizeInvitationToken(tokens.single);
}
