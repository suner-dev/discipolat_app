// B9 — Test du service d'administration des invitations (contrat §B9).
//
// Verrouille la conformité AU CONTRAT RÉEL d'InvitationController : chemins,
// params de requête, corps d'envoi et normalisation des deux formes de liste
// (PageResponse ET tableau). Aucun endpoint inventé (gate G-B.4).
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/invitation_admin_service.dart';

/// ApiService factice : journalise le dernier appel et renvoie une réponse posée.
class _FakeApi extends ApiService {
  _FakeApi() : super(baseUrl: 'http://fake');

  String? lastMethod;
  String? lastPath;
  Map<String, dynamic>? lastParams;
  dynamic lastData;
  dynamic nextResponse;
  bool shouldThrow = false;

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    lastMethod = 'GET';
    lastPath = path;
    lastParams = params ?? queryParameters;
    return _respond(path);
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    lastMethod = 'POST';
    lastPath = path;
    lastData = data;
    return _respond(path);
  }

  @override
  Future<Response> delete(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    lastMethod = 'DELETE';
    lastPath = path;
    return _respond(path);
  }

  Response _respond(String path) {
    if (shouldThrow) {
      throw DioException(requestOptions: RequestOptions(path: path));
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: nextResponse,
    );
  }
}

void main() {
  late _FakeApi api;
  late InvitationAdminService service;

  setUp(() {
    api = _FakeApi();
    service = InvitationAdminService(apiService: api);
  });

  group('list', () {
    test('demande la pagination et parse une PageResponse', () async {
      api.nextResponse = {
        'content': [
          {
            'id': 'inv-1',
            'email': 'a@b.com',
            'role': 'MEMBRE',
            'status': 'pending',
            'scopeType': 'TENANT',
            'expiresAt': '2026-10-01T00:00:00Z',
          },
        ],
        'page': 0,
        'size': 50,
        'totalElements': 1,
        'totalPages': 1,
      };

      final result = await service.list(page: 0, size: 50, status: 'PENDING');

      expect(api.lastMethod, 'GET');
      expect(api.lastPath, '/admin/invitations');
      expect(api.lastParams, {'page': 0, 'size': 50, 'status': 'PENDING'});
      expect(result.items.length, 1);
      expect(result.items.first.email, 'a@b.com');
      // Le statut est normalisé en MAJUSCULES côté service (comparaison fiable).
      expect(result.items.first.status, 'PENDING');
      expect(result.items.first.isPending, isTrue);
      expect(result.totalPages, 1);
    });

    test('accepte la forme liste simple (rétro-compat, sans param page)',
        () async {
      api.nextResponse = [
        {'id': 'x', 'email': 'c@d.com', 'role': 'FAISEUR', 'status': 'ACCEPTED'},
      ];

      final result = await service.list();

      // Aucun paramètre → le backend renvoie la liste brute ; on ne l'envoie pas.
      expect(api.lastParams, isNull);
      expect(result.items.first.status, 'ACCEPTED');
      expect(result.items.first.isPending, isFalse);
    });

    test('propage l\'erreur réseau (l\'écran gère l\'état d\'erreur)', () async {
      api.shouldThrow = true;
      expect(() => service.list(), throwsA(isA<DioException>()));
    });
  });

  group('create', () {
    test('envoie exactement le contrat (email, role, scopeType)', () async {
      api.nextResponse = {
        'success': true,
        'invitationId': 'inv-2',
        'invitationLink': 'https://app/accept?token=abc',
        'emailSent': false,
        'requiresTenantSwitch': false,
      };

      final result = await service.create(email: '  a@b.com ', role: 'MEMBRE');

      expect(api.lastMethod, 'POST');
      expect(api.lastPath, '/admin/invitations');
      expect(api.lastData, {
        'email': 'a@b.com',
        'role': 'MEMBRE',
        'scopeType': 'TENANT',
      });
      expect(result.emailSent, isFalse);
      expect(result.invitationLink, 'https://app/accept?token=abc');
      expect(result.isDirectMembership, isFalse);
    });

    test('ajoute scopeId / organizationNodeId seulement s\'ils sont fournis',
        () async {
      api.nextResponse = {'success': true, 'invitedUserId': 'u-1'};

      await service.create(
        email: 'a@b.com',
        role: 'RESPONSABLE',
        scopeType: 'ORGANIZATION',
        scopeId: 'sc-1',
        organizationNodeId: '', // vide → jamais envoyé
      );

      expect(api.lastData, {
        'email': 'a@b.com',
        'role': 'RESPONSABLE',
        'scopeType': 'ORGANIZATION',
        'scopeId': 'sc-1',
      });
    });

    test('détecte l\'ajout direct (compte existant) via invitedUserId', () async {
      api.nextResponse = {
        'success': true,
        'invitedUserId': 'u-9',
        'crossTenantIdentity': false,
      };

      final result = await service.create(email: 'a@b.com', role: 'MEMBRE');
      expect(result.isDirectMembership, isTrue);
    });
  });

  group('resend / cancel / validate / accept', () {
    test('resend appelle /{id}/resend et parse le nouveau lien', () async {
      api.nextResponse = {
        'success': true,
        'invitationLink': 'https://app/accept?token=new',
        'emailSent': true,
        'expiresAt': '2026-12-31T00:00:00Z',
      };

      final result = await service.resend('inv/1');

      expect(api.lastMethod, 'POST');
      // L'id est encodé dans le chemin (sécurité d'identifiant opaque).
      expect(api.lastPath, '/admin/invitations/inv%2F1/resend');
      expect(result.emailSent, isTrue);
      expect(result.invitationLink, 'https://app/accept?token=new');
    });

    test('cancel appelle DELETE /{id}', () async {
      api.nextResponse = {};
      await service.cancel('inv-3');
      expect(api.lastMethod, 'DELETE');
      expect(api.lastPath, '/admin/invitations/inv-3');
    });

    test('validate appelle GET /validate/{token}', () async {
      api.nextResponse = {'valid': true, 'email': 'a@b.com', 'accountExists': true};
      final data = await service.validate('tok en');
      expect(api.lastMethod, 'GET');
      expect(api.lastPath, '/admin/invitations/validate/tok%20en');
      expect(data['accountExists'], isTrue);
    });

    test('accept appelle POST /accept/{token} avec le corps fourni', () async {
      api.nextResponse = {'success': true, 'crossTenantIdentity': false};
      await service.accept('tk', password: 'Azerty1!', firstName: 'Jean');
      expect(api.lastMethod, 'POST');
      expect(api.lastPath, '/admin/invitations/accept/tk');
      expect(api.lastData, {'password': 'Azerty1!', 'firstName': 'Jean'});
    });
  });
}
