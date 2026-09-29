// B7 — Tests du service du wizard d'onboarding.
// Vérifie : URLs exactes du contrat §3.1, payloads, parsing strict, erreurs.

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/tenant_onboarding_service.dart';
import 'package:discipolat_mobile/models/onboarding_step.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';

/// Fake conforme a la convention du depot : `Response` de dio avec `RequestOptions`.
class _FakeApi extends ApiService {
  _FakeApi(this.responses) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> responses;
  final List<String> calls = [];
  final List<Map<String, dynamic>?> bodies = [];

  dynamic _payload(String path) => responses[path] ?? responses['POST ' + path];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add('GET $path');
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: responses[path],
    );
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    calls.add('POST $path');
    bodies.add(data is Map<String, dynamic> ? data : null);
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: _payload(path),
    );
  }
}


Map<String, dynamic> stepJson({
  String id = 's0',
  String stepType = 'CHURCH_IDENTITY',
  int order = 0,
  String status = 'PENDING',
  bool completed = false,
  bool skippable = false,
  bool skipReason = false,
  Map<String, dynamic>? data,
}) =>
    {
      'id': id,
      'stepType': stepType,
      'stepOrder': order,
      'title': 'Identité',
      'description': 'Nom et contact',
      'status': status,
      'isCompleted': completed,
      'isSkippable': skippable,
      'skipRequiresReason': skipReason,
      'startedAt': null,
      'completedAt': null,
      'completedData': data,
    };

void main() {
  group('B7 — URLs du contrat §3.1', () {
    test('fetchSteps appelle GET /onboarding-wizard', () async {
      final api = _FakeApi({'/onboarding-wizard': [stepJson()]});
      final steps = await TenantOnboardingService(apiService: api).fetchSteps();
      expect(api.calls, ['GET /onboarding-wizard']);
      expect(steps, hasLength(1));
      expect(steps.first.stepType, OnboardingStepType.churchIdentity);
    });

    test('fetchProgress appelle GET /onboarding-wizard/progress', () async {
      final api = _FakeApi({
        '/onboarding-wizard/progress': {
          'totalSteps': 7,
          'completedSteps': 1,
          'skippedSteps': 0,
          'percentage': 14,
          'isComplete': false,
          'steps': [stepJson()],
        }
      });
      final p = await TenantOnboardingService(apiService: api).fetchProgress();
      expect(api.calls, ['GET /onboarding-wizard/progress']);
      expect(p.totalSteps, 7);
      expect(p.percentage, 14);
      expect(p.steps, hasLength(1));
    });

    test('fetchStatus appelle GET /onboarding-wizard/status', () async {
      final api = _FakeApi({
        '/onboarding-wizard/status': {
          'completed': false,
          'completedAt': null,
          'completedBy': null,
          'totalSteps': 7,
          'completedSteps': 0,
          'skippedSteps': 0,
          'percentage': 0,
        }
      });
      final s = await TenantOnboardingService(apiService: api).fetchStatus();
      expect(api.calls, ['GET /onboarding-wizard/status']);
      expect(s.completed, isFalse);
      expect(s.totalSteps, 7);
    });

    test('completeStep envoie data en OBJET (pas en String)', () async {
      final api = _FakeApi({'POST /onboarding-wizard/s0/complete': stepJson(completed: true)});
      await TenantOnboardingService(apiService: api)
          .completeStep('s0', {'churchName': 'Église Grâce'});
      expect(api.calls, ['POST /onboarding-wizard/s0/complete']);
      expect(api.bodies.single, isA<Map<String, dynamic>>());
      expect((api.bodies.single!['data'] as Map)['churchName'], 'Église Grâce');
    });

    test('D7 : completeStep sans data envoie un corps vide {} (pas d exception)', () async {
      final api = _FakeApi({'POST /onboarding-wizard/s0/complete': stepJson(completed: true)});
      final r = await TenantOnboardingService(apiService: api).completeStep('s0');
      expect(api.bodies.single, isEmpty);
      expect(r.isCompleted, isTrue);
    });

    test('skipStep envoie reason quand fourni', () async {
      final api = _FakeApi({'POST /onboarding-wizard/s1/skip': stepJson(id: 's1', status: 'SKIPPED', completed: true)});
      await TenantOnboardingService(apiService: api).skipStep('s1', reason: '  plus tard  ');
      expect(api.bodies.single, {'reason': 'plus tard'});
    });

    test('skipStep sans motif n envoie pas de clé reason', () async {
      final api = _FakeApi({'POST /onboarding-wizard/s1/skip': stepJson(id: 's1', status: 'SKIPPED', completed: true)});
      await TenantOnboardingService(apiService: api).skipStep('s1');
      expect(api.bodies.single, isEmpty);
    });
  });

  group('B7 — parsing STRICT (une réponse hors contrat doit échouer)', () {
    test('champ manquant -> FormatException', () async {
      final bad = stepJson()..remove('isSkippable');
      final api = _FakeApi({'/onboarding-wizard': [bad]});
      expect(
        () => TenantOnboardingService(apiService: api).fetchSteps(),
        throwsA(isA<FormatException>()),
      );
    });

    test('stepType inconnu -> FormatException', () async {
      final api = _FakeApi({'/onboarding-wizard': [stepJson(stepType: 'INCONNU')]});
      expect(
        () => TenantOnboardingService(apiService: api).fetchSteps(),
        throwsA(isA<FormatException>()),
      );
    });

    test('statut inconnu -> FormatException', () async {
      final api = _FakeApi({'/onboarding-wizard': [stepJson(status: 'PEUT_ETRE')]});
      expect(
        () => TenantOnboardingService(apiService: api).fetchSteps(),
        throwsA(isA<FormatException>()),
      );
    });

    test('réponse non-liste sur /onboarding-wizard -> FormatException', () async {
      final api = _FakeApi({'/onboarding-wizard': 'oups'});
      expect(
        () => TenantOnboardingService(apiService: api).fetchSteps(),
        throwsA(isA<FormatException>()),
      );
    });
  });

  group('B7 — comportement métier', () {
    test('trie les étapes par stepOrder (le contrat garantit l\'ordre)', () async {
      final api = _FakeApi({
        '/onboarding-wizard': [
          stepJson(id: 'b', order: 2, stepType: 'ROLES'),
          stepJson(id: 'a', order: 0),
          stepJson(id: 'c', order: 1, stepType: 'MEMBER_IMPORT'),
        ]
      });
      final steps = await TenantOnboardingService(apiService: api).fetchSteps();
      expect(steps.map((s) => s.stepOrder).toList(), [0, 1, 2]);
      expect(steps.map((s) => s.id).toList(), ['a', 'c', 'b']);
    });

    test('completedData est exposé comme Map, pas comme String', () async {
      final api = _FakeApi({
        '/onboarding-wizard': [stepJson(completed: true, data: {'churchName': 'Grâce'})]
      });
      final steps = await TenantOnboardingService(apiService: api).fetchSteps();
      expect(steps.first.completedData, isA<Map<String, dynamic>>());
      expect(steps.first.completedData!['churchName'], 'Grâce');
    });
  });
}
