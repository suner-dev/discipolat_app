// Garde-fou de CONTRAT — type des identifiants côté mobile.
//
// POURQUOI CE TEST EXISTE
// Le backend a migré vers des identifiants UUID (250 entités sur 260 ont
// `UUID id` ; 10 seulement ont `Long id`). Le mobile n'a PAS suivi : 7 modèles
// déclarent encore `required int id`. Conséquence : un appel comme
// `PUT /events/42` part vers un backend qui attend un UUID → 400, et un
// `GET /streams/7` cible une ressource inexistante. Le symptôme n'est pas une
// erreur visible à l'écran : c'est une 400 dans le vide, ou un écran vide.
//
// Ce test est un CRÉMAILLON (ratchet) : il maintient une liste d'exceptions
// explicite, qui doit DIMINUER. Ajouter un nouveau `int id` échoue
// immédiatement ; retirer une exception sans corriger le modèle échoue aussi.
// La liste vide est l'objectif, pas le point de départ.
//
// Ce qu'il ne fait PAS : il ne corrige rien tout seul. Il rend une dette
// invisible visible, et il empêche qu'elle s'agrandisse.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// Chemins encore en `int id`, avec la raison. Toute entrée doit être
/// supprimée au fur et à mesure de la migration.
const _allowList = <String, String>{
  // stream_model.dart MIGRÉ (V240) : l'id du module streaming reste
  // structurellement `int` — LiveStream.id est BIGSERIAL côté serveur et
  // toutes les routes /streams/{id} sont en Long (précédent task_model
  // V234 : ce n'est PAS de la dette). La dette réelle était ailleurs :
  // tenantId/createdBy passés par le client (IDOR), champs inventés
  // (totalViews, isOwn/isSystem sur le fil), count vendu comme «
  // spectateurs » — tout corrigé. Le constructeur est en plain Dart
  // (`required this.id`, sans type répété) : il ne correspond plus à la
  // regex du ratchet, qui cible les déclarations `required int id`.
  // finance_model.dart MIGRÉ (V236) : identifiants String (UUID), champs
  // alignés sur les vues FinanceService — l'exception a été retirée ici.
  // discipleship_model.dart MIGRÉ : les refs personnes (createdById,
  // discipleId, mentorId, verifiedById) sont passées en String (UUID serveur
  // vérifié, le serveur fait UUID.fromString). Les ids structurelles restent
  // volontairement int : journey/stage/progress/assignment/meeting sont
  // BIGSERIAL (Long) côté DiscipleshipService — ce n'est pas de la dette.
  // task_model.dart MIGRÉ : le serveur (TaskController V234) génère les ids
  // structurels en GenerationType.IDENTITY (Long) — `int id` y est ICI le
  // contrat correct, pas la dette. La dette réelle était ailleurs : les refs
  // personnes (assignedToId, assignedById, authorId, uploadedById, userId)
  // sont des UUID serveur → String dans le modèle, et le routeur a récupéré
  // la route /tasks que cet écran consommait enfin.
  // message_model.dart MIGRÉ : toutes les entités messages sont UUID côté
  // serveur (Conversation/ConversationMessage/GroupConversation,
  // GenerationType.UUID vérifié) → ids et refs personnes (senderId,
  // otherUserId…) sont des String ici, comme ConversationResponse/
  // MessageResponse/GroupConversationResponse. Le service consomme les
  // endpoints réels /api/v1/messages/** (parity avec MessagesPage.tsx web).
  // health_model.dart MIGRÉ : les 11 entités du module santé (PatientRecord,
  // MedicalConsultation, Prescription, PharmacyItem, PharmacyStock,
  // HealthCampaign, HealthMedication, HealthKit, HealthDuty,
  // CampaignParticipant, Family) sont toutes `GenerationType.UUID` côté
  // serveur (vérifié sur les classes ET sur les migrations V141/V235).
  // Le modèle déclarait `required int id` sur les dix d'entre elles :
  // `(json['id'] as num).toInt()` levait un TypeError sur la chaîne UUID
  // renvoyée par le serveur, ce qui cassait l'écran Santé dès le premier
  // chargement. Les classes sont désormais en Dart simple, sans freezed
  // (un .freezed.dart périmé compilait avec l'ancien contrat en silence).
  // Reste hors périmètre : la dette « modèle serveur sans contrepartie »
  // (MedicalKit.items, StaffDuty.startTime/endTime, Account.code,
  // Budget.startDate, Tontine.maxMembers) — arbitrage produit, cf.
  // docs/SPEC_BACKEND_SERVICES_MOBILES_V233.md §7.3.
};

