import { describe, it, expect } from 'vitest';
import { navForRole } from '@/workspaces';

/* ============================================================================
 * Espaces métiers — chaque rôle ne voit que les menus dont les routes lui
 * sont réellement accessibles (croisement avec les gardes de App.tsx).
 * ========================================================================== */

const allHrefs = (role: string) =>
  navForRole(role).flatMap((s) => s.items.map((i) => i.href));

describe('navForRole — cohérence menus / gardes de routes', () => {
  it('PASTEUR ne voit aucun écran réservé à l’ADMIN (aucun bouton mort)', () => {
    const hrefs = allHrefs('PASTEUR');
    expect(hrefs).not.toContain('/permissions');
    expect(hrefs).not.toContain('/admin');
    expect(hrefs).not.toContain('/admin/settings');
    expect(hrefs).not.toContain('/admin/modules');
    expect(hrefs).not.toContain('/admin/menus');
    expect(hrefs).not.toContain('/admin/custom-fields');
  });

  it('PASTEUR garde la vue complète hors écrans admin', () => {
    const hrefs = allHrefs('PASTEUR');
    expect(hrefs).toContain('/dashboard');
    expect(hrefs).toContain('/souls');
    expect(hrefs).toContain('/users');
    expect(hrefs).toContain('/audit');
    expect(hrefs).toContain('/admin/transfers');
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
