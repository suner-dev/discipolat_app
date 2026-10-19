import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// PARAMÉTRISATION PAR ÉGLISE — ratchet mobile (miroir de
/// `frontend/src/__tests__/parametrisationGate.test.ts`).
///
/// Le backend est gelé par la règle d'architecture R7 et le web par le ratchet
/// `parametrisationGate.test.ts`. Le mobile, lui, n'avait aucun gate capable de
/// voir deux familles de défauts :
///
///  (A) une clé de permission consommée par l'app qui N'EXISTE PAS dans le
///      catalogue servi : le contrôle d'accès ne peut jamais être accordé,
///      l'action est morte — sans aucun signal dans la CI ;
///  (B) une clé dont la matrice par défaut ne vaut PAS exactement les rôles du
///      littéral legacy qu'elle remplace : conversion « shortcut » qui élargit
///      ou retire silencieusement un accès à l'installation.
///
/// Les deux sont vérifiés ici contre les MIGRATIONS (vérité du chantier), lues
/// depuis `../backend/src/main/resources/db/migration` — un fixture recopié se
/// désynchroniserait au premier `INSERT` oublié, exactement le défaut qui a
/// survécu des mois sur le web (`DRESS_CODE_CREATE`).
///
/// (C)_fixe en plus le ratchet chiffré du résiduel : toute NOUVELLE garde à
/// littéral de rôle sur un écran de production fait échouer la suite.
class ServedCatalog {
  ServedCatalog(this.keys, this.matrix);

  /// Toutes les clés du catalogue `permissions`.
  final Set<String> keys;

  /// permission → rôles globaux qui la détiennent par défaut.
  final Map<String, Set<String>> matrix;

  /// Union des rôles servis par une liste de clés (any-of, comme les gardes).
  Set<String> servedRoles(List<String> permissionKeys) {
    final out = <String>{};
    for (final k in permissionKeys) {
      out.addAll(matrix[k] ?? const <String>{});
    }
    return out;
  }
}

const _catalogMigrations = <String>[
  'V135__create_multi_tenant_core_tables.sql',
  'V136__add_membership_scope_and_role_fk.sql',
  'V258__parametrisation_permissions_par_eglise.sql',
  'V259__parametrisation_permissions_suite_phase3.sql',
  'V260__parametrisation_admin_events_read.sql',
  'V261__parametrisation_cles_ui_routes.sql',
  'V262__parametrisation_cles_ui_intra_pages.sql',
  'V270__parametrisation_cles_ui_residuel_dashboard.sql',
];

final _keyRe = RegExp(r'^[A-Z][A-Z0-9_]*$');

/// Jetons d'un littéral SQL (`''` = apostrophe échappée).
List<String> _sqlTokens(String chunk) {
  final out = <String>[];
  var i = 0;
  while (i < chunk.length) {
    if (chunk[i] != "'") {
      i++;
      continue;
    }
    var j = i + 1;
    final buf = StringBuffer();
    while (j < chunk.length) {
      if (chunk[j] == "'") {
        if (j + 1 < chunk.length && chunk[j + 1] == "'") {
          buf.write("'");
          j += 2;
          continue;
        }
        break;
      }
      buf.write(chunk[j]);
      j++;
    }
    out.add(buf.toString());
    i = j + 1;
  }
  return out;
}

/// Contenu de chaque tuple parenthèse d'un statement (du plus externe au plus
/// interne) — indispensable : un libellé contenant `''` ou des parenthèses
/// casse toute regex ligne à ligne.
List<String> _tuplesOf(String statement) {
  final out = <String>[];
  final stack = <int>[];
  var inString = false;
  for (var i = 0; i < statement.length; i++) {
    final c = statement[i];
    if (inString) {
      if (c == "'") {
        if (i + 1 < statement.length && statement[i + 1] == "'") {
          i++;
          continue;
        }
        inString = false;
      }
      continue;
    }
    if (c == "'") {
      inString = true;
      continue;
    }
    if (c == '(') {
      stack.add(i);
      continue;
    }
    if (c == ')' && stack.isNotEmpty) {
      out.add(statement.substring(stack.removeLast() + 1, i));
    }
  }
  return out;
}

List<String> _insertStatements(String sql, String table) {
  final re = RegExp('INSERT\\s+INTO\\s+$table\\b[\\s\\S]*?(?<!\\\\);',
      caseSensitive: false);
  return re.allMatches(sql).map((m) => m.group(0)!).toList();
}

