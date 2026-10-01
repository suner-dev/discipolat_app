import 'package:google_sign_in/google_sign_in.dart';

import 'social_credential.dart';

/// Obtient un `id_token` Google via le SDK officiel `google_sign_in`.
///
/// Points de vigilance, tous vérifiés :
///
/// * **Le `serverClientId` est le client WEB de la plateforme.** Sur Android,
///   c'est lui qui permet d'obtenir un `id_token` exploitable côté serveur ;
///   le client Android seul ne produit qu'un credential sans jeton.
/// * **L'audience diffère par plateforme.** Le backend accepte trois client-id
///   (web, Android, iOS) : c'est pourquoi il les faut tous les trois.
/// * **Le SDK n'est appelé qu'au clic.** Aucun coût au démarrage.
class GoogleCredentialSource implements SocialCredentialSource {
  GoogleCredentialSource({
    GoogleSignIn? signIn,
    this.serverClientId = '',
  }) : _signIn = signIn;

  final GoogleSignIn? _signIn;

  /// Client-id Web (celui de `VITE_GOOGLE_CLIENT_ID` côté web).
  final String serverClientId;

  GoogleSignIn get _instance => _signIn ??
      GoogleSignIn.instance;

  @override
  SocialProvider get provider => SocialProvider.google;

  @override
  Future<SocialCredential> signIn() async {
    if (serverClientId.isEmpty) {
      throw const SocialAuthException(
        'SOCIAL_PROVIDER_NOT_CONFIGURED',
        'La connexion Google n’est pas configurée sur ce serveur.',
      );
    }

    try {
      // `serverClientId` est fourni ici plutôt qu'à l'initialisation pour que
      // l'appel reste explicite et testable.
      await _instance.initialize(
        serverClientId: serverClientId,
        clientId: null,
      );
      final GoogleSignInAccount account = await _instance.authenticate();
      final String? idToken = account.authentication.idToken;

      if (idToken == null || idToken.isEmpty) {
        throw const SocialAuthException(
          'SOCIAL_CREDENTIAL_INVALID',
          'Google n’a pas renvoyé d’identité.',
        );
      }
      return SocialCredential(provider: provider, idToken: idToken);
    } on SocialAuthException {
      rethrow;
    } catch (error) {
      // L'utilisateur a fermé la fenêtre native : ce n'est pas une panne, et
      // afficher « erreur » serait trompeur.
      if (_isCancellation(error)) {
        throw const SocialAuthException(
          'SOCIAL_LOGIN_CANCELLED',
          'Connexion Google annulée.',
        );
      }
      throw SocialAuthException(
        'SOCIAL_CREDENTIAL_REJECTED',
        'Connexion Google impossible. Veuillez réessayer.',
      );
    }
  }

  bool _isCancellation(Object error) {
    final String text = error.toString().toLowerCase();
    return text.contains('canceled') ||
        text.contains('cancelled') ||
        text.contains('aborted');
  }
}
