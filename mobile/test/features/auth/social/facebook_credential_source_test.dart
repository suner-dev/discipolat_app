import 'package:discipolat_mobile/features/auth/social/facebook_credential_source.dart';
import 'package:discipolat_mobile/features/auth/social/social_credential.dart';
import 'package:flutter_test/flutter_test.dart';

/// Tests du flux Facebook (OIDC par redirection) sans appareil Android/iOS :
/// le navigateur est injecté, donc le comportement est vérifiable en CI.
void main() {
  const appId = '1234567890123456';
  // Dart n'autorise pas la repetition de chaines en const : List.filled + join.
  final String state = List<String>.filled(64, 'a').join();

  FacebookCredentialSource sourceWith(
    Future<String> Function({required String url, required String callbackUrlScheme}) browser, {
    String Function()? stateGenerator,
  }) {
    return FacebookCredentialSource(
      appId: appId,
      browser: browser,
      stateGenerator: stateGenerator ?? () => state,
      redirectUri: 'com.discipolat.app://auth/facebook',
    );
  }

  group('FacebookCredentialSource', () {
    test('construit une URL OIDC avec response_type=id_token', () async {
      String? requested;
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async {
          requested = url;
          return 'com.discipolat.app://auth/facebook#id_token=jwt&state=$state';
        },
      );

      await source.signIn();

      expect(requested, isNotNull);
      expect(requested, contains('client_id=$appId'));
      expect(requested, contains('response_type=id_token'));
      expect(requested, contains('email%2Cpublic_profile'));
      expect(requested, contains('/v21.0/dialog/oauth'));
    });

    test('retourne le credential quand le state correspond', () async {
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async =>
            'com.discipolat.app://auth/facebook#id_token=jwt.facebook&state=$state',
      );

      final SocialCredential credential = await source.signIn();

      expect(credential.provider, SocialProvider.facebook);
      expect(credential.idToken, 'jwt.facebook');
      expect(credential.toPayload(), {
        'provider': 'facebook',
        'credential': 'jwt.facebook',
      });
    });

    test('lit les paramètres en query comme en fragment', () async {
      // Selon la plateforme, le retour peut arriver en query.
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async =>
            'com.discipolat.app://auth/facebook?id_token=jwt.query&state=$state',
      );

      expect((await source.signIn()).idToken, 'jwt.query');
    });

    test('REFUSE un state qui ne correspond pas (protection CSRF)', () async {
      // Test de sécurité central : sans ce contrôle, un attaquant pourrait
      // faire connecter une victime avec SON propre compte Facebook.
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async =>
            'com.discipolat.app://auth/facebook#id_token=jeton.attaquant&state=${List<String>.filled(64, 'b').join()}',
      );

      await expectLater(
        source.signIn(),
        throwsA(isA<SocialAuthException>()
            .having((SocialAuthException e) => e.code, 'code', 'SOCIAL_STATE_MISMATCH')),
      );
    });

    test('traduit une annulation en code silencieux', () async {
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async =>
            'com.discipolat.app://auth/facebook#error=access_denied&state=$state',
      );

      await expectLater(
        source.signIn(),
        throwsA(isA<SocialAuthException>()
            .having((SocialAuthException e) => e.code, 'code', 'SOCIAL_LOGIN_CANCELLED')
            .having((SocialAuthException e) => e.isCancelled, 'isCancelled', isTrue)),
      );
    });

    test('traduit un refus Facebook en erreur visible', () async {
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async =>
            'com.discipolat.app://auth/facebook#error=server_error&state=$state',
      );

      await expectLater(
        source.signIn(),
        throwsA(isA<SocialAuthException>()
            .having((SocialAuthException e) => e.code, 'code', 'SOCIAL_CREDENTIAL_REJECTED')),
      );
    });

    test('refuse un retour sans id_token', () async {
      final source = sourceWith(
        ({required String url, required String callbackUrlScheme}) async =>
            'com.discipolat.app://auth/facebook#state=$state',
      );

      await expectLater(
        source.signIn(),
        throwsA(isA<SocialAuthException>().having(
            (SocialAuthException e) => e.code, 'code', 'SOCIAL_CREDENTIAL_INVALID')),
      );
    });

    test('échoue explicitement sans App ID (fail-closed)', () async {
      final source = FacebookCredentialSource(
        appId: '',
        browser: ({required String url, required String callbackUrlScheme}) async => '',
      );

      await expectLater(
        source.signIn(),
        throwsA(isA<SocialAuthException>().having((SocialAuthException e) => e.code,
            'code', 'SOCIAL_PROVIDER_NOT_CONFIGURED')),
      );
    });

    test('le state généré est aléatoire et diffère à chaque connexion', () async {
      final List<String> requested = <String>[];
      final source = FacebookCredentialSource(
        appId: appId,
        redirectUri: 'com.discipolat.app://auth/facebook',
        browser: ({required String url, required String callbackUrlScheme}) async {
          requested.add(url);
          // State echoed back: on le récupère depuis l'URL émise.
          final match = RegExp(r'state=([0-9a-f]+)').firstMatch(url);
          return 'com.discipolat.app://auth/facebook#id_token=jwt&state=${match?.group(1)}';
        },
      );

      await source.signIn();
      await source.signIn();

      final states = requested
          .map((url) => RegExp(r'state=([0-9a-f]+)').firstMatch(url)?.group(1))
          .toList();

      expect(states[0], hasLength(64));
      expect(states[1], hasLength(64));
      expect(states[0], isNot(states[1]));
    });
  });
}