/// Exceptions de la règle « référence personne en `int` », par fichier.
///
/// Pourquoi une exception est nécessaire ici alors que la règle est « sans
/// exception » ? Parce que la règle ne peut pas distinguer un modèle réellement
/// adossé au serveur d'un modèle qui n'existe que côté mobile. `asset_model.dart`
/// contient `AssetTransfer`, `StockMovement` et `AssetInsurance` : aucune
/// entité serveur ne porte ces noms (vérifié : `grep class AssetTransfer|
/// StockMovement|AssetInsurance` sur `backend/` ne renvoie rien) et le
/// `AssetsService` ne les instancie jamais. C'est de l'échafaudage mobile orphelin,
/// pas un contrat enfreint — le convertir en `String` serait inventer un
/// contrat. Le jour où le serveur expose ces routes, la migration s'impose et
/// cette exception doit disparaître.
const _personRefAllowList = <String, String>{
  'lib/features/assets/models/asset_model.dart':
      'AssetTransfer.requestedById : aucun AssetTransfer côté serveur '
      '(ni entité, ni route, ni usage — AssetsService n\'instancie que Asset, '
      'AssetMaintenance et AssetCheckout, déjà migrés en String). '
      'Échafaudage mobile orphelin : à migrer le jour où la route existe.',
};

/// Chemin du fichier propriétaire d'une occurrence (« chemin : declaration »).
String _owner(String offender) => offender.split(' : ').first;

/// Réduit un fichier Dart à son CODE exécutable (commentaires retirés).
///
/// Indispensable : `task_model.dart` documente l'ancien bug dans un commentaire
/// — `// ... l'ancien \`required int userId\`` — qui ferait crier la règle
/// personne si on analysait le texte brut.
String _stripComments(String src) {
  final out = StringBuffer();
  for (final line in src.split('\n')) {
    if (line.trimLeft().startsWith('//')) continue;
    out.writeln(line.replaceAll(RegExp(r'//.*$'), ''));
  }
  return out.toString();
}

/// Chemin POSIX : `_allowList` est écrit en `/`, et sous Windows `File.path`
/// utilise `\`. Sans normalisation, la comparaison n'aboutit jamais.
String _posixOf(FileSystemEntity e) => e.path.replaceAll('\\', '/');

/// Liste les fichiers de MODÈLES.
///
/// BUG CORRIGÉ — le filtre précédent était `path.contains('/models/')`. Sous
/// Windows `File.path` sépare avec `\`, donc ce test était TOUJOURS faux : la
/// liste restait vide et le garde-fou passait dans le vide. Il ne protégeait
/// que sur une CI Linux, et aucun rapport de test n'aurait montré la différence.
/// Un garde-fou qui ne lit pas le code est pire que pas de garde-fou : il donne
/// une fausse assurance. Le test « le garde-fou scanne réellement… » verrouille
/// désormais ce point.
List<File> _modelFiles() {
  final out = <File>[];
  for (final entity in Directory('lib/features').listSync(recursive: true)) {
    if (entity is! File) continue;
    final posix = _posixOf(entity);
    if (!posix.contains('/models/') || !posix.endsWith('.dart')) continue;
    // Les fichiers générés sont la conséquence, pas la cause.
    if (posix.endsWith('.g.dart') || posix.endsWith('.freezed.dart')) continue;
    out.add(entity);
  }
  return out;
}

