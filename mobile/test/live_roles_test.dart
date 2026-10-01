import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/realtime_bus_service.dart';

/// §G4.4 — Rôles vivants mobile : un push PermissionsChanged ciblé re-pulse
/// la session via GET /auth/me SANS déconnexion, et l'epoch fait re-render
/// menus/actions (AppDrawer l'écoute). Un push pour un AUTRE utilisateur est
/// ignoré.
class _FakeMeApi extends ApiService {
  _FakeMeApi() : super(baseUrl: 'http://fake');

  int calls = 0;
  Map<String, dynamic> me = {
    'userId': 'u1',
    'roles': ['PASTEUR', 'CHEF_DE_FAMILLE'],
    'activeRole': 'PASTEUR',
    'estChefDeFamille': false,
  };

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    if (path == '/auth/me') {
      calls++;
      return Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 200,
          data: me);
    }
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: {});
  }
}

void fire(String eventType, {String? userId, int eventId = 1}) {
  RealtimeBus.instance.handleRawFrame(jsonEncode({
    'type': 'OUTBOX',
    'eventId': eventId,
    'eventType': eventType,
    'payload': {if (userId != null) 'userId': userId},
  }));
}

Future<void> settle() =>
    Future.delayed(const Duration(milliseconds: 20));

void main() {
  late AuthState auth;
  late _FakeMeApi api;

  setUp(() {
    auth = AuthState();
    api = _FakeMeApi();
    auth.livePermissionsApi = api;
    auth.setAuthenticated(true, userData: {
      'userId': 'u1',
      'email': 'x@y.cd',
      'role': 'MEMBRE',
      'roles': ['MEMBRE'],
      'activeRole': 'MEMBRE',
    });
  });

  tearDown(() {
    auth.logout();
    auth.livePermissionsApi = null;
  });

  group('AuthState — rôles vivants (G4.4)', () {
    test('PermissionsChanged ciblé → nouvelle session sans déconnexion',
        () async {
      final epoch = auth.permissionsEpoch.value;

      fire('PermissionsChanged', userId: 'u1');
      await settle();

      expect(api.calls, 1);
      expect(auth.permissionsEpoch.value, epoch + 1);
      expect(auth.activeRole, 'PASTEUR');
      expect(auth.roles, containsAll(['PASTEUR', 'CHEF_DE_FAMILLE']));
      // Jamais de déconnexion (§G4.4-4) :
      expect(auth.isAuthenticated, isTrue);
    });

    test('PastorEnded → le rôle pastoral retiré côté serveur est appliqué',
        () async {
      api.me = {
        'userId': 'u1',
        'roles': ['CHEF_DE_FAMILLE'],
        'activeRole': 'CHEF_DE_FAMILLE',
      };
      fire('PastorEnded', userId: 'u1', eventId: 2);
      await settle();

      expect(auth.activeRole, 'CHEF_DE_FAMILLE');
      expect(auth.roles, isNot(contains('PASTEUR')));
      expect(auth.isAuthenticated, isTrue);
    });

    test('push destiné à un AUTRE utilisateur → ignoré', () async {
      final epoch = auth.permissionsEpoch.value;
      fire('PermissionsChanged', userId: 'autre', eventId: 3);
      await settle();

      expect(api.calls, 0);
      expect(auth.permissionsEpoch.value, epoch);
      expect(auth.activeRole, 'MEMBRE');
    });

    test('rétrogradation : ancien rôle actif retiré → repli sans logout',
        () async {
      api.me = {
        'userId': 'u1',
        'roles': ['FAISEUR'],
        // pas d'activeRole → l'ancien « MEMBRE » n'est plus dans la liste.
      };
      fire('RoleEnded', userId: 'u1', eventId: 4);
      await settle();

      expect(auth.activeRole, 'FAISEUR');
      expect(auth.isAuthenticated, isTrue);
    });
  });
}
