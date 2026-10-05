import { describe, it, expect } from 'vitest';
import { canRoleAccessPath, rolesForPath } from '@/lib/routeAccess';

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §9.9 — T-Q2.
 *
 * Les écrans « modulable » (niveaux, fiche nœud, assignations) sont scopés au
 * tenant. Un SUPER ADMIN plateforme ne doit PAS pouvoir les joindre : la
 * redirection V2 (T-W2) repose sur `rolesForPath` qui ne liste jamais le rôle
 * plateforme, donc `canRoleAccessPath` renvoie false pour un rôle inconnu de la
 * liste tenant.
 */
const V3_PATHS = [
  '/tenant/organization/levels',
  '/tenant/organization/nodes/123',
  '/tenant/members/456/roles',
];

describe('routeAccess — écrans V3 scopés tenant (T-W15)', () => {
  it.each(V3_PATHS)('restreint %s aux rôles tenant (ADMIN/PASTEUR)', (path) => {
    const roles = rolesForPath(path);
    expect(roles).not.toBeNull();
    expect(roles).toContain('ADMIN');
    expect(roles).toContain('PASTEUR');
    expect(roles).not.toContain('MEMBRE');
  });

  it.each(V3_PATHS)('un simple MEMBRE est refusé sur %s', (path) => {
    expect(canRoleAccessPath(path, 'MEMBRE')).toBe(false);
  });

  it.each(V3_PATHS)('ADMIN et PASTEUR sont admis sur %s', (path) => {
    expect(canRoleAccessPath(path, 'ADMIN')).toBe(true);
    expect(canRoleAccessPath(path, 'PASTEUR')).toBe(true);
  });
});