ServedCatalog loadServedCatalog() {
  var dir = Directory('../backend/src/main/resources/db/migration');
  if (!dir.existsSync()) {
    dir = Directory('backend/src/main/resources/db/migration');
  }
  if (!dir.existsSync()) {
    throw StateError(
        'migrations backend introuvables depuis mobile/ ($dir) — layout change ?');
  }

  final keys = <String>{};
  final matrix = <String, Set<String>>{};
  for (final file in _catalogMigrations) {
    final f = File('${dir.path}/$file');
    if (!f.existsSync()) {
      throw StateError(
          'migration de catalogue absente : $file (liste du test a mettre a jour)');
    }
    final sql = f.readAsStringSync();
    for (final stmt in _insertStatements(sql, 'permissions')) {
      for (final tuple in _tuplesOf(stmt)) {
        final toks = _sqlTokens(tuple);
        if (toks.isEmpty) continue;
        final first = toks.first.trim();
        if (_keyRe.hasMatch(first)) keys.add(first);
      }
    }
    for (final stmt in _insertStatements(sql, 'role_permissions')) {
      for (final tuple in _tuplesOf(stmt)) {
        final toks = _sqlTokens(tuple).map((t) => t.trim()).toList();
        if (toks.length < 2) continue;
        if (_keyRe.hasMatch(toks[0]) && _keyRe.hasMatch(toks[1])) {
          matrix.putIfAbsent(toks[1], () => <String>{}).add(toks[0]);
        }
      }
    }
  }
  return ServedCatalog(keys, matrix);
}

/// Rôle de l'ACTEUR dans un prédicat `legacy:` — sert à prouver la parité (B).
Set<String> _legacyRoles(String body) {
  final out = <String>{};
  final comparisons = RegExp(
      r'''(?:activeRole|_activeRole|\brole)\s*(?:===|!==|==|!=)\s*['"]([A-Z][A-Z0-9_]*)['"]''');
  for (final m in comparisons.allMatches(body)) {
    out.add(m.group(1)!);
  }
  final lists = RegExp(r'''hasActiveRole\(\s*\[([^\]]*)\]''');
  for (final m in lists.allMatches(body)) {
    for (final k in RegExp(r'''['"]([A-Z][A-Z0-9_]*)['"]''').allMatches(m.group(1)!)) {
      out.add(k.group(1)!);
    }
  }
  out.remove('INCONNU');
  return out;
}

/// Corps du prédicat `() => …` / `legacy: () => …` d'un `canServed(`.
String _legacyBody(String text, int from) {
  final arrow = text.indexOf('=>', from);
  if (arrow < 0) return '';
  var depth = 0;
  var instr = false;
  for (var i = arrow + 2; i < text.length; i++) {
    final c = text[i];
    if (instr) {
      if (c == "'" || c == '"') {
        if (text[i + 1] == c) {
          i++;
          continue;
        }
        instr = false;
      }
      continue;
    }
    if (c == "'" || c == '"') {
      instr = true;
      continue;
    }
    if (c == '(' || c == '[' || c == '{') {
      depth++;
    } else if (c == ')' || c == ']' || c == '}') {
      if (depth == 0) return text.substring(arrow + 2, i);
      depth--;
    }
  }
  return text.substring(arrow + 2);
}

/// Source de production (hors tests).
List<File> _prodSources() {
  final dir = Directory('lib');
  return dir
      .listSync(recursive: true)
      .whereType<File>()
      .where((f) => f.path.endsWith('.dart') && !f.path.endsWith('_test.dart'))
      .toList();
}

String _read(File f) {
  final raw = f.readAsStringSync();
  // commentaires neutralisés (numérotation des lignes préservée)
  return raw
      .replaceAllMapped(RegExp(r'/\*[\s\S]*?\*/'),
          (m) => m.group(0)!.replaceAll(RegExp(r'[^\n]'), ' '))
      .replaceAll(RegExp(r'//[^\n]*'), '');
}

