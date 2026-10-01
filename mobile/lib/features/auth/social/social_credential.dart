import 'package:flutter/foundation.dart';

/// Fournisseur d'identité externe supporté par l'application mobile.
///
/// Volontairement restreint à deux fournisseurs, tous deux **gratuits et sans
/// plafond** côté serveur : les `id_token` sont validés localement contre les
/// clés publiques du fournisseur (JWKS), donc aucun appel à un service de
/// vérification payant et aucun quota.
///
/// Apple (99 $/an) et le SMS (facturé par message) sont hors périmètre pour
/// l'instant ; l'énumération s'étend sans changement d'architecture.
enum SocialProvider {
  google('google', 'Google'),
  microsoft('microsoft', 'Microsoft');

  const SocialProvider(this.wireName, this.label);

  /// Nom attendu par l'API (`/auth/social/{provider}`).
  final String wireName;

  /// Libellé affiché, non traduisible (nom de marque).
  final String label;
}

/// Credential OIDC obtenu d'un fournisseur (un `id_token`).
///
/// L'application ne le décode **jamais** : elle le transmet au serveur, seul
/// habilité à décider. Le client n'est donc pas un facteur de confiance.
@immutable
class SocialCredential {
  const SocialCredential({
    required this.provider,
    required this.idToken,
  });

  final SocialProvider provider;
  final String idToken;

  Map<String, dynamic> toPayload() => {
        'provider': provider.wireName,
        'credential': idToken,
      };

  @override
  String toString() => 'SocialCredential(${provider.wireName})';
}

/// Erreur de connexion sociale, avec un code serveur exploitable par l'interface.
///
/// L'`id_token` n'apparaît **jamais** dans le message : il est dans les logs
/// applicatifs, donc potentiellement lisibles.
@immutable
class SocialAuthException implements Exception {
  const SocialAuthException(this.code, this.message);

  /// Code métier renvoyé par l'API (champ `title` du ProblemDetail).
  final String code;

  /// Message déjà traduisible par l'interface.
  final String message;

  /// L'utilisateur doit passer par une invitation : l'interface doit le dire.
  bool get requiresInvitation => code == 'SOCIAL_ACCOUNT_NOT_LINKED';

  /// Le serveur ne sert pas ce fournisseur : le bouton doit disparaître.
  bool get providerUnavailable => code == 'SOCIAL_PROVIDER_NOT_CONFIGURED';

  /// Annulation volontaire : aucun message d'erreur ne doit être affiché.
  bool get isCancelled => code == 'SOCIAL_LOGIN_CANCELLED';

  @override
  String toString() => 'SocialAuthException($code)';
}

/// Contrat d'obtention d'un credential, implémenté par chaque fournisseur.
///
/// Séparer le contrat du plugin permet de tester la logique sans le SDK natif
/// (donc sans Android/iOS) : les tests injectent un faux signataire.
abstract class SocialCredentialSource {
  SocialProvider get provider;

  /// Retourne un `id_token`, ou lève une [SocialAuthException].
  Future<SocialCredential> signIn();
}
