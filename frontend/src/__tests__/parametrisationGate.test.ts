import { describe, it, expect } from 'vitest';
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { ROUTE_PERMISSIONS, ROUTE_ROLES } from '@/lib/routeAccess';
import { loadServedCatalog, servedRoles, migrationsDirExists } from './helpers/servedCatalog';
import { stripComments, balancedFrom } from './helpers/sourceScan';

/**
 * RATCHET PARAMÉTRISATION PAR ÉGLISE — le verrou qui manquait au chantier.
 * ---------------------------------------------------------------------
 * Le backend est gelé par la règle d'architecture R7 (`plafond|R7|0` : zéro
 * `@PreAuthorize` à littéral de rôle) et le web par
 * `routeAccessPermissions.test.ts` (registre de routes ↔ catalogue). Mais AUCUN
 * gate ne vérifiait, en CI :
 *
 *  (A) que toute clé de permission consommée par le client EXISTE dans le
 *      catalogue servi — c'est le trou par lequel `DressCodePage` consommait
 *      `DRESS_CODE_CREATE|UPDATE|ARCHIVE`, clés jamais créées : ses trois
 *      boutons ne pouvaient pas être rendus, pour aucun rôle, et aucun test ne
 *      le voyait ;
 *  (B) que la matrice SERVIE d'une clé est exactement l'ensemble des rôles que
 *      admet le littéral legacy qu'elle remplace (une conversion « shortcut »
 *      élargit ou retire silencieusement un accès à l'installation) ;
 *  (C) qu'aucune NOUVELLE garde d'UI à littéral de rôle n'apparaît (ratchet à
 *      liste blanche : le résiduel assumé est nommé, tout le reste échoue).
 *
 * Les trois règles sont additives : aucune ne peut retirer un droit existant.
 */
