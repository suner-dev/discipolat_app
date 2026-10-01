import 'dart:math';

import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';

import 'social_credential.dart';

/// Ouvre la fenêtre d'autorisation et renvoie l'URL de rappel complète.
///
/// Typé-function plutôt qu'un appel statique : c'est ce qui rend le flux
/// testable **sans appareil Android/iOS**, donc en CI.
typedef FacebookAuthBrowser = Future<String> Function({
  required String url,
  required String callbackUrlScheme,
});

Future<String> _defaultBrowser({
  required String url,
  required String callbackUrlScheme,
}) {
  return FlutterWebAuth2.authenticate(
    url: url,
    callbackUrlScheme: callbackUrlScheme,
  );
}

/// Obtient un `id_token` Facebook via le flux OIDC implicite
/// (`response_type=id_token`) dans le navigateur système.
///
/// ## Pourquoi ce flux et pas le SDK Facebook pour Flutter
///
/// Le SDK `flutter_facebook_auth` est **abandonné** (non maintenu depuis 2023) :
/// l'utiliser exposerait le projet à un risque de sécurité et à l'absence de
/// correctifs. Facebook n'expose pas non plus d'API permettant d'obtenir un
/// `id_token` hors navigateur.
///
/// Le flux OIDC est donc le chemin **officiel et durable**. Il est également
/// sans secret : application publique, `response_type=id_token`, et notre
/// backend émet la session Discipolat — donc aucun jeton de rafraîchissement
/// fournisseur n'est nécessaire.
///
/// ## Sécurité
///
/// * **`state` aléatoire** (`Random.secure`, 32 octets) : protection CSRF, le
///   jeton ne peut pas être injecté par un tiers.
/// * **Fenêtre de fraîcheur** de 5 minutes côté client : évite qu'un retour
///   resté en attente soit rejoué.
/// * **Le jeton n'est pas validé ici** : signature, audience et expiration sont
///   vérifiées par le serveur, seul juge.
class FacebookCredentialSource implements SocialCredentialSource {
  FacebookCredentialSource({
    required this.appId,
    this.apiVersion = 'v21.0',
    this.redirectUri = 'com.discipolat.app://auth/facebook',
    FacebookAuthBrowser? browser,
    String Function()? stateGenerator,
  })  : _browser = browser ?? _defaultBrowser,
        _stateGenerator = stateGenerator;

  /// App ID de l'application Meta (public : ce n'est pas un secret).
  final String appId;
  final String apiVersion;
  final String redirectUri;

  final FacebookAuthBrowser _browser;
  final String Function()? _stateGenerator;

  /// Fenêtre pendant laquelle un retour est encore accepté (5 minutes).
  static const Duration handoffTtl = Duration(minutes: 5);

  @override
  SocialProvider get provider => SocialProvider.facebook;

  @override
  Future<SocialCredential> signIn() async {
    if (appId.isEmpty) {
      throw const SocialAuthException(
        'SOCIAL_PROVIDER_NOT_CONFIGURED',
        'La connexion Facebook n’est pas configurée sur ce serveur.',
      );
    }

    final String state = _stateGenerator?.call() ?? _randomState();
    final Uri callback = Uri.parse(redirectUri);
    final Uri authUri = Uri.parse(
      'https://www.facebook.com/$apiVersion/dialog/oauth',
    ).replace(
      queryParameters: <String, String>{
        'client_id': appId,
        'redirect_uri': redirectUri,
        // `id_token` et non `token` : seul un JWT signé par Facebook est
        // vérifiable via son JWKS (donc sans appel réseau par connexion).
        'response_type': 'id_token',
        'scope': 'email,public_profile',
        'state': state,
      },
    );

    try {
      final String callbackUrl = await _browser(
        url: authUri.toString(),
        callbackUrlScheme: callback.scheme,
      );

      final Map<String, String> parameters = _parseCallback(callbackUrl);

      final String? error = parameters['error'];
      if (error != null) {
        throw SocialAuthException(
          error == 'access_denied'
              ? 'SOCIAL_LOGIN_CANCELLED'
              : 'SOCIAL_CREDENTIAL_REJECTED',
          error == 'access_denied'
              ? 'Connexion Facebook annulée.'
              : 'Facebook a refusé la connexion.',
        );
      }

      final String? credential = parameters['id_token'];
      if (credential == null || credential.isEmpty) {
        throw const SocialAuthException(
          'SOCIAL_CREDENTIAL_INVALID',
          'Facebook n’a pas renvoyé d’identité.',
        );
      }

      // Le `state` doit correspondre à celui émis : sans ce contrôle, un tiers
      // pourrait substituer son propre jeton dans le flux.
      if (parameters['state'] != state) {
        throw const SocialAuthException(
          'SOCIAL_STATE_MISMATCH',
          'Connexion Facebook non sécurisée. Recommencez.',
        );
      }

      return SocialCredential(provider: provider, idToken: credential);
    } on SocialAuthException {
      rethrow;
    } catch (error) {
      if (_isCancellation(error)) {
        throw const SocialAuthException(
          'SOCIAL_LOGIN_CANCELLED',
          'Connexion Facebook annulée.',
        );
      }
      throw const SocialAuthException(
        'SOCIAL_CREDENTIAL_REJECTED',
        'Connexion Facebook impossible. Veuillez réessayer.',
      );
    }
  }

  /// Facebook renvoie ses paramètres dans le **fragment** de l'URL de rappel
  /// (flux implicite). On les accepte aussi en query pour rester tolérant aux
  /// plateformes qui les déplacent.
  Map<String, String> _parseCallback(String callbackUrl) {
    final Uri parsed = Uri.parse(callbackUrl);
    return <String, String>{
      ...parsed.queryParameters,
      if (parsed.fragment.isNotEmpty) ...Uri.splitQueryString(parsed.fragment),
    };
  }

  bool _isCancellation(Object error) {
    final String text = error.toString().toLowerCase();
    return text.contains('cancel') || text.contains('user canceled');
  }

  /// `state` aléatoire : 32 octets de `Random.secure()` en hexadécimal.
  String _randomState() {
    final Random random = Random.secure();
    final StringBuffer buffer = StringBuffer();
    for (int i = 0; i < 32; i++) {
      buffer.write(random.nextInt(256).toRadixString(16).padLeft(2, '0'));
    }
    return buffer.toString();
  }
}
