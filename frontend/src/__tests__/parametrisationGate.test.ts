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

/** Rôles littéraux d'un prédicat `legacy:` — sert à prouver la parité (B2). */
function legacyRoles(body: string): Set<string> {
  const out = new Set<string>();
  for (const m of body.matchAll(/['"]([A-Z][A-Z0-9_]*)['"]/g)) out.add(m[1]);
  // on ne garde que les rôles : le prédicat ne contient que des littéraux de rôle
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
      for (const m of text.matchAll(/hasPermission\(\s*['"]([A-Z][A-Z0-9_]{2,})['"]/g)) push(m[1], m.index!);
      for (const m of text.matchAll(/\.(?:can|canAny)\(\s*['"]([A-Z][A-Z0-9_]{2,})['"]/g)) push(m[1], m.index!);
      for (const m of text.matchAll(/\.(?:can|canAny)\(\s*\[([^\]]*)\]/g)) {
        for (const k of m[1].matchAll(KEY_IN_LIST)) push(k[1], m.index!);
      }
    }
    expect(
      missing,
      'clés consommées absentes du catalogue servi : le contrôle d’accès ne peut JAMAIS être accordé (bouton mort)',
    ).toEqual([]);
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
      for (const m of text.matchAll(/\.(?:can|canAny)\(\s*(?:['"]([A-Z][A-Z0-9_]{2,})['"]|\[([^\]]*)\])/g)) {
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
   * Liste fermée : toute nouvelle garde à littéral fait échouer le build.
   * Clé = chemin du fichier (relatif à `frontend/`) — le motif est documenté.
   */
  const ALLOWED_RESIDUAL: Record<string, string> = {
    'src/App.tsx': 'repli `roles` de ProtectedRoute (gardes servies en tête) + bascule ADMIN/PASTEUR',
    'src/contexts/AuthContext.tsx': 'activeRole : source de session, pas une décision d’accès',
    'src/components/layout/Sidebar.tsx': 'libellé de badge + `enabled` de requête (perf, pas accès)',
    'src/pages/UsersPage.tsx': 'rôle de la FICHE observée (isResponsable)',
    "src/pages/TransferCreatePage.tsx": "filtre des cibles faiseur (donnee)",
    "src/pages/FamilyCreatePage.tsx": "portee par defaut du formulaire (donnee)",
    "src/pages/PastoralMinistryPage.tsx": "role propose par defaut dans le formulaire (donnee)",
    "src/pages/ObjectivesPage.tsx": "valeurs de filtre du formulaire (donnee)",
    'src/pages/MarketplacePage.tsx': '_isMine : prédicat inutilisé',
    'src/pages/EventsPage.tsx': 'sélecteur de rôle actif dans les filtres (donnée)',
    'src/lib/routeAccess.ts': 'helpers de repli legacy (canRoleAccessPath) : le chemin servi prime en amont',
    "src/pages/ProfilePage.tsx": "libelle daide 2FA (Obligatoire pour les administrateurs), pas une garde",
    "src/pages/UserRolesPage.tsx": "roles de la fiche observee (donnee), pas la capacite de lacteur",
  };

  /**
   * Plafond CHIFFRÉ du résiduel toléré (constante committée, et non un budget
   * par variable d'environnement : un ratchet que l'on peut relever au
   * grattage n'est pas un ratchet). Chaque conversion de garde fait BAISSER le compte :
   * on met alors la constante à jour — la dette ne peut pas croître en silence.
   * Mesuré après l'audit profond (DressCodePage, MemberDashboardPage,
   * ResponsableDashboardPage, ChefFamilleDashboardPage, MemberRequestsPage,
   * PeopleDirectoryPage, blocs DashboardPage, hardDelete mobile).
   */
  const RESIDUAL_BASELINE = 6;

  /** Sites résiduels : décision d'affichage ou d'action portée par un littéral de rôle. */
  const residuals: string[] = [];
  for (const { rel, text } of files) {
    const lines = text.split('\n');
    lines.forEach((line, i) => {
      // une garde servie-first : le littéral est le repli `legacy`, jamais une garde
      if (/\b(?:can|canAny|hasPermission)\(/.test(line) || /\blegacy\s*:/.test(line)) return;
      if (!ROLE_LITERAL.test(line)) return;
      // on ne retient que les décisions de garde (variable/JSX conditionnel)
      const isDecision =
        /\b(?:const|let)\s+(?:can|has|is|show|allow)[A-Z]\w*\s*=/.test(line) ||
        /\{\s*(?:!{0,2})?[A-Za-z_$][\w.?[\]]*\s*(?:===|!==|==|!=)\s*['"][A-Z][A-Z0-9_]*['"]\s*(&&|\?|})/.test(line) ||
        /\?\s*[A-Za-z_$][\w.?[\]]*\s*(?:===|!==|==|!=)\s*['"][A-Z][A-Z0-9_]*['"]\s*:/.test(line) ||
        /\.(?:includes|hasRole|hasAnyRole|hasActiveRole)\(/.test(line);
      if (!isDecision) return;
      // suite d'une décision servie (ligne de continuation) : déjà couverte
      if (/^\s*(?:\|\||&&|\?|:)\s*/.test(line) && !/\b(?:const|let)\s+/.test(line)) return;
      residuals.push(`${rel}:${i + 1}`);
    });
  }

  it('(C) aucune nouvelle garde à littéral de rôle hors liste blanche', () => {
    const uncovered = residuals.filter((r) => !(r.split(':')[0] in ALLOWED_RESIDUAL));
    expect(
      uncovered,
      'nouvelles gardes à littéral de rôle : convertir en `can(<CLÉ>, legacy)` — une église doit pouvoir les retirer/accorder',
    ).toEqual([]);
  });

  it('(C) la liste blanche ne doit pas périmer (fichiers toujours présents)', () => {
    const stale = Object.keys(ALLOWED_RESIDUAL).filter((f) => !statSync(join(process.cwd(), f)).isFile());
    expect(stale, 'entrées de liste blanche obsolètes : retirer ou déplacer la garde').toEqual([]);
  });

  it('(C) le résiduel compte-tenu des listes blanches est bien connu', () => {
    // Trace de l'audit : fige le nombre de sites résiduels tolérés (ratchet
    // chiffré). Toute conversion fait baisser le compte : on régénère alors
    // cette constante au lieu de laisser la dette croître en silence.
    const tolerated = residuals.filter((r) => r.split(':')[0] in ALLOWED_RESIDUAL).length;
    expect(tolerated).toBeLessThanOrEqual(RESIDUAL_BASELINE);
  });
});
