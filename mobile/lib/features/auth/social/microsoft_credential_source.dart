import 'dart:convert';
import 'dart:math';

import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';
import 'package:http/http.dart' as http;
import 'package:oauth2/oauth2.dart' as oauth2;

import 'social_credential.dart';

/// Ouvre la fenêtre d'autorisation et renvoie l'URL de rappel complète.
///
/// Typé-function plutôt qu'un appel statique direct : c'est ce qui permet de
/// tester tout le flux (PKCE, échange du code, erreurs) **sans appareil
/// Android/iOS**, donc en CI.
typedef MicrosoftAuthBrowser = Future<String> Function({
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

/// Obtient un `id_token` Microsoft via le flux OAuth 2.0 « authorization code +
/// PKCE » dans le navigateur système.
///
/// Pourquoi ce flux et pas un SDK : Microsoft ne publie pas de SDK natif Flutter.
/// Le flux code + PKCE est la voie officielle, et il est **entièrement gratuit**
/// — aucun SDK payant, aucun quota, aucun quota de jetons.
///
/// Sécurité :
/// * **PKCE** : `oauth2` génère un `code_verifier` aléatoire et son défi S256 ;
///   un `code` intercepté est inutilisable sans le secret.
/// * **`state` aléatoire** : protection CSRF sur le flux implicite.
/// * **Aucun secret client** : application publique, donc pas de `client_secret`
///   et rien à protéger dans le binaire.
/// * **Le jeton n'est pas validé ici** : il est transmis au serveur, seul juge
///   (signature, audience, tenant). L'application ne fait pas autorité.
class MicrosoftCredentialSource implements SocialCredentialSource {
  MicrosoftCredentialSource({
    required this.clientId,
    this.tenantId = 'common',
    this.scopes = const ['openid', 'email', 'profile'],
    this.redirectUri = 'com.discipolat.app://auth/microsoft',
    MicrosoftAuthBrowser? browser,
    http.Client? httpClient,
    String Function()? stateGenerator,
  })  : _browser = browser ?? _defaultBrowser,
        _httpClient = httpClient,
        _stateGenerator = stateGenerator;

  final String clientId;
  final String tenantId;
  final List<String> scopes;

  /// URI de redirection : doit être enregistrée **exactement** dans le portail
  /// Entra (type « Application mobile et desktop »), sinon l'échange du code
  /// est rejeté.
  final String redirectUri;

  final MicrosoftAuthBrowser _browser;
  final http.Client? _httpClient;
  final String Function()? _stateGenerator;

  @override
  SocialProvider get provider => SocialProvider.microsoft;

  String get authority => 'https://login.microsoftonline.com/$tenantId';

  @override
  Future<SocialCredential> signIn() async {
    if (clientId.isEmpty) {
      throw const SocialAuthException(
        'SOCIAL_PROVIDER_NOT_CONFIGURED',
        'La connexion Microsoft n’est pas configurée sur ce serveur.',
      );
    }

    // `oauth2` ne conserve pas `id_token` dans ses Credentials : on le capture
    // au passage, via le hook `getParameters`, qui reçoit le corps brut de la
    // réponse du serveur de jetons. C'est le point d'extension prévu pour les
    // réponses non standard — et `id_token` en fait partie (extension OIDC).
    String? capturedIdToken;

    try {
      final oauth2.AuthorizationCodeGrant grant = oauth2.AuthorizationCodeGrant(
        clientId,
        Uri.parse('$authority/oauth2/v2.0/authorize'),
        Uri.parse('$authority/oauth2/v2.0/token'),
        httpClient: _httpClient,
        // Pas de `secret` : application publique + PKCE.
        basicAuth: false,
        getParameters: (contentType, body) {
          final Object? decoded = jsonDecode(body);
          if (decoded is Map<String, dynamic>) {
            capturedIdToken = decoded['id_token'] as String?;
          }
          return jsonDecode(body) as Map<String, dynamic>;
        },
      );

      final Uri callback = Uri.parse(redirectUri);
      final Uri authUrl = grant.getAuthorizationUrl(
        callback,
        scopes: scopes,
        state: _stateGenerator?.call() ?? _randomState(),
      );

      final String callbackUrl = await _browser(
        url: authUrl.toString(),
        callbackUrlScheme: callback.scheme,
      );

      final Uri parsed = Uri.parse(callbackUrl);
      final Map<String, String> parameters = <String, String>{
        ...parsed.queryParameters,
      };
      // `flutter_web_auth_2` renvoie parfois les paramètres dans le fragment
      // (selon la plateforme) : on les regroupe dans tous les cas.
      if (parsed.fragment.isNotEmpty) {
        parameters.addAll(Uri.splitQueryString(parsed.fragment));
      }

      await grant.handleAuthorizationResponse(parameters);

      if (capturedIdToken == null || capturedIdToken!.isEmpty) {
        // Repli : un jeton d'accès sans `id_token` ne permet PAS de prouver
        // l'identité — on refuse plutôt que de transmettre un jeton opaque.
        throw const SocialAuthException(
          'SOCIAL_CREDENTIAL_INVALID',
          'Microsoft n’a pas renvoyé d’identité vérifiable.',
        );
      }
      return SocialCredential(provider: provider, idToken: capturedIdToken!);
    } on SocialAuthException {
      rethrow;
    } on oauth2.AuthorizationException catch (exception) {
      throw SocialAuthException(
        _isCancellation(exception) ? 'SOCIAL_LOGIN_CANCELLED' : 'SOCIAL_CREDENTIAL_REJECTED',
        'Connexion Microsoft impossible.',
      );
    } catch (error) {
      if (_isCancellation(error)) {
        throw const SocialAuthException(
          'SOCIAL_LOGIN_CANCELLED',
          'Connexion Microsoft annulée.',
        );
      }
      throw const SocialAuthException(
        'SOCIAL_CREDENTIAL_REJECTED',
        'Connexion Microsoft impossible. Veuillez réessayer.',
      );
    }
  }

  bool _isCancellation(Object error) {
    final String text = error.toString().toLowerCase();
    return text.contains('cancel') ||
        text.contains('access_denied') ||
        text.contains('user_cancelled') ||
        text.contains('user_cancelled_login');
  }

  /// `state` aléatoire : 32 octets issus de `Random.secure()`, encodés base64url.
  ///
  /// <b>Pourquoi `Random.secure()` et pas `Random()` :</b> `state` est la
  /// protection CSRF du flux. Avec un générateur prévisible, un attaquant
  /// pourrait le deviner et faire accepter une réponse forgée.
  String _randomState() => _base64Url(_secureBytes(32));

  List<int> _secureBytes(int length) {
    final Random random = Random.secure();
    return List<int>.generate(length, (_) => random.nextInt(256));
  }

  String _base64Url(List<int> bytes) {
    const String alphabet =
        'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
    final StringBuffer buffer = StringBuffer();
    for (int i = 0; i < bytes.length; i += 3) {
      final int b0 = bytes[i];
      final int b1 = i + 1 < bytes.length ? bytes[i + 1] : 0;
      final int b2 = i + 2 < bytes.length ? bytes[i + 2] : 0;
      buffer.write(alphabet[b0 >> 2]);
      buffer.write(alphabet[((b0 & 0x03) << 4) | (b1 >> 4)]);
      buffer.write(i + 1 < bytes.length ? alphabet[((b1 & 0x0f) << 2) | (b2 >> 6)] : '');
      buffer.write(i + 2 < bytes.length ? alphabet[b2 & 0x3f] : '');
    }
    return buffer.toString();
  }
}
