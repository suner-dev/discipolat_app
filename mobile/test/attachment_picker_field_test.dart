import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/widgets/attachment_picker_field.dart';

/// ApiService factice : renvoie les documents du module Fichiers et enregistre
/// les corps des POST (création de document).
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<Map<String, dynamic>> postedBodies = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/files') {
      return _json(path, {
        'content': [
          {'id': 'f1', 'nom': 'Compte rendu réunion', 'taille': 1024},
          {'id': 'f2', 'nom': 'Photo culte', 'taille': 2048},
        ],
      });
    }
    throw StateError('Chemin inattendu: $path');
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postedBodies.add(Map<String, dynamic>.from(data as Map));
    if (path == '/files') {
      // Réponse du backend : le document créé (contract reel typeFichier/chemin).
      return _json(path, {'id': 'f-created', 'nom': 'Nouveau document'});
    }
    throw StateError('Chemin inattendu: $path');
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

/// Monte le picker et retourne le Set partagé : onChanged le mute en place
/// (comme les écrans réels), la référence reste donc toujours à jour.
Future<Set<String>> pumpPicker(WidgetTester tester, ApiService api) async {
  final selected = <String>{};
  await tester.pumpWidget(MaterialApp(
    home: Scaffold(
      body: AttachmentPickerField(
        apiService: api,
        value: selected,
        onChanged: (ids) => selected..clear()..addAll(ids),
      ),
    ),
  ));
  return selected;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('affiche le bouton puis ouvre le sélecteur avec la liste des documents', (tester) async {
    final api = _FakeApiService();
    await pumpPicker(tester, api);

    expect(find.text('Joindre des documents'), findsOneWidget);

    await tester.tap(find.text('Joindre des documents'));
    await tester.pumpAndSettle();

    // Feuille ouverte : titre + documents du module Fichiers.
    expect(find.text('Pièces jointes'), findsOneWidget);
    expect(find.text('Compte rendu réunion'), findsOneWidget);
    expect(find.text('Photo culte'), findsOneWidget);
    expect(find.text('Valider la sélection'), findsOneWidget);
  });

  testWidgets('sélection multi puis validation → onChanged reçoit les deux ids', (tester) async {
    final api = _FakeApiService();
    final selected = await pumpPicker(tester, api);

    await tester.tap(find.text('Joindre des documents'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Compte rendu réunion'));
    await tester.pump();
    await tester.tap(find.text('Photo culte'));
    await tester.pump();

    await tester.tap(find.text('Valider la sélection'));
    await tester.pumpAndSettle();

    expect(selected, {'f1', 'f2'});
  });

  testWidgets('décocher une pièce déjà sélectionnée la retire de la sélection', (tester) async {
    final api = _FakeApiService();
    final selected = await pumpPicker(tester, api);

    await tester.tap(find.text('Joindre des documents'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Compte rendu réunion'));
    await tester.pump();
    // Décoche la même ligne.
    await tester.tap(find.text('Compte rendu réunion'));
    await tester.pump();

    await tester.tap(find.text('Valider la sélection'));
    await tester.pumpAndSettle();

    expect(selected, isEmpty);
  });

  testWidgets('crée un document directement et l’ajoute à la sélection', (tester) async {
    final api = _FakeApiService();
    final selected = await pumpPicker(tester, api);

    await tester.tap(find.text('Joindre des documents'));
    await tester.pumpAndSettle();

    // Bouton + du sélecteur → dialogue de création de document.
    await tester.tap(find.byIcon(Icons.add_circle_outline));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField).at(0), 'Compte rendu réunion Mars');
    await tester.enterText(find.byType(TextField).at(1), 'https://drive.google.com/rapport');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    // Le POST /files utilise le contrat backend reel (chemin/typeFichier).
    expect(api.postedBodies, hasLength(1));
    expect(api.postedBodies.single['nom'], 'Compte rendu réunion Mars');
    expect(api.postedBodies.single['chemin'], 'https://drive.google.com/rapport');
    expect(api.postedBodies.single['typeFichier'], 'application/pdf');
    expect(api.postedBodies.single['categorie'], 'AUTRE');

    // Retour au sélecteur : le document créé est dans la sélection locale.
    await tester.tap(find.text('Valider la sélection'));
    await tester.pumpAndSettle();

    expect(selected, {'f-created'});
  });

  testWidgets('pré-sélection affichée : le bouton passe en « Modifier les documents »', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: AttachmentPickerField(
          apiService: api,
          value: {'f1'},
          onChanged: (_) {},
        ),
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Modifier les documents'), findsOneWidget);
    expect(find.text('Joindre des documents'), findsNothing);
  });
}