/// LIMITE ASSUMÉE de la règle `required int id`.
///
/// Elle ne détecte QUE l'écriture historique `required int id` (Freezed). Un
/// `final int id;` en Dart simple lui échappe — et c'est VOLONTAIRE : sur les
/// 21 modèles concernés, `int id` est souvent correct. `Task`, `TaskComment`,
/// `KanbanColumn`, `StreamModel`… sont `GenerationType.IDENTITY` / BIGSERIAL
/// côté serveur, donc `int` est le bon type. Une règle « aucun `int id` »
/// serait donc fausse et bruyante : mieux vaut une règle étroite et vraie
/// qu'une règle large et ignorée.
///
/// La couverture réelle des références PERSONNES — dont le type ne peut PAS
/// être `int` — est assurée par la règle dédiée ci-dessous, qui elle est
/// sound.
void main() {
  test('le garde-fou scanne réellement les modèles (non-régression Windows)', () {
    final scanned = _modelFiles().map(_posixOf).toSet();
    expect(
      scanned,
      isNotEmpty,
      reason: 'Aucun modèle trouvé : le filtre de chemin est cassé '
          '(séparateur `\\` sous Windows).',
    );

    // Complétude : chaque répertoire `models/` doit avoir fourni au moins un
    // fichier. Les répertoires sont découverts par un chemin INDÉPENDANT, donc
    // le test ne devient pas circulaire quand un module est ajouté.
    final modelDirs = Directory('lib/features')
        .listSync(recursive: true)
        .whereType<Directory>()
        .map(_posixOf)
        .where((p) => p.endsWith('/models'))
        .toSet();

    for (final dir in modelDirs) {
      expect(
        scanned.any((p) => p.startsWith('$dir/')),
        isTrue,
        reason: 'Le répertoire `$dir` n\'a fourni aucun fichier au scan : '
            'ce module n\'est pas couvert par le garde-fou.',
      );
    }
  });

  test('aucun modèle ne déclare `int id` hors liste d\'exceptions', () {
    final offenders = <String>[];
    final models = _modelFiles();

    for (final file in models) {
      final source = _stripComments(file.readAsStringSync());
      if (RegExp(r'required\s+int\s+id\b').hasMatch(source)) {
        offenders.add(_posixOf(file));
      }
    }

    // Toute nouvelle occurrence doit être signalée, même si elle n'est pas
    // encore dans la liste : sinon on laisserait repartir la dette.
    final unknown = offenders.where((p) => !_allowList.containsKey(p)).toList()
      ..sort();
    expect(
      unknown,
      isEmpty,
      reason:
          'Modèle(s) avec `int id` non encore recensés :\n'
          '${unknown.join('\n')}\n'
          'Le backend utilise des UUID (250/260 entités). '
          'Ajoute une exception motivée dans _allowList, ou migre le modèle.',
    );

    // La dette ne doit pas augmenter.
    expect(
      offenders.length,
      lessThanOrEqualTo(_allowList.length),
      reason:
          '${offenders.length} modèle(s) en `int id` pour '
          '${_allowList.length} exception(s) : la dette a augmenté.',
    );
  });

  test('aucune référence PERSONNE n\'est déclarée `int` dans un modèle', () {
    // Règle PLUS STRICTE que la précédente, et sans exception possible : côté
    // serveur, `User` et `Person` portent un `UUID id`. Il n\'existe donc
    // AUCUNE entité « personne » à identifiant numérique — un `int patientId`
    // est nécessairement faux, il n\'y a rien à discuter.
    //
    // Elle comble un trou de la règle précédente : `health_model.dart`
    // déclarait `required int id` (capté) MAIS AUSSI `required int patientId`,
    // `required int doctorId`, `required int medicationId` (NON captés). Un
    // module qui n\'aurait eu qu\'un `int patientId` serait passé au travers.
    //
    // C\'est exactement la famille de bogues qui a cassé l\'écran Santé : un
    // UUID lu comme un `int` lève une TypeError au premier chargement.
    final personRef = '(?:patient|user|author|assigned|assignee|mentor|disciple|'
        'doctor|practitioner|createdBy|updatedBy|deletedBy|owner|coordinator|'
        'verifiedBy|approvedBy|uploadedBy|staff|member|donor|beneficiary|'
        'requestedBy|supervisor|accountant)s?Id\\b';

    // `required int xId`, `final int xId` comme `int xId;`. Les suffixes
    // `Count`/`Total` ne sont pas retenus : ils portent une valeur, pas un id.
    //
    // Piège évité : `$personRef` doit être dans une chaîne NON raw. Dans une
    // raw string (`r'…'`) Dart n'interpole pas, et la regex cherchait
    // littéralement le texte « $personRef » — donc ne matchait jamais, sans
    // jamais échouer. Le motif est donc assemblé hors de la raw string.
    final re = RegExp(
      r'\b(?:required\s+|final\s+|const\s+)*int\s+' + personRef,
    );

    final offenders = <String>[];
    for (final file in _modelFiles()) {
      final m = re.firstMatch(_stripComments(file.readAsStringSync()));
      if (m != null) offenders.add('${_posixOf(file)} : ${m.group(0)}');
    }

    final unknown = offenders
        .where((o) => !_personRefAllowList.containsKey(_owner(o)))
        .toList()
      ..sort();

    expect(
      unknown,
      isEmpty,
      reason: 'Référence personne déclarée en `int`, alors que le serveur '
          'sérialise `User.id` en chaîne UUID :\n${unknown.join('\n')}\n'
          'Passe le champ en `String`, ou justifie une exception.',
    );

    // La dette ne doit pas augmenter (ratchet).
    expect(
      offenders.length,
      lessThanOrEqualTo(_personRefAllowList.length),
      reason: '${offenders.length} référence(s) personne en `int` pour '
          '${_personRefAllowList.length} exception(s) : la dette a augmenté.',
    );
  });

  test('chaque exception « référence personne » pointe un fichier existant', () {
    for (final path in _personRefAllowList.keys) {
      expect(
        File(path).existsSync(),
        isTrue,
        reason: 'Exception « $path » obsolète : le fichier n\'existe plus. '
            'Retire-la.',
      );
    }
  });

  test('toute exception de la liste pointe vers un fichier qui existe', () {
    for (final path in _allowList.keys) {
      expect(
        File(path).existsSync(),
        isTrue,
        reason:
            'Exception « $path » obsolète : le fichier n\'existe plus, '
            'mais l\'exception est encore listée. Retire-la.',
      );
    }
  });
}
