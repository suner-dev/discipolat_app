import { describe, it, expect } from 'vitest';
import {
  ROUTE_PERMISSIONS,
  permissionsForPath,
  canAccessPath,
  canRoleAccessPath,
  filterNavByAccess,
} from '@/lib/routeAccess';

/**
 * PARAMÉTRISATION PAR ÉGLISE (Phase 2-client) — verrou d'équivalence du client.
 *
 * La tranche MEMBERS est gardée par les CLÉS de permissions SERVIES
 * (`GET /api/v1/me/permissions`, miroir exact de `@authz.can` côté serveur —
 * pont d'autorités actives et surcouche tenant inclus). Les règles prouvées ici
 * reproduisent le contrat du pont côté FE :
 * (1) route convertie + clé accordée → accès, même si le rôle littéral ne la
 *     liste pas (l'église a pu étendre : customisation = widening) ;
 * (2) route convertie + clé refusée → accès refusé, même si le rôle littéral
 *     la liste (l'église a pu restreindre : narrowing) ;
 * (3) sans ensemble servi (chargement, test hors TenantProvider) → repli
 *     strict sur le registre par rôle, identique à la transition ;
 * (4) route non convertie → le registre par rôle seul décide (additif).
 */

const QR = '/qr-checkin';
const REQUESTS = '/members/requests';

describe('routeAccess — accès par permissions servies (tranche MEMBERS)', () => {
  it('la tranche convertie est bien adressée par les clés du catalogue V258', () => {
    expect(permissionsForPath(REQUESTS)).toEqual(['MEMBERS_EXECUTE']);
    expect(permissionsForPath(QR)).toEqual(['MEMBERS_EXECUTE_QRCHECKIN']);
    expect(permissionsForPath('/geofencing')).toEqual(['MEMBERS_EXECUTE_R4A']);
    expect(permissionsForPath('/member-competences')).toEqual(['MEMBERS_EXECUTE']);
  });

  it('(1) widening par église : la clé servie accède même sans rôle littéral', () => {
    // Un FAISEUR à qui l'église a redonné MEMBERS_EXECUTE_QRCHECKIN (surcouche
    // tenant de la matrice) ouvre l'écran : rien n'est figé.
    const has = (k: string) => k === 'MEMBERS_EXECUTE_QRCHECKIN';
    expect(canAccessPath(QR, 'FAISEUR', has)).toBe(true);
  });

  it('(2) narrowing par église : la clé retirée refuse, même au rôle littéral admis', () => {
    // ROUTE_ROLES liste bien MEMBERS_EXECUTE à CHEF_DE_FAMILLE ; si l'ensemble
    // servi ne le contient plus, l'accès est refusé — le cache servi est la
    // source unique, le littéral ne sauve plus personne.
    expect(canRoleAccessPath(REQUESTS, 'CHEF_DE_FAMILLE')).toBe(true);
    const has = (_k: string) => false;
    expect(canAccessPath(REQUESTS, 'CHEF_DE_FAMILLE', has)).toBe(false);
  });

  it('(2 bis) liste de clés any-of : une seule clé accordée suffit', () => {
    const keys = ROUTE_PERMISSIONS['/geofencing'] as string[];
    const has = (k: string) => k === keys[0];
    expect(canAccessPath('/geofencing', 'MEMBRE', has)).toBe(true);
  });

  it('(3) repli transition : sans checker, comportement par rôle strictement identique', () => {
    for (const path of [QR, REQUESTS, '/geofencing', '/member-competences', '/souls', '/audit']) {
      for (const role of ['ADMIN', 'PASTEUR', 'MEMBRE', 'FAISEUR', 'INCONNU']) {
        expect(canAccessPath(path, role, undefined)).toBe(canRoleAccessPath(path, role));
      }
    }
  });

  it('(4) route non convertie : le checker servi ne change rien (additif)', () => {
    const has = (_k: string) => false;
    // /audit n'est pas dans ROUTE_PERMISSIONS → décision par rôle seulement.
    expect(canAccessPath('/audit', 'ADMIN', has)).toBe(true);
    expect(canAccessPath('/audit', 'MEMBRE', has)).toBe(false);
  });

  it('filterNavByAccess filtre les entrées converties par les clés servies', () => {
    const items = [
      { href: QR },
      { href: REQUESTS },
      { href: '/audit' },
    ];
    const has = (k: string) => k === 'MEMBERS_EXECUTE';
    const kept = filterNavByAccess(items, 'PASTEUR', has);
    // Pointage QR refusé (clé QR absente de l'ensemble servi), demandes admis,
    // /audit reste par rôle (ADMIN/PASTEUR).
    expect(kept.map((i) => i.href)).toEqual([REQUESTS, '/audit']);
  });
});
