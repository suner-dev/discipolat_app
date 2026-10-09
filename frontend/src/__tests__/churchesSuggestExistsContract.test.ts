import { describe, it, expect } from 'vitest';
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

/**
 * LOT 1 §GLISE-D'ABORD (T1.7, [CONTRAT]) — parité web ↔ mobile ↔ serveur.
 *
 * <p>Les deux endpoints NEUFS du picker (« église d'abord ») —
 * {@code GET /api/v1/public/churches/suggest} et
 * {@code GET /api/v1/public/churches/exists} — constituent un CONTRAT figé
 * (§6 du plan). Trois couches doivent le parler à l'identique :
 * <ul>
 *   <li><b>serveur</b> — {@code PublicChurchesController.java} (qui produit les
 *       corps de réponse) + son test Spring (qui les vérifie à l'exécution) ;</li>
 *   <li><b>web</b> — {@code ChurchPicker.tsx} (qui lit les mêmes champs) ;</li>
 *   <li><b>mobile</b> — tout futur consommateur Dart de ces routes (T1.6).</li>
 * </ul>
 *
 * <p>Discipline « déjà pratiquée dans ce dépôt » (cf.
 * {@code mobile/test/contract/identifier_contract_test.dart}) : un garde-fou qui
 * LIT les sources des trois couches et échoue dès qu'une dérive de nom de champ
 * ou de chemin apparaît. Ici, de surcroît, il interdit toute FUITE de PII hors
 * de la liste blanche (R2/R3), y compris par une couche mobile future.</p>
 *
 * <p>Le mobile n'a PAS encore de consommateur (T1.6 est en aval de T1.7 — c'est
 * l'objet même de ce verrou : geler le contrat avant que le mobile ne le
 * consomme). La branche « mobile » est donc un CRÉMAILLON : elle passe tant
 * qu'aucun consommateur n'existe, et échoue à la seconde où l'on ajoute un
 * écran Dart qui emploierait un chemin ou un champ divergent.</p>
 */

// ---- Racine du dépôt : on remonte depuis cwd jusqu'au dossier qui porte
// backend/src/main/java (robust quel que soit l'endroit où vitest est lancé). ----
function findRepoRoot(start: string): string {
  let dir = resolve(start);
  for (let i = 0; i < 8; i += 1) {
    if (existsSync(join(dir, 'backend/src/main/java'))) return dir;
    dir = dirname(dir);
  }
  throw new Error('Racine du dépôt introuvable (backend/src/main/java absent).');
}
const REPO = findRepoRoot(process.cwd());
function readRepo(rel: string): string {
  return readFileSync(join(REPO, rel), 'utf8');
}

const CONTROLLER = 'backend/src/main/java/com/discipolat/modules/network/api/PublicChurchesController.java';
const CONTROLLER_TEST = 'backend/src/test/java/com/discipolat/modules/network/api/PublicChurchesSuggestExistsTest.java';
const PICKER = 'frontend/src/components/auth/ChurchPicker.tsx';
const MOBILE_LIB = 'mobile/lib';

// ---- Le contrat, une seule fois, comme source de vérité du test. ----
const SUGGEST_TOP_KEYS = new Set(['total', 'items']);
const SUGGEST_ITEM_KEYS = new Set(['name', 'slug', 'city', 'country']);
const EXISTS_KEYS = new Set(['found', 'slug', 'name']);
const SUGGEST_PATH = '/public/churches/suggest';
const EXISTS_PATH = '/public/churches/exists';

/** Champs JAMAIS admis dans la réponse des deux endpoints (PII / vitrine élargie / interne). */
const FORBIDDEN = [
  'tenantId', 'tenant_id', 'email', 'phone', 'password', 'address',
  'denomination', 'website', 'description', 'listedAt', 'slogan',
  'logoUrl', 'coverUrl', 'pastor', 'member', 'members',
];

