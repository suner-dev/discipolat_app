import { describe, it, expect } from 'vitest';
import {
  ROUTE_PERMISSIONS,
  ROUTE_ROLES,
  permissionsForPath,
  canAccessPath,
  canRoleAccessPath,
  filterNavByAccess,
} from '@/lib/routeAccess';

/**
 * Audit Dress Code / permissions (§G3.4) — verrou d'accès à la page
 * d'administration des tenues de service.
 *
 * La page `/admin/dress-codes` a été réintroduite dans le produit : elle doit
 * rester fermée aux rôles non administrateurs, et ouverte à l'ADMIN — que ce
 * soit par la clé servie (`ADMIN_EXECUTE`) ou par le registre littéral.
 */
const DRESS_CODES = '/admin/dress-codes';

describe('routeAccess — page /admin/dress-codes', () => {
  it('est convertie en permissions servies (ADMIN_EXECUTE)', () => {
    expect(permissionsForPath(DRESS_CODES)).toEqual(['ADMIN_EXECUTE']);
    expect(ROUTE_PERMISSIONS[DRESS_CODES]).toEqual(['ADMIN_EXECUTE']);
  });

  it('est fermée au rôle MEMBRE par le registre littéral', () => {
    expect(ROUTE_ROLES[DRESS_CODES]).toBeDefined();
    expect(canRoleAccessPath(DRESS_CODES, 'MEMBRE')).toBe(false);
    expect(canRoleAccessPath(DRESS_CODES, 'RESPONSABLE')).toBe(false);
    expect(canRoleAccessPath(DRESS_CODES, 'CHEF_DE_FAMILLE')).toBe(false);
    expect(canRoleAccessPath(DRESS_CODES, 'FAISEUR')).toBe(false);
  });

  it('est ouverte à l\'ADMIN', () => {
    expect(canRoleAccessPath(DRESS_CODES, 'ADMIN')).toBe(true);
  });

  it('respecte la clé servie quand le contexte est chargé (narrowing)', () => {
    // L'église a retiré ADMIN_EXECUTE à l'ADMIN : l'UI doit se refermer
    // malgré le registre littéral qui l'ouvre.
    const has = (_k: string) => false;
    expect(canAccessPath(DRESS_CODES, 'ADMIN', has)).toBe(false);
  });

  it('respecte la clé servie quand le contexte est chargé (widening)', () => {
    // L'église a accordé ADMIN_EXECUTE à un PASTEUR : l'UI doit s'ouvrir.
    const has = (k: string) => k === 'ADMIN_EXECUTE';
    expect(canAccessPath(DRESS_CODES, 'PASTEUR', has)).toBe(true);
  });

  it('repli strict sur le registre littéral sans ensemble servi', () => {
    expect(canAccessPath(DRESS_CODES, 'ADMIN')).toBe(true);
    expect(canAccessPath(DRESS_CODES, 'MEMBRE')).toBe(false);
  });

  it('filtre un menu latéral contenant le lien vers les tenues', () => {
    const items = [
      { href: '/admin/dashboard', label: 'Dashboard' },
      { href: DRESS_CODES, label: 'Tenues de service' },
      { href: '/dashboard', label: 'Accueil' },
    ];

    const visibleAsAdmin = filterNavByAccess(items, 'ADMIN');
    expect(visibleAsAdmin.map((i) => i.href)).toContain(DRESS_CODES);

    // Un MEMBRE ne voit ni la page d'administration, ni le lien vers les
    // tenues ; mais la route non restreinte reste visible.
    const visibleAsMember = filterNavByAccess(items, 'MEMBRE');
    expect(visibleAsMember.map((i) => i.href)).not.toContain(DRESS_CODES);
    expect(visibleAsMember.map((i) => i.href)).not.toContain('/admin/dashboard');
    expect(visibleAsMember.map((i) => i.href)).toContain('/dashboard');
  });
});