const SRC = resolve(process.cwd(), 'src');
const catalog = loadServedCatalog();
const KEY_IN_LIST = /['"]([A-Z][A-Z0-9_]{2,})['"]/g;
/**
 * Appel d'un garde servi. ⚠ `can`/`canAny` sont APPELÉS NU (hook déstructuré :
 * `const { can } = useServedAccess()` puis `can('CLÉ', …)`) — pas en méthode
 * `.can(`. Une regex exigeant le point ne matche donc AUCUN site de
 * production et rend les règles (A)/(B) vacantes : c'est exactement le défaut
 * corrigé ici. Le lookbehind exclut `.` et les caractères de mot pour ne pas
 * compter deux fois un même appel ni attraper un suffixe (`scan(`).
 */
const SERVED_CALL_RE = /(?<![\w$])(?:can|canAny|hasPermission)\(\s*(?:['"]([A-Z][A-Z0-9_]{2,})['"]|\[([^\]]*)\])/g;
/**
 * littéral de rôledecisionnaire. Volontairement restreint aux expressions qui
 * désignent le rôle de l'ACTEUR (`activeRole`, `user.role`, `hasRole(...)`) :
 * un `a.role === 'CHEF'` de tableau ou un libellé de badge ne sont pas des
 * décisions d'accès et ne doivent pas bruire le ratchet.
 */
const ROLE_LITERAL =
  /(?:activeRole|_activeRole|user\?\.role|user\.role|user\?\.activeRole|user\.activeRole|currentRole)\s*(?:===|!==|==|!=)\s*['"][A-Z][A-Z0-9_]*['"]|hasRole\(\s*['"][A-Z][A-Z0-9_]*['"]|roles\??\.includes\(\s*['"][A-Z][A-Z0-9_]*['"]|hasActiveRole\(/;

const lineOf = (text: string, index: number) => text.slice(0, index).split('\n').length;

/** Fichiers de production, commentaires retirés, une entrée par chemin relatif. */
const files: { rel: string; text: string }[] = (() => {
  const acc: { rel: string; text: string }[] = [];
  const walk = (dir: string) => {
    for (const entry of readdirSync(dir)) {
      if (entry === '__tests__' || entry === 'i18n') continue;
      const full = join(dir, entry);
      if (statSync(full).isDirectory()) { walk(full); continue; }
      if (!/\.(ts|tsx)$/.test(entry)) continue;
      acc.push({ rel: relative(process.cwd(), full), text: stripComments(readFileSync(full, 'utf8')) });
    }
  };
  walk(SRC);
  return acc;
})();

/**
 * Rôles d'un prédicat `legacy:` — extraction STRUCTURELLE, jamais « tous les
 * tokens en majuscules » : une garde comme
 * `hasPermission('BRANDING_UPDATE') && locale === 'EUR'` contient `'EUR'`,
 * `'XAF'`, `'PENDING'`… qui ne sont pas des rôles. Les confondre produisait des
 * faux positifs (et donc, en pratique, des gens qui suppriment la garde pour
 * faire passer le build — le pire scénario pour un ratchet).
 *
 * Seul un littéral EN POSITION DE RÔLE est retenu : comparaison à
 * `activeRole`/`user.role`, ou membre d'un tableau passé à `.includes`/
 * `hasActiveRole`/`hasAnyRole`.
 */
function legacyRoles(body: string): Set<string> {
  const out = new Set<string>();
  const add = (re: RegExp) => { for (const m of body.matchAll(re)) out.add(m[1]); };
  add(/(?:activeRole|_activeRole|user\?\.role|user\.role|user\?\.activeRole|user\.activeRole|currentRole)\s*(?:===|!==|==|!=)\s*['"]([A-Z][A-Z0-9_]*)['"]/g);
  add(/roles\??\.includes\(\s*['"]([A-Z][A-Z0-9_]*)['"]/g);
  add(/hasRole\(\s*['"]([A-Z][A-Z0-9_]*)['"]/g);
  for (const m of body.matchAll(/hasRole\(\s*((?:['"][A-Z][A-Z0-9_]*['"]\s*,?\s*)+)\)/g)) {
    for (const k of m[1].matchAll(KEY_IN_LIST)) out.add(k[1]);
  }
  for (const m of body.matchAll(/(?:hasActiveRole|hasAnyRole)\(\s*\[([^\]]*)\]/g)) {
    for (const k of m[1].matchAll(KEY_IN_LIST)) out.add(k[1]);
  }
  for (const m of body.matchAll(/\[\s*((?:['"][A-Z][A-Z0-9_]*['"]\s*,\s*)+['"][A-Z][A-Z0-9_]*['"])\s*\]\s*\.includes\(/g)) {
    for (const k of m[1].matchAll(KEY_IN_LIST)) out.add(k[1]);
  }
  out.delete('INCONNU');
  return out;
}

/** Corps du prédicat `() => …` d'un `can(` / `canAny(`. */
function legacyBody(text: string, from: number): string {
  const arrow = text.indexOf('=>', from);
  return arrow < 0 ? '' : balancedFrom(text, arrow + 2);
}

describe('ratchet parametrisation — catalogue servi', () => {
  it('les migrations du catalogue sont bien présentes (layout backend stable)', () => {
    expect(migrationsDirExists()).toBe(true);
    expect(catalog.keys.size).toBeGreaterThan(400);
  });

  it('(A) TOUTE clé consommée par le client existe dans le catalogue servi', () => {
    const missing: string[] = [];
    for (const { rel, text } of files) {
      const push = (key: string, index: number) => {
        if (!catalog.keys.has(key)) missing.push(`${key} — ${rel}:${lineOf(text, index)}`);
      };
      for (const m of text.matchAll(SERVED_CALL_RE)) {
        if (m[1]) push(m[1], m.index!);
        for (const k of (m[2] ?? '').matchAll(KEY_IN_LIST)) push(k[1], m.index!);
      }
    }
    expect(
      missing,
      'clés consommées absentes du catalogue servi : le contrôle d’accès ne peut JAMAIS être accordé (bouton mort)',
    ).toEqual([]);
  });

  it('(A2) TOUTE clé des props `permissions=` de ProtectedRoute existe au catalogue', () => {
    // 185 props `permissions={[…]}` dans App.tsx (146 clés) : une faute de frappe
    // y ferme une ROUTE entière en silence. Vérifié explicitement car ces clés ne
    // passent par aucun appel de fonction (le ratchet (A) ne les voyait pas :
    // contrôle négatif effectué, le build restait vert).
    const missing: string[] = [];
    for (const { rel, text } of files) {
      for (const m of text.matchAll(/permissions=\{\[([^\]]*)\]\}/g)) {
        for (const k of m[1].matchAll(KEY_IN_LIST)) {
          if (!catalog.keys.has(k[1])) {
            missing.push(`${k[1]} — ${rel}:${lineOf(text, m.index!)}`);
          }
        }
      }
    }
    expect(missing, 'clé absente du catalogue dans une prop `permissions=` = route morte').toEqual([]);
  });

  it('(A3) les clés COMPOSÉES par `canAccess(resource, action)` sont vérifiables', () => {
    // `canAccess('FAMILIES','READ')` compose `${RESOURCE}_${ACTION}` : la clé
    // n'apparaît nulle part dans le code, donc (A) ne la voit pas — une faute de
    // frappe fermerait silencieusement l'accès. Règle : les deux premiers
    // arguments doivent être des littéraux (seule forme vérifiable contre le
    // catalogue) ET la clé composée doit exister.
    //
    // Exception : les deux GARDES GÉNÉRIQUES (`RequirePermission` /
    // `RequireTenantAccess`) transmettent `resource`/`action` en props — ce ne
    // sont pas des sites d'appel, ce sont des fabriques. Les voirlistés, et le
    // constat « aucun appelant en production » est tracé au handover §6.
    const GENERIC_GUARDS = ['src/components/guards/RouteGuards.tsx',
      'src/components/guards/RouteGuardsG54.tsx'];
    const problems: string[] = [];
    for (const { rel, text } of files) {
      for (const m of text.matchAll(/(?<![\w$])canAccess\(([^()]*)\)/g)) {
        if (GENERIC_GUARDS.includes(rel)) continue;
        const args = m[1].split(',').map((a) => a.trim());
        const [res, act] = args;
        const lit = (e?: string) => !!e && /^['"][A-Za-z][A-Za-z0-9_]*['"]$/.test(e);
        if (!lit(res) || !lit(act)) {
          problems.push(`${rel}:${lineOf(text, m.index!)} canAccess(${m[1].trim()}) — arguments non litteraux : cle non verifiable`);
          continue;
        }
        const key = `${res!.replace(/['"]/g, '').toUpperCase()}_${act!.replace(/['"]/g, '').toUpperCase()}`;
        if (!catalog.keys.has(key)) {
          problems.push(`${rel}:${lineOf(text, m.index!)} canAccess -> ${key} ABSENT du catalogue servi`);
        }
      }
    }
    expect(problems, 'canAccess : passer des litteraux, et verifier la cle composee').toEqual([]);
  });

  it('(A) le registre ROUTE_PERMISSIONS ne consomme que des clés servies', () => {
    const missing: string[] = [];
    for (const [path, keys] of Object.entries(ROUTE_PERMISSIONS)) {
      for (const k of keys) if (!catalog.keys.has(k)) missing.push(`${path} → ${k}`);
    }
    expect(missing, 'routes adossées à une clé inexistante = écran mort').toEqual([]);
  });
});

describe('ratchet parametrisation — parité de matrice (aucun élargissement silencieux)', () => {
  it('(B1) chaque route convertie : matrice servie ≡ rôles legacy de ROUTE_ROLES', () => {
    const drift: string[] = [];
    for (const [path, keys] of Object.entries(ROUTE_PERMISSIONS)) {
      const legacy = new Set(ROUTE_ROLES[path] ?? []);
      if (!legacy.size) continue;
      const served = servedRoles(catalog, keys);
      const extra = [...served].filter((r) => !legacy.has(r));
      const lost = [...legacy].filter((r) => !served.has(r));
      if (extra.length || lost.length) drift.push(`${path}: +${extra.join(',') || '-'} -${lost.join(',') || '-'}`);
    }
    expect(drift, 'matrice servie divergente du littéral legacy').toEqual([]);
  });

  it('(B2) chaque garde intra-page : matrice de la clé ≡ rôles du prédicat legacy', () => {
    const drift: string[] = [];
    for (const { rel, text } of files) {
      for (const m of text.matchAll(SERVED_CALL_RE)) {
        const keys = m[1] ? [m[1]] : [...(m[2] ?? '').matchAll(KEY_IN_LIST)].map((k) => k[1]);
        if (!keys.length) continue;
        const legacy = legacyRoles(legacyBody(text, m.index!));
        if (!legacy.size) continue; // sélecteur de contexte (cible d'une ligne) : hors périmètre
        const served = servedRoles(catalog, keys);
        const extra = [...served].filter((r) => !legacy.has(r));
        const lost = [...legacy].filter((r) => !served.has(r));
        if (extra.length || lost.length) {
          drift.push(`${rel}:${lineOf(text, m.index!)} ${keys.join('|')} +${extra.join(',') || '-'} -${lost.join(',') || '-'}`);
        }
      }
    }
    expect(drift, 'garde intra-page non paritaire (matrice servie ≠ littéral legacy)').toEqual([]);
  });
});

describe('ratchet parametrisation — résiduel de gardes à littéral de rôle', () => {
  /**
   * Sites de PRODUCTION où un littéral de rôle décide encore d'un affichage ou
   * d'une action. Chacun est soit un sélecteur de contexte assumé (le rôle de la
   * DONNÉE visée, pas la capacité de l'acteur), soit un reliquat documenté.
   *
   * ⚠ Liste par SITE, pas par fichier. Une version antérieure listait le
   * fichier (`App.tsx`, `ProfilePage.tsx`…) : n'importe quelle nouvelle garde
   * à littéral ajoutée dans un fichier déjà listé passait — exactement la fuite
   * qu'un ratchet doit fermer. Une entrée = `fichier::début du code normalisé`
   * (espaces compactés, coupée à 60 caractères) : stable malgré les insertions
   * de lignes, assez précis pour interdire l'ajout ailleurs dans le fichier.
   */
  const ALLOWED_RESIDUAL: Record<string, string> = {
    "src/pages/ProfilePage.tsx::{user?.role === 'ADMIN' ? 'Obligatoire":
      "libelle d'aide 2FA, pas une garde",
    "src/pages/ProfilePage.tsx::Statut: {user?.role === 'ADMIN'":
      "libelle du statut 2FA, pas une garde",
    "src/pages/TransferCreatePage.tsx::const faiseurs = useMemo":
      "filtre des cibles faiseur (donnee)",
    "src/pages/UsersPage.tsx::const isResponsable = user?.activeRole":
      "role de la FICHE observee, pas la capacite de l'acteur",
    "src/pages/UsersPage.tsx::{user.role === 'FAISEUR' && (":
      "actions proposees pour une cible FAISEUR (donnee)",
    "src/pages/UsersPage.tsx::{user.role !== 'FAISEUR' && user.role !== 'ADMIN'":
      "actions proposees pour une cible non-FAISEUR (donnee)",
  };

  const normalise = (line: string) => line.replace(/\s+/g, ' ').trim().slice(0, 60);

  /** Plafond CHIFFRÉ du résiduel toléré (constante committée, pas un budget par
   *  variable d'environnement : un ratchet que l'on peut relever au grattage
   *  n'est pas un ratchet). Chaque conversion fait BAISSER le compte. */
  const RESIDUAL_BASELINE = 6;

  /** Sites résiduels : décision d'affichage ou d'action portée par un littéral. */
  const residuals: string[] = [];
  for (const { rel, text } of files) {
    const lines = text.split('\n');
    lines.forEach((line, i) => {
      // garde servie-first : le littéral est le repli `legacy`, jamais une garde
      if (/\b(?:can|canAny|hasPermission)\(/.test(line) || /\blegacy\s*:/.test(line)) return;
      if (!ROLE_LITERAL.test(line)) return;
      const isDecision =
        /\b(?:const|let)\s+(?:can|has|is|show|allow)[A-Z]\w*\s*=/.test(line) ||
        /\{\s*(?:!{0,2})?[A-Za-z_$][\w.?[\]]*\s*(?:===|!==|==|!=)\s*['"][A-Z][A-Z0-9_]*['"]\s*(&&|\?|})/.test(line) ||
        /\?\s*[A-Za-z_$][\w.?[\]]*\s*(?:===|!==|==|!=)\s*['"][A-Z][A-Z0-9_]*['"]\s*:/.test(line) ||
        /\.(?:includes|hasRole|hasAnyRole|hasActiveRole)\(/.test(line);
      if (!isDecision) return;
      // suite d'une décision servie (ligne de continuation) : déjà couverte
      if (/^\s*(?:\|\||&&|\?|:)\s*/.test(line) && !/\b(?:const|let)\s+/.test(line)) return;
      residuals.push(`${rel}::${normalise(line)}`);
    });
  }

  const covered = (site: string) => Object.keys(ALLOWED_RESIDUAL).some((k) => site.startsWith(k));

  it('(C) aucune nouvelle garde à littéral de rôle hors liste blanche', () => {
    expect(
      residuals.filter((s) => !covered(s)),
      "nouvelles gardes a litteral de role : convertir en can(<CLE>, legacy) une eglise doit pouvoir les retirer/accorder",
    ).toEqual([]);
  });

  it('(C) la liste blanche ne doit pas périmer (chaque entrée correspond à un site réel)', () => {
    const stale = Object.keys(ALLOWED_RESIDUAL).filter(
      (entry) => !residuals.some((s) => s.startsWith(entry)),
    );
    expect(stale, "entrees de liste blanche obsoletes : convertir le site ou retirer l'entree").toEqual([]);
  });

  it('(C) le résiduel toléré ne peut pas croître', () => {
    expect(residuals.length).toBeLessThanOrEqual(RESIDUAL_BASELINE);
  });
});
