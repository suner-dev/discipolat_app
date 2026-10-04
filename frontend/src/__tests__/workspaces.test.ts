import { describe, it, expect } from 'vitest';
import { isTenantAdmin, navForRole, PLATFORM_NAV } from '@/workspaces';

/* ============================================================================
 * Espaces métiers — chaque rôle ne voit que les menus dont les routes lui
 * sont réellement accessibles (croisement avec les gardes de App.tsx).
 *
 * T-W1 (SPEC_ORGANISATION_DENOMINATION_V2 §7.2, faille F1) : `navForRole`
 * reçoit désormais l'UTILISATEUR et non le seul rôle, pour que la garde
 * « un Super Admin plateforme n'obtient jamais les menus d'église » soit
 * structurelle. Les appels ci-dessous sont donc `{ role }`, et un cas
 * dedicated couvre le flag plateforme.
 * ========================================================================== */

const allHrefs = (role: string) =>
  navForRole({ role }).flatMap((s) => s.items.map((i) => i.href));

describe('isTenantAdmin — contrat du rôle actif', () => {
  it('accepte uniquement les rôles tenant autorisés par le backend', () => {
    expect(isTenantAdmin('TENANT_ADMIN')).toBe(true);
    expect(isTenantAdmin('TENANT_OWNER')).toBe(true);
    expect(isTenantAdmin('ADMIN')).toBe(true);
    expect(isTenantAdmin('PASTEUR')).toBe(true);
    expect(isTenantAdmin('TENANT_SUPER_ADMIN')).toBe(false);
  });
});

describe('navForRole — cohérence menus / gardes de routes', () => {
  // T-W1 (F1) : la garantie doit être STRUCTURELLE, pas dépendre du composant
  // appelant. Ces cas le prouvent en appelant directement navForRole, sans
  // passer par Sidebar — c'était précisément la faille : la correction
  // initiale vivait dans les composants.
  it('T-W1 (F1) : un Super Admin plateforme obtient PLATFORM_NAV, même avec le rôle ADMIN', () => {
    const nav = navForRole({ platformSuperAdmin: true, role: 'ADMIN', activeRole: 'ADMIN' });
    expect(nav).toBe(PLATFORM_NAV);
    const hrefs = nav.flatMap((s) => s.items.map((i) => i.href));
    expect(hrefs).toContain('/platform/dashboard');
    expect(hrefs).toContain('/platform/governance');
    // Aucun écran d'église, quel que soit le rôle legacy porté.
    for (const churchHref of ['/souls', '/families', '/departments', '/permissions', '/dashboard']) {
      expect(hrefs, `menu d'église inattendu : ${churchHref}`).not.toContain(churchHref);
    }
  });

  it('T-W1 (F1) : la garde plateforme prime sur activeRole=PASTEUR comme sur FAISEUR', () => {
    for (const role of ['PASTEUR', 'FAISEUR', 'CHEF_DE_FAMILLE', 'RESPONSABLE', 'MEMBRE']) {
      const hrefs = navForRole({ platformSuperAdmin: true, role, activeRole: role })
        .flatMap((s) => s.items.map((i) => i.href));
      expect(hrefs, `fuite pour activeRole=${role}`).not.toContain('/souls');
      expect(hrefs, `fuite pour activeRole=${role}`).toContain('/platform/dashboard');
    }
  });

  it('T-W1 (F1) : sans le flag, un ADMIN d\'église conserve sa vue complète', () => {
    const hrefs = navForRole({ role: 'ADMIN', activeRole: 'ADMIN' }).flatMap((s) => s.items.map((i) => i.href));
    expect(hrefs).toContain('/souls');
    expect(hrefs).toContain('/dashboard');
  });

  it('PASTEUR voit tous les écrans admin accessibles (configuration plateforme)', () => {
    const hrefs = allHrefs('PASTEUR');
    expect(hrefs).toContain('/permissions');
    expect(hrefs).toContain('/admin');
    expect(hrefs).toContain('/admin/settings');
    expect(hrefs).toContain('/admin/modules');
    expect(hrefs).toContain('/admin/menus');
    expect(hrefs).toContain('/admin/custom-fields');
    expect(hrefs).toContain('/admin/dictionaries');
    expect(hrefs).toContain('/admin/pages');
  });

  it('PASTEUR garde la vue complète + écrans admin', () => {
    const hrefs = allHrefs('PASTEUR');
    expect(hrefs).toContain('/dashboard');
    expect(hrefs).toContain('/souls');
    expect(hrefs).toContain('/users');
    expect(hrefs).toContain('/audit');
    expect(hrefs).toContain('/admin/transfers');
    expect(hrefs).toContain('/admin/notifications');
    expect(hrefs).toContain('/admin/system');
  });

  it('ADMIN voit tous les écrans, y compris la configuration', () => {
    const hrefs = allHrefs('ADMIN');
    expect(hrefs).toContain('/permissions');
    expect(hrefs).toContain('/admin');
    expect(hrefs).toContain('/admin/settings');
    expect(hrefs).toContain('/admin/custom-fields');
  });

  it('RESPONSABLE ne voit que les écrans de son espace (pas /crm/faiseur, pas /admin)', () => {
    const hrefs = allHrefs('RESPONSABLE');
    expect(hrefs).not.toContain('/crm/faiseur');
    expect(hrefs).not.toContain('/admin');
    expect(hrefs).not.toContain('/prayers');
    expect(hrefs).toContain('/dashboard/responsable');
    expect(hrefs).toContain('/departments');
    expect(hrefs).toContain('/users');
    expect(hrefs).toContain('/members/requests');
  });

  it('FAISEUR ne voit que son espace discipolat', () => {
    const hrefs = allHrefs('FAISEUR');
    expect(hrefs).toContain('/crm/faiseur');
    expect(hrefs).toContain('/souls');
    expect(hrefs).toContain('/reports/maker');
    expect(hrefs).not.toContain('/departments');
    expect(hrefs).not.toContain('/users');
    expect(hrefs).not.toContain('/families/compare');
  });

  it('CHEF_DE_FAMILLE voit sa famille mais pas les départements', () => {
    const hrefs = allHrefs('CHEF_DE_FAMILLE');
    expect(hrefs).toContain('/dashboard/chef-famille');
    expect(hrefs).toContain('/families');
    expect(hrefs).toContain('/reports/family');
    expect(hrefs).not.toContain('/departments');
    expect(hrefs).not.toContain('/crm/faiseur');
  });

  it('MEMBRE ne voit que son espace personnel', () => {
    const hrefs = allHrefs('MEMBRE');
    expect(hrefs).toContain('/dashboard/membre');
    expect(hrefs).toContain('/trainings');
    expect(hrefs).toContain('/badges');
    expect(hrefs).toContain('/events');
    expect(hrefs).not.toContain('/souls');
    expect(hrefs).not.toContain('/alerts');
    expect(hrefs).not.toContain('/departments');
  });
});
