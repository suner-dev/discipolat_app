/**
 * PARAMÉTRISATION PAR ÉGLISE — lecture de la vérité serveur (catalogue servi +
 * matrice rôle→permission par défaut) directement dans les migrations Flyway.
 *
 * Pourquoi un parseur plutôt qu'un fixture figé : la vérité du chantier est la
 * migration. Un catalogue recopié dans un test se désynchronise au premier
 * `INSERT INTO permissions` oublié, et c'est exactement le défaut qui a fait
 * qu'une page web consomme `DRESS_CODE_CREATE` (clé absente du catalogue) :
 * ses trois boutons n'ont jamais été rendus, pour aucun rôle. Ce module rend
 * l'invariant vérifiable en CI sur l'arbre réellement commité.
 *
 * Sémantique couverte :
 *  - `INSERT INTO permissions … VALUES ('CLÉ','libellé','CAT')` → catalogue ;
 *  - `INSERT INTO role_permissions … VALUES ('RÔLE','CLÉ')` → matrice ;
 *  - `INSERT INTO roles …` → rôles globaux créés (une paire dont le rôle n'a pas
 *    de ligne globale est MORTE en production : le JOIN `tenant_id IS NULL` du
 *    pont d'équivalence la filtre — cf. V259 §1).
 *
 * Les appariements sont faits par analyse de parenthèses (et non par regex
 * naïve sur la ligne) : un libellé contenant `''` — apostrophe SQL échappée —
 * ou des parenthèses ne casse pas la lecture. C'est le piège qui rendait le
 * générateur de seed H2 (`gen_h2_seed.py`) et ses regex `[^']+` silencieusement
 * aveugles sur ces tuples.
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, resolve } from 'node:path';

export type ServedCatalog = {
  /** Toutes les clés du catalogue `permissions`. */
  keys: Set<string>;
  /** permission → rôles globaux qui la détiennent par défaut. */
  matrix: Map<string, Set<string>>;
  /** Clés du catalogue sans aucune paire par défaut. */
  orphaned: string[];
};

/** migrations qui portent le catalogue servi + la matrice par défaut. */
const CATALOG_MIGRATIONS = [
  // Catalogue de base (multi-tenant V3) — les clés `BRANDING_*`, `ROLE_*`,
  // `PERMISSION_ASSIGN`, `MODULES_*`, `ORG_NODE_*`… y sont nées et sont
  // réellement servies : les omettre ferait crier des faux positifs.
  'V135__create_multi_tenant_core_tables.sql',
  'V136__add_membership_scope_and_role_fk.sql',
  // Catalogue paramétrable par église (V258) + phases 3→7.
  'V258__parametrisation_permissions_par_eglise.sql',
  'V259__parametrisation_permissions_suite_phase3.sql',
  'V260__parametrisation_admin_events_read.sql',
  'V261__parametrisation_cles_ui_routes.sql',
  'V262__parametrisation_cles_ui_intra_pages.sql',
  'V270__parametrisation_cles_ui_residuel_dashboard.sql',
];

const KEY_RE = /^[A-Z][A-Z0-9_]*$/;

/** Contenu de chaque tuple `( … )` d'un statement, du plus externe au plus interne. */
function tuplesOf(statement: string): string[] {
  const out: string[] = [];
  const stack: number[] = [];
  let inString = false;
  for (let i = 0; i < statement.length; i++) {
    const c = statement[i];
    if (inString) {
      if (c === "'") {
        if (statement[i + 1] === "'") { i++; continue; }
        inString = false;
      }
      continue;
    }
    if (c === "'") { inString = true; continue; }
    if (c === '(') { stack.push(i); continue; }
    if (c === ')' && stack.length) { out.push(statement.slice(stack.pop()! + 1, i)); }
  }
  return out;
}

/** Jetons d'un litteral SQL (`''` = apostrophe échappée, `"` = identifiant). */
function sqlTokens(chunk: string): string[] {
  const out: string[] = [];
  let i = 0;
  while (i < chunk.length) {
    const c = chunk[i];
    if (c === "'") {
      let j = i + 1;
      let value = '';
      while (j < chunk.length) {
        if (chunk[j] === "'") {
          if (chunk[j + 1] === "'") { value += "'"; j += 2; continue; }
          break;
        }
        value += chunk[j++];
      }
      out.push(value);
      i = j + 1;
      continue;
    }
    i++;
  }
  return out;
}

/** Statements `INSERT INTO <table> … ;` d'un fichier de migration. */
function insertStatements(sql: string, table: string): string[] {
  const re = new RegExp(`INSERT\\s+INTO\\s+${table}\\b[\\s\\S]*?(?<!\\\\);`, 'gi');
  return sql.match(re) ?? [];
}

function migrationsDir(): string {
  // frontend/ → racine du dépôt → backend/src/main/resources/db/migration
  return resolve(process.cwd(), '..', 'backend', 'src', 'main', 'resources', 'db', 'migration');
}

/** Vrai si le chemin existe (le test doit échouer bruyamment si le layout change). */
export function migrationsDirExists(): boolean {
  try { return statSync(migrationsDir()).isDirectory(); } catch { return false; }
}

export function loadServedCatalog(): ServedCatalog {
  const dir = migrationsDir();
  const present = readdirSync(dir);
  for (const m of CATALOG_MIGRATIONS) {
    if (!present.includes(m)) {
      throw new Error(`migration de catalogue absente : ${m} (liste du test à mettre à jour)`);
    }
  }
  const keys = new Set<string>();
  const matrix = new Map<string, Set<string>>();

  for (const file of CATALOG_MIGRATIONS) {
    const sql = readFileSync(join(dir, file), 'utf8');
    for (const stmt of insertStatements(sql, 'permissions')) {
      for (const tuple of tuplesOf(stmt)) {
        const first = sqlTokens(tuple)[0]?.trim();
        if (first && KEY_RE.test(first)) keys.add(first);
      }
    }
    for (const stmt of insertStatements(sql, 'role_permissions')) {
      for (const tuple of tuplesOf(stmt)) {
        const [role, perm] = sqlTokens(tuple).map((t) => t.trim());
        if (role && perm && KEY_RE.test(role) && KEY_RE.test(perm)) {
          if (!matrix.has(perm)) matrix.set(perm, new Set());
          matrix.get(perm)!.add(role);
        }
      }
    }
  }
  const orphaned = [...keys].filter((k) => !matrix.get(k)?.size).sort();
  return { keys, matrix, orphaned };
}

/** Union des rôles servis par une liste de clés (any-of, comme les gardes). */
export function servedRoles(catalog: ServedCatalog, keys: readonly string[]): Set<string> {
  const out = new Set<string>();
  for (const k of keys) for (const r of catalog.matrix.get(k) ?? []) out.add(r);
  return out;
}