/** Extrait le bloc équilibré {@code { ... }} qui suit une ancre (le premier `{`). */
function balancedBlock(src: string, anchor: string): string {
  const at = src.indexOf(anchor);
  if (at < 0) throw new Error(`Ancre introuvable : ${anchor}`);
  const open = src.indexOf('{', at);
  if (open < 0) throw new Error(`Ouverture de bloc introuvable après : ${anchor}`);
  let depth = 0;
  for (let k = open; k < src.length; k += 1) {
    const c = src[k];
    if (c === '{') depth += 1;
    else if (c === '}') {
      depth -= 1;
      if (depth === 0) return src.slice(open, k + 1);
    }
  }
  throw new Error(`Bloc non équilibré après : ${anchor}`);
}

/** Clés JSON émises par un bloc Java via {@code x.put("K", …)} ou {@code Map.of("K", …)}. */
function javaEmittedKeys(block: string): Set<string> {
  const keys = new Set<string>();
  const re = /(?:\.put|Map\.of)\s*\(\s*"([A-Za-z_][A-Za-z0-9_]*)"/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(block)) !== null) keys.add(m[1]);
  return keys;
}

/** Clés d'un type TS inline `{ a: T; b?: T }` (sans les types). */
function tsTypeKeys(body: string): Set<string> {
  const keys = new Set<string>();
  const re = /([A-Za-z_][A-Za-z0-9_]*)\??\s*:/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(body)) !== null) keys.add(m[1]);
  return keys;
}

function sameSet(a: Set<string>, b: Set<string>): boolean {
  return a.size === b.size && [...a].every((k) => b.has(k));
}

/** Liste récursive des fichiers .dart sous un chemin relatif du dépôt. */
function dartFiles(relDir: string): string[] {
  const out: string[] = [];
  const walk = (dir: string) => {
    for (const name of readdirSync(dir)) {
      const child = join(dir, name);
      if (name.endsWith('.dart')) out.push(child);
      else if (statSync(child).isDirectory() && name !== 'build') walk(child);
    }
  };
  walk(join(REPO, relDir));
  return out;
}

