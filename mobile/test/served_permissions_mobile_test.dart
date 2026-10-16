import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';

/// PARAMÉTRISATION PAR ÉGLISE (Phase 2-client mobile) — verrou d'équivalence
/// du client. Miroir mobile du verrou web `routeAccessPermissions.test.ts` :
/// la tranche MEMBERS est gardée par les CLÉS de permissions SERVIES
/// (`GET /api/v1/me/permissions`, ensemble résolu serveur ≡ @authz.can,
/// pont d'autorités actives et surcouche tenant inclus). Règles prouvées :
/// (1) widening — la clé servie accède même sans rôle littéral élargi ;
/// (2) narrowing — la clé retirée refuse, le littéral ne sauve plus ;
/// (3) cache non prêt → repli strict sur la table par rôle (transition) ;
/// (4) route non convertie → le filtre servi ne change rien (additif).
class _FakeServedApi extends ApiService {
  _FakeServedApi() : super(baseUrl: 'http://fake');

  int calls = 0;
  bool fail = false;
  List<String> permissions = const ['MEMBERS_EXECUTE'];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    if (fail) {
      throw DioException(requestOptions: RequestOptions(path: path));
    }
    if (path == '/me/permissions') {
      calls++;
      return Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 200,
          data: {'permissions': permissions, 'version': 7});
    }
    return Response(
        requestOptions: RequestOptions(path: path), statusCode: 200, data: {});
  }
}

