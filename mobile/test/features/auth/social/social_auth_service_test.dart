import 'package:discipolat_mobile/features/auth/social/social_auth_service.dart';
import 'package:discipolat_mobile/features/auth/social/social_credential.dart';
import 'package:flutter_test/flutter_test.dart';

/// Faux signataire : permet de tester l'orchestration **sans SDK natif**
/// (donc sans Android/iOS, donc en CI).
class _FakeSource implements SocialCredentialSource {
  _FakeSource(this.provider, {this.idToken = 'fake-id-token', this.error});

  final SocialProvider provider;
  final String idToken;
  final Object? error;

  @override
  Future<SocialCredential> signIn() async {
    final Object? failure = error;
    if (failure != null) throw failure;
    return SocialCredential(provider: provider, idToken: idToken);
  }
}

void main() {
  group('SocialAuthService', () {
    test('n’expose que les fournisseurs réellement configurés', () {
      final SocialAuthService service = SocialAuthService(
        sources: {
          SocialProvider.google: _FakeSource(SocialProvider.google),
        },
      );

      expect(service.isAvailable(SocialProvider.google), isTrue);
      // Microsoft absent : l'interface ne doit pas proposer un bouton mort.
      expect(service.isAvailable(SocialProvider.microsoft), isFalse);
      expect(service.availableProviders, [SocialProvider.google]);
    });

    test('filtre les fournisseurs annoncés par le serveur', () {
      final SocialAuthService service = SocialAuthService(
        sources: {
          SocialProvider.google: _FakeSource(SocialProvider.google),
          SocialProvider.microsoft: _FakeSource(SocialProvider.microsoft),
        },
      );

      expect(
        service.enabledProviders(const [
          {'provider': 'google'},
          {'provider': 'microsoft'},
        ]),
        [SocialProvider.google, SocialProvider.microsoft],
      );
    });

    test('ignore un fournisseur inconnu ou absent de la build', () {
      final SocialAuthService service = SocialAuthService(
        sources: {
          SocialProvider.google: _FakeSource(SocialProvider.google),
        },
      );

      // Le serveur ne peut pas nous imposer un fournisseur que l'app ne sait
      // pas utiliser (ni Apple, ni le téléphone : hors périmètre, payants).
      expect(
        service.enabledProviders(const [
          {'provider': 'apple'},
          {'provider': 'phone'},
          {'provider': 'google'},
        ]),
        [SocialProvider.google],
      );
    });

    test('retourne le credential du fournisseur demandé', () async {
      final SocialAuthService service = SocialAuthService(
        sources: {
          SocialProvider.google: _FakeSource(SocialProvider.google, idToken: 'google-token'),
        },
      );

      final SocialCredential credential =
          await service.signIn(SocialProvider.google);

      expect(credential.provider, SocialProvider.google);
      expect(credential.idToken, 'google-token');
      expect(credential.toPayload(), {
        'provider': 'google',
        'credential': 'google-token',
      });
    });

    test('échoue explicitement si le fournisseur n’est pas configuré', () async {
      final SocialAuthService service = SocialAuthService(sources: const {});

      await expectLater(
        service.signIn(SocialProvider.microsoft),
        throwsA(
          isA<SocialAuthException>()
              .having((SocialAuthException e) => e.code, 'code',
                  'SOCIAL_PROVIDER_NOT_CONFIGURED')
              .having((SocialAuthException e) => e.providerUnavailable,
                  'providerUnavailable', isTrue),
        ),
      );
    });
  });

  group('SocialCredential', () {
    test('ne laisse jamais fuir l’id_token dans un message d’erreur', () {
      const SocialCredential credential = SocialCredential(
        provider: SocialProvider.google,
        idToken: 'jeton-secret',
      );

      // Les logs applicatifs peuvent être lus : le toString doit être neutre.
      expect(credential.toString(), isNot(contains('jeton-secret')));
    });

    test('toString n’expose que le fournisseur', () {
      const SocialCredential credential = SocialCredential(
        provider: SocialProvider.microsoft,
        idToken: 'jeton-secret',
      );

      expect(credential.toString(), 'SocialCredential(microsoft)');
    });
  });

  group('SocialAuthException', () {
    test('signale le cas « il faut une invitation »', () {
      const SocialAuthException failure =
          SocialAuthException('SOCIAL_ACCOUNT_NOT_LINKED', 'Aucun compte');

      expect(failure.requiresInvitation, isTrue);
      expect(failure.providerUnavailable, isFalse);
      expect(failure.isCancelled, isFalse);
    });

    test('distingue une annulation d’une panne', () {
      const SocialAuthException cancelled =
          SocialAuthException('SOCIAL_LOGIN_CANCELLED', 'Annulée');
      const SocialAuthException broken =
          SocialAuthException('SOCIAL_CREDENTIAL_REJECTED', 'Rejeté');

      expect(cancelled.isCancelled, isTrue);
      expect(broken.isCancelled, isFalse);
      // Le toString ne doit pas non plus divulguer de détail sensible.
      expect(broken.toString(), isNot(contains('Rejeté')));
    });
  });
}