describe('LOT 1 §GLISE-D\'ABORD (T1.7) — contrat /suggest & /exists figé sur les trois couches', () => {
  describe('serveur (PublicChurchesController.java)', () => {
    const src = readRepo(CONTROLLER);
    const suggestBlock = balancedBlock(src, 'public ResponseEntity<Map<String, Object>> suggest(');
    const existsBlock = balancedBlock(src, 'public ResponseEntity<Map<String, Object>> exists(');
    const itemBlock = balancedBlock(src, 'static Map<String, Object> toSuggestView(');

    it('expose bien les deux nouveaux chemins (sans toucher à list())', () => {
      expect(src).toContain('@GetMapping("/suggest")');
      expect(src).toContain('@GetMapping("/exists")');
      // list() reste le contrat historique { total, content } : rien n'est venu le modifier.
      const listBlock = balancedBlock(src, 'public ResponseEntity<Map<String, Object>> list(');
      expect(javaEmittedKeys(listBlock)).toEqual(new Set(['total', 'content']));
    });

    it('/suggest émet exactement { total, items } au niveau racine', () => {
      expect(javaEmittedKeys(suggestBlock)).toEqual(SUGGEST_TOP_KEYS);
    });

    it('les items /suggest sont la projection minimale { name, slug, city, country }', () => {
      expect(javaEmittedKeys(itemBlock)).toEqual(SUGGEST_ITEM_KEYS);
    });

    it('/exists émet un sous-ensemble de { found, slug, name }', () => {
      const emitted = javaEmittedKeys(existsBlock);
      for (const k of emitted) expect(EXISTS_KEYS.has(k), `clé hors liste : ${k}`).toBe(true);
      expect(emitted.has('found'), 'found doit toujours être présent').toBe(true);
    });

    it('aucune PII / vitrine élargie ne fuit dans /suggest ou /exists', () => {
      const blocks = suggestBlock + itemBlock + existsBlock;
      for (const bad of FORBIDDEN) {
        expect(blocks, `champ interdit présent : ${bad}`).not.toContain(`"${bad}"`);
      }
    });

    it('les deux endpoints sont rate-limités + no-store (anti-énumération R3)', () => {
      expect(suggestBlock).toContain('tryConsumeChurchSuggest');
      expect(existsBlock).toContain('tryConsumeChurchExists');
      expect(suggestBlock).toContain('CacheControl.noStore()');
      expect(existsBlock).toContain('CacheControl.noStore()');
    });

    it('le test Spring du serveur épingle le même contrat (total/items/found)', () => {
      const t = readRepo(CONTROLLER_TEST);
      expect(t).toContain('$.total');
      expect(t).toContain('$.items');
      expect(t).toContain('$.found');
      // Il vérifie aussi l'absence des mêmes interdits (cohérence garde-fou ↔ preuve).
      for (const bad of ['tenant_id', 'denomination', 'website']) {
        expect(t, `le test serveur devrait écarter ${bad}`).toContain(bad);
      }
    });
  });

  describe('web (ChurchPicker.tsx)', () => {
    const src = readRepo(PICKER);

    it('parle les deux mêmes chemins, params { q }', () => {
      expect(src).toContain(`'${SUGGEST_PATH}'`);
      expect(src).toContain(`'${EXISTS_PATH}'`);
      expect(src).toContain('params: { q }');
    });

    it('sa projection attendue est { name, slug, city, country }', () => {
      const m = /type SuggestItem = \{([^}]*)\}/.exec(src);
      expect(m, 'type SuggestItem introuvable').not.toBeNull();
      expect(sameSet(tsTypeKeys(m![1]), SUGGEST_ITEM_KEYS)).toBe(true);
    });

    it('la réponse /exists est lue comme { found, slug, name }', () => {
      const m = /api\.get<\{\s*found[^}]*\}>/.exec(src);
      expect(m, 'générique api.get /exists introuvable').not.toBeNull();
      expect(sameSet(tsTypeKeys(m![0].replace(/api\.get<|\}>/g, '')), EXISTS_KEYS)).toBe(true);
    });

    it('ne lit AUCUN champ interdit (pas d\'invention côté client)', () => {
      for (const bad of FORBIDDEN) {
        expect(src, `le picker lit un champ interdit : ${bad}`).not.toContain(bad);
      }
    });
  });

  describe('mobile (ratchet — pas de consommateur divergent)', () => {
    it('tout écran Dart consommant ces routes respecte le contrat gelé', () => {
      const offenders: string[] = [];
      for (const file of dartFiles(MOBILE_LIB)) {
        const rel = file.slice(REPO.length + 1);
        const body = readFileSync(file, 'utf8');
        const touchesSuggest = body.includes('churches/suggest');
        const touchesExists = body.includes('churches/exists');
        if (!touchesSuggest && !touchesExists) continue;
        // Chemins attendus à l'identique (le préfixe /public/ doit être présent).
        if (touchesSuggest && !body.includes('public/churches/suggest')) {
          offenders.push(`${rel} : chemin /suggest non conforme`);
        }
        if (touchesExists && !body.includes('public/churches/exists')) {
          offenders.push(`${rel} : chemin /exists non conforme`);
        }
        // Aucun champ interdit ne doit apparaître dans la lecture.
        for (const bad of FORBIDDEN) {
          if (body.includes(bad)) offenders.push(`${rel} : lit un champ interdit (${bad})`);
        }
      }
      // T1.6 en aval : tant qu'aucun consommateur n'existe, ce test verrouille
      // l'absence de dérive. S'il en apparaît un, il doit être conforme.
      expect(offenders, offList(offenders)).toEqual([]);
    });
  });
});

function offList(o: string[]): string {
  return o.length
    ? `Dérive de contrat mobile :\n${o.join('\n')}`
    : 'aucune dérive détectée';
}
