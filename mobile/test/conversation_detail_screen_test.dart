import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/messages/conversation_detail_screen.dart';

/// ApiService factice : renvoie les messages d'une conversation et enregistre
/// les POST / PATCH (envoi, marquage lu).
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler, {this.failPosts = false}) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final bool failPosts;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
  final List<String> patchPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postDatas.add(data is Map<String, dynamic> ? data : null);
    if (failPosts) throw DioException(requestOptions: RequestOptions(path: path));
    return _json(path, {});
  }

  @override
  Future<Response> patch(String path, {dynamic data}) async {
    patchPaths.add(path);
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

const _messages = [
  {
    'id': 'm1', 'conversationId': 'c1', 'senderId': 'user-2',
    'senderName': 'Marie Martin', 'content': 'Bonjour !',
    'createdAt': '2026-08-10T09:00:00',
  },
  {
    'id': 'm2', 'conversationId': 'c1', 'senderId': 'user-1',
    'senderName': 'Jean Dupont', 'content': 'Bonjour Marie, comment vas-tu ?',
    'createdAt': '2026-08-10T09:05:00',
  },
];

void _setRole(String role) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'admin@discipolat.test',
    'roles': [role],
    'activeRole': role,
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => _setRole('ADMIN'));
  tearDown(() => AuthState().logout());

  Future<void> pumpScreen(WidgetTester tester, ApiService api, {String title = 'Marie Martin'}) async {
    await tester.pumpWidget(MaterialApp(
      home: ConversationDetailScreen(conversationId: 'c1', title: title, apiService: api),
    ));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche le fil de messages avec expéditeur et contenu', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/messages/conversations/c1/messages') return _json(path, _messages);
      return _json(path, <dynamic>[]);
    });
    await pumpScreen(tester, api);

    expect(find.text('Marie Martin'), findsWidgets); // AppBar + premier message
    expect(find.text('Bonjour !'), findsOneWidget);
    expect(find.text('Bonjour Marie, comment vas-tu ?'), findsOneWidget);
    // Le nom de l'expéditeur n'apparaît que pour les messages reçus :
    // m2 est envoyé par l'utilisateur courant (user-1) → pas de nom.
    expect(find.text('Jean Dupont'), findsNothing);
  });

  testWidgets('marque la conversation comme lue → PATCH /read', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/messages/conversations/c1/messages') return _json(path, _messages);
      return _json(path, <dynamic>[]);
    });
    await pumpScreen(tester, api);

    expect(api.patchPaths, contains('/messages/conversations/c1/read'));
  });

  testWidgets('envoie un message → POST avec le contenu puis recharge', (tester) async {
    var messages = List<dynamic>.from(_messages);
    final api = _FakeApiService((path, params) {
      if (path == '/messages/conversations/c1/messages') return _json(path, messages);
      return _json(path, <dynamic>[]);
    });
    await pumpScreen(tester, api);

    await tester.enterText(find.byType(TextField), 'Coucou depuis le mobile');
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.send));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/messages/conversations/c1/messages'));
    expect(api.postDatas.last?['content'], 'Coucou depuis le mobile');
  });

  testWidgets('échec d’envoi → SnackBar d’erreur', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/messages/conversations/c1/messages') return _json(path, _messages);
      return _json(path, <dynamic>[]);
    }, failPosts: true);
    await pumpScreen(tester, api);

    await tester.enterText(find.byType(TextField), 'message');
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.send));
    await tester.pumpAndSettle();

    expect(find.text('Erreur lors de l\'envoi'), findsOneWidget);
  });

  testWidgets('état vide → invite à écrire', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/messages/conversations/c1/messages') return _json(path, <dynamic>[]);
      return _json(path, <dynamic>[]);
    });
    await pumpScreen(tester, api);

    expect(find.text('Aucun message — dites bonjour !'), findsOneWidget);
  });

  testWidgets('erreur de chargement → message et bouton Réessayer', (tester) async {
    final api = _FakeApiService((path, params) {
      throw DioException(requestOptions: RequestOptions(path: path));
    });
    await pumpScreen(tester, api);

    expect(find.text('Impossible de charger la conversation'), findsOneWidget);
    expect(find.text('Réessayer'), findsOneWidget);
  });
}