void main() {
  late AuthState auth;
  late _FakeServedApi api;

  setUp(() async {
    auth = AuthState();
    auth.logout();
    api = _FakeServedApi();
    auth.servedPermissionsApi = api;
    auth.setAuthenticated(true, userData: {
      'userId': 'u1',
      'email': 'x@y.cd',
      'role': 'PASTEUR',
      'roles': ['PASTEUR'],
      'activeRole': 'PASTEUR',
    });
    // Laisse l'amorçage déclenché par setAuthenticated se résoudre : les
    // tests jugent ensuite un nombre d'appels déterministe.
    await Future<void>.delayed(const Duration(milliseconds: 20));
  });

  tearDown(() {
    auth.logout();
    auth.servedPermissionsApi = null;
    auth.livePermissionsApi = null;
  });

  group('routePermissions — registre servi (miroir du web)', () {
    test('la tranche MEMBERS (Phase 2) est adressée par les clés exactes V258',
        () {
      // Contrat partagé avec frontend/src/lib/routeAccess.ts (ROUTE_PERMISSIONS)
      // et le catalogue V258 — une divergence casse l'alignement UI/serveur.
      expect(permissionsForRoute('/members/requests'), ['MEMBERS_EXECUTE']);
      expect(permissionsForRoute('/member-competences'), ['MEMBERS_EXECUTE']);
      expect(permissionsForRoute('/qr-checkin'),
          ['MEMBERS_EXECUTE_QRCHECKIN']);
      expect(permissionsForRoute('/geofencing'), ['MEMBERS_EXECUTE_R4A']);
    });

    test('Phase 5 — les modules convertis portent la même clé que le web', () {
      // Chaque clé ci-dessous n'a été câblée que parce que l'ensemble de rôles
      // littéraux mobile est EXACTEMENT la matrice serveur de la clé (catalogue
      // V258-V261). Le verrou fige l'alignement mobile ≡ web ≡ serveur.
      expect(permissionsForRoute('/audit'), ['AUDIT_EXECUTE']);
      expect(permissionsForRoute('/souls'), ['SOULS_EXECUTE']);
      expect(permissionsForRoute('/souls/:id'), ['SOULS_EXECUTE']);
      expect(permissionsForRoute('/events'), ['EVENTS_EXECUTE_R6A']);
      expect(permissionsForRoute('/families'), ['FAMILIES_EXECUTE_R5A']);
      expect(permissionsForRoute('/departments/:id'), ['DEPARTMENTS_EXECUTE']);
      expect(permissionsForRoute('/admin'), ['ADMIN_EXECUTE']);
      expect(permissionsForRoute('/search'), ['SEARCH_EXECUTE']);
      expect(permissionsForRoute('/alerts'), ['ALERTS_EXECUTE']);
    });

    test(
        'Phase 5 — toute entrée du registre sert au moins une clé non vide',
        () {
      // Invariant « rien de figé mais jamais de route aveugle » : une clé vide
      // ferait passer TOUTES les permissions (le any-of ne filtre plus), ce qui
      // serait un élargissement silencieux. On le interdit structurellement.
      for (final e in routePermissions.entries) {
        expect(e.value, isNotEmpty, reason: 'route ${e.key} sans clé servie');
        for (final k in e.value) {
          expect(k.trim(), isNotEmpty, reason: 'clé vide sur ${e.key}');
        }
      }
    });

    test('(1) widening : une clé servie admise sans rôle littéral élargit',
        () {
      // MEMBRE ne figure pas dans _routeRoles['/qr-checkin'] ; si l'église a
      // redonné MEMBERS_EXECUTE_QRCHECKIN à son rôle, l'accès est admis.
      bool hasQrKey(String k) => k == 'MEMBERS_EXECUTE_QRCHECKIN';
      expect(routeAdmittedByServedKeys('/qr-checkin', hasQrKey), isTrue);
    });

    test('(2) narrowing : ensemble servi vide refuse une route convertie', () {
      const none = _alwaysFalse;
      expect(routeAdmittedByServedKeys('/members/requests', none), isFalse);
      // Phase 5 : les modules convertis obéissent désormais au servi.
      expect(routeAdmittedByServedKeys('/events', none), isFalse);
      expect(routeAdmittedByServedKeys('/audit', none), isFalse);
    });

    test('(4) route non convertie : le filtre servi ne change rien', () {
      const none = _alwaysFalse;
      // '/dashboard' (tous rôles, non servi) et '/souls/:id/qr' (route propre
      // au mobile, absente du web) restent governés par le littéral par rôle.
      expect(permissionsForRoute('/dashboard'), isNull);
      expect(permissionsForRoute('/souls/:id/qr'), isNull);
      expect(routeAdmittedByServedKeys('/dashboard', none), isTrue);
      expect(routeAdmittedByServedKeys('/souls/:id/qr', none), isTrue);
    });
  });

  group('AuthState — cache de permissions servies', () {
    test('refreshServedPermissions normalise et publie la liste', () async {
      // Amorçage déjà passé par setAuthenticated (cf. setUp).
      expect(api.calls, 1);
      expect(auth.isServedPermissionsReady, isTrue);
      expect(auth.servedPermissions, {'MEMBERS_EXECUTE'});

      await auth.refreshServedPermissions();
      expect(api.calls, 2);
      // Insensible à la casse (le serveur normalise en uppercase).
      expect(auth.hasPermission('members_execute'), isTrue);
      expect(auth.hasPermission('MEMBERS_EXECUTE_QRCHECKIN'), isFalse);
    });

    test('clés servies en minuscules → normalisées uppercase', () async {
      api.permissions = const ['members_execute', 'finance_view'];
      await auth.refreshServedPermissions();
      expect(auth.hasPermission('MEMBERS_EXECUTE'), isTrue);
      expect(auth.hasPermission('FINANCE_VIEW'), isTrue);
    });

    test('(3) échec réseau → cache laissé intact, jamais de purge',
        () async {
      // Session repartie de zéro (cache vidé au logout) puis réseau en échec.
      auth.logout();
      api.fail = true;
      await auth.refreshServedPermissions();

      // Jamais de logout forcé (§G4.4-4) : non prêt → la garde reste sur
      // la table littérale par rôle (repli de transition).
      expect(auth.isServedPermissionsReady, isFalse);
      expect(auth.hasPermission('MEMBERS_EXECUTE'), isFalse);
    });

    test('logout → le cache ne fuit pas vers la session suivante', () async {
      await auth.refreshServedPermissions();
      expect(auth.hasPermission('MEMBERS_EXECUTE'), isTrue);

      auth.logout();
      expect(auth.isServedPermissionsReady, isFalse);
      expect(auth.servedPermissions, isEmpty);
      expect(auth.hasPermission('MEMBERS_EXECUTE'), isFalse);
    });

    test('amorçage à la connexion : setAuthenticated déclenche le re-pull',
        () async {
      // setUp a déjà appelé setAuthenticated → un appel vers /me/permissions
      // est parti (async) ; on laisse la microtask se résoudre.
      await Future<void>.delayed(const Duration(milliseconds: 20));
      expect(api.calls, greaterThanOrEqualTo(1));
      expect(auth.isServedPermissionsReady, isTrue);
    });
  });
}

bool _alwaysFalse(String key) => false;
