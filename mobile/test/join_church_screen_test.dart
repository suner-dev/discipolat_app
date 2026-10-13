import 'package:dio/dio.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/login/join_church_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// `/j/<slug>` — le lien d'invitation que `RegisterScreen` affiche au fondateur
/// et qui est transmis par email ou WhatsApp.
///
/// L'application ne servait AUCUNE route pour ce chemin : ouvrir le lien dans
/// l'application menait à « page introuvable ». Le fondateur.displayait un lien,
/// le destinataire le suivait, et rien ne se passait — sans le moindre message
/// d'erreur côté émetteur. Ces tests couvrent la résolution publique du slug et
/// ses deux issues (résolu / non résolu).
class _FakeApi extends ApiService {
  _FakeApi({this.resolveBody = const {'found': false}, this.resolveStatus = 200})
      : super(baseUrl: 'http://fake');

  final Map<String, dynamic> resolveBody;
  final int resolveStatus;
  final List<String> getPaths = [];
  final List<Map<String, dynamic>> getParams = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    getParams.add(params ?? queryParameters ?? const {});
    if (path.endsWith('/resolve')) {
      // Dio lève sur un statut >= 400 : le fake doit faire pareil, sinon le
      // chemin 429 de l'écran ne serait jamais atteint en test.
      final request = RequestOptions(path: path);
      if (resolveStatus >= 400) {
        throw DioException(
          requestOptions: request,
          response: Response(
            requestOptions: request,
            statusCode: resolveStatus,
            data: const <String, dynamic>{},
          ),
        );
      }
      return Response(
        requestOptions: request,
        statusCode: resolveStatus,
        data: resolveBody,
      );
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: const <String, dynamic>{},
    );
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: const <String, dynamic>{},
    );
  }
}

Widget _app(ApiService api, {String? slug, String? code}) {
  return MaterialApp(
    home: JoinChurchScreen(apiService: api, initialSlug: slug, initialCode: code),
  );
}

const _CHURCH = {
  'found': true,
  'churchName': 'Église Bethel de Cocody',
  'slug': 'eglise-bethel-de-cocody',
  'joinMode': 'OPEN',
  'requiresApproval': false,
};

void main() {
  group('JoinChurchScreen — lien /j/<slug>', () {
    testWidgets('un lien valide interroge /public/join/resolve avec le slug', (tester) async {
      final api = _FakeApi(resolveBody: Map<String, dynamic>.from(_CHURCH));
      await tester.pumpWidget(_app(api, slug: 'eglise-bethel-de-cocody'));
      await tester.pumpAndSettle();

      expect(api.getPaths, contains('/public/join/resolve'));
      expect(api.getParams.first, {'slug': 'eglise-bethel-de-cocody'});
      expect(find.text('Église Bethel de Cocody'), findsOneWidget);
    });

    testWidgets('aucune saisie de code n’est demandée sur un lien', (tester) async {
      final api = _FakeApi(resolveBody: Map<String, dynamic>.from(_CHURCH));
      await tester.pumpWidget(_app(api, slug: 'eglise-bethel-de-cocody'));
      await tester.pumpAndSettle();

      expect(find.text("Code de l'église"), findsNothing);
    });

    testWidgets('lien non résolu : message actionnable, jamais un écran vide',
        (tester) async {
      final api = _FakeApi(resolveBody: const {'found': false});
      await tester.pumpWidget(_app(api, slug: 'eglise-obsolete'));
      await tester.pumpAndSettle();

      expect(find.textContaining('plus valable'), findsOneWidget);
      // Le même écran propose par ailleurs un champ de saisie de code : on ne
      // compte donc pas les occurrences, seulement la présence de la piste.
      expect(find.textContaining("code d'entrée"), findsWidgets);
    });

    testWidgets('rate-limit (429) : on ne prétend pas que le lien est périmé',
        (tester) async {
      final api = _FakeApi(resolveStatus: 429);
      await tester.pumpWidget(_app(api, slug: 'eglise-bethel-de-cocody'));
      await tester.pumpAndSettle();

      // Confondre quota et lien périmé enverrait l'utilisateur ressaisir un
      // code alors que le lien est parfaitement bon.
      expect(find.textContaining('Trop de tentatives'), findsOneWidget);
      expect(find.textContaining('plus valable'), findsNothing);
    });

    testWidgets('le parcours par code reste inchangé', (tester) async {
      final api = _FakeApi();
      await tester.pumpWidget(_app(api, code: 'bethel-7k2m'));
      await tester.pumpAndSettle();

      expect(find.text("Code de l'église"), findsOneWidget);
      expect(api.getPaths, isNot(contains('/public/join/resolve')));
    });
  });
}
