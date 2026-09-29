// Tests du service RGPD mobile — prouvent la correspondance avec le contrat réel
// du backend livré par l'Agent A (ConsentController, PublicLegalController).

import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/compliance_service.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeApi extends ApiService {
  _FakeApi(this.responses) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> responses;
  final List<String> calls = [];
  final List<dynamic> bodies = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add('GET $path');
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: responses[path]);
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    calls.add('POST $path');
    bodies.add(data);
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: responses[path]);
  }
}

void main() {
  group('ConsentType — mapping exact vers le backend', () {
    test('les 6 types du backend sont representes, sans plus', () {
      // ALLOWED_TYPES du ConsentController : CGU, PRIVACY, CONSENT_ART9, WHATSAPP, MARKETING, PHOTO
      expect(ConsentType.values.map((t) => t.wire).toList(), [
        'CGU', 'PRIVACY', 'CONSENT_ART9', 'WHATSAPP', 'MARKETING', 'PHOTO',
      ]);
    });

    test('tryParse accepte la forme exacte et refuse le reste', () {
      expect(ConsentType.tryParse('CGU'), ConsentType.cgu);
      expect(ConsentType.tryParse('  consent_art9 '), ConsentType.consentArt9);
      expect(ConsentType.tryParse('INCONNU'), isNull);
    });
  });

  group('POST /compliance/consents', () {
    test('envoie le type au format wire attendu + granted', () async {
      final api = _FakeApi({'POST /compliance/consents': {}});
      await ComplianceService(apiService: api)
          .recordConsent(ConsentType.consentArt9, granted: true, policyVersion: 'v1');

      expect(api.calls, ['POST /compliance/consents']);
      final body = api.bodies.single as Map<String, dynamic>;
      expect(body['type'], 'CONSENT_ART9');
      expect(body['granted'], isTrue);
      expect(body['policyVersion'], 'v1');
    });

    test('un retrait (granted:false) est transmissible (RGPD art. 7.3)', () async {
      final api = _FakeApi({'POST /compliance/consents': {}});
      await ComplianceService(apiService: api)
          .recordConsent(ConsentType.marketing, granted: false);
      final body = api.bodies.single as Map<String, dynamic>;
      expect(body['granted'], isFalse);
      expect(body['type'], 'MARKETING');
    });

    test('les champs optionnels absents ne sont PAS envoyes (pas de null parasite)', () async {
      final api = _FakeApi({'POST /compliance/consents': {}});
      await ComplianceService(apiService: api)
          .recordConsent(ConsentType.photo, granted: true);
      final body = api.bodies.single as Map<String, dynamic>;
      expect(body.containsKey('policyVersion'), isFalse);
      expect(body.containsKey('details'), isFalse);
    });
  });

  group('GET /compliance/consents/mine', () {
    test('parse une liste de consentements', () async {
      final api = _FakeApi({
        '/compliance/consents/mine': [
          {'type': 'CGU', 'granted': true, 'policyVersion': 'v2'},
          {'type': 'MARKETING', 'granted': false},
        ]
      });
      final list = await ComplianceService(apiService: api).myConsents();
      expect(api.calls, ['GET /compliance/consents/mine']);
      expect(list, hasLength(2));
      expect(list.first.type, ConsentType.cgu);
      expect(list.first.granted, isTrue);
      expect(list.first.policyVersion, 'v2');
      expect(list.last.granted, isFalse);
    });

    test('un type inconnu du backend -> FormatException (parsing strict)', () async {
      final api = _FakeApi({
        '/compliance/consents/mine': [
          {'type': 'TYPE_FANTOME', 'granted': true}
        ]
      });
      expect(
        () => ComplianceService(apiService: api).myConsents(),
        throwsA(isA<FormatException>()),
      );
    });

    test('reponse non-liste -> FormatException', () async {
      final api = _FakeApi({'/compliance/consents/mine': 'oups'});
      expect(
        () => ComplianceService(apiService: api).myConsents(),
        throwsA(isA<FormatException>()),
      );
    });
  });

  group('GET /public/legal', () {
    test('liste les documents légaux', () async {
      final api = _FakeApi({
        '/public/legal': [
          {'code': 'CGU', 'title': "Conditions d'utilisation", 'version': 'v3'},
          {'code': 'PRIVACY', 'title': 'Politique de confidentialité'},
        ]
      });
      final docs = await ComplianceService(apiService: api).legalDocuments();
      expect(api.calls, ['GET /public/legal']);
      expect(docs.map((d) => d.code).toList(), ['CGU', 'PRIVACY']);
      expect(docs.first.title, "Conditions d'utilisation");
    });

    test('GET /public/legal/{code} cible le bon document', () async {
      final api = _FakeApi({
        '/public/legal/CGU': {'code': 'CGU', 'title': 'CGU', 'content': 'texte...'}
      });
      final doc = await ComplianceService(apiService: api).legalDocument('CGU');
      expect(api.calls, ['GET /public/legal/CGU']);
      expect(doc.code, 'CGU');
      expect(doc.content, 'texte...');
    });

    test('document sans code -> FormatException', () async {
      final api = _FakeApi({'/public/legal/X': {'title': 'sans code'}});
      expect(
        () => ComplianceService(apiService: api).legalDocument('X'),
        throwsA(isA<FormatException>()),
      );
    });
  });
}