void main() {
  // chargé à la demande : `loadServedCatalog` ne doit pas s'exécuter hors `test()`
  //(sinon `expect` lève OutsideTestException).
  ServedCatalog? cache;
  ServedCatalog catalogOf() => cache ??= loadServedCatalog();

  test('ratchet mobile : le catalogue servi est bien lu', () {
    expect(catalogOf().keys.length, greaterThan(400));
  });

  test('(A) toute cle consommee par le mobile existe dans le catalogue servi', () {
    final missing = <String>[];
    for (final file in _prodSources()) {
      final text = _read(file);
      for (final m in RegExp(r"""canServed\(\s*['"]([A-Z][A-Z0-9_]{2,})['"]""").allMatches(text)) {
        if (!catalogOf().keys.contains(m.group(1))) {
          missing.add('${m.group(1)} — ${file.path}');
        }
      }
      for (final m in RegExp(r"""hasPermission\(\s*['"]([A-Z][A-Z0-9_]{2,})['"]""").allMatches(text)) {
        if (!catalogOf().keys.contains(m.group(1))) {
          missing.add('${m.group(1)} — ${file.path}');
        }
      }
    }
    expect(missing, isEmpty,
        reason: 'cles consommees absentes du catalogue servi : action morte');
  });

  test('(A) le registre routePermissions ne consomme que des cles servies', () {
    final app = File('lib/app.dart').readAsStringSync();
    final missing = <String>[];
    final block = RegExp(r'routePermissions\s*=\s*\{([\s\S]*?)\n\};').firstMatch(app);
    expect(block, isNotNull);
    for (final m
        in RegExp(r"""'[^']+'\s*:\s*\[([^\]]*)\]""").allMatches(block!.group(1)!)) {
      for (final k in RegExp(r"""['"]([A-Z][A-Z0-9_]*)['"]""").allMatches(m.group(1)!)) {
        if (!catalogOf().keys.contains(k.group(1))) {
          missing.add('${k.group(1)}');
        }
      }
    }
    expect(missing, isEmpty);
  });

  test('(B) canServed : matrice de la cle identique aux roles du repli legacy', () {
    final drift = <String>[];
    for (final file in _prodSources()) {
      final text = _read(file);
      for (final m
          in RegExp(r"""canServed\(\s*['"]([A-Z][A-Z0-9_]{2,})['"]""").allMatches(text)) {
        final keys = <String>[m.group(1)!];
        final legacy = _legacyRoles(_legacyBody(text, m.start));
        if (legacy.isEmpty) continue;
        final servedSet = catalogOf().servedRoles(keys);
        final extra = servedSet.difference(legacy);
        final lost = legacy.difference(servedSet);
        if (extra.isNotEmpty || lost.isNotEmpty) {
          drift.add('${file.path} ${keys.join('|')} '
              '+${extra.join(',')} -${lost.join(',')}');
        }
      }
    }
    expect(drift, isEmpty, reason: 'canServed non paritaire (matrice servie != litteral legacy)');
  });

  test('(C) ratchet chiffre : aucune nouvelle garde a litteral de role', () {
    // Plafond committé du résiduel toléré. Toute conversion fait baisser le
    // compte ; on met alors la constante à jour. La dette ne croît pas seule.
    // Site toléré (1) : `users_list_screen._buildWorkloadSection` — le littéral
    // ne garde rien, il LIBELLE une portée que le serveur calcule lui-même
    // (`/departments/by-responsable/{id}` pour un responsable, tous les
    // départements sinon). Le libellé doit donc suivre le rôle actif, pas une
    // permission : le rendre « servi » mentirait quand l'église accorde la
    // capacité à un rôle personnalisé.
    const residualBaseline = 1;
    final residuals = <String>[];
    final decision = RegExp(
        r'''(?:final|var|bool)\s+(?:can|has|is|show|allow)[A-Za-z_]*\s*=\s*[^;]*''');
    final roleLiteral = RegExp(
        r'''(?:activeRole|_activeRole|\brole)\s*(?:==|!=)\s*['"][A-Z][A-Z0-9_]*['"]|hasActiveRole\(\s*\[''');
    for (final file in _prodSources()) {
      final lines = _read(file).split('\n');
      for (var i = 0; i < lines.length; i++) {
        final line = lines[i];
        if (!decision.hasMatch(line)) continue;
        if (!roleLiteral.hasMatch(line)) continue;
        // garde servie-first : le litteral est le repli `legacy`, pas une garde
        if (RegExp(r'canServed\(').hasMatch(line)) continue;
        residuals.add('${file.path}:${i + 1}');
      }
    }
    expect(residuals.length <= residualBaseline, isTrue,
        reason: 'nouvelles gardes a litteral de role : passer par canServed(<CLE>, legacy: ...)\n'
            '${residuals.join('\n')}');
  });
}
