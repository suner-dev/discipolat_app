import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/core/invitation_token.dart';

void main() {
  test('normalizes a valid invitation token', () {
    expect(
      normalizeInvitationToken('  ABCDEF0123456789ABCDEF0123456789  '),
      'abcdef0123456789abcdef0123456789',
    );
  });

  test('rejects malformed invitation tokens', () {
    expect(normalizeInvitationToken(null), isNull);
    expect(normalizeInvitationToken(''), isNull);
    expect(normalizeInvitationToken('abc123'), isNull);
    expect(
        normalizeInvitationToken('gbcdef0123456789abcdef0123456789'), isNull);
  });

  test('extracts exactly one token from the canonical route', () {
    final uri = Uri.parse(
      '/accept-invitation?token=abcdef0123456789abcdef0123456789',
    );
    expect(
      invitationTokenFromUri(uri),
      'abcdef0123456789abcdef0123456789',
    );
  });

  test('rejects wrong paths fragments and duplicate token parameters', () {
    expect(
      invitationTokenFromUri(
        Uri.parse('/other?token=abcdef0123456789abcdef0123456789'),
      ),
      isNull,
    );
    expect(
      invitationTokenFromUri(
        Uri.parse(
          '/accept-invitation?token=abcdef0123456789abcdef0123456789#fragment',
        ),
      ),
      isNull,
    );
    expect(
      invitationTokenFromUri(
        Uri.parse(
          '/accept-invitation?token=abcdef0123456789abcdef0123456789&token=0123456789abcdef0123456789abcdef',
        ),
      ),
      isNull,
    );
  });
}
