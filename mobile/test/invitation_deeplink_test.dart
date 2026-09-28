// B8 — Deep links d'invitation.
//
// Ces tests verrouillent le contrat entre la manifeste (AndroidManifest.xml /
// Info.plist) et le parseur (core/invitation_token.dart).
//
// RÈGLE STRUCTURANTE : invitationTokenFromUri() exige
//   uri.path == '/accept-invitation'
// La manifeste doit donc TOUJOURS produire cette forme. Le cas
// "discipolat://accept-invitation" (host sans path) est volontairement
// couvert pour garantir qu'on ne réintroduit jamais ce défaut.

import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/core/invitation_token.dart';

const String _token = 'abcdef0123456789abcdef0123456789'; // 32 hex

void main() {
  group('B8 — formes produites par la manifeste (doivent fonctionner)', () {
    test('App Link https : https://app.discipolat.com/accept-invitation?token=…', () {
      final uri = Uri.parse(
          'https://app.discipolat.com/accept-invitation?token=$_token');
      expect(uri.path, '/accept-invitation');
      expect(invitationTokenFromUri(uri), _token);
    });

    test('scheme custom : disciplat://app.discipolat.com/accept-invitation?token=…',
        () {
      final uri = Uri.parse(
          'discipolat://app.discipolat.com/accept-invitation?token=$_token');
      // Point clé de la correction : le path DOIT être non vide.
      expect(uri.path, '/accept-invitation');
      expect(invitationTokenFromUri(uri), _token);
    });
  });

  group('B8 — cas invalides (doivent renvoyer null)', () {
    test('token absent', () {
      expect(
        invitationTokenFromUri(
            Uri.parse('https://app.discipolat.com/accept-invitation')),
        isNull,
      );
    });

    test('token mal formé (pas 32 hex)', () {
      expect(
        invitationTokenFromUri(Uri.parse(
            'https://app.discipolat.com/accept-invitation?token=abc123')),
        isNull,
      );
    });

    test('query dupliquée', () {
      expect(
        invitationTokenFromUri(Uri.parse(
            'https://app.discipolat.com/accept-invitation?token=$_token&token=$_token')),
        isNull,
      );
    });

    test('fragment parasite', () {
      expect(
        invitationTokenFromUri(Uri.parse(
            'https://app.discipolat.com/accept-invitation?token=$_token#frag')),
        isNull,
      );
    });

    test('mauvais chemin', () {
      expect(
        invitationTokenFromUri(
            Uri.parse('https://app.discipolat.com/autre-page?token=$_token')),
        isNull,
      );
    });

    test('AUTRE HÔTE', () {
      expect(
        invitationTokenFromUri(
            Uri.parse('https://evil.example.com/accept-invitation?token=$_token')),
        isNull,
      );
    });

    test('RÉGRESSION : host « accept-invitation » sans path -> null', () {
      // C'est la forme que produisait la manifeste v1.0 du plan.
      // Elle doit rester rejetée : le parseur n'a pas été modifié.
      final uri = Uri.parse('discipolat://accept-invitation?token=$_token');
      expect(uri.host, 'accept-invitation');
      expect(uri.path, isNot('/accept-invitation'));
      expect(invitationTokenFromUri(uri), isNull);
    });
  });
}
