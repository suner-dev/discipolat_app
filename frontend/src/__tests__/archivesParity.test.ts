import { describe, it, expect } from 'vitest';
import {
  ROUTE_PERMISSIONS,
  ROUTE_ROLES,
  canAccessPath,
  canRoleAccessPath,
} from '@/lib/routeAccess';
import { loadServedCatalog, servedRoles } from './helpers/servedCatalog';

/**
 * V272 — parité du parcours « Archives » : UI ≡ serveur, par la matrice SERVIE.
 *
 * Le fichier `TODO_ARCHIVES_V272.md` (§A §1, §D) décide que la consultation
 * des archives est accordée aux six détenteurs de la garde serveur
 * `EVENTS_EXECUTE_R6A2` (la classe `ChurchEventController`, qui sert aussi
 * `/archives`, `/archives/search`, `/archives/facets`, `/archives/{id}`).
 * L'UI web doit donc être exactement le même ensemble de rôles — ni plus
 * (bouton mort, refuse 403 côté serveur), ni moins (accès serveur disponible
 * mais route masquée). Ce test est le verrou de cette égalité, lu directement
 * dans les migrations Flyway (la vérité servie), pas dans un fixture recopié.
 *
 * Additif : rien ici ne peut retirer un accès — les assertions comparent des
 * ensembles ; si la matrice s'élargit des deux côtés à la fois, le test reste
 * vert ; si les deux côtés divergent (un seul élargi), il rouge et nomme
 * l'écart.
 */

const catalog = loadServedCatalog();

/** Ensemble attendu (§A §1 — parité UI_ARCHIVES_ACCESS ≡ EVENTS_EXECUTE_R6A2). */
const SIX_ROLES = [
  'ADMIN', 'CHEF_DE_FAMILLE', 'DEPARTMENT_LEADER', 'FAMILY_LEADER', 'PASTEUR', 'RESPONSABLE',
];

const asSet = (roles: Iterable<string>) => new Set([...roles]);

describe('parité Archives (V272) — UI_ARCHIVES_ACCESS ≡ EVENTS_EXECUTE_R6A2 ≡ route /archives', () => {
  it('les deux clés existent dans le catalogue servi', () => {
    expect(catalog.keys.has('UI_ARCHIVES_ACCESS')).toBe(true);
    expect(catalog.keys.has('EVENTS_EXECUTE_R6A2')).toBe(true);
  });

  it('la matrice servie de UI_ARCHIVES_ACCESS est exactement celle de EVENTS_EXECUTE_R6A2 (6 = 6)', () => {
    const ui = servedRoles(catalog, ['UI_ARCHIVES_ACCESS']);
    const serveur = servedRoles(catalog, ['EVENTS_EXECUTE_R6A2']);
    const versUi = [...serveur].filter((r) => !ui.has(r)).sort();
    const versServeur = [...ui].filter((r) => !serveur.has(r)).sort();
    expect(
      { dansServeurPasUI: versServeur, dansUIpPasServeur: versUi },
      'UI_ARCHIVES_ACCESS doit être servi aux mêmes rôles que la garde serveur /church-events',
    ).toEqual({ dansServeurPasUI: [], dansUIpPasServeur: [] });
    expect([...ui].sort()).toEqual(SIX_ROLES);
  });

  it('ROUTE_ROLES["/archives"] (repli legacy d\'App.tsx et de la nav) couvre les six rôles servis', () => {
    const rolesRoute = asSet(ROUTE_ROLES['/archives'] ?? []);
    const servis = servedRoles(catalog, ['UI_ARCHIVES_ACCESS']);
    // Toute route autorisée par rôle doit être un rôle réellement servi…
    for (const r of rolesRoute) expect(servis.has(r)).toBe(true);
    // …et tout rôle servi ne peut pas être bloqué par la route.
    for (const r of servis) expect(rolesRoute.has(r)).toBe(true);
    expect([...rolesRoute].sort()).toEqual(SIX_ROLES);
  });

  it('ROUTE_PERMISSIONS["/archives"] est convertie sur la clé UI dédiée', () => {
    expect(ROUTE_PERMISSIONS['/archives']).toEqual(['UI_ARCHIVES_ACCESS']);
  });

  it('repli par rôle : DEPARTMENT_LEADER/FAMILY_LEADER accèdent, MEMBRE non ; la clé servie primerait', () => {
    expect(canRoleAccessPath('/archives', 'DEPARTMENT_LEADER')).toBe(true);
    expect(canRoleAccessPath('/archives', 'FAMILY_LEADER')).toBe(true);
    expect(canRoleAccessPath('/archives', 'MEMBRE')).toBe(false);
    // « Servi d'abord » : un porteur de la clé passe, même sans rôle littéral.
    expect(canAccessPath('/archives', 'DEPARTMENT_LEADER', (k) => k === 'UI_ARCHIVES_ACCESS')).toBe(true);
    expect(canAccessPath('/archives', 'MEMBRE', () => false)).toBe(false);
  });
});
