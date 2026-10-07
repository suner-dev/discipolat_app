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
  'lib/features/streaming/models/stream_model.dart':
      'À migrer : LiveStreamController attend Long sur /streams/{id}, écart à trancher',
  'lib/features/health/models/health_model.dart':
      'À migrer : module sans contrôleur événements encore — arbitrage préalable requis',
  // finance_model.dart MIGRÉ (V236) : identifiants String (UUID), champs
  // alignés sur les vues FinanceService — l'exception a été retirée ici.
  // discipleship_model.dart MIGRÉ : les refs personnes (createdById,
  // discipleId, mentorId, verifiedById) sont passées en String (UUID serveur
  // vérifié, le serveur fait UUID.fromString). Les ids structurelles restent
  // volontairement int : journey/stage/progress/assignment/meeting sont
  // BIGSERIAL (Long) côté DiscipleshipService — ce n'est pas de la dette.
  'lib/features/tasks/models/task_model.dart':
      'Module orphelin : aucun contrôleur /api/v1/tasks (voir NEED-HELP-TASKS)',
  // message_model.dart MIGRÉ : toutes les entités messages sont UUID côté
  // serveur (Conversation/ConversationMessage/GroupConversation,
  // GenerationType.UUID vérifié) → ids et refs personnes (senderId,
  // otherUserId…) sont des String ici, comme ConversationResponse/
  // MessageResponse/GroupConversationResponse. Le service consomme les
  // endpoints réels /api/v1/messages/** (parity avec MessagesPage.tsx web).
};

void main() {
  test('aucun modèle ne déclare `int id` hors liste d\'exceptions', () {
    final offenders = <String>[];
    final models = Directory('lib/features')
        .listSync(recursive: true)
        .whereType<File>()
        .where((f) => f.path.contains('/models/') && f.path.endsWith('.dart'))
        // Les fichiers générés sont la conséquence, pas la cause.
        .where((f) => !f.path.endsWith('.g.dart') && !f.path.endsWith('.freezed.dart'));

    for (final file in models) {
      final source = file.readAsStringSync();
      if (RegExp(r'required\s+int\s+id\b').hasMatch(source)) {
        offenders.add(file.path);
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
